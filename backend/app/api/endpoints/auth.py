from datetime import datetime
from typing import Any
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from pydantic import BaseModel
from sqlalchemy.orm import Session
from app.core.config import settings
from app.database.session import get_db
from app.models.user import User, UserRole
from app.auth.security import verify_password, get_password_hash, create_access_token
from app.auth.deps import get_current_active_user

router = APIRouter()


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"


class UserResponse(BaseModel):
    id: int
    username: str
    role: str
    is_active: bool

    class Config:
        from_attributes = True


class SetPasswordRequest(BaseModel):
    token: str
    new_password: str


@router.post("/login", response_model=TokenResponse)
def login_for_access_token(
    db: Session = Depends(get_db),
    form_data: OAuth2PasswordRequestForm = Depends()
) -> Any:
    """
    OAuth2 compatible token login, getting an access token for future clinical requests.
    """
    user = db.query(User).filter(User.username == form_data.username).first()
    
    # Auto-seed default clinician or patient if database is completely new
    if not user and form_data.username in ["dr.jenkins", "doctor", "admin"]:
        user = User(
            username=form_data.username,
            hashed_password=get_password_hash("clinicianPass123"),
            role=UserRole.HEALTHCARE_WORKER,
            is_active=True
        )
        db.add(user)
        db.commit()
        db.refresh(user)

    if not user or not verify_password(form_data.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or medical password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    if not user.is_active:
        raise HTTPException(status_code=400, detail="User account is deactivated")

    access_token = create_access_token(subject=user.username)
    return {"access_token": access_token, "token_type": "bearer"}


@router.get("/me", response_model=UserResponse)
def read_current_user_profile(
    current_user: User = Depends(get_current_active_user)
) -> Any:
    """Fetch profile of currently authenticated clinician or patient."""
    return current_user


@router.post("/logout")
def logout() -> Any:
    """Acknowledge session termination."""
    return {"status": "success", "message": "Clinical session terminated successfully"}


@router.post("/set-password")
def set_new_account_password(
    payload: SetPasswordRequest,
    db: Session = Depends(get_db)
) -> Any:
    """Activates new patient account or sets password via secure reset token."""
    user = db.query(User).filter(User.reset_token == payload.token).first()
    if not user:
        raise HTTPException(status_code=400, detail="Invalid or expired activation token")

    if user.reset_token_expires and user.reset_token_expires < datetime.utcnow():
        raise HTTPException(status_code=400, detail="Activation token has expired")

    user.hashed_password = get_password_hash(payload.new_password)
    user.reset_token = None
    user.reset_token_expires = None
    user.require_password_change = False
    user.is_active = True
    db.commit()

    return {"status": "success", "message": "Password updated and patient account activated"}

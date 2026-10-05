import secrets
from datetime import datetime, timedelta
from typing import List, Optional, Any
from io import BytesIO
import openpyxl
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from fastapi import APIRouter, Depends, HTTPException, status, Response
from pydantic import BaseModel, EmailStr
from sqlalchemy.orm import Session
from app.core.config import settings
from app.database.session import get_db
from app.models.user import User, UserRole
from app.models.patient import Patient
from app.auth.deps import require_role
from app.auth.security import generate_reset_token, get_password_hash
from app.notifications.email import send_welcome_activation_email
from app.rag.store import vector_store

router = APIRouter()


class PatientBase(BaseModel):
    first_name: str
    last_name: str
    email: str
    phone: Optional[str] = None


class PatientCreate(PatientBase):
    pass


class PatientResponse(PatientBase):
    id: int
    patient_access_id: str
    created_at: Optional[datetime] = None

    class Config:
        from_attributes = True


class PatientCreateResponse(PatientResponse):
    username: str
    set_password_link: str


@router.get("/", response_model=List[PatientResponse])
def get_all_patients(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role([UserRole.HEALTHCARE_WORKER, UserRole.ADMIN, UserRole.SPECIALIST]))
) -> Any:
    """Retrieve full clinical patient registry."""
    patients = db.query(Patient).order_by(Patient.id.desc()).all()
    return patients


@router.post("/", response_model=PatientCreateResponse)
def register_new_patient(
    payload: PatientCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role([UserRole.HEALTHCARE_WORKER, UserRole.ADMIN]))
) -> Any:
    """
    Registers a new clinical patient:
    1. Generates unique patient_access_id (e.g. MV-PAT-XXXXXX)
    2. Creates unactivated User account with 48h reset token
    3. Triggers welcome email with activation link (defensive fallback)
    4. Returns patient and activation link
    """
    clean_first = payload.first_name.strip()
    clean_last = payload.last_name.strip()
    
    # Check if patient email already exists
    existing = db.query(Patient).filter(Patient.email == payload.email).first()
    if existing:
        raise HTTPException(status_code=400, detail="A patient with this email address is already registered")

    rand_suffix = secrets.token_hex(2).lower()
    username = f"{clean_first.lower()}.{clean_last.lower()}_{rand_suffix}"
    access_id = f"MV-PAT-{secrets.token_hex(3).upper()}"
    
    reset_token = generate_reset_token()
    token_expires = datetime.utcnow() + timedelta(hours=48)

    # Create associated user account
    patient_user = User(
        username=username,
        hashed_password=None,
        role=UserRole.PATIENT,
        is_active=True,
        reset_token=reset_token,
        reset_token_expires=token_expires,
        require_password_change=True
    )
    db.add(patient_user)
    db.commit()
    db.refresh(patient_user)

    # Create Patient record
    new_patient = Patient(
        user_id=patient_user.id,
        patient_access_id=access_id,
        first_name=clean_first,
        last_name=clean_last,
        email=payload.email,
        phone=payload.phone
    )
    db.add(new_patient)
    db.commit()
    db.refresh(new_patient)

    activation_link = f"{settings.BASE_URL}/auth/set-password?token={reset_token}"

    # Dispatch welcome activation email (failsafe)
    send_welcome_activation_email(
        to_email=payload.email,
        first_name=clean_first,
        patient_access_id=access_id,
        username=username,
        set_password_link=activation_link
    )

    return {
        "id": new_patient.id,
        "patient_access_id": new_patient.patient_access_id,
        "first_name": new_patient.first_name,
        "last_name": new_patient.last_name,
        "email": new_patient.email,
        "phone": new_patient.phone,
        "created_at": new_patient.created_at,
        "username": username,
        "set_password_link": activation_link
    }


@router.delete("/{patient_id}")
def delete_patient_record(
    patient_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role([UserRole.HEALTHCARE_WORKER, UserRole.ADMIN]))
) -> Any:
    """
    Cascading removal of patient, associated user, screenings,
    PDF reports, and ChromaDB embeddings.
    """
    patient = db.query(Patient).filter(Patient.id == patient_id).first()
    if not patient:
        raise HTTPException(status_code=404, detail="Patient record not found")

    user = patient.user

    # Remove embeddings from vector store
    vector_store.delete_patient_records(patient_id)

    db.delete(patient)
    if user:
        db.delete(user)
    db.commit()

    return {"status": "success", "message": f"Patient #{patient_id} and all related records deleted"}


@router.get("/export/excel")
def export_patients_to_excel(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role([UserRole.HEALTHCARE_WORKER, UserRole.ADMIN, UserRole.SPECIALIST]))
) -> Any:
    """Exports clinical registry and diagnostic screening history to formatted Excel."""
    patients = db.query(Patient).order_by(Patient.id.asc()).all()

    wb = openpyxl.Workbook()
    ws = wb.active
    ws.title = "Patient Registry"

    # Header styling
    header_font = Font(name="Arial", size=11, bold=True, color="FFFFFF")
    header_fill = PatternFill(start_color="0F766E", end_color="0F766E", fill_type="solid")
    border = Border(
        left=Side(style="thin", color="CBD5E1"),
        right=Side(style="thin", color="CBD5E1"),
        top=Side(style="thin", color="CBD5E1"),
        bottom=Side(style="thin", color="CBD5E1")
    )

    headers = [
        "Patient ID", "Access ID", "First Name", "Last Name",
        "Email", "Phone", "Registration Date", "Total Screenings", "Latest Diagnosis"
    ]
    ws.append(headers)

    for col_idx in range(1, len(headers) + 1):
        cell = ws.cell(row=1, column=col_idx)
        cell.font = header_font
        cell.fill = header_fill
        cell.alignment = Alignment(horizontal="center", vertical="center")

    for p in patients:
        latest_screening = p.screenings[0] if p.screenings else None
        latest_diag = latest_screening.prediction if latest_screening else "None"
        row = [
            p.id,
            p.patient_access_id,
            p.first_name,
            p.last_name,
            p.email,
            p.phone or "N/A",
            p.created_at.strftime("%Y-%m-%d %H:%M"),
            len(p.screenings),
            latest_diag
        ]
        ws.append(row)

    # Adjust column widths
    for col in ws.columns:
        max_len = max(len(str(cell.value or "")) for cell in col)
        col_letter = openpyxl.utils.get_column_letter(col[0].column)
        ws.column_dimensions[col_letter].width = max(max_len + 4, 12)

    # Save to memory buffer
    stream = BytesIO()
    wb.save(stream)
    stream.seek(0)

    filename = f"MedVisionAI_Registry_{datetime.utcnow().strftime('%Y%m%d')}.xlsx"
    return Response(
        content=stream.getvalue(),
        media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        headers={"Content-Disposition": f"attachment; filename={filename}"}
    )

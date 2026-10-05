import enum
from datetime import datetime
from typing import Optional
from sqlalchemy import Column, Integer, String, Boolean, DateTime, Enum
from sqlalchemy.orm import relationship
from app.models.base import Base


class UserRole(str, enum.Enum):
    HEALTHCARE_WORKER = "HEALTHCARE_WORKER"
    PATIENT = "PATIENT"
    SPECIALIST = "SPECIALIST"
    ADMIN = "ADMIN"


class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String(100), unique=True, index=True, nullable=False)
    hashed_password = Column(String(255), nullable=True)
    role = Column(Enum(UserRole), default=UserRole.HEALTHCARE_WORKER, nullable=False)
    is_active = Column(Boolean, default=True, nullable=False)
    reset_token = Column(String(255), nullable=True, index=True)
    reset_token_expires = Column(DateTime, nullable=True)
    require_password_change = Column(Boolean, default=False, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)

    # One-to-one relationship with Patient
    patient = relationship("Patient", back_populates="user", uselist=False, cascade="all, delete-orphan")
    
    # Screenings conducted if user is a healthcare worker
    conducted_screenings = relationship("Screening", back_populates="healthcare_worker", cascade="all, delete-orphan")

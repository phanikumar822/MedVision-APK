from datetime import datetime
from sqlalchemy import Column, Integer, String, Float, Text, DateTime, ForeignKey
from sqlalchemy.orm import relationship
from app.models.base import Base


class Screening(Base):
    __tablename__ = "screenings"

    id = Column(Integer, primary_key=True, index=True)
    screening_id = Column(String(50), unique=True, index=True, nullable=False)
    patient_id = Column(Integer, ForeignKey("patients.id", ondelete="CASCADE"), nullable=False)
    healthcare_worker_id = Column(Integer, ForeignKey("users.id", ondelete="SET NULL"), nullable=True)
    
    image_path = Column(String(500), nullable=False)
    heatmap_path = Column(String(500), nullable=False)
    
    prediction = Column(String(50), nullable=False)  # "DR PRESENT" or "NO DR"
    probability_dr = Column(Float, nullable=False)
    probability_no_dr = Column(Float, nullable=False)
    confidence = Column(Float, nullable=False)
    risk_level = Column(String(50), nullable=False)  # "LOW", "MODERATE", "HIGH"
    recommendation = Column(Text, nullable=False)
    ai_context = Column(Text, nullable=False)
    
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)

    # Relationships
    patient = relationship("Patient", back_populates="screenings")
    healthcare_worker = relationship("User", back_populates="conducted_screenings")
    report = relationship("Report", back_populates="screening", uselist=False, cascade="all, delete-orphan")

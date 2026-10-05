from datetime import datetime
from sqlalchemy import Column, Integer, String, Boolean, DateTime, ForeignKey
from sqlalchemy.orm import relationship
from app.models.base import Base


class Report(Base):
    __tablename__ = "reports"

    id = Column(Integer, primary_key=True, index=True)
    screening_id = Column(Integer, ForeignKey("screenings.id", ondelete="CASCADE"), unique=True, nullable=False)
    pdf_path = Column(String(500), nullable=False)
    is_published = Column(Boolean, default=False, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)

    # Relationships
    screening = relationship("Screening", back_populates="report")

import os
from pathlib import Path
from typing import List, Any
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.responses import FileResponse
from pydantic import BaseModel
from sqlalchemy.orm import Session
from app.core.config import settings
from app.database.session import get_db
from app.models.user import User, UserRole
from app.models.patient import Patient
from app.models.screening import Screening
from app.models.report import Report
from app.auth.deps import require_role, get_current_active_user
from app.reports.pdf_generator import generate_medical_pdf_report
from app.notifications.email import send_report_ready_email

router = APIRouter()


class ReportResponse(BaseModel):
    id: int
    screening_id: str
    patient_name: str
    prediction: str
    risk_level: str
    is_published: bool
    download_url: str

    class Config:
        from_attributes = True


@router.post("/{screening_id}/generate")
def compile_screening_report(
    screening_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role([UserRole.HEALTHCARE_WORKER, UserRole.ADMIN]))
) -> Any:
    """Compiles a signed clinical PDF report using ReportLab."""
    screening = db.query(Screening).filter(Screening.screening_id == screening_id).first()
    if not screening:
        raise HTTPException(status_code=404, detail="Screening record not found")

    patient = screening.patient

    # Generate PDF
    pdf_path = generate_medical_pdf_report(
        screening=screening,
        patient=patient,
        doctor_name=f"Dr. {current_user.username}"
    )

    # Save or update Report record
    report = db.query(Report).filter(Report.screening_id == screening.id).first()
    if not report:
        report = Report(
            screening_id=screening.id,
            pdf_path=pdf_path,
            is_published=False
        )
        db.add(report)
    else:
        report.pdf_path = pdf_path
    
    db.commit()
    db.refresh(report)

    return {
        "status": "success",
        "report_id": report.id,
        "screening_id": screening.screening_id,
        "pdf_path": str(pdf_path),
        "download_url": f"{settings.BASE_URL}/reports/{report.id}/download"
    }


@router.post("/{report_id}/publish")
def publish_clinical_report(
    report_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role([UserRole.HEALTHCARE_WORKER, UserRole.ADMIN]))
) -> Any:
    """Publishes report to patient portal and dispatches email notification."""
    report = db.query(Report).filter(Report.id == report_id).first()
    if not report:
        raise HTTPException(status_code=404, detail="Report not found")

    report.is_published = True
    db.commit()

    patient = report.screening.patient
    if patient and patient.email:
        send_report_ready_email(
            to_email=patient.email,
            first_name=patient.first_name,
            screening_id=report.screening.screening_id,
            prediction=report.screening.prediction,
            risk_level=report.screening.risk_level
        )

    return {
        "status": "success",
        "message": "Report officially published to Patient Portal and notification dispatched",
        "report_id": report.id
    }


@router.get("/{report_id}/download")
def download_pdf_report(
    report_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_active_user)
) -> Any:
    """Stream binary PDF report to client."""
    report = db.query(Report).filter(Report.id == report_id).first()
    if not report:
        raise HTTPException(status_code=404, detail="Report not found")

    # If patient, ensure report is published and belongs to them
    if current_user.role == UserRole.PATIENT:
        if not report.is_published:
            raise HTTPException(status_code=403, detail="Report is pending clinician signature")
        if not current_user.patient or report.screening.patient_id != current_user.patient.id:
            raise HTTPException(status_code=403, detail="Access to another patient's report is forbidden")

    if not os.path.exists(report.pdf_path):
        raise HTTPException(status_code=404, detail="PDF report file is missing on storage")

    filename = Path(report.pdf_path).name
    return FileResponse(
        path=report.pdf_path,
        media_type="application/pdf",
        filename=filename
    )


@router.get("/my-reports")
def get_patient_reports(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_active_user)
) -> Any:
    """Retrieve published reports for the authenticated patient."""
    patient = current_user.patient
    if not patient:
        # If user is a clinician previewing patient mode, pick the first registered patient
        patient = db.query(Patient).first()
        if not patient:
            return []

    screenings = db.query(Screening).filter(Screening.patient_id == patient.id).order_by(Screening.created_at.desc()).all()
    results = []
    for s in screenings:
        # Include screening summary
        pdf_url = ""
        is_pub = False
        if s.report:
            is_pub = s.report.is_published
            pdf_url = f"{settings.BASE_URL}/reports/{s.report.id}/download"

        results.append({
            "id": s.id,
            "screening_id": s.screening_id,
            "patient_name": patient.full_name,
            "prediction": s.prediction,
            "risk_level": s.risk_level,
            "confidence": s.confidence,
            "probability_dr": s.probability_dr,
            "date": s.created_at.strftime("%Y-%m-%d"),
            "is_published": is_pub,
            "summary": s.recommendation,
            "download_url": pdf_url
        })

    return results

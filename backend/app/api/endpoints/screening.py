import secrets
import shutil
from datetime import datetime, date
from pathlib import Path
from typing import Any, Dict, Optional
from fastapi import APIRouter, Depends, HTTPException, UploadFile, File, Query, status
from pydantic import BaseModel
from sqlalchemy.orm import Session
from app.core.config import settings
from app.database.session import get_db
from app.models.user import User, UserRole
from app.models.patient import Patient
from app.models.screening import Screening
from app.auth.deps import require_role, get_current_active_user
from app.services.inference import inference_engine
from app.rag.store import vector_store

router = APIRouter()


class ScreeningStatsResponse(BaseModel):
    total_screenings: int
    screenings_today: int
    dr_present_count: int
    no_dr_count: int


class ScreeningResponse(BaseModel):
    id: int
    screening_id: str
    patient_id: int
    image_url: str
    heatmap_url: str
    prediction: str
    probability_dr: float
    probability_no_dr: float
    confidence: float
    risk_level: str
    recommendation: str
    ai_context: str
    created_at: datetime

    class Config:
        from_attributes = True


@router.get("/stats", response_model=ScreeningStatsResponse)
def get_screening_statistics(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role([UserRole.HEALTHCARE_WORKER, UserRole.ADMIN, UserRole.SPECIALIST]))
) -> Any:
    """Computes real-time clinic screening metrics."""
    total = db.query(Screening).count()
    dr_count = db.query(Screening).filter(Screening.prediction == "DR PRESENT").count()
    no_dr_count = db.query(Screening).filter(Screening.prediction == "NO DR").count()

    today_start = datetime.combine(date.today(), datetime.min.time())
    today_count = db.query(Screening).filter(Screening.created_at >= today_start).count()

    return {
        "total_screenings": total,
        "screenings_today": today_count,
        "dr_present_count": dr_count,
        "no_dr_count": no_dr_count
    }


@router.post("/", response_model=ScreeningResponse)
async def perform_screening_inference(
    patient_id: int = Query(..., description="ID of registered patient"),
    file: UploadFile = File(..., description="Retinal fundus scan (JPEG/PNG)"),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_role([UserRole.HEALTHCARE_WORKER, UserRole.ADMIN]))
) -> Any:
    """
    Primary AI Screening Endpoint:
    1. Validates patient existence and uploaded image format
    2. Stores raw fundus photo in uploads/fundus/
    3. Runs PyTorch EfficientNet-B0 inference & Grad-CAM layer analysis
    4. Records clinical screening in database and indexes in ChromaDB
    5. Returns full diagnostic result
    """
    patient = db.query(Patient).filter(Patient.id == patient_id).first()
    if not patient:
        raise HTTPException(status_code=404, detail=f"Patient #{patient_id} does not exist in registry")

    # Validate image mime type
    if not file.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="Uploaded file must be an image (JPEG/PNG)")

    screening_id = f"MV-{secrets.token_hex(4).upper()}"
    file_ext = Path(file.filename).suffix or ".jpg"
    fundus_filename = f"{screening_id}_raw{file_ext}"
    fundus_path = settings.FUNDUS_DIR / fundus_filename

    # Save uploaded fundus image
    try:
        with open(fundus_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to store fundus image: {str(e)}")

    # Execute deep learning inference + Grad-CAM
    try:
        result = inference_engine.analyze_fundus_scan(
            image_path=fundus_path,
            screening_id=screening_id
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"AI Inference pipeline error: {str(e)}")

    # Persist Screening Entity
    screening = Screening(
        screening_id=screening_id,
        patient_id=patient.id,
        healthcare_worker_id=current_user.id,
        image_path=str(fundus_path),
        heatmap_path=result["heatmap_path"],
        prediction=result["prediction"],
        probability_dr=result["probability_dr"],
        probability_no_dr=result["probability_no_dr"],
        confidence=result["confidence"],
        risk_level=result["risk_level"],
        recommendation=result["recommendation"],
        ai_context=result["ai_context"]
    )
    db.add(screening)
    db.commit()
    db.refresh(screening)

    # Index into ChromaDB vector store
    vector_store.index_screening_report(
        patient_id=patient.id,
        screening_id=screening_id,
        patient_name=patient.full_name,
        prediction=result["prediction"],
        confidence=result["confidence"],
        risk_level=result["risk_level"],
        recommendation=result["recommendation"],
        ai_context=result["ai_context"]
    )

    return {
        "id": screening.id,
        "screening_id": screening.screening_id,
        "patient_id": screening.patient_id,
        "image_url": f"{settings.BASE_URL}/static/fundus/{fundus_filename}",
        "heatmap_url": f"{settings.BASE_URL}/static/heatmaps/{result['heatmap_filename']}",
        "prediction": screening.prediction,
        "probability_dr": screening.probability_dr,
        "probability_no_dr": screening.probability_no_dr,
        "confidence": screening.confidence,
        "risk_level": screening.risk_level,
        "recommendation": screening.recommendation,
        "ai_context": screening.ai_context,
        "created_at": screening.created_at
    }


@router.get("/{screening_id}", response_model=ScreeningResponse)
def get_screening_by_id(
    screening_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_active_user)
) -> Any:
    """Retrieve full screening and Grad-CAM telemetry by clinical ID."""
    screening = db.query(Screening).filter(Screening.screening_id == screening_id).first()
    if not screening:
        raise HTTPException(status_code=404, detail="Screening record not found")

    fundus_filename = Path(screening.image_path).name
    heatmap_filename = Path(screening.heatmap_path).name

    return {
        "id": screening.id,
        "screening_id": screening.screening_id,
        "patient_id": screening.patient_id,
        "image_url": f"{settings.BASE_URL}/static/fundus/{fundus_filename}",
        "heatmap_url": f"{settings.BASE_URL}/static/heatmaps/{heatmap_filename}",
        "prediction": screening.prediction,
        "probability_dr": screening.probability_dr,
        "probability_no_dr": screening.probability_no_dr,
        "confidence": screening.confidence,
        "risk_level": screening.risk_level,
        "recommendation": screening.recommendation,
        "ai_context": screening.ai_context,
        "created_at": screening.created_at
    }

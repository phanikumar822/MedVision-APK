import os
import logging
from pathlib import Path
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import JSONResponse

from app.core.config import settings
from app.database.session import engine, SessionLocal
from app.models.base import Base
from app.models.user import User, UserRole
from app.models.patient import Patient
from app.models.screening import Screening
from app.models.report import Report
from app.auth.security import get_password_hash
from app.api.api import api_router
from app.rag.store import vector_store
from app.reports.pdf_generator import generate_medical_pdf_report

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("medvision.backend")

# Initialize database schema
Base.metadata.create_all(bind=engine)

app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    description="Production-grade clinical backend for MedVisionAI Diabetic Retinopathy screening with PyTorch EfficientNet-B0 and Grad-CAM explainability.",
    openapi_url="/openapi.json",
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.BACKEND_CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Static file mounts for fundus photography, Grad-CAM heatmaps, and generated reports
settings.init_directories()

app.mount("/static/fundus", StaticFiles(directory=str(settings.FUNDUS_DIR)), name="fundus")
app.mount("/static/heatmaps", StaticFiles(directory=str(settings.HEATMAP_DIR)), name="heatmaps")
app.mount("/static/reports", StaticFiles(directory=str(settings.REPORT_DIR)), name="reports")

# Include all API routers
app.include_router(api_router, prefix=settings.API_V1_STR)


@app.on_event("startup")
def startup_event():
    """Initializes runtime directories, database seed, and model weights."""
    logger.info("Initializing MedVisionAI clinical services...")
    settings.init_directories()
    
    db = SessionLocal()
    try:
        # Seed clinician user if not exists
        clinician = db.query(User).filter(User.username == "dr.jenkins").first()
        if not clinician:
            logger.info("Seeding default clinician account: dr.jenkins")
            clinician = User(
                username="dr.jenkins",
                hashed_password=get_password_hash("clinicianPass123"),
                role=UserRole.HEALTHCARE_WORKER,
                is_active=True
            )
            db.add(clinician)
            db.commit()
            db.refresh(clinician)

        # Seed demo patient Eleanor Vance if not exists
        demo_patient = db.query(Patient).filter(Patient.email == "eleanor.vance@example.com").first()
        if not demo_patient:
            logger.info("Seeding demo patient record: Eleanor Vance (#PT-2026-0814)")
            patient_user = User(
                username="eleanor.vance",
                hashed_password=get_password_hash("patientPass123"),
                role=UserRole.PATIENT,
                is_active=True,
                require_password_change=False
            )
            db.add(patient_user)
            db.commit()
            db.refresh(patient_user)

            demo_patient = Patient(
                user_id=patient_user.id,
                patient_access_id="MV-PAT-2026-0814",
                first_name="Eleanor",
                last_name="Vance",
                email="eleanor.vance@example.com",
                phone="+1 (555) 438-9021"
            )
            db.add(demo_patient)
            db.commit()
            db.refresh(demo_patient)

            # Seed sample fundus scan placeholder files if they don't exist
            sample_fundus = settings.FUNDUS_DIR / "MV-8A4F12C9_raw.jpg"
            sample_heatmap = settings.HEATMAP_DIR / "MV-8A4F12C9_gradcam.jpg"

            # Create standard placeholder retina images if absent
            import cv2
            import numpy as np
            if not sample_fundus.exists():
                img = np.zeros((400, 400, 3), dtype=np.uint8)
                cv2.circle(img, (200, 200), 180, (15, 23, 140), -1)  # Dark retinal red
                cv2.circle(img, (140, 200), 30, (30, 160, 240), -1)  # Optic disc
                cv2.imwrite(str(sample_fundus), img)

            if not sample_heatmap.exists():
                img = np.zeros((400, 400, 3), dtype=np.uint8)
                cv2.circle(img, (200, 200), 180, (15, 23, 140), -1)
                cv2.circle(img, (220, 190), 40, (0, 0, 255), -1)    # DR hotspot
                cv2.imwrite(str(sample_heatmap), img)

            # Seed initial high-risk screening
            screening = Screening(
                screening_id="MV-8A4F12C9",
                patient_id=demo_patient.id,
                healthcare_worker_id=clinician.id,
                image_path=str(sample_fundus),
                heatmap_path=str(sample_heatmap),
                prediction="DR PRESENT",
                probability_dr=0.884,
                probability_no_dr=0.116,
                confidence=0.884,
                risk_level="HIGH",
                recommendation="Urgent referral to Vitreoretinal Specialist within 1 to 2 weeks for fluorescein angiography and anti-VEGF therapy consideration. Target glycemic HbA1c < 7.0%.",
                ai_context="EfficientNet-B0 activated in model.features[-1]. Grad-CAM reveals concentrated lesion density at macular temporal quadrant with microaneurysms and intraretinal blot hemorrhages."
            )
            db.add(screening)
            db.commit()
            db.refresh(screening)

            # Seed PDF Report
            pdf_path = generate_medical_pdf_report(screening, demo_patient)
            report = Report(
                screening_id=screening.id,
                pdf_path=pdf_path,
                is_published=True
            )
            db.add(report)
            db.commit()

            # Index in ChromaDB
            vector_store.index_screening_report(
                patient_id=demo_patient.id,
                screening_id=screening.screening_id,
                patient_name=demo_patient.full_name,
                prediction=screening.prediction,
                confidence=screening.confidence,
                risk_level=screening.risk_level,
                recommendation=screening.recommendation,
                ai_context=screening.ai_context
            )
            logger.info("Demo patient and clinical screening successfully seeded.")
    finally:
        db.close()


@app.get("/health", tags=["System Health"])
def health_check():
    """Health check for load balancers and container monitoring."""
    return {
        "status": "healthy",
        "service": settings.PROJECT_NAME,
        "version": settings.VERSION,
        "environment": settings.ENVIRONMENT
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)

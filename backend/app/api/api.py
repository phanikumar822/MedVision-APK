from fastapi import APIRouter
from app.api.endpoints import auth, patients, screening, reports, chat

api_router = APIRouter()

api_router.include_router(auth.router, prefix="/auth", tags=["Authentication & Biometrics"])
api_router.include_router(patients.router, prefix="/patients", tags=["Patient Registry"])
api_router.include_router(screening.router, prefix="/screen", tags=["AI Screening & Grad-CAM"])
api_router.include_router(reports.router, prefix="/reports", tags=["Diagnostic Reports & PDF"])
api_router.include_router(chat.router, prefix="/chat", tags=["Grounded RAG Clinical Assistant"])

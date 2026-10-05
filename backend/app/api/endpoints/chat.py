from typing import List, Dict, Any, Optional
from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from sqlalchemy.orm import Session
from app.database.session import get_db
from app.models.user import User, UserRole
from app.models.patient import Patient
from app.auth.deps import get_current_active_user
from app.rag.llm import generate_rag_clinical_response

router = APIRouter()


class ChatRequest(BaseModel):
    message: str
    history: Optional[List[Dict[str, Any]]] = []


class ChatResponse(BaseModel):
    answer: str


@router.post("/", response_model=ChatResponse)
async def chat_with_clinical_assistant(
    payload: ChatRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_active_user)
) -> Any:
    """
    RAG-grounded Clinical AI Chatbot:
    Answers questions grounded in patient's indexed diagnostic reports & scans.
    """
    if not payload.message.strip():
        raise HTTPException(status_code=400, detail="Query message cannot be empty")

    patient_id = 1
    if current_user.patient:
        patient_id = current_user.patient.id
    else:
        first_patient = db.query(Patient).first()
        if first_patient:
            patient_id = first_patient.id

    answer = await generate_rag_clinical_response(
        query=payload.message,
        history=payload.history or [],
        patient_id=patient_id
    )

    return {"answer": answer}

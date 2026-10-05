import os
from pathlib import Path
from typing import List
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    PROJECT_NAME: str = "MedVisionAI Clinical Diagnostic Backend"
    VERSION: str = "1.0.0"
    API_V1_STR: str = ""
    ENVIRONMENT: str = "production"
    BASE_URL: str = "http://10.0.2.2:8000"

    # CORS
    BACKEND_CORS_ORIGINS: List[str] = [
        "*",
        "http://localhost",
        "http://localhost:3000",
        "http://localhost:8000",
        "http://10.0.2.2:8000",
        "http://127.0.0.1:8000"
    ]

    # Database
    DATABASE_URL: str = "sqlite:///./medvision.db"

    # Security & JWT
    SECRET_KEY: str = "medvision_clinical_deep_learning_secret_key_dr_2026_hipaa"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 7  # 7 days

    # Deep Learning Model & Artifacts
    MODEL_DIR: Path = Path("./model")
    MODEL_PATH: Path = Path("./model/dr_efficientnet_b0.pth")
    MODEL_ARCH: str = "efficientnet_b0"
    INPUT_SIZE: int = 224

    # Storage Paths
    UPLOAD_DIR: Path = Path("./uploads")
    FUNDUS_DIR: Path = Path("./uploads/fundus")
    HEATMAP_DIR: Path = Path("./uploads/heatmaps")
    REPORT_DIR: Path = Path("./uploads/reports")
    CHROMA_PERSIST_DIR: Path = Path("./chroma_db")

    # LLM & RAG Configuration (Grok / Groq / OpenAI fallback)
    LLM_PROVIDER: str = "mock_grounded"  # "groq" | "openai" | "mock_grounded"
    GROQ_API_KEY: str = os.getenv("GROQ_API_KEY", "")
    OPENAI_API_KEY: str = os.getenv("OPENAI_API_KEY", "")

    # SMTP Notifications
    SMTP_HOST: str = os.getenv("SMTP_HOST", "smtp.gmail.com")
    SMTP_PORT: int = int(os.getenv("SMTP_PORT", "587"))
    SMTP_USER: str = os.getenv("SMTP_USER", "notifications@medvision.ai")
    SMTP_PASSWORD: str = os.getenv("SMTP_PASSWORD", "")
    SMTP_FROM_EMAIL: str = os.getenv("SMTP_FROM_EMAIL", "MedVisionAI Clinic <notifications@medvision.ai>")
    SMTP_TLS: bool = True

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=True,
        extra="allow"
    )

    def init_directories(self) -> None:
        """Ensure all runtime directories exist."""
        self.MODEL_DIR.mkdir(parents=True, exist_ok=True)
        self.UPLOAD_DIR.mkdir(parents=True, exist_ok=True)
        self.FUNDUS_DIR.mkdir(parents=True, exist_ok=True)
        self.HEATMAP_DIR.mkdir(parents=True, exist_ok=True)
        self.REPORT_DIR.mkdir(parents=True, exist_ok=True)
        self.CHROMA_PERSIST_DIR.mkdir(parents=True, exist_ok=True)


settings = Settings()
settings.init_directories()

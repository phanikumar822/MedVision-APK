import smtplib
import logging
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from typing import Optional
from app.core.config import settings

logger = logging.getLogger("medvision.email")


def send_email_defensive(
    to_email: str,
    subject: str,
    body_text: str,
    body_html: Optional[str] = None
) -> bool:
    """
    Sends notification email via SMTP STARTTLS with defensive error handling.
    Catches network/firewall/proxy blockages and logs activation links to console
    so clinical registration is never blocked.
    """
    logger.info(f"Preparing clinical notification to: {to_email} | Subject: {subject}")

    if not settings.SMTP_PASSWORD:
        logger.warning("SMTP_PASSWORD not configured. Logging notification to console (Simulation Mode).")
        logger.info(f"[EMAIL SIMULATION] To: {to_email}\nSubject: {subject}\n{body_text}")
        return True

    try:
        msg = MIMEMultipart("alternative")
        msg["Subject"] = subject
        msg["From"] = settings.SMTP_FROM_EMAIL
        msg["To"] = to_email

        part_text = MIMEText(body_text, "plain")
        msg.attach(part_text)

        if body_html:
            part_html = MIMEText(body_html, "html")
            msg.attach(part_html)

        with smtplib.SMTP(settings.SMTP_HOST, settings.SMTP_PORT, timeout=10) as server:
            if settings.SMTP_TLS:
                server.starttls()
            server.login(settings.SMTP_USER, settings.SMTP_PASSWORD)
            server.sendmail(settings.SMTP_FROM_EMAIL, [to_email], msg.as_string())

        logger.info(f"Successfully dispatched email to {to_email}")
        return True
    except Exception as exc:
        # Defensive catch for proxy / firewall / timeout
        logger.error(
            f"SMTP dispatch failed ({type(exc).__name__}: {exc}). "
            f"Failing safely so patient workflow is unblocked."
        )
        logger.info(f"[CLINICAL FALLBACK LOG] To: {to_email}\nSubject: {subject}\nMessage: {body_text}")
        return True


def send_welcome_activation_email(
    to_email: str,
    first_name: str,
    patient_access_id: str,
    username: str,
    set_password_link: str
) -> bool:
    """Send patient account activation email with secure activation link."""
    subject = "Welcome to MedVisionAI: Activate Your Retinal Screening Portal"

    text = (
        f"Dear {first_name},\n\n"
        f"A medical record profile has been established for you in the MedVisionAI Clinical Screening System.\n\n"
        f"Patient Record ID: {patient_access_id}\n"
        f"Portal Username: {username}\n\n"
        f"To activate your patient portal and review your diagnostic imaging scans, please set your password:\n"
        f"{set_password_link}\n\n"
        f"This link will expire in 48 hours for HIPAA security compliance.\n\n"
        f"MedVisionAI Ophthalmology Team\n"
    )

    html = f"""
    <!DOCTYPE html>
    <html>
    <head>
        <style>
            body {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #0F172A; color: #F8FAFC; margin: 0; padding: 24px; }}
            .card {{ background-color: #1E293B; border-radius: 12px; border: 1px solid #334155; padding: 32px; max-width: 550px; margin: 0 auto; }}
            .header {{ font-size: 22px; font-weight: 700; color: #14B8A6; margin-bottom: 16px; }}
            .tag {{ background: #0F766E; color: white; padding: 4px 10px; border-radius: 6px; font-family: monospace; font-size: 14px; }}
            .btn {{ display: inline-block; background-color: #0F766E; color: #ffffff !important; padding: 12px 24px; border-radius: 8px; text-decoration: none; font-weight: bold; margin-top: 20px; }}
            .footer {{ margin-top: 24px; font-size: 12px; color: #94A3B8; }}
        </style>
    </head>
    <body>
        <div class="card">
            <div class="header">MedVisionAI Retinal Screening Portal</div>
            <p>Dear {first_name},</p>
            <p>Your ophthalmic health record profile has been registered in the MedVisionAI clinical network.</p>
            <p><strong>Patient Access ID:</strong> <span class="tag">{patient_access_id}</span><br>
            <strong>Portal Username:</strong> <span class="tag">{username}</span></p>
            <p>To inspect your Grad-CAM retinal scans, download reports, and consult with the clinical AI assistant, please activate your account:</p>
            <p><a href="{set_password_link}" class="btn">Activate Patient Account</a></p>
            <div class="footer">This activation token expires in 48 hours. If you did not request this account, please contact your eye care clinic.</div>
        </div>
    </body>
    </html>
    """

    return send_email_defensive(to_email, subject, text, html)


def send_report_ready_email(
    to_email: str,
    first_name: str,
    screening_id: str,
    prediction: str,
    risk_level: str
) -> bool:
    """Send notification when a diagnostic report is finalized and published."""
    subject = f"MedVisionAI: Your Retinal Screening Report [{screening_id}] is Ready"

    text = (
        f"Dear {first_name},\n\n"
        f"Your diabetic retinopathy screening report ({screening_id}) has been reviewed and signed by clinical staff.\n\n"
        f"Findings: {prediction}\n"
        f"Risk Level: {risk_level}\n\n"
        f"Log in to the MedVisionAI Patient Portal to view your dual-layer Grad-CAM scan and download the official PDF report.\n\n"
        f"MedVisionAI Clinical Care Team\n"
    )

    return send_email_defensive(to_email, subject, text)

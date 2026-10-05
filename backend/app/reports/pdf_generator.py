import os
from pathlib import Path
from datetime import datetime
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.lib.units import inch
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image as RLImage, KeepTogether, HRFlowable
)
from app.core.config import settings
from app.models.screening import Screening
from app.models.patient import Patient


def generate_medical_pdf_report(
    screening: Screening,
    patient: Patient,
    doctor_name: str = "Dr. Sarah Jenkins, MD"
) -> str:
    """
    Compiles a medical diagnostic report PDF using ReportLab
    including patient demographics, fundus photography, Grad-CAM activation,
    quantitative risk stratification, and clinical guidance.
    """
    settings.REPORT_DIR.mkdir(parents=True, exist_ok=True)
    pdf_filename = f"report_{screening.screening_id}.pdf"
    pdf_path = settings.REPORT_DIR / pdf_filename

    doc = SimpleDocTemplate(
        str(pdf_path),
        pagesize=letter,
        rightMargin=36,
        leftMargin=36,
        topMargin=36,
        bottomMargin=36
    )

    styles = getSampleStyleSheet()
    
    # Custom Medical Styling Tokens
    color_teal = colors.HexColor("#0F766E")
    color_mint = colors.HexColor("#14B8A6")
    color_slate = colors.HexColor("#0F172A")
    color_card = colors.HexColor("#F8FAFC")
    color_crimson = colors.HexColor("#9F1239")
    color_emerald = colors.HexColor("#065F46")

    title_style = ParagraphStyle(
        "MedTitle",
        parent=styles["Heading1"],
        fontName="Helvetica-Bold",
        fontSize=20,
        leading=24,
        textColor=color_teal
    )

    subtitle_style = ParagraphStyle(
        "MedSubTitle",
        parent=styles["Normal"],
        fontName="Helvetica",
        fontSize=10,
        leading=13,
        textColor=colors.HexColor("#475569")
    )

    section_header_style = ParagraphStyle(
        "SectionHeader",
        parent=styles["Heading2"],
        fontName="Helvetica-Bold",
        fontSize=13,
        leading=16,
        textColor=color_teal,
        spaceBefore=10,
        spaceAfter=4
    )

    body_style = ParagraphStyle(
        "MedBody",
        parent=styles["Normal"],
        fontName="Helvetica",
        fontSize=9.5,
        leading=13.5,
        textColor=color_slate
    )

    bold_label_style = ParagraphStyle(
        "BoldLabel",
        parent=styles["Normal"],
        fontName="Helvetica-Bold",
        fontSize=9.5,
        leading=13,
        textColor=colors.HexColor("#1E293B")
    )

    elements = []

    # 1. Header Banner
    elements.append(Paragraph("MedVisionAI Clinical Ophthalmology", title_style))
    elements.append(Paragraph("Automated Diabetic Retinopathy Diagnostic Screening Report (FDA/HIPAA Regulated)", subtitle_style))
    elements.append(Spacer(1, 10))
    elements.append(HRFlowable(width="100%", thickness=2, color=color_teal, spaceBefore=2, spaceAfter=12))

    # 2. Patient & Exam Demographics Table
    patient_data = [
        [
            Paragraph("<b>Patient Name:</b>", bold_label_style),
            Paragraph(patient.full_name, body_style),
            Paragraph("<b>Diagnostic Scan ID:</b>", bold_label_style),
            Paragraph(f"<font color='#0F766E'><b>{screening.screening_id}</b></font>", body_style)
        ],
        [
            Paragraph("<b>Patient Access ID:</b>", bold_label_style),
            Paragraph(patient.patient_access_id, body_style),
            Paragraph("<b>Exam Timestamp:</b>", bold_label_style),
            Paragraph(screening.created_at.strftime("%Y-%m-%d %H:%M UTC"), body_style)
        ],
        [
            Paragraph("<b>Contact Phone:</b>", bold_label_style),
            Paragraph(patient.phone or "N/A", body_style),
            Paragraph("<b>Screening Clinician:</b>", bold_label_style),
            Paragraph(doctor_name, body_style)
        ]
    ]

    patient_table = Table(patient_data, colWidths=[1.4*inch, 2.1*inch, 1.6*inch, 2.4*inch])
    patient_table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), color_card),
        ("BOX", (0, 0), (-1, -1), 0.75, colors.HexColor("#CBD5E1")),
        ("INNERGRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#E2E8F0")),
        ("PADDING", (0, 0), (-1, -1), 6),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
    ]))
    elements.append(patient_table)
    elements.append(Spacer(1, 14))

    # 3. Diagnostic Results Banner
    is_dr = screening.prediction == "DR PRESENT"
    result_color = color_crimson if is_dr else color_emerald
    result_text = f"<font color='{result_color.hexval()}'><b>{screening.prediction}</b></font>"

    summary_data = [
        [
            Paragraph("<b>DIAGNOSTIC STATUS</b>", bold_label_style),
            Paragraph("<b>RISK LEVEL</b>", bold_label_style),
            Paragraph("<b>AI CONFIDENCE</b>", bold_label_style),
            Paragraph("<b>DR PROBABILITY</b>", bold_label_style)
        ],
        [
            Paragraph(result_text, ParagraphStyle("ResLarge", fontName="Helvetica-Bold", fontSize=13, leading=16)),
            Paragraph(f"<b>{screening.risk_level}</b>", ParagraphStyle("RiskLarge", fontName="Helvetica-Bold", fontSize=12, leading=15, textColor=result_color)),
            Paragraph(f"{screening.confidence * 100:.1f}%", body_style),
            Paragraph(f"{screening.probability_dr * 100:.1f}%", body_style)
        ]
    ]
    summary_table = Table(summary_data, colWidths=[2.1*inch, 1.8*inch, 1.8*inch, 1.8*inch])
    summary_table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#F1F5F9")),
        ("BACKGROUND", (0, 1), (-1, 1), colors.white),
        ("BOX", (0, 0), (-1, -1), 1, colors.HexColor("#94A3B8")),
        ("INNERGRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#E2E8F0")),
        ("ALIGN", (0, 0), (-1, -1), "CENTER"),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("PADDING", (0, 0), (-1, -1), 8),
    ]))
    elements.append(summary_table)
    elements.append(Spacer(1, 14))

    # 4. Medical Imaging Section: Fundus Scan & Grad-CAM Heatmap
    elements.append(Paragraph("Retinal Fundus Photography & Explainability Heatmap", section_header_style))

    image_cells = []
    has_orig = screening.image_path and os.path.exists(screening.image_path)
    has_heat = screening.heatmap_path and os.path.exists(screening.heatmap_path)

    if has_orig and has_heat:
        img1 = RLImage(screening.image_path, width=3.3*inch, height=3.3*inch)
        img2 = RLImage(screening.heatmap_path, width=3.3*inch, height=3.3*inch)
        image_data = [
            [img1, img2],
            [
                Paragraph("<b>Figure 1:</b> Primary Retinal Fundus Scan (Macular Field)", subtitle_style),
                Paragraph("<b>Figure 2:</b> Grad-CAM Activation Map (model.features[-1])", subtitle_style)
            ]
        ]
        image_table = Table(image_data, colWidths=[3.7*inch, 3.7*inch])
        image_table.setStyle(TableStyle([
            ("ALIGN", (0, 0), (-1, -1), "CENTER"),
            ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ("PADDING", (0, 0), (-1, -1), 4),
        ]))
        elements.append(image_table)
    elements.append(Spacer(1, 12))

    # 5. AI Findings & Explainability
    elements.append(Paragraph("Grad-CAM Clinical Interpretation", section_header_style))
    elements.append(Paragraph(screening.ai_context, body_style))
    elements.append(Spacer(1, 10))

    # 6. Specialist Recommendations
    elements.append(Paragraph("Clinical Recommendations & Care Plan", section_header_style))
    elements.append(Paragraph(screening.recommendation, body_style))
    elements.append(Spacer(1, 16))

    # 7. Regulatory Disclaimer & Signature Block
    footer_text = (
        "<b>Notice:</b> MedVisionAI is an assistive deep-learning clinical screening system based on PyTorch EfficientNet-B0. "
        "This report is intended for qualified eye care specialists to support diagnostic workflow. Final staging must be verified "
        "by clinical evaluation. Complies with 21 CFR Part 11 and HIPAA security standards."
    )
    elements.append(HRFlowable(width="100%", thickness=1, color=colors.HexColor("#CBD5E1"), spaceBefore=4, spaceAfter=8))
    elements.append(Paragraph(footer_text, ParagraphStyle("Footer", parent=styles["Normal"], fontSize=8, leading=10, textColor=colors.HexColor("#64748B"))))

    doc.build(elements)
    return str(pdf_path)

package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// Official Clinical Healthcare Palette
// =========================================================================

// Primary Brand & Accent
val MedicalPrimary = Color(0xFF0F766E)         // Core clinical teal
val MedicalPrimaryDark = Color(0xFF115E59)     // Deep teal for pressed/hover states
val MedicalPrimaryLight = Color(0xFFCCFBF1)    // Soft teal tint for highlights
val MedicalPrimarySubtle = Color(0xFFF0FDFA)   // Ultra soft teal background tint

// Neutrals & Surfaces (Clean Clinical Hierarchy)
val DarkNavy = Color(0xFF0F172A)               // Primary text and clinical headers
val SlateSecondary = Color(0xFF64748B)         // Secondary supportive text & labels
val SlateTertiary = Color(0xFF94A3B8)          // Muted metadata & placeholders
val BorderSubtle = Color(0xFFE2E8F0)           // Clean 1dp structural dividing lines
val BorderStrong = Color(0xFFCBD5E1)           // Input focus & selected state border
val BackgroundClinical = Color(0xFFF7F9F9)     // Main screen background
val SurfaceWhite = Color(0xFFFFFFFF)           // Primary card/modal surface
val SurfaceSubtle = Color(0xFFF1F5F9)          // Subtle card secondary background

// Clinical Imaging & Ophthalmic PACS Viewport (High-contrast medical viewing)
val PacsCanvasBlack = Color(0xFF090E17)         // Diagnostic darkroom canvas
val PacsCardSurface = Color(0xFF111827)        // Retinal viewer dark border & controls
val PacsBorderDark = Color(0xFF1F2937)         // Divider for image viewer HUD

// Semantic Clinical Diagnostics
val StatusSuccess = Color(0xFF15803D)          // No DR detected / normal
val StatusSuccessBg = Color(0xFFF0FDF4)        // Reassuring light green container
val StatusSuccessBorder = Color(0xFFBBF7D0)

val StatusWarning = Color(0xFFB45309)          // Moderate risk / review required
val StatusWarningBg = Color(0xFFFFFBEB)        // Amber alert container
val StatusWarningBorder = Color(0xFFFDE68A)

val StatusCritical = Color(0xFFB91C1C)         // DR Present / high risk referral
val StatusCriticalBg = Color(0xFFFEF2F2)       // Soft red container
val StatusCriticalBorder = Color(0xFFFECACA)

// Legacy alias compatibility so all existing models/repos continue working seamlessly
val MedicalTealPrimary = MedicalPrimary
val MedicalTealLight = MedicalPrimaryLight
val ActiveMintAccent = MedicalPrimary
val ActiveMintGlow = MedicalPrimary
val CanvasDeepSlate = BackgroundClinical
val CardDarkSlate = SurfaceWhite
val CardDarkSlateHover = SurfaceSubtle
val HairlineBorderDark = BorderSubtle
val TextPrimaryDark = DarkNavy
val TextSecondaryDark = SlateSecondary
val TextTertiaryDark = SlateTertiary
val SemanticDrCrimson = StatusCritical
val SemanticDrText = StatusCritical
val SemanticNoDrEmerald = StatusSuccess
val SemanticNoDrText = StatusSuccess
val WarningAmber = StatusWarning

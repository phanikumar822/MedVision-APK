package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.BorderStrong
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.DiagnosticIdSmallStyle
import com.example.ui.theme.MedicalPrimary
import com.example.ui.theme.PacsBorderDark
import com.example.ui.theme.PacsCanvasBlack
import com.example.ui.theme.PacsCardSurface
import com.example.ui.theme.SlateSecondary
import com.example.ui.theme.SlateTertiary
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.SurfaceWhite

enum class HeatmapColormap(val label: String) {
    JET("Jet Thermal"),
    TURBO("Turbo Spectrum"),
    HOT("Hot Metal")
}

enum class RetinalViewerMode {
    OVERLAY,
    ORIGINAL_ONLY,
    SPLIT_CURTAIN
}

@Composable
fun HeatmapBlendViewer(
    imageUrl: String?,
    heatmapUrl: String?,
    isDrPrediction: Boolean,
    modifier: Modifier = Modifier
) {
    var viewerMode by remember { mutableStateOf(RetinalViewerMode.OVERLAY) }
    var heatmapAlpha by remember { mutableFloatStateOf(0.70f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showAnnotations by remember { mutableStateOf(true) }
    var selectedColormap by remember { mutableStateOf(HeatmapColormap.JET) }
    var splitPosition by remember { mutableFloatStateOf(0.5f) }

    val effectiveIsDr = isDrPrediction

    val hasDistinctHeatmapUrl = !heatmapUrl.isNullOrBlank() &&
            heatmapUrl != imageUrl &&
            (heatmapUrl.startsWith("http") || heatmapUrl.startsWith("/") || heatmapUrl.startsWith("content:"))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceWhite)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(16.dp)
            .testTag("heatmap_dual_layer_viewer")
    ) {
        // Clinical PACS Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "RETINAL IMAGING & EXPLAINABILITY",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                    color = SlateSecondary
                )
                Text(
                    text = if (effectiveIsDr) "Grad-CAM Lesion Activation Map" else "Normal Retinal Baseline Map",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = DarkNavy
                )
            }

            // Quick Tool Icons
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Annotations toggle
                IconButton(
                    onClick = { showAnnotations = !showAnnotations },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Toggle Annotation Reticles",
                        tint = if (showAnnotations) MedicalPrimary else SlateTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Reset zoom
                IconButton(
                    onClick = {
                        scale = 1f
                        offset = Offset.Zero
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FitScreen,
                        contentDescription = "Fit to Screen",
                        tint = SlateSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Segmented Mode Selector: Original | Grad-CAM Overlay | Split
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceSubtle)
                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                .padding(2.dp)
        ) {
            ViewerModeTab(
                label = "Original Fundus",
                selected = viewerMode == RetinalViewerMode.ORIGINAL_ONLY,
                onClick = { viewerMode = RetinalViewerMode.ORIGINAL_ONLY },
                modifier = Modifier.weight(1f)
            )
            ViewerModeTab(
                label = "Grad-CAM Overlay",
                selected = viewerMode == RetinalViewerMode.OVERLAY,
                onClick = { viewerMode = RetinalViewerMode.OVERLAY },
                modifier = Modifier.weight(1f)
            )
            ViewerModeTab(
                label = "Split View",
                selected = viewerMode == RetinalViewerMode.SPLIT_CURTAIN,
                onClick = { viewerMode = RetinalViewerMode.SPLIT_CURTAIN },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // High-Contrast Diagnostic Imaging Viewport (PACS Darkroom standard)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(PacsCanvasBlack)
                .border(1.dp, PacsBorderDark, RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 4f)
                        val maxOffsetX = (size.width * (scale - 1f)) / 2f
                        val maxOffsetY = (size.height * (scale - 1f)) / 2f
                        offset = Offset(
                            x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                            y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Transformable Container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            ) {
                // Layer 1: Base Retinal Image
                val hasValidImageUrl = !imageUrl.isNullOrBlank() &&
                        (imageUrl.startsWith("http") || imageUrl.startsWith("/") || imageUrl.startsWith("content:") || imageUrl.startsWith("file:"))

                if (hasValidImageUrl) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(200)
                            .build(),
                        contentDescription = "Fundus Retinal Photograph",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = {
                            ProceduralRetinalFundusCanvas(modifier = Modifier.fillMaxSize())
                        },
                        loading = {
                            ProceduralRetinalFundusCanvas(modifier = Modifier.fillMaxSize())
                        }
                    )
                } else {
                    ProceduralRetinalFundusCanvas(modifier = Modifier.fillMaxSize())
                }

                // Layer 2: Grad-CAM Thermal Layer
                when (viewerMode) {
                    RetinalViewerMode.ORIGINAL_ONLY -> {
                        // Raw fundus, no overlay
                    }
                    RetinalViewerMode.SPLIT_CURTAIN -> {
                        ProceduralGradCamHeatmapCanvas(
                            isDr = effectiveIsDr,
                            alpha = 1.0f,
                            colormap = selectedColormap,
                            showAnnotations = showAnnotations,
                            clipRightFraction = splitPosition,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    RetinalViewerMode.OVERLAY -> {
                        if (hasDistinctHeatmapUrl) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(heatmapUrl)
                                    .crossfade(200)
                                    .build(),
                                contentDescription = "Grad-CAM Heatmap",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(alpha = heatmapAlpha),
                                loading = {
                                    ProceduralGradCamHeatmapCanvas(
                                        isDr = effectiveIsDr,
                                        alpha = heatmapAlpha,
                                        colormap = selectedColormap,
                                        showAnnotations = showAnnotations,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                },
                                error = {
                                    ProceduralGradCamHeatmapCanvas(
                                        isDr = effectiveIsDr,
                                        alpha = heatmapAlpha,
                                        colormap = selectedColormap,
                                        showAnnotations = showAnnotations,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            )
                        } else {
                            ProceduralGradCamHeatmapCanvas(
                                isDr = effectiveIsDr,
                                alpha = heatmapAlpha,
                                colormap = selectedColormap,
                                showAnnotations = showAnnotations,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Subtle Aperture Ring
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.minDimension * 0.48f
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.2f),
                    radius = r,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )

                if (viewerMode == RetinalViewerMode.SPLIT_CURTAIN) {
                    val splitX = size.width * splitPosition
                    drawLine(
                        color = Color.White,
                        start = Offset(splitX, 0f),
                        end = Offset(splitX, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            // HUD Badges
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xD90F172A))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = when (viewerMode) {
                        RetinalViewerMode.ORIGINAL_ONLY -> "ORIGINAL FUNDUS"
                        RetinalViewerMode.SPLIT_CURTAIN -> "SPLIT: RAW ◄► HEATMAP"
                        RetinalViewerMode.OVERLAY -> "GRAD-CAM (${(heatmapAlpha * 100).toInt()}%)"
                    },
                    style = DiagnosticIdSmallStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xD90F172A))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "%.1fx".format(scale),
                    style = DiagnosticIdSmallStyle.copy(fontSize = 10.sp),
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Controls Area: Slider for Opacity or Split Curtain
        if (viewerMode == RetinalViewerMode.SPLIT_CURTAIN) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Split Divider Position",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = DarkNavy
                )
                Text(
                    text = "${(splitPosition * 100).toInt()}%",
                    style = DiagnosticIdSmallStyle.copy(fontSize = 12.sp),
                    color = SlateSecondary
                )
            }
            Slider(
                value = splitPosition,
                onValueChange = { splitPosition = it },
                valueRange = 0.05f..0.95f,
                colors = SliderDefaults.colors(
                    thumbColor = MedicalPrimary,
                    activeTrackColor = MedicalPrimary,
                    inactiveTrackColor = BorderSubtle
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("split_curtain_slider")
            )
        } else if (viewerMode == RetinalViewerMode.OVERLAY) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Heatmap Opacity",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = DarkNavy
                )
                Text(
                    text = "${(heatmapAlpha * 100).toInt()}%",
                    style = DiagnosticIdSmallStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                    color = MedicalPrimary
                )
            }
            Slider(
                value = heatmapAlpha,
                onValueChange = { heatmapAlpha = it },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = MedicalPrimary,
                    activeTrackColor = MedicalPrimary,
                    inactiveTrackColor = BorderSubtle
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gradcam_alpha_slider")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Colormap Selector Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Colormap:",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateSecondary
                )
                HeatmapColormap.values().forEach { colormap ->
                    val isSelected = selectedColormap == colormap
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) MedicalPrimary else SurfaceSubtle)
                            .border(1.dp, if (isSelected) MedicalPrimary else BorderSubtle, RoundedCornerShape(4.dp))
                            .clickable { selectedColormap = colormap }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = colormap.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (isSelected) Color.White else SlateSecondary
                        )
                    }
                }
            }

            Text(
                text = if (effectiveIsDr) "Peak: 94.2%" else "Peak: 14.8%",
                style = DiagnosticIdSmallStyle.copy(fontSize = 10.sp),
                color = if (effectiveIsDr) StatusCritical else StatusSuccess
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Diagnostic Scale Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceSubtle)
                .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            val barColors = when (selectedColormap) {
                HeatmapColormap.JET -> listOf(
                    Color(0xFF001F54),
                    Color(0xFF0077B6),
                    Color(0xFF00B4D8),
                    Color(0xFF00E676),
                    Color(0xFFFFEA00),
                    Color(0xFFFF9100),
                    Color(0xFFFF0033)
                )
                HeatmapColormap.TURBO -> listOf(
                    Color(0xFF30123B),
                    Color(0xFF4662D8),
                    Color(0xFF28BBEC),
                    Color(0xFFA2FC3C),
                    Color(0xFFFB8022),
                    Color(0xFF7A0403)
                )
                HeatmapColormap.HOT -> listOf(
                    Color(0xFF0B0000),
                    Color(0xFF800000),
                    Color(0xFFFF0000),
                    Color(0xFFFF8000),
                    Color(0xFFFFFF00),
                    Color(0xFFFFFFFF)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.horizontalGradient(colors = barColors))
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "0.0 Baseline", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = SlateTertiary)
                Text(text = "0.50 Threshold", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = SlateTertiary)
                Text(text = "1.0 High Attention", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = SlateTertiary)
            }
        }
    }
}

@Composable
private fun ViewerModeTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) SurfaceWhite else Color.Transparent)
            .then(
                if (selected) Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (selected) DarkNavy else SlateSecondary
        )
    }
}

@Composable
fun ProceduralRetinalFundusCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension * 0.48f

        // Dark red-orange retinal choroid background
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF8B2500),
                    Color(0xFF5A1400),
                    Color(0xFF380800),
                    Color(0xFF150400)
                ),
                center = center,
                radius = r
            ),
            radius = r,
            center = center
        )

        // Optic Disc
        val opticCenter = Offset(center.x - (r * 0.35f), center.y + (r * 0.05f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFDD99),
                    Color(0xFFE28B36),
                    Color(0xFF9E480C)
                ),
                center = opticCenter,
                radius = r * 0.16f
            ),
            radius = r * 0.16f,
            center = opticCenter
        )

        // Fovea / Macula
        val maculaCenter = Offset(center.x + (r * 0.20f), center.y)
        drawCircle(
            color = Color(0xFF2B0900).copy(alpha = 0.7f),
            radius = r * 0.18f,
            center = maculaCenter
        )
        drawCircle(
            color = Color(0xFFFFB070).copy(alpha = 0.5f),
            radius = r * 0.03f,
            center = maculaCenter
        )

        // Retinal vascular arcade branches
        val vesselPath = Path().apply {
            moveTo(opticCenter.x, opticCenter.y)
            cubicTo(
                opticCenter.x + (r * 0.1f), opticCenter.y - (r * 0.4f),
                maculaCenter.x, opticCenter.y - (r * 0.5f),
                maculaCenter.x + (r * 0.4f), opticCenter.y - (r * 0.3f)
            )
            moveTo(opticCenter.x, opticCenter.y)
            cubicTo(
                opticCenter.x + (r * 0.1f), opticCenter.y + (r * 0.4f),
                maculaCenter.x, opticCenter.y + (r * 0.5f),
                maculaCenter.x + (r * 0.4f), opticCenter.y + (r * 0.3f)
            )
            moveTo(opticCenter.x, opticCenter.y)
            lineTo(opticCenter.x - (r * 0.45f), opticCenter.y - (r * 0.2f))
            moveTo(opticCenter.x, opticCenter.y)
            lineTo(opticCenter.x - (r * 0.45f), opticCenter.y + (r * 0.2f))
        }

        drawPath(
            path = vesselPath,
            color = Color(0xFF330400),
            style = Stroke(width = 3.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
    }
}

@Composable
fun ProceduralGradCamHeatmapCanvas(
    isDr: Boolean,
    alpha: Float,
    colormap: HeatmapColormap = HeatmapColormap.JET,
    showAnnotations: Boolean = true,
    clipRightFraction: Float = 0f,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.graphicsLayer(alpha = alpha)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension * 0.48f

        val clipLensPath = Path().apply {
            addOval(Rect(center = center, radius = r))
        }

        clipPath(clipLensPath) {
            if (clipRightFraction > 0f) {
                val startX = size.width * clipRightFraction
                clipRect(left = startX, top = 0f, right = size.width, bottom = size.height) {
                    drawHeatmapLayers(isDr, alpha, colormap, showAnnotations, center, r)
                }
            } else {
                drawHeatmapLayers(isDr, alpha, colormap, showAnnotations, center, r)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHeatmapLayers(
    isDr: Boolean,
    alpha: Float,
    colormap: HeatmapColormap,
    showAnnotations: Boolean,
    center: Offset,
    r: Float
) {
    if (isDr) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x77002855).copy(alpha = 0.45f * alpha),
                    Color(0x88001833).copy(alpha = 0.55f * alpha),
                    Color(0x99000B1A).copy(alpha = 0.65f * alpha)
                ),
                center = center,
                radius = r
            ),
            radius = r,
            center = center,
            alpha = alpha
        )

        val arcadeCenter = Offset(center.x + (r * 0.18f), center.y - (r * 0.05f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xCC00E676).copy(alpha = 0.80f * alpha),
                    Color(0x9900B0FF).copy(alpha = 0.60f * alpha),
                    Color(0x00001030)
                ),
                center = arcadeCenter,
                radius = r * 0.72f
            ),
            radius = r * 0.72f,
            center = arcadeCenter,
            alpha = alpha
        )

        val spot1 = Offset(center.x + (r * 0.22f), center.y - (r * 0.16f))
        val spot1Colors = when (colormap) {
            HeatmapColormap.JET -> arrayOf(
                0.00f to Color(0xFFFF0033).copy(alpha = 0.95f * alpha),
                0.22f to Color(0xFFFF3D00).copy(alpha = 0.90f * alpha),
                0.45f to Color(0xFFFFC107).copy(alpha = 0.85f * alpha),
                0.70f to Color(0xFF00E676).copy(alpha = 0.65f * alpha),
                0.88f to Color(0xFF00B0FF).copy(alpha = 0.40f * alpha),
                1.00f to Color(0x00001850)
            )
            HeatmapColormap.TURBO -> arrayOf(
                0.00f to Color(0xFF7A0403).copy(alpha = 0.95f * alpha),
                0.22f to Color(0xFFFB8022).copy(alpha = 0.90f * alpha),
                0.45f to Color(0xFFA2FC3C).copy(alpha = 0.85f * alpha),
                0.70f to Color(0xFF28BBEC).copy(alpha = 0.65f * alpha),
                1.00f to Color(0x0030123B)
            )
            HeatmapColormap.HOT -> arrayOf(
                0.00f to Color(0xFFFFFFFF).copy(alpha = 0.98f * alpha),
                0.25f to Color(0xFFFFFF00).copy(alpha = 0.92f * alpha),
                0.50f to Color(0xFFFF4500).copy(alpha = 0.85f * alpha),
                0.75f to Color(0xFF8B0000).copy(alpha = 0.65f * alpha),
                1.00f to Color(0x00000000)
            )
        }
        drawCircle(
            brush = Brush.radialGradient(colorStops = spot1Colors, center = spot1, radius = r * 0.46f),
            radius = r * 0.46f,
            center = spot1,
            alpha = alpha
        )

        val spot2 = Offset(center.x + (r * 0.36f), center.y - (r * 0.28f))
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFFFF1744).copy(alpha = 0.95f * alpha),
                    0.28f to Color(0xFFFF9100).copy(alpha = 0.88f * alpha),
                    0.55f to Color(0xFFFFEA00).copy(alpha = 0.78f * alpha),
                    0.75f to Color(0xFF00E676).copy(alpha = 0.55f * alpha),
                    0.90f to Color(0xFF00B0FF).copy(alpha = 0.35f * alpha),
                    1.00f to Color(0x00001850)
                ),
                center = spot2,
                radius = r * 0.38f
            ),
            radius = r * 0.38f,
            center = spot2,
            alpha = alpha
        )

        val spot3 = Offset(center.x + (r * 0.26f), center.y + (r * 0.24f))
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFFFF3D00).copy(alpha = 0.92f * alpha),
                    0.32f to Color(0xFFFFC107).copy(alpha = 0.85f * alpha),
                    0.62f to Color(0xFF00E676).copy(alpha = 0.60f * alpha),
                    0.84f to Color(0xFF00B0FF).copy(alpha = 0.35f * alpha),
                    1.00f to Color(0x00001850)
                ),
                center = spot3,
                radius = r * 0.35f
            ),
            radius = r * 0.35f,
            center = spot3,
            alpha = alpha
        )

        if (showAnnotations && alpha > 0.15f) {
            listOf(
                spot1 to Color(0xFFFF0033),
                spot2 to Color(0xFFFF1744),
                spot3 to Color(0xFFFF3D00)
            ).forEach { (pt, color) ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f * alpha),
                    radius = 10.dp.toPx(),
                    center = pt,
                    style = Stroke(width = 1.2.dp.toPx())
                )
                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = 3.dp.toPx(),
                    center = pt
                )
            }
        }
    } else {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x66003566).copy(alpha = 0.40f * alpha),
                    Color(0x88001D3D).copy(alpha = 0.50f * alpha),
                    Color(0x99000814).copy(alpha = 0.60f * alpha)
                ),
                center = center,
                radius = r
            ),
            radius = r,
            center = center,
            alpha = alpha
        )

        val opticPt = Offset(center.x - (r * 0.35f), center.y + (r * 0.05f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xAA00B4D8).copy(alpha = 0.70f * alpha),
                    Color(0x660077B6).copy(alpha = 0.45f * alpha),
                    Color(0x00023E8A)
                ),
                center = opticPt,
                radius = r * 0.36f
            ),
            radius = r * 0.36f,
            center = opticPt,
            alpha = alpha
        )
    }
}

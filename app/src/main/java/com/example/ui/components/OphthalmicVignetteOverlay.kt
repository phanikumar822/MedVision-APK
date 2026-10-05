package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MedicalPrimary
import kotlin.math.min

@Composable
fun OphthalmicVignetteOverlay(
    modifier: Modifier = Modifier,
    onTapToFocus: (Offset) -> Unit = {}
) {
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    val focusRingScale = remember { Animatable(1.5f) }
    val focusRingAlpha = remember { Animatable(0f) }

    LaunchedEffect(focusPoint) {
        focusPoint?.let {
            focusRingScale.snapTo(1.6f)
            focusRingAlpha.snapTo(1.0f)
            focusRingScale.animateTo(1.0f, tween(200))
            focusRingAlpha.animateTo(0.0f, tween(400))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    focusPoint = offset
                    onTapToFocus(offset)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val lensRadius = min(size.width, size.height) * 0.42f

            val maskPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }

            // Dark vignette mask
            drawPath(
                path = maskPath,
                color = Color(0xF00A0F1D)
            )

            // Punch out transparent aperture circle
            drawCircle(
                color = Color.Transparent,
                radius = lensRadius,
                center = center,
                blendMode = BlendMode.Clear
            )

            // Medical Lens Aperture Border Rim
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = lensRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Inner focus guide ring
            drawCircle(
                color = MedicalPrimary.copy(alpha = 0.4f),
                radius = lensRadius * 0.45f,
                center = center,
                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
            )

            // Center alignment crosshairs
            val crosshairSize = 14.dp.toPx()
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(center.x - crosshairSize, center.y),
                end = Offset(center.x + crosshairSize, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(center.x, center.y - crosshairSize),
                end = Offset(center.x, center.y + crosshairSize),
                strokeWidth = 1.dp.toPx()
            )

            // 4 Orientation ticks
            val tickLength = 10.dp.toPx()
            // Top
            drawLine(
                color = Color.White.copy(alpha = 0.8f),
                start = Offset(center.x, center.y - lensRadius),
                end = Offset(center.x, center.y - lensRadius + tickLength),
                strokeWidth = 1.5.dp.toPx()
            )
            // Bottom
            drawLine(
                color = Color.White.copy(alpha = 0.8f),
                start = Offset(center.x, center.y + lensRadius),
                end = Offset(center.x, center.y + lensRadius - tickLength),
                strokeWidth = 1.5.dp.toPx()
            )
            // Left
            drawLine(
                color = Color.White.copy(alpha = 0.8f),
                start = Offset(center.x - lensRadius, center.y),
                end = Offset(center.x - lensRadius + tickLength, center.y),
                strokeWidth = 1.5.dp.toPx()
            )
            // Right
            drawLine(
                color = Color.White.copy(alpha = 0.8f),
                start = Offset(center.x + lensRadius, center.y),
                end = Offset(center.x + lensRadius - tickLength, center.y),
                strokeWidth = 1.5.dp.toPx()
            )

            // Tap-to-focus ring animation
            focusPoint?.let { pt ->
                if (focusRingAlpha.value > 0.01f) {
                    val ringRadius = 28.dp.toPx() * focusRingScale.value
                    drawCircle(
                        color = MedicalPrimary.copy(alpha = focusRingAlpha.value),
                        radius = ringRadius,
                        center = pt,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
        }
    }
}

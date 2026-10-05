package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.DiagnosticIdSmallStyle
import com.example.ui.theme.SlateSecondary
import com.example.ui.theme.SlateTertiary
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusCriticalBg
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessBg
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningBg
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.SurfaceWhite

@Composable
fun ProbabilityBarChart(
    probabilityDr: Float,
    probabilityNoDr: Float,
    modifier: Modifier = Modifier,
    patientLabel: String = "Patient Assessment",
    showThresholdLegend: Boolean = true
) {
    val animatedDr by animateFloatAsState(
        targetValue = probabilityDr.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "DrProbAnim"
    )
    val animatedNoDr by animateFloatAsState(
        targetValue = probabilityNoDr.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "NoDrProbAnim"
    )

    val riskLevel = when {
        animatedDr >= 0.60f -> "High Risk"
        animatedDr >= 0.20f -> "Moderate Risk"
        else -> "Low Risk"
    }

    val (riskColor, riskBg) = when {
        animatedDr >= 0.60f -> StatusCritical to StatusCriticalBg
        animatedDr >= 0.20f -> StatusWarning to StatusWarningBg
        else -> StatusSuccess to StatusSuccessBg
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceWhite)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(16.dp)
            .testTag("probability_distribution_card")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CLASSIFICATION PROBABILITY",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                    color = SlateSecondary
                )
                Text(
                    text = patientLabel,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = DarkNavy
                )
            }

            // Risk badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(riskBg)
                    .border(1.dp, riskColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = riskLevel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = riskColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // DR Present Probability Bar
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Diabetic Retinopathy (DR Present)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkNavy
                )
                Text(
                    text = "%.1f%%".format(animatedDr * 100f),
                    style = DiagnosticIdSmallStyle.copy(fontWeight = FontWeight.Bold),
                    color = StatusCritical
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                drawRoundRect(
                    color = SurfaceSubtle,
                    size = size,
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = StatusCritical,
                    size = Size(size.width * animatedDr, size.height),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // No DR (Normal Retina) Bar
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "No Diabetic Retinopathy (Normal)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkNavy
                )
                Text(
                    text = "%.1f%%".format(animatedNoDr * 100f),
                    style = DiagnosticIdSmallStyle.copy(fontWeight = FontWeight.Bold),
                    color = StatusSuccess
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                drawRoundRect(
                    color = SurfaceSubtle,
                    size = size,
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = StatusSuccess,
                    size = Size(size.width * animatedNoDr, size.height),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }

        if (showThresholdLegend) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "CLINICAL RISK THRESHOLDS",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                color = SlateSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            ) {
                val w = size.width
                val h = size.height
                drawRect(
                    color = StatusSuccess,
                    topLeft = Offset(0f, 0f),
                    size = Size(w * 0.20f, h)
                )
                drawRect(
                    color = StatusWarning,
                    topLeft = Offset(w * 0.20f, 0f),
                    size = Size(w * 0.40f, h)
                )
                drawRect(
                    color = StatusCritical,
                    topLeft = Offset(w * 0.60f, 0f),
                    size = Size(w * 0.40f, h)
                )

                val markerX = w * animatedDr
                drawLine(
                    color = DarkNavy,
                    start = Offset(markerX, -3f),
                    end = Offset(markerX, h + 3f),
                    strokeWidth = 2.dp.toPx()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ThresholdTag(color = StatusSuccess, text = "Low (<20%)")
                ThresholdTag(color = StatusWarning, text = "Moderate (20-60%)")
                ThresholdTag(color = StatusCritical, text = "High (>60%)")
            }
        }
    }
}

@Composable
private fun ThresholdTag(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = SlateSecondary
        )
    }
}

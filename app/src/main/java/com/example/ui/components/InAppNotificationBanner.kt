package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.MedicalPrimary
import com.example.ui.theme.SlateSecondary
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusCriticalBg
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessBg
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningBg
import com.example.ui.theme.SurfaceWhite
import com.example.util.ClinicalNotificationHelper
import com.example.util.InAppNotificationMessage
import com.example.util.NotificationType
import kotlinx.coroutines.delay

@Composable
fun InAppNotificationHost(
    modifier: Modifier = Modifier
) {
    var currentMessage by remember { mutableStateOf<InAppNotificationMessage?>(null) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ClinicalNotificationHelper.inAppNotifications.collect { message ->
            currentMessage = message
            isVisible = true
            delay(5000)
            isVisible = false
        }
    }

    AnimatedVisibility(
        visible = isVisible && currentMessage != null,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("in_app_notification_banner")
    ) {
        currentMessage?.let { msg ->
            val (icon, iconTint, bgCol, borderCol) = when (msg.type) {
                NotificationType.SUCCESS -> Quadruple(Icons.Default.CheckCircle, StatusSuccess, StatusSuccessBg, StatusSuccess.copy(alpha = 0.4f))
                NotificationType.CRITICAL -> Quadruple(Icons.Default.NotificationsActive, StatusCritical, StatusCriticalBg, StatusCritical.copy(alpha = 0.4f))
                NotificationType.WARNING -> Quadruple(Icons.Default.Warning, StatusWarning, StatusWarningBg, StatusWarning.copy(alpha = 0.4f))
                NotificationType.INFO -> Quadruple(Icons.Default.Info, MedicalPrimary, SurfaceWhite, MedicalPrimary.copy(alpha = 0.3f))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgCol)
                    .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                    .clickable {
                        msg.onAction?.invoke()
                        isVisible = false
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = msg.title,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = DarkNavy
                        )
                        Text(
                            text = msg.description,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = SlateSecondary,
                            maxLines = 2
                        )
                    }

                    IconButton(
                        onClick = { isVisible = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss Notification",
                            tint = SlateSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

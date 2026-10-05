package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.MedicalPrimary
import com.example.ui.theme.SurfaceWhite

@Composable
fun BiometricLaserScanner(
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceWhite)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("biometric_fingerprint_scanner"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = "Biometric Authentication",
            tint = MedicalPrimary,
            modifier = Modifier.size(26.dp)
        )
    }
}

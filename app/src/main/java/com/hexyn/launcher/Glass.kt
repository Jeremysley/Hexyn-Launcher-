package com.hexyn.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Frosted-glass look: soft dark tint, a white sheen that fades downward,
 * and a thin rim light that is brighter on top.
 */
fun Modifier.glass(shape: Shape, strength: Float = 1f): Modifier = this
    .clip(shape)
    .background(Color.Black.copy(alpha = 0.16f))
    .background(
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.30f * strength),
                Color.White.copy(alpha = 0.12f * strength),
            )
        )
    )
    .border(
        0.8.dp,
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.08f),
            )
        ),
        shape,
    )

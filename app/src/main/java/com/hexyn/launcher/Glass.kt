package com.hexyn.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/** The smooth "continuous corner" shape iOS uses for app icons (a superellipse). */
object SquircleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        val a = size.width / 2f
        val b = size.height / 2f
        val n = 5.0
        val steps = 90
        for (i in 0..steps) {
            val t = 2.0 * Math.PI * i / steps
            val c = cos(t)
            val s = sin(t)
            val x = a + a * (sign(c) * abs(c).pow(2.0 / n)).toFloat()
            val y = b + b * (sign(s) * abs(s).pow(2.0 / n)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/** The icon shape chosen in Launcher Settings. */
fun iconShape(): Shape = when (S.iconStyle) {
    0 -> SquircleShape
    1 -> RoundedCornerShape(22)
    else -> CircleShape
}

private fun alpha(v: Float): Float = v.coerceIn(0f, 1f)

/**
 * Frosted-glass look: tint, a sheen that fades downward, a diagonal specular highlight,
 * and a rim light that is brighter on top. Strength follows the Glass setting.
 * Light glass uses a white tint; "Reduce effects" drops the highlight layers (faster on weak phones).
 */
fun Modifier.glass(shape: Shape, strength: Float = 1f): Modifier {
    val k = strength * S.glass
    val light = S.glassLight
    val tint = if (light) Color.White.copy(alpha = alpha(0.22f * k)) else Color.Black.copy(alpha = alpha(0.14f * k))
    val base = this.clip(shape).background(tint)
    val rim = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = alpha(0.75f * k)),
            Color.White.copy(alpha = alpha(0.12f * k)),
        )
    )
    if (S.reduceEffects) {
        return base.border(1.dp, rim, shape)
    }
    return base
        // soft top-to-bottom sheen
        .background(
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = alpha((if (light) 0.34f else 0.26f) * k)),
                    Color.White.copy(alpha = alpha(0.10f * k)),
                )
            )
        )
        // diagonal specular highlight from the top-left corner
        .background(
            Brush.linearGradient(
                0f to Color.White.copy(alpha = alpha(0.24f * k)),
                0.45f to Color.Transparent,
                1f to Color.White.copy(alpha = alpha(0.07f * k)),
            )
        )
        // soft reflection band along the bottom edge
        .background(
            Brush.verticalGradient(
                0.8f to Color.Transparent,
                1f to Color.White.copy(alpha = alpha(0.10f * k)),
            )
        )
        .border(1.dp, rim, shape)
}

package com.hexyn.launcher

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A built-in wallpaper: a diagonal gradient with three soft colour blobs, like the iOS ones. */
data class WallpaperPreset(
    val name: String,
    val bg1: Long,
    val bg2: Long,
    val blob1: Long,
    val blob2: Long,
    val blob3: Long,
)

val WALLPAPER_PRESETS = listOf(
    WallpaperPreset("Midnight", 0xFF0B0B2B, 0xFF1B1464, 0xFF5B5BFF, 0xFFB06AFF, 0xFF2D9CDB),
    WallpaperPreset("Sunset", 0xFFFF512F, 0xFFDD2476, 0xFFFFC371, 0xFFFF6B6B, 0xFF8E2DE2),
    WallpaperPreset("Ocean", 0xFF0F2027, 0xFF2C5364, 0xFF00C9FF, 0xFF92FE9D, 0xFF1E3C72),
    WallpaperPreset("Candy", 0xFFFFC3E1, 0xFFC3D5FF, 0xFFFF8AD8, 0xFF8AB4FF, 0xFFFFE08A),
    WallpaperPreset("Forest", 0xFF0B3D2E, 0xFF134E4A, 0xFF2ECC71, 0xFF16A085, 0xFFB8E986),
    WallpaperPreset("Graphite", 0xFF141414, 0xFF2A2A2E, 0xFF3A3A44, 0xFF5A5A66, 0xFF222228),
)

private fun withAlpha(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or (alpha shl 24)

fun renderWallpaper(width: Int, height: Int, preset: WallpaperPreset): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val w = width.toFloat()
    val h = height.toFloat()

    paint.shader = LinearGradient(
        0f, 0f, w, h,
        preset.bg1.toInt(), preset.bg2.toInt(),
        Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, 0f, w, h, paint)

    fun blob(cx: Float, cy: Float, radius: Float, color: Int) {
        paint.shader = RadialGradient(
            cx, cy, radius,
            intArrayOf(withAlpha(color, 190), withAlpha(color, 0)),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, radius, paint)
    }

    blob(w * 0.20f, h * 0.22f, w * 0.90f, preset.blob1.toInt())
    blob(w * 0.90f, h * 0.55f, w * 0.95f, preset.blob2.toInt())
    blob(w * 0.25f, h * 0.92f, w * 0.90f, preset.blob3.toInt())
    return bitmap
}

/** Sets the wallpaper for both the home screen and the lock screen. Returns true on success. */
suspend fun applyWallpaper(context: Context, preset: WallpaperPreset): Boolean =
    withContext(Dispatchers.Default) {
        try {
            val metrics = context.resources.displayMetrics
            val bitmap = renderWallpaper(metrics.widthPixels, metrics.heightPixels, preset)
            WallpaperManager.getInstance(context).setBitmap(
                bitmap,
                null,
                true,
                WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK,
            )
            true
        } catch (e: Exception) {
            false
        }
    }

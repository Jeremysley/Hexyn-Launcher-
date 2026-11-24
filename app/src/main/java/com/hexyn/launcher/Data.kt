package com.hexyn.launcher

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

data class AppItem(
    val label: String,
    val packageName: String,
    val category: String,
    val icon: ImageBitmap,
)

private const val PREFS = "launcher"
private const val KEY_PINNED = "pinned"

/**
 * Draws an app icon edge-to-edge (like iOS) so it can be clipped to a rounded square.
 * Adaptive icons use their background + foreground layers; old-style icons sit on a soft tile.
 */
fun renderIcon(drawable: Drawable, size: Int = 160): ImageBitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    if (drawable is AdaptiveIconDrawable) {
        val pad = (size * 0.25f).toInt()
        drawable.background?.let {
            it.setBounds(-pad, -pad, size + pad, size + pad)
            it.draw(canvas)
        }
        drawable.foreground?.let {
            it.setBounds(-pad, -pad, size + pad, size + pad)
            it.draw(canvas)
        }
    } else {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(
            0f, 0f, 0f, size.toFloat(),
            0xFFFFFFFF.toInt(), 0xFFD9DDE8.toInt(), Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        val inset = (size * 0.14f).toInt()
        drawable.setBounds(inset, inset, size - inset, size - inset)
        drawable.draw(canvas)
    }
    return bitmap.asImageBitmap()
}

private fun categoryOf(info: ApplicationInfo): String = when (info.category) {
    ApplicationInfo.CATEGORY_SOCIAL -> "Social"
    ApplicationInfo.CATEGORY_GAME -> "Games"
    ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO -> "Entertainment"
    ApplicationInfo.CATEGORY_IMAGE -> "Creativity"
    ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
    ApplicationInfo.CATEGORY_NEWS -> "Reading & News"
    ApplicationInfo.CATEGORY_MAPS, ApplicationInfo.CATEGORY_ACCESSIBILITY -> "Utilities"
    else -> if ((info.flags and ApplicationInfo.FLAG_SYSTEM) != 0) "Utilities" else "Other"
}

fun loadApps(context: Context): List<AppItem> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .filter { it.activityInfo.packageName != context.packageName }
        .distinctBy { it.activityInfo.packageName }
        .map { ri ->
            AppItem(
                label = ri.loadLabel(pm).toString(),
                packageName = ri.activityInfo.packageName,
                category = categoryOf(ri.activityInfo.applicationInfo),
                icon = renderIcon(ri.loadIcon(pm)),
            )
        }
        .sortedBy { it.label.lowercase() }
}

fun launchApp(context: Context, view: View, packageName: String, bounds: Rect? = null) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
    val options = if (bounds != null) {
        ActivityOptions.makeScaleUpAnimation(
            view,
            bounds.left.toInt(),
            bounds.top.toInt(),
            bounds.width.toInt(),
            bounds.height.toInt(),
        )
    } else {
        ActivityOptions.makeBasic()
    }
    try {
        context.startActivity(intent, options.toBundle())
    } catch (e: Exception) {
        // App could not be opened; ignore.
    }
}

fun startSafely(context: Context, intent: Intent) {
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        // No app can handle this action on this phone; ignore.
    }
}

private fun resolvePackage(context: Context, intent: Intent): String? =
    context.packageManager
        .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        ?.activityInfo
        ?.packageName

private fun defaultDock(context: Context): List<String> {
    val intents = listOf(
        Intent(Intent.ACTION_DIAL),
        Intent(Intent.ACTION_VIEW, Uri.parse("sms:")),
        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")),
        Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
    )
    return intents
        .mapNotNull { resolvePackage(context, it) }
        .filter { it != "android" }
        .distinct()
}

fun loadPinned(context: Context): List<String> {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    if (!prefs.contains(KEY_PINNED)) {
        val defaults = defaultDock(context).take(Cfg.MAX_DOCK)
        savePinned(context, defaults)
        return defaults
    }
    return prefs.getString(KEY_PINNED, "").orEmpty().split(",").filter { it.isNotBlank() }
}

fun savePinned(context: Context, pinned: List<String>) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_PINNED, pinned.joinToString(","))
        .apply()
}

/** First page has room for the clock widget (2 rows), the rest are full grids. */
fun paginate(apps: List<AppItem>): List<List<AppItem>> {
    val firstRows = if (S.showWidgets) Cfg.HOME_ROWS - 2 else Cfg.HOME_ROWS
    val firstPage = S.columns * firstRows
    val perPage = S.columns * Cfg.HOME_ROWS
    if (apps.size <= firstPage) return listOf(apps)
    return listOf(apps.take(firstPage)) + apps.drop(firstPage).chunked(perPage)
}

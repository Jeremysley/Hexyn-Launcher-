package com.hexyn.launcher

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** Fixed layout values. */
object Cfg {
    const val HOME_ROWS = 6
    const val MAX_DOCK = 4
}

val ACCENT_NAMES = listOf("Red", "Orange", "Blue", "Green", "White")
val ACCENT_COLORS = listOf(
    Color(0xFFFF453A),
    Color(0xFFFF9F0A),
    Color(0xFF0A84FF),
    Color(0xFF34C759),
    Color.White,
)

fun accentColor(index: Int): Color = ACCENT_COLORS[index.coerceIn(0, ACCENT_COLORS.lastIndex)]

/**
 * User settings. Every value is Compose state, so the UI updates instantly when you change
 * something in Launcher Settings or the widget editor. Values are saved on the phone.
 */
object S {
    private var prefs: SharedPreferences? = null

    // Icons and look
    var iconSize by mutableIntStateOf(60)
    var iconStyle by mutableIntStateOf(0) // 0 = squircle, 1 = rounded, 2 = circle
    var showLabels by mutableStateOf(true)
    var columns by mutableIntStateOf(4)
    var glass by mutableFloatStateOf(1f)
    var glassLight by mutableStateOf(false)
    var reduceEffects by mutableStateOf(false)
    var showWidgets by mutableStateOf(true)

    // Status bar and Dynamic Island
    var customStatusBar by mutableStateOf(true)
    var islandEnabled by mutableStateOf(true)
    var islandWidth by mutableIntStateOf(120)
    var islandHeight by mutableIntStateOf(34)
    var islandTop by mutableIntStateOf(10)
    var islandMusic by mutableStateOf(true)
    var islandNotifications by mutableStateOf(true)
    var islandActivity by mutableStateOf(true)
    var islandBluetooth by mutableStateOf(false)
    var islandLowBattery by mutableStateOf(true)
    var photoUri by mutableStateOf("")

    // Widgets
    var calWeekSize by mutableIntStateOf(13)
    var calDateSize by mutableIntStateOf(60)
    var calMonthSize by mutableIntStateOf(13)
    var calAccent by mutableIntStateOf(0)
    var clockStyle by mutableIntStateOf(0) // 0 = analog, 1 = digital
    var clockTimeSize by mutableIntStateOf(46)
    var clockSeconds by mutableStateOf(true)
    var clockAccent by mutableIntStateOf(1)
    var widgetHeight by mutableFloatStateOf(2f)

    fun load(context: Context) {
        val p = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs = p
        iconSize = p.getInt("iconSize", 60)
        iconStyle = p.getInt("iconStyle", 0)
        showLabels = p.getBoolean("showLabels", true)
        columns = p.getInt("columns", 4)
        glass = p.getFloat("glass", 1f)
        glassLight = p.getBoolean("glassLight", false)
        reduceEffects = p.getBoolean("reduceEffects", false)
        showWidgets = p.getBoolean("showWidgets", true)
        customStatusBar = p.getBoolean("customStatusBar", true)
        islandEnabled = p.getBoolean("islandEnabled", true)
        islandWidth = p.getInt("islandWidth", 120)
        islandHeight = p.getInt("islandHeight", 34)
        islandTop = p.getInt("islandTop", 10)
        islandMusic = p.getBoolean("islandMusic", true)
        islandNotifications = p.getBoolean("islandNotifications", true)
        islandActivity = p.getBoolean("islandActivity", true)
        islandBluetooth = p.getBoolean("islandBluetooth", false)
        islandLowBattery = p.getBoolean("islandLowBattery", true)
        photoUri = p.getString("photoUri", "") ?: ""
        calWeekSize = p.getInt("calWeekSize", 13)
        calDateSize = p.getInt("calDateSize", 60)
        calMonthSize = p.getInt("calMonthSize", 13)
        calAccent = p.getInt("calAccent", 0)
        clockStyle = p.getInt("clockStyle", 0)
        clockTimeSize = p.getInt("clockTimeSize", 46)
        clockSeconds = p.getBoolean("clockSeconds", true)
        clockAccent = p.getInt("clockAccent", 1)
        widgetHeight = p.getFloat("widgetHeight", 2f)
    }

    fun save() {
        prefs?.edit()
            ?.putInt("iconSize", iconSize)
            ?.putInt("iconStyle", iconStyle)
            ?.putBoolean("showLabels", showLabels)
            ?.putInt("columns", columns)
            ?.putFloat("glass", glass)
            ?.putBoolean("glassLight", glassLight)
            ?.putBoolean("reduceEffects", reduceEffects)
            ?.putBoolean("showWidgets", showWidgets)
            ?.putBoolean("customStatusBar", customStatusBar)
            ?.putBoolean("islandEnabled", islandEnabled)
            ?.putInt("islandWidth", islandWidth)
            ?.putInt("islandHeight", islandHeight)
            ?.putInt("islandTop", islandTop)
            ?.putBoolean("islandMusic", islandMusic)
            ?.putBoolean("islandNotifications", islandNotifications)
            ?.putBoolean("islandActivity", islandActivity)
            ?.putBoolean("islandBluetooth", islandBluetooth)
            ?.putBoolean("islandLowBattery", islandLowBattery)
            ?.putString("photoUri", photoUri)
            ?.putInt("calWeekSize", calWeekSize)
            ?.putInt("calDateSize", calDateSize)
            ?.putInt("calMonthSize", calMonthSize)
            ?.putInt("calAccent", calAccent)
            ?.putInt("clockStyle", clockStyle)
            ?.putInt("clockTimeSize", clockTimeSize)
            ?.putBoolean("clockSeconds", clockSeconds)
            ?.putInt("clockAccent", clockAccent)
            ?.putFloat("widgetHeight", widgetHeight)
            ?.apply()
    }

    fun reset() {
        iconSize = 60
        iconStyle = 0
        showLabels = true
        columns = 4
        glass = 1f
        glassLight = false
        reduceEffects = false
        showWidgets = true
        customStatusBar = true
        islandEnabled = true
        islandWidth = 120
        islandHeight = 34
        islandTop = 10
        islandMusic = true
        islandNotifications = true
        islandActivity = true
        islandBluetooth = false
        islandLowBattery = true
        calWeekSize = 13
        calDateSize = 60
        calMonthSize = 13
        calAccent = 0
        clockStyle = 0
        clockTimeSize = 46
        clockSeconds = true
        clockAccent = 1
        widgetHeight = 2f
        save()
    }
}

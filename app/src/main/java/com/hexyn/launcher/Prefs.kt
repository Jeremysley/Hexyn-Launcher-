package com.hexyn.launcher

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Fixed layout values. */
object Cfg {
    const val HOME_ROWS = 6
    const val MAX_DOCK = 4
}

/**
 * User settings. Every value is Compose state, so the UI updates instantly when you change
 * something in Launcher Settings. Values are saved on the phone.
 */
object S {
    private var prefs: SharedPreferences? = null

    var iconSize by mutableIntStateOf(60)
    var iconStyle by mutableIntStateOf(0) // 0 = squircle, 1 = rounded, 2 = circle
    var showLabels by mutableStateOf(true)
    var columns by mutableIntStateOf(4)
    var glass by mutableFloatStateOf(1f)
    var showWidgets by mutableStateOf(true)
    var customStatusBar by mutableStateOf(true)
    var islandEnabled by mutableStateOf(true)
    var islandWidth by mutableIntStateOf(120)
    var islandHeight by mutableIntStateOf(34)
    var islandTop by mutableIntStateOf(10)
    var islandMusic by mutableStateOf(true)

    fun load(context: Context) {
        val p = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs = p
        iconSize = p.getInt("iconSize", 60)
        iconStyle = p.getInt("iconStyle", 0)
        showLabels = p.getBoolean("showLabels", true)
        columns = p.getInt("columns", 4)
        glass = p.getFloat("glass", 1f)
        showWidgets = p.getBoolean("showWidgets", true)
        customStatusBar = p.getBoolean("customStatusBar", true)
        islandEnabled = p.getBoolean("islandEnabled", true)
        islandWidth = p.getInt("islandWidth", 120)
        islandHeight = p.getInt("islandHeight", 34)
        islandTop = p.getInt("islandTop", 10)
        islandMusic = p.getBoolean("islandMusic", true)
    }

    fun save() {
        prefs?.edit()
            ?.putInt("iconSize", iconSize)
            ?.putInt("iconStyle", iconStyle)
            ?.putBoolean("showLabels", showLabels)
            ?.putInt("columns", columns)
            ?.putFloat("glass", glass)
            ?.putBoolean("showWidgets", showWidgets)
            ?.putBoolean("customStatusBar", customStatusBar)
            ?.putBoolean("islandEnabled", islandEnabled)
            ?.putInt("islandWidth", islandWidth)
            ?.putInt("islandHeight", islandHeight)
            ?.putInt("islandTop", islandTop)
            ?.putBoolean("islandMusic", islandMusic)
            ?.apply()
    }

    fun reset() {
        iconSize = 60
        iconStyle = 0
        showLabels = true
        columns = 4
        glass = 1f
        showWidgets = true
        customStatusBar = true
        islandEnabled = true
        islandWidth = 120
        islandHeight = 34
        islandTop = 10
        islandMusic = true
        save()
    }
}

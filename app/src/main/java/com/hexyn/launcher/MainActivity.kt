package com.hexyn.launcher

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Quick settings you can change to restyle the launcher. */
object Cfg {
    /** Draw our own iOS-style status bar + Dynamic Island and hide the system status bar. */
    const val CUSTOM_STATUS_BAR = true

    const val ISLAND_WIDTH_DP = 120
    const val ISLAND_HEIGHT_DP = 34
    const val ISLAND_TOP_DP = 10

    const val GRID_COLUMNS = 4
    const val HOME_ROWS = 6
    const val MAX_DOCK = 4
}

class MainActivity : ComponentActivity() {
    private var resumeTick by mutableIntStateOf(0)
    private var homeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    surface = Color(0xFF2C2C2E),
                    onSurface = Color.White,
                ),
                shapes = Shapes(extraSmall = RoundedCornerShape(16.dp)),
            ) {
                LauncherScreen(
                    resumeTick = resumeTick,
                    homeTick = homeTick,
                    onBlur = { px -> setBlur(px) },
                )
            }
        }
        applySystemBars()
    }

    override fun onResume() {
        super.onResume()
        resumeTick++ // refresh app list after installs / uninstalls
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        homeTick++ // Home pressed: close overlays, go back to page 1
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applySystemBars()
    }

    private fun applySystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        if (Cfg.CUSTOM_STATUS_BAR) {
            controller.hide(WindowInsetsCompat.Type.statusBars())
        } else {
            controller.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    /** Blurs the wallpaper behind the launcher (Android 12+, when the phone allows it). */
    private fun setBlur(px: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                window.setBackgroundBlurRadius(px)
            } catch (e: Throwable) {
                // Blur not supported on this device: the dark scrim is used instead.
            }
        }
    }
}

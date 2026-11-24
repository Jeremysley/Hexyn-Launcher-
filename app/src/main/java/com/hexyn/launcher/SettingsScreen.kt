package com.hexyn.launcher

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val Green = Color(0xFF34C759)
private val Blue = Color(0xFF0A84FF)

@Composable
fun SettingsScreen(resumeTick: Int, onClose: () -> Unit) {
    val context = LocalContext.current
    val hasAccess = remember(resumeTick) { hasListenerAccess(context) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xF2111114))
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = 56.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Launcher Settings",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Done",
                    color = Blue,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onClose() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Section("Wallpapers") {
                    WallpaperRow()
                }

                Section("Icons") {
                    ChipRow("Shape", listOf("Squircle", "Rounded", "Circle"), S.iconStyle) {
                        S.iconStyle = it
                        S.save()
                    }
                    SliderRow("Icon size", S.iconSize.toFloat(), 48f..68f, "${S.iconSize}") {
                        S.iconSize = it.roundToInt()
                        S.save()
                    }
                    ChipRow("Columns", listOf("4", "5"), S.columns - 4) {
                        S.columns = it + 4
                        S.save()
                    }
                    SwitchRow("Show app names", S.showLabels) {
                        S.showLabels = it
                        S.save()
                    }
                }

                Section("Look") {
                    SliderRow("Glass strength", S.glass, 0.5f..1.6f, "${(S.glass * 100).roundToInt()}%") {
                        S.glass = it
                        S.save()
                    }
                    SwitchRow("Home widgets (calendar and clock)", S.showWidgets) {
                        S.showWidgets = it
                        S.save()
                    }
                }

                Section("Status bar and Dynamic Island") {
                    SwitchRow("iOS-style status bar", S.customStatusBar) {
                        S.customStatusBar = it
                        S.save()
                    }
                    SwitchRow("Dynamic Island", S.islandEnabled) {
                        S.islandEnabled = it
                        S.save()
                    }
                    SwitchRow("Notifications pop up in the island", S.islandNotifications) {
                        S.islandNotifications = it
                        S.save()
                    }
                    SwitchRow("Calls, camera and mic in the island", S.islandActivity) {
                        S.islandActivity = it
                        S.save()
                    }
                    SliderRow("Island width", S.islandWidth.toFloat(), 80f..200f, "${S.islandWidth}") {
                        S.islandWidth = it.roundToInt()
                        S.save()
                    }
                    SliderRow("Island height", S.islandHeight.toFloat(), 24f..46f, "${S.islandHeight}") {
                        S.islandHeight = it.roundToInt()
                        S.save()
                    }
                    SliderRow("Island distance from top", S.islandTop.toFloat(), 0f..40f, "${S.islandTop}") {
                        S.islandTop = it.roundToInt()
                        S.save()
                    }
                }

                Section("Music in the island") {
                    SwitchRow("Show what's playing", S.islandMusic) {
                        S.islandMusic = it
                        S.save()
                    }
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text(
                            text = if (hasAccess) "Notification access: granted" else "Notification access: not granted",
                            color = if (hasAccess) Green else Color(0xFFFF9F0A),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Android only lets an app read the song that's playing if you turn on " +
                                "Notification access for it. Nothing is sent anywhere.",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "If the switch is greyed out: Settings > Apps > Hexyn Launcher > " +
                                "three dots menu > Allow restricted settings, then try again.",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Open notification access",
                            color = Blue,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    startSafely(
                                        context,
                                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                                    )
                                }
                                .padding(vertical = 6.dp),
                        )
                    }
                }

                Section("Reset") {
                    Text(
                        text = "Reset all settings to default",
                        color = Color(0xFFFF453A),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { S.reset() }
                            .padding(vertical = 12.dp),
                    )
                }

                Text(
                    text = "Hexyn Launcher 0.5",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }
    }
}

/**
 * Press and hold a home-screen widget to open this editor. The widget stays visible above it
 * and changes live as you move the sliders. kind: 0 = Calendar, 1 = Clock.
 */
@Composable
fun WidgetEditor(kind: Int, onClose: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { onClose() } }
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color(0xF2111114))
                .pointerInput(Unit) { detectTapGestures { } }
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (kind == 0) "Edit Calendar widget" else "Edit Clock widget",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Done",
                    color = Blue,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onClose() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }

            Column(
                Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (kind == 0) {
                    SliderRow("Day name size", S.calWeekSize.toFloat(), 10f..22f, "${S.calWeekSize}") {
                        S.calWeekSize = it.roundToInt()
                        S.save()
                    }
                    SliderRow("Date number size", S.calDateSize.toFloat(), 36f..88f, "${S.calDateSize}") {
                        S.calDateSize = it.roundToInt()
                        S.save()
                    }
                    SliderRow("Month text size", S.calMonthSize.toFloat(), 10f..22f, "${S.calMonthSize}") {
                        S.calMonthSize = it.roundToInt()
                        S.save()
                    }
                    ChipRow("Accent colour", ACCENT_NAMES, S.calAccent) {
                        S.calAccent = it
                        S.save()
                    }
                } else {
                    ChipRow("Style", listOf("Analog", "Digital"), S.clockStyle) {
                        S.clockStyle = it
                        S.save()
                    }
                    if (S.clockStyle == 1) {
                        SliderRow("Time size", S.clockTimeSize.toFloat(), 28f..60f, "${S.clockTimeSize}") {
                            S.clockTimeSize = it.roundToInt()
                            S.save()
                        }
                    }
                    SwitchRow(if (S.clockStyle == 0) "Second hand" else "Show seconds", S.clockSeconds) {
                        S.clockSeconds = it
                        S.save()
                    }
                    ChipRow("Accent colour", ACCENT_NAMES, S.clockAccent) {
                        S.clockAccent = it
                        S.save()
                    }
                }
                SliderRow(
                    "Widget height",
                    S.widgetHeight,
                    1.6f..2.4f,
                    String.format("%.1f", S.widgetHeight),
                ) {
                    S.widgetHeight = it
                    S.save()
                }
            }
        }
    }
}

/** Tap a swatch to set it as both the home and lock screen wallpaper. */
@Composable
private fun WallpaperRow() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }

    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = "Home and lock screen wallpaper",
            color = Color.White,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WALLPAPER_PRESETS.forEach { preset ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(width = 66.dp, height = 116.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(preset.bg1),
                                        Color(preset.blob1),
                                        Color(preset.blob2),
                                    )
                                )
                            )
                            .clickable {
                                status = "Applying ${preset.name}..."
                                scope.launch {
                                    val ok = applyWallpaper(context, preset)
                                    status = if (ok) "Applied: ${preset.name}" else "Could not set wallpaper"
                                }
                            }
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = preset.name,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                    )
                }
            }
        }
        if (status.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(text = status, color = Green, fontSize = 13.sp)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.09f))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            content = content,
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Green,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(alpha = 0.25f),
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(text = label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text(text = valueText, color = Color.White.copy(alpha = 0.6f), fontSize = 15.sp)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Green,
                inactiveTrackColor = Color.White.copy(alpha = 0.25f),
            ),
        )
    }
}

@Composable
private fun ChipRow(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = label, color = Color.White, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .padding(3.dp)
        ) {
            options.forEachIndexed { i, name ->
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (i == selected) Color.White.copy(alpha = 0.92f) else Color.Transparent)
                        .clickable { onSelect(i) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = name,
                        color = if (i == selected) Color.Black else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

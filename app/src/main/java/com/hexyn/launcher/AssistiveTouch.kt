package com.hexyn.launcher

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private class AtItem(val label: String, val icon: ImageVector, val action: () -> Unit)

/**
 * A floating button you can drag anywhere. Tap it for a small menu of shortcuts,
 * like iOS AssistiveTouch. Turn it on in Settings.
 */
@Composable
fun AssistiveTouch(
    onNotifications: () -> Unit,
    onControl: () -> Unit,
    onSwitcher: () -> Unit,
    onSearch: () -> Unit,
    onHome: () -> Unit,
    onSettings: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var dx by remember { mutableFloatStateOf(0f) }
    var dy by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxX = with(density) { (maxWidth - 70.dp).toPx() }
        val maxY = with(density) { (maxHeight / 2 - 90.dp).toPx() }

        if (!menu) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .offset { IntOffset(dx.roundToInt(), dy.roundToInt()) }
                    .padding(end = 6.dp)
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            dx = (dx + drag.x).coerceIn(-maxX, 0f)
                            dy = (dy + drag.y).coerceIn(-maxY, maxY)
                        }
                    }
                    .pointerInput(Unit) { detectTapGestures { menu = true } },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(20.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)))
                }
            }
        } else {
            val items = listOf(
                AtItem("Notifications", Icons.Rounded.Notifications, onNotifications),
                AtItem("Control", Icons.Rounded.Tune, onControl),
                AtItem("Recents", Icons.Rounded.Apps, onSwitcher),
                AtItem("Search", Icons.Rounded.Search, onSearch),
                AtItem("Home", Icons.Rounded.Home, onHome),
                AtItem("Settings", Icons.Rounded.Settings, onSettings),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .pointerInput(Unit) { detectTapGestures { menu = false } }
            )
            Column(
                Modifier
                    .align(Alignment.Center)
                    .width(270.dp)
                    .glass(RoundedCornerShape(36.dp), 1.2f)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { item ->
                            Column(
                                Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        menu = false
                                        item.action()
                                    }
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(item.icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
                                }
                                Text(
                                    text = item.label,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 5.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

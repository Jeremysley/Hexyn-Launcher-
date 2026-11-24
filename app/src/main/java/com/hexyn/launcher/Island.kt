package com.hexyn.launcher

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.min

// ------------------------------------------------------------------
// Status bar
// ------------------------------------------------------------------

private fun networkKind(context: Context): String {
    return try {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        when {
            caps == null -> "none"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cell"
            else -> "none"
        }
    } catch (e: Exception) {
        "none"
    }
}

@Composable
fun IosStatusBar(battery: BatteryInfo) {
    val context = LocalContext.current
    val is24 = remember { android.text.format.DateFormat.is24HourFormat(context) }
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(1_000)
        }
    }
    val net by produceState("none") {
        while (true) {
            value = networkKind(context)
            delay(4_000)
        }
    }
    val timeText = now.format(DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm"))

    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = S.islandTop.dp)
            .height(S.islandHeight.dp)
            .padding(horizontal = 30.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = timeText,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            style = ShadowText,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (net == "wifi") {
                Icon(Icons.Rounded.Wifi, null, tint = Color.White, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
            } else if (net == "cell") {
                Icon(
                    Icons.Rounded.SignalCellularAlt,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            BatteryGlyph(battery)
        }
    }
}

@Composable
fun BatteryGlyph(battery: BatteryInfo) {
    val fill = when {
        battery.charging -> Color(0xFF34C759)
        battery.level <= 20 -> Color(0xFFFF3B30)
        else -> Color.White
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "${battery.level}",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            style = ShadowText,
        )
        Spacer(Modifier.width(4.dp))
        Box(
            Modifier
                .size(width = 25.dp, height = 12.dp)
                .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                .padding(1.5.dp)
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(battery.level.coerceIn(0, 100) / 100f)
                    .background(fill, RoundedCornerShape(2.5.dp))
            )
        }
        Spacer(Modifier.width(1.dp))
        Box(
            Modifier
                .size(width = 1.5.dp, height = 4.dp)
                .background(
                    Color.White.copy(alpha = 0.45f),
                    RoundedCornerShape(topEnd = 1.dp, bottomEnd = 1.dp),
                )
        )
    }
}

// ------------------------------------------------------------------
// Dynamic Island: idle pill, music (compact + full player), charging
// ------------------------------------------------------------------

@Composable
fun DynamicIsland(battery: BatteryInfo, media: MediaState?) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    var tapped by remember { mutableStateOf(false) }
    var chargeFlash by remember { mutableStateOf(false) }
    var wasCharging by remember { mutableStateOf(battery.charging) }

    LaunchedEffect(battery.charging) {
        if (battery.charging && !wasCharging) {
            chargeFlash = true
            delay(3_200)
            chargeFlash = false
        }
        wasCharging = battery.charging
    }
    LaunchedEffect(tapped) {
        if (tapped) {
            delay(8_000)
            tapped = false
        }
    }

    val showPlayer = tapped && media != null
    val showBattery = (tapped && media == null) || chargeFlash
    val compactMusic = !tapped && !chargeFlash && media != null && media.playing

    val targetWidth: Dp = when {
        showPlayer -> min(screenWidth - 24, 360).dp
        showBattery -> min(screenWidth - 60, 310).dp
        compactMusic -> (S.islandWidth + 76).dp
        else -> S.islandWidth.dp
    }
    val targetHeight: Dp = when {
        showPlayer -> 150.dp
        showBattery -> 76.dp
        else -> S.islandHeight.dp
    }

    val width by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow),
        label = "islandWidth",
    )
    val height by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow),
        label = "islandHeight",
    )
    val corner = if (showPlayer) 38.dp else height / 2

    Box(
        Modifier.fillMaxWidth().padding(top = S.islandTop.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            Modifier
                .size(width, height)
                .clip(RoundedCornerShape(corner))
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { tapped = !tapped },
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = compactMusic,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(100)),
            ) {
                if (media != null) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AlbumArt(media, 22.dp, 6.dp)
                        EqBars(playing = true)
                    }
                }
            }

            AnimatedVisibility(
                visible = showPlayer,
                enter = fadeIn(tween(250, delayMillis = 120)),
                exit = fadeOut(tween(100)),
            ) {
                if (media != null) PlayerContent(media)
            }

            AnimatedVisibility(
                visible = showBattery,
                enter = fadeIn(tween(250, delayMillis = 120)),
                exit = fadeOut(tween(100)),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 26.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (battery.charging) "Charging" else "Battery",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${battery.level}%",
                        color = if (battery.charging) Color(0xFF34C759) else Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumArt(media: MediaState, size: Dp, radius: Dp) {
    val shape = RoundedCornerShape(radius)
    if (media.art != null) {
        Image(
            bitmap = media.art,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(shape),
        )
    } else {
        Box(
            Modifier
                .size(size)
                .clip(shape)
                .background(
                    Brush.verticalGradient(listOf(Color(0xFFFF5E7E), Color(0xFF8E54E9)))
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.6f),
            )
        }
    }
}

@Composable
private fun PlayerContent(media: MediaState) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AlbumArt(media, 56.dp, 12.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = media.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (media.artist.isNotBlank()) {
                    Text(
                        text = media.artist,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            EqBars(playing = media.playing)
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ControlIcon(Icons.Rounded.SkipPrevious) {
                media.controller.transportControls.skipToPrevious()
            }
            ControlIcon(if (media.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow) {
                if (media.playing) {
                    media.controller.transportControls.pause()
                } else {
                    media.controller.transportControls.play()
                }
            }
            ControlIcon(Icons.Rounded.SkipNext) {
                media.controller.transportControls.skipToNext()
            }
        }
    }
}

@Composable
private fun ControlIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(32.dp))
    }
}

/** The little bouncing bars you see on the iOS island while music plays. */
@Composable
private fun EqBars(playing: Boolean, color: Color = Color(0xFFFF6B9A)) {
    val transition = rememberInfiniteTransition(label = "eq")
    val durations = listOf(420, 560, 380, 500)
    Row(
        Modifier.height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        durations.forEach { d ->
            val h by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(d, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "bar$d",
            )
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight(if (playing) h else 0.25f)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
    }
}

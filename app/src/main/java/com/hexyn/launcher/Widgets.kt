package com.hexyn.launcher

import androidx.compose.foundation.Canvas
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Two square glass widgets: Calendar and Clock.
 * Press and hold either one to edit its size, text size and colour.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WidgetsRow(
    modifier: Modifier = Modifier,
    onEditCalendar: () -> Unit,
    onEditClock: () -> Unit,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        val calendarModifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .glass(RoundedCornerShape(30.dp))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = onEditCalendar,
            )
        CalendarWidget(calendarModifier)

        WidgetStack(Modifier.weight(1f).fillMaxHeight(), onEditClock)
    }
}

@Composable
fun CalendarWidget(modifier: Modifier = Modifier) {
    val today by produceState(LocalDate.now()) {
        while (true) {
            value = LocalDate.now()
            delay(30_000)
        }
    }
    Column(
        modifier.padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase(),
            color = accentColor(S.calAccent),
            fontSize = S.calWeekSize.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Text(
            text = today.dayOfMonth.toString(),
            color = Color.White,
            fontSize = S.calDateSize.sp,
            fontWeight = FontWeight.Light,
            maxLines = 1,
        )
        Text(
            text = today.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
            color = Color.White.copy(alpha = 0.75f),
            fontSize = S.calMonthSize.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
fun ClockWidget(modifier: Modifier = Modifier) {
    if (S.clockStyle == 0) AnalogClockWidget(modifier) else DigitalClockWidget(modifier)
}

@Composable
fun DigitalClockWidget(modifier: Modifier = Modifier) {
    val time by produceState(LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(1_000)
        }
    }
    Column(
        modifier.padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = time.format(DateTimeFormatter.ofPattern("HH:mm")),
            color = Color.White,
            fontSize = S.clockTimeSize.sp,
            fontWeight = FontWeight.Light,
            maxLines = 1,
            softWrap = false,
        )
        if (S.clockSeconds) {
            Text(
                text = time.format(DateTimeFormatter.ofPattern("ss")),
                color = accentColor(S.clockAccent),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun AnalogClockWidget(modifier: Modifier = Modifier) {
    val time by produceState(LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(1_000)
        }
    }
    val hourAngle = ((time.hour % 12) + time.minute / 60f) * 30f
    val minuteAngle = (time.minute + time.second / 60f) * 6f
    val secondAngle = time.second * 6f
    val accent = accentColor(S.clockAccent)
    val showSeconds = S.clockSeconds

    Canvas(modifier.padding(12.dp).fillMaxSize()) {
        val r = size.minDimension / 2f
        val c = center

        drawCircle(Color.Black.copy(alpha = 0.55f), radius = r, center = c)

        for (i in 0 until 60) {
            val major = i % 5 == 0
            val ang = Math.toRadians(i * 6.0 - 90.0)
            val outer = r * 0.93f
            val inner = if (major) r * 0.80f else r * 0.88f
            drawLine(
                color = Color.White.copy(alpha = if (major) 0.95f else 0.4f),
                start = Offset(
                    c.x + (cos(ang) * inner).toFloat(),
                    c.y + (sin(ang) * inner).toFloat(),
                ),
                end = Offset(
                    c.x + (cos(ang) * outer).toFloat(),
                    c.y + (sin(ang) * outer).toFloat(),
                ),
                strokeWidth = if (major) 3f else 1.5f,
                cap = StrokeCap.Round,
            )
        }

        fun hand(deg: Float, length: Float, width: Float, color: Color) {
            val ang = Math.toRadians((deg - 90f).toDouble())
            drawLine(
                color = color,
                start = c,
                end = Offset(
                    c.x + (cos(ang) * r * length).toFloat(),
                    c.y + (sin(ang) * r * length).toFloat(),
                ),
                strokeWidth = width,
                cap = StrokeCap.Round,
            )
        }

        hand(hourAngle, 0.5f, 7f, Color.White)
        hand(minuteAngle, 0.74f, 5f, Color.White)
        if (showSeconds) hand(secondAngle, 0.80f, 2.5f, accent)
        drawCircle(accent, radius = 5f, center = c)
    }
}

/** A swipeable stack of widgets: Clock, Battery, Music, Photo. Swipe sideways on it to change. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WidgetStack(modifier: Modifier, onEditClock: () -> Unit) {
    val context = LocalContext.current
    val pager = rememberPagerState(pageCount = { 4 })
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                // Permission could not be kept; the photo may not load next time.
            }
            S.photoUri = uri.toString()
            S.save()
        }
    }
    Box(modifier.glass(RoundedCornerShape(30.dp))) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> ClockWidget(
                    Modifier
                        .fillMaxSize()
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                            onLongClick = onEditClock,
                        )
                )
                1 -> BatteryWidget(Modifier.fillMaxSize())
                2 -> MusicWidget(Modifier.fillMaxSize())
                else -> PhotoWidget(Modifier.fillMaxSize()) { picker.launch(arrayOf("image/*")) }
            }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            repeat(4) { i ->
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (pager.currentPage == i) 0.9f else 0.35f))
                )
            }
        }
    }
}

@Composable
fun BatteryWidget(modifier: Modifier = Modifier) {
    val battery = rememberBattery()
    val ring = when {
        battery.charging -> Color(0xFF34C759)
        battery.level <= 20 -> Color(0xFFFF453A)
        else -> Color.White
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize(0.7f)) {
            val stroke = 9.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = Color.White.copy(alpha = 0.2f),
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = ring,
                startAngle = -90f, sweepAngle = 360f * battery.level.coerceIn(0, 100) / 100f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${battery.level}%", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                if (battery.charging) "Charging" else "Battery",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
fun MusicWidget(modifier: Modifier = Modifier) {
    val media = rememberNowPlaying(true, 0)
    Column(
        modifier.padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (media == null) {
            Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(34.dp))
            Text("Nothing playing", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        } else {
            val art = media.art
            if (art != null) {
                Image(
                    bitmap = art,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)),
                )
            } else {
                Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Text(
                media.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                media.artist, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Row(
                Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (media.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    null, tint = Color.White,
                    modifier = Modifier.size(30.dp).clickable {
                        if (media.playing) media.controller.transportControls.pause()
                        else media.controller.transportControls.play()
                    },
                )
                Icon(
                    Icons.Rounded.SkipNext, null, tint = Color.White,
                    modifier = Modifier.size(28.dp).clickable { media.controller.transportControls.skipToNext() },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoWidget(modifier: Modifier = Modifier, onPick: () -> Unit) {
    val context = LocalContext.current
    val uri = S.photoUri
    val photo by produceState<ImageBitmap?>(null, uri) {
        value = if (uri.isBlank()) null else withContext(Dispatchers.IO) { loadPhoto(context, uri) }
    }
    Box(
        modifier
            .clip(RoundedCornerShape(30.dp))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = onPick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val p = photo
        if (p != null) {
            Image(
                bitmap = p,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                "Hold to choose a photo",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

private fun loadPhoto(context: android.content.Context, uriString: String): ImageBitmap? {
    return try {
        val uri = android.net.Uri.parse(uriString)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > 900 || bounds.outHeight / sample > 900) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

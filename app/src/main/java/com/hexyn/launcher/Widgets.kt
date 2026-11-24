package com.hexyn.launcher

import androidx.compose.foundation.Canvas
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

        val clockModifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .glass(RoundedCornerShape(30.dp))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = onEditClock,
            )
        ClockWidget(clockModifier)
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

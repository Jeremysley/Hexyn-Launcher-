package com.hexyn.launcher

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pull down on the LEFT side of the home screen. A full-screen layer (the home screen is
 * hidden behind it): lock-screen style clock, then your notifications grouped by app.
 * Swipe a card sideways to clear it, long-press to expand its text, tap to open it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotificationCenter(resumeTick: Int, onClose: () -> Unit) {
    val context = LocalContext.current
    val hasAccess = remember(resumeTick) { hasListenerAccess(context) }
    val list = NotificationStore.items.filter { !it.ongoing }
    val groups = list.groupBy { it.packageName }.values.toList()
    val expanded = remember { mutableStateListOf<String>() }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    val timeText = remember(now / 60_000L) { SimpleDateFormat("h:mm", Locale.getDefault()).format(Date(now)) }
    val dateText = remember(now / 60_000L) { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(now)) }

    val openItem: (NotifItem) -> Unit = { n ->
        val pi = n.intent
        if (pi == null) {
            openPackage(context, n.packageName)
        } else {
            try {
                pi.send()
            } catch (e: Exception) {
                // The notification's own action is gone: just open the app.
                openPackage(context, n.packageName)
            }
        }
        onClose()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .pointerInput(Unit) { detectTapGestures { onClose() } }
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total < -120f) onClose() },
                    onDragCancel = {},
                    onVerticalDrag = { _, dy -> total += dy },
                )
            }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 50.dp, start = 16.dp, end = 16.dp)
        ) {
            // Lock-screen style header
            Column(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = dateText,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = timeText,
                    color = Color.White,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Notifications",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (list.isNotEmpty()) {
                    Text(
                        text = "Clear all",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .glass(RoundedCornerShape(50), 0.9f)
                            .clickable { NotificationStore.clearAll() }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    )
                }
            }

            if (!hasAccess) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .glass(RoundedCornerShape(24.dp))
                        .pointerInput(Unit) { detectTapGestures { } }
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Turn on notification access",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Android needs your OK once before this launcher can show your " +
                            "notifications. They stay on your phone.",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Open notification access",
                        color = Color(0xFF64B5FF),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                startSafely(
                                    context,
                                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                                )
                                onClose()
                            }
                            .padding(vertical = 4.dp),
                    )
                }
            } else if (list.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(44.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "No notifications",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 16.sp,
                        )
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (group in groups) {
                        val pkg = group.first().packageName
                        val isOpen = pkg in expanded
                        if (group.size == 1 || isOpen) {
                            items(group, key = { it.key }) { n ->
                                NotificationCard(
                                    item = n,
                                    modifier = Modifier.animateItem(),
                                    onOpen = { openItem(n) },
                                    onDismiss = { NotificationStore.dismiss(n.key) },
                                )
                            }
                            if (group.size > 1) {
                                item(key = "less_$pkg") {
                                    Text(
                                        text = "Show less",
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier
                                            .animateItem()
                                            .fillMaxWidth()
                                            .clickable { expanded.remove(pkg) }
                                            .padding(vertical = 4.dp),
                                    )
                                }
                            }
                        } else {
                            item(key = "group_$pkg") {
                                Column(Modifier.animateItem()) {
                                    NotificationCard(
                                        item = group.first(),
                                        modifier = Modifier,
                                        onOpen = { openItem(group.first()) },
                                        onDismiss = { group.forEach { NotificationStore.dismiss(it.key) } },
                                    )
                                    // peeking stack under the newest card
                                    Box(
                                        Modifier
                                            .padding(horizontal = 14.dp)
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .glass(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp), 0.8f)
                                    )
                                    Text(
                                        text = "${group.size - 1} more from ${group.first().appName}",
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { expanded.add(pkg) }
                                            .padding(vertical = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotificationCard(
    item: NotifItem,
    modifier: Modifier,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    var expandedText by remember { mutableStateOf(false) }

    Row(
        modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = offset.value
                alpha = 1f - (kotlin.math.abs(offset.value) / 700f).coerceIn(0f, 1f)
            }
            .glass(RoundedCornerShape(24.dp))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (kotlin.math.abs(offset.value) > 220f) {
                                val dir = if (offset.value > 0f) 1f else -1f
                                offset.animateTo(dir * 1000f, tween(160))
                                onDismiss()
                            } else {
                                offset.animateTo(0f, spring())
                            }
                        }
                    },
                    onDragCancel = { scope.launch { offset.animateTo(0f, spring()) } },
                    onHorizontalDrag = { change, dx ->
                        change.consume()
                        scope.launch { offset.snapTo(offset.value + dx) }
                    },
                )
            }
            .combinedClickable(
                onClick = onOpen,
                onLongClick = { expandedText = !expandedText },
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        val icon = item.icon
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(iconShape()),
            )
        } else {
            Box(
                Modifier.size(40.dp).clip(iconShape()).background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Notifications, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.appName.uppercase(),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = timeAgo(item.whenMs),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                )
            }
            if (item.title.isNotBlank()) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (item.text.isNotBlank()) {
                Text(
                    text = item.text,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    maxLines = if (expandedText) 12 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

private fun timeAgo(whenMs: Long): String {
    val minutes = (System.currentTimeMillis() - whenMs) / 60_000L
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 24 * 60 -> "${minutes / 60}h ago"
        else -> "${minutes / (24 * 60)}d ago"
    }
}

package com.hexyn.launcher

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RecentApp(val app: AppItem, val lastUsed: Long)

/** Remembers when you swiped an app away, so it only comes back after you use it again. */
object SwitcherState {
    val dismissedAt = HashMap<String, Long>()
}

fun hasUsageAccess(context: Context): Boolean {
    return try {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName,
            )
        }
        mode == AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) {
        false
    }
}

/** Apps you used in the last 24 hours, newest first. Needs Usage access. */
fun loadRecents(context: Context, apps: List<AppItem>): List<RecentApp> {
    val usm = context.getSystemService(UsageStatsManager::class.java) ?: return emptyList()
    val end = System.currentTimeMillis()
    val events = usm.queryEvents(end - 24L * 60L * 60L * 1000L, end) ?: return emptyList()
    val last = HashMap<String, Long>()
    val e = UsageEvents.Event()
    while (events.hasNextEvent()) {
        events.getNextEvent(e)
        @Suppress("DEPRECATION")
        val foreground = if (Build.VERSION.SDK_INT >= 29) {
            e.eventType == UsageEvents.Event.ACTIVITY_RESUMED
        } else {
            e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
        }
        if (foreground) last[e.packageName] = e.timeStamp
    }
    val byPackage = apps.associateBy { it.packageName }
    return last.entries
        .sortedByDescending { it.value }
        .mapNotNull { entry -> byPackage[entry.key]?.let { RecentApp(it, entry.value) } }
        .filter { it.app.packageName != context.packageName }
        .filter { it.lastUsed > (SwitcherState.dismissedAt[it.app.packageName] ?: 0L) }
        .take(12)
}

private fun closeApp(context: Context, packageName: String) {
    SwitcherState.dismissedAt[packageName] = System.currentTimeMillis()
    try {
        context.getSystemService(ActivityManager::class.java).killBackgroundProcesses(packageName)
    } catch (e: Exception) {
        // Android refused; the card is still removed from the list.
    }
}

/**
 * iOS-style App Switcher. Swipe sideways through cards, tap one to open it,
 * swipe a card up to close it. (Android does not let a launcher take live screenshots
 * of other apps, so each card shows the app icon.)
 */
@Composable
fun SwitcherOverlay(apps: List<AppItem>, resumeTick: Int, onClose: () -> Unit) {
    val context = LocalContext.current
    val hasAccess = remember(resumeTick) { hasUsageAccess(context) }
    val recents = remember { mutableStateListOf<RecentApp>() }

    LaunchedEffect(hasAccess, apps) {
        if (hasAccess) {
            val list = withContext(Dispatchers.IO) { loadRecents(context, apps) }
            recents.clear()
            recents.addAll(list)
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .pointerInput(Unit) { detectTapGestures { onClose() } }
    ) {
        val cardW: Dp = maxWidth * 0.62f
        val cardH: Dp = maxHeight * 0.55f
        val sidePad: Dp = (maxWidth - cardW) / 2

        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(top = 56.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recent apps",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (recents.isNotEmpty()) {
                    Text(
                        text = "Close all",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .glass(RoundedCornerShape(50), 0.9f)
                            .clickable {
                                recents.toList().forEach { closeApp(context, it.app.packageName) }
                                recents.clear()
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    )
                }
            }

            if (!hasAccess) {
                Column(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .glass(RoundedCornerShape(24.dp))
                        .pointerInput(Unit) { detectTapGestures { } }
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Turn on usage access",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Android needs your OK once before the launcher can see which apps " +
                            "you used recently. It stays on your phone.",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Open usage access",
                        color = Color(0xFF64B5FF),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                startSafely(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                onClose()
                            }
                            .padding(vertical = 4.dp),
                    )
                }
            } else if (recents.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No recent apps", color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp)
                }
            } else {
                val listState = rememberLazyListState()
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    LazyRow(
                        state = listState,
                        flingBehavior = rememberSnapFlingBehavior(listState),
                        contentPadding = PaddingValues(horizontal = sidePad),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(recents, key = { it.app.packageName }) { r ->
                            SwitcherCard(
                                app = r.app,
                                width = cardW,
                                height = cardH,
                                onOpen = {
                                    openPackage(context, r.app.packageName)
                                    onClose()
                                },
                                onDismiss = {
                                    closeApp(context, r.app.packageName)
                                    recents.remove(r)
                                },
                            )
                        }
                    }
                }
                Text(
                    text = "Swipe a card up to close it",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
                )
            }
        }
    }
}

@Composable
private fun SwitcherCard(
    app: AppItem,
    width: Dp,
    height: Dp,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    Column(
        Modifier
            .width(width)
            .height(height)
            .graphicsLayer {
                translationY = offsetY.value
                alpha = 1f - (-offsetY.value / 900f).coerceIn(0f, 1f)
            }
            .glass(RoundedCornerShape(32.dp), 1.2f)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (offsetY.value < -260f) {
                                offsetY.animateTo(-1400f, tween(180))
                                onDismiss()
                            } else {
                                offsetY.animateTo(0f, spring())
                            }
                        }
                    },
                    onDragCancel = { scope.launch { offsetY.animateTo(0f, spring()) } },
                    onVerticalDrag = { change, dy ->
                        change.consume()
                        scope.launch { offsetY.snapTo((offsetY.value + dy).coerceAtMost(0f)) }
                    },
                )
            }
            .clickable(onClick = onOpen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            modifier = Modifier.size(104.dp).clip(iconShape()),
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = app.label,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 14.dp),
        )
    }
}

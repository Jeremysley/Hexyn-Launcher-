package com.hexyn.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val IconShape = RoundedCornerShape(22)

val ShadowText = TextStyle(
    shadow = Shadow(
        color = Color.Black.copy(alpha = 0.45f),
        offset = Offset(0f, 1.5f),
        blurRadius = 5f,
    )
)

// ------------------------------------------------------------------
// Root screen: home pages + overlays (Control Center, Search, Folders)
// ------------------------------------------------------------------

@Composable
fun LauncherScreen(resumeTick: Int, homeTick: Int, onBlur: (Int) -> Unit) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf(emptyList<AppItem>()) }
    var pinned by remember { mutableStateOf(loadPinned(context)) }
    var controlOpen by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var folder by remember { mutableStateOf<String?>(null) }
    val battery = rememberBattery()

    LaunchedEffect(resumeTick) {
        apps = withContext(Dispatchers.Default) { loadApps(context) }
    }

    val homeApps = remember(apps, pinned) { apps.filter { it.packageName !in pinned } }
    val dockApps = remember(apps, pinned) {
        pinned.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }
    }
    val pages = remember(homeApps) { paginate(homeApps) }
    val pagerState = rememberPagerState(pageCount = { pages.size + 1 })

    LaunchedEffect(homeTick) {
        controlOpen = false
        searchOpen = false
        folder = null
        if (homeTick > 0) pagerState.animateScrollToPage(0)
    }

    val overlayOpen = controlOpen || searchOpen || folder != null
    BackHandler(enabled = overlayOpen) {
        if (folder != null) {
            folder = null
        } else {
            controlOpen = false
            searchOpen = false
        }
    }

    val blur by animateFloatAsState(
        targetValue = if (overlayOpen) 1f else 0f,
        animationSpec = tween(300),
        label = "blur",
    )
    LaunchedEffect(blur) { onBlur((blur * 80f).toInt()) }

    val togglePin: (String) -> Unit = { pkg ->
        val updated = when {
            pkg in pinned -> pinned - pkg
            pinned.size < Cfg.MAX_DOCK -> pinned + pkg
            else -> pinned
        }
        pinned = updated
        savePinned(context, updated)
    }

    Box(Modifier.fillMaxSize()) {
        HomeLayer(
            pages = pages,
            dockApps = dockApps,
            pinned = pinned,
            allApps = apps,
            pagerState = pagerState,
            gesturesEnabled = !overlayOpen,
            onTogglePin = togglePin,
            onOpenSearch = { searchOpen = true },
            onOpenControl = { controlOpen = true },
            onOpenFolder = { folder = it },
        )

        if (blur > 0.01f) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.22f * blur)))
        }

        AnimatedVisibility(
            visible = controlOpen,
            enter = fadeIn(tween(200)) + slideInVertically(tween(300)) { -it / 8 },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(200)) { -it / 8 },
        ) {
            ControlCenter(onClose = { controlOpen = false })
        }

        AnimatedVisibility(
            visible = searchOpen,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150)),
        ) {
            SpotlightOverlay(apps = apps, onClose = { searchOpen = false })
        }

        val openFolder = folder
        if (openFolder != null) {
            FolderOverlay(
                title = openFolder,
                apps = apps.filter { it.category == openFolder },
                pinned = pinned,
                onTogglePin = togglePin,
                onClose = { folder = null },
            )
        }

        if (Cfg.CUSTOM_STATUS_BAR) {
            IosStatusBar(battery)
            DynamicIsland(battery)
        }
    }
}

@Composable
fun HomeLayer(
    pages: List<List<AppItem>>,
    dockApps: List<AppItem>,
    pinned: List<String>,
    allApps: List<AppItem>,
    pagerState: PagerState,
    gesturesEnabled: Boolean,
    onTogglePin: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenControl: () -> Unit,
    onOpenFolder: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(gesturesEnabled) {
                var total = 0f
                var startX = 0f
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        total = 0f
                        startX = offset.x
                    },
                    onDragEnd = {
                        if (gesturesEnabled && total > 140f) {
                            if (startX > size.width * 0.55f) onOpenControl() else onOpenSearch()
                        }
                    },
                    onDragCancel = {},
                    onVerticalDrag = { _, dy -> total += dy },
                )
            }
    ) {
        Spacer(Modifier.height(if (Cfg.CUSTOM_STATUS_BAR) 62.dp else 12.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page ->
            if (page < pages.size) {
                AppGridPage(
                    apps = pages[page],
                    showWidget = page == 0,
                    pinned = pinned,
                    onTogglePin = onTogglePin,
                )
            } else {
                AppLibraryPage(
                    apps = allApps,
                    onSearch = onOpenSearch,
                    onOpenFolder = onOpenFolder,
                )
            }
        }

        PageDots(current = pagerState.currentPage, count = pagerState.pageCount)
        Spacer(Modifier.height(10.dp))
        SearchPill(onClick = onOpenSearch)
        Spacer(Modifier.height(12.dp))
        Dock(dockApps = dockApps, onTogglePin = onTogglePin)
        Spacer(Modifier.height(10.dp))
    }
}

// ------------------------------------------------------------------
// Home pages
// ------------------------------------------------------------------

@Composable
fun AppGridPage(
    apps: List<AppItem>,
    showWidget: Boolean,
    pinned: List<String>,
    onTogglePin: (String) -> Unit,
) {
    val appRows = if (showWidget) Cfg.HOME_ROWS - 2 else Cfg.HOME_ROWS
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        if (showWidget) {
            ClockWidget(Modifier.fillMaxWidth().weight(2f).padding(bottom = 14.dp))
        }
        for (r in 0 until appRows) {
            Row(Modifier.fillMaxWidth().weight(1f)) {
                for (c in 0 until Cfg.GRID_COLUMNS) {
                    val app = apps.getOrNull(r * Cfg.GRID_COLUMNS + c)
                    Box(
                        Modifier.weight(1f).fillMaxHeight().padding(top = 4.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        if (app != null) {
                            AppIcon(
                                app = app,
                                pinned = false,
                                canPin = pinned.size < Cfg.MAX_DOCK,
                                onTogglePin = { onTogglePin(app.packageName) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClockWidget(modifier: Modifier = Modifier) {
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(5_000)
        }
    }
    Row(
        modifier
            .glass(RoundedCornerShape(28.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = now.format(DateTimeFormatter.ofPattern("EEEE")).uppercase(),
                color = Color(0xFFFF6B6B),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = now.dayOfMonth.toString(),
                color = Color.White,
                fontSize = 52.sp,
                fontWeight = FontWeight.Light,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = now.format(DateTimeFormatter.ofPattern("HH:mm")),
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Light,
            )
            Text(
                text = now.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
fun PageDots(current: Int, count: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(count) { i ->
            Box(
                Modifier
                    .padding(horizontal = 3.5.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (i == current) Color.White else Color.White.copy(alpha = 0.35f))
            )
        }
    }
}

@Composable
fun SearchPill(onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .glass(RoundedCornerShape(50), 0.9f)
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Search",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun Dock(dockApps: List<AppItem>, onTogglePin: (String) -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 14.dp)
            .fillMaxWidth()
            .glass(RoundedCornerShape(34.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .heightIn(min = 56.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dockApps.isEmpty()) {
            Text(
                text = "Long-press an app to add it to the Dock",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
            )
        }
        dockApps.forEach { app ->
            AppIcon(
                app = app,
                pinned = true,
                canPin = true,
                showLabel = false,
                onTogglePin = { onTogglePin(app.packageName) },
            )
        }
    }
}

// ------------------------------------------------------------------
// App icon with press animation, zoom-launch and long-press menu
// ------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(
    app: AppItem,
    pinned: Boolean,
    canPin: Boolean,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 56.dp,
    showLabel: Boolean = true,
    onLaunched: () -> Unit = {},
) {
    val context = LocalContext.current
    val view = LocalView.current
    val haptics = LocalHapticFeedback.current
    var menuOpen by remember { mutableStateOf(false) }
    var bounds by remember { mutableStateOf<Rect?>(null) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "press",
    )

    Box(modifier, contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = {
                        launchApp(context, view, app.packageName, bounds)
                        onLaunched()
                    },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    },
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(iconSize)
                    .onGloballyPositioned { bounds = it.boundsInWindow() }
                    .shadow(5.dp, IconShape, clip = false)
                    .clip(IconShape)
                    .border(
                        0.6.dp,
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0.04f))
                        ),
                        IconShape,
                    )
            ) {
                Image(
                    bitmap = app.icon,
                    contentDescription = app.label,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (showLabel) {
                Text(
                    text = app.label,
                    color = Color.White,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    style = ShadowText,
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .widthIn(max = iconSize + 22.dp),
                )
            }
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (pinned) "Remove from Dock" else "Add to Dock",
                        color = Color.White,
                    )
                },
                enabled = pinned || canPin,
                onClick = {
                    menuOpen = false
                    onTogglePin()
                },
            )
            DropdownMenuItem(
                text = { Text("App info", color = Color.White) },
                onClick = {
                    menuOpen = false
                    startSafely(
                        context,
                        Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.parse("package:${app.packageName}"),
                        ),
                    )
                },
            )
            DropdownMenuItem(
                text = { Text("Remove app", color = Color(0xFFFF6B6B)) },
                onClick = {
                    menuOpen = false
                    startSafely(
                        context,
                        Intent(
                            Intent.ACTION_DELETE,
                            android.net.Uri.parse("package:${app.packageName}"),
                        ),
                    )
                },
            )
        }
    }
}

// ------------------------------------------------------------------
// Status bar + Dynamic Island
// ------------------------------------------------------------------

data class BatteryInfo(val level: Int, val charging: Boolean)

private fun readBattery(intent: Intent?): BatteryInfo {
    if (intent == null) return BatteryInfo(100, false)
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    return BatteryInfo(if (scale > 0) level * 100 / scale else level, charging)
}

@Composable
fun rememberBattery(): BatteryInfo {
    val context = LocalContext.current
    var info by remember {
        mutableStateOf(
            readBattery(context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))
        )
    }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                info = readBattery(intent)
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_EXPORTED, // battery broadcast is a protected system broadcast
        )
        onDispose { context.unregisterReceiver(receiver) }
    }
    return info
}

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
            .padding(top = Cfg.ISLAND_TOP_DP.dp)
            .height(Cfg.ISLAND_HEIGHT_DP.dp)
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

@Composable
fun DynamicIsland(battery: BatteryInfo) {
    var expanded by remember { mutableStateOf(false) }
    var wasCharging by remember { mutableStateOf(battery.charging) }

    LaunchedEffect(battery.charging) {
        if (battery.charging && !wasCharging) {
            expanded = true
            delay(3_200)
            expanded = false
        }
        wasCharging = battery.charging
    }

    val width by animateDpAsState(
        targetValue = if (expanded) 310.dp else Cfg.ISLAND_WIDTH_DP.dp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow),
        label = "islandWidth",
    )
    val height by animateDpAsState(
        targetValue = if (expanded) 76.dp else Cfg.ISLAND_HEIGHT_DP.dp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow),
        label = "islandHeight",
    )

    Box(
        Modifier.fillMaxWidth().padding(top = Cfg.ISLAND_TOP_DP.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            Modifier
                .size(width, height)
                .clip(RoundedCornerShape(50))
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { expanded = !expanded },
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {
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

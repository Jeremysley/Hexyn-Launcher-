package com.hexyn.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import kotlinx.coroutines.withContext

val ShadowText = TextStyle(
    shadow = Shadow(
        color = Color.Black.copy(alpha = 0.45f),
        offset = Offset(0f, 1.5f),
        blurRadius = 5f,
    )
)

// ------------------------------------------------------------------
// Root screen: home pages + overlays (Control Center, Search, Folders, Settings)
// ------------------------------------------------------------------

@Composable
fun LauncherScreen(
    resumeTick: Int,
    homeTick: Int,
    onBlur: (Int) -> Unit,
    onSystemBars: () -> Unit,
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf(emptyList<AppItem>()) }
    var pinned by remember { mutableStateOf(loadPinned(context)) }
    var controlOpen by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var notifOpen by remember { mutableStateOf(false) }
    var widgetEdit by remember { mutableStateOf(-1) } // -1 none, 0 calendar, 1 clock
    var folder by remember { mutableStateOf<String?>(null) }
    val battery = rememberBattery()
    val media = rememberNowPlaying(S.islandEnabled && S.islandMusic, resumeTick)
    val activity = rememberActivity(S.islandEnabled && S.islandActivity)

    LaunchedEffect(resumeTick) {
        apps = withContext(Dispatchers.Default) { loadApps(context) }
    }
    LaunchedEffect(S.customStatusBar) { onSystemBars() }

    val homeApps = remember(apps, pinned) { apps.filter { it.packageName !in pinned } }
    val dockApps = remember(apps, pinned) {
        pinned.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }
    }
    val pages = remember(homeApps, S.columns, S.showWidgets) { paginate(homeApps) }
    val pagerState = rememberPagerState(pageCount = { pages.size + 1 })

    LaunchedEffect(homeTick) {
        controlOpen = false
        searchOpen = false
        settingsOpen = false
        notifOpen = false
        widgetEdit = -1
        folder = null
        if (homeTick > 0) pagerState.animateScrollToPage(0)
    }

    val overlayOpen = controlOpen || notifOpen || searchOpen || settingsOpen || folder != null
    BackHandler(enabled = overlayOpen || widgetEdit >= 0) {
        when {
            widgetEdit >= 0 -> widgetEdit = -1
            settingsOpen -> settingsOpen = false
            folder != null -> folder = null
            else -> {
                controlOpen = false
                notifOpen = false
                searchOpen = false
            }
        }
    }

    // While Control Center or Notification Center is open, the home screen is hidden completely.
    val homeAlpha by animateFloatAsState(
        targetValue = if (controlOpen || notifOpen) 0f else 1f,
        animationSpec = tween(250),
        label = "homeAlpha",
    )

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
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = homeAlpha }) {
            HomeLayer(
                pages = pages,
                dockApps = dockApps,
                pinned = pinned,
                allApps = apps,
                pagerState = pagerState,
                gesturesEnabled = !overlayOpen && widgetEdit < 0,
                onTogglePin = togglePin,
                onOpenSearch = { searchOpen = true },
                onOpenControl = { controlOpen = true },
                onOpenNotifications = { notifOpen = true },
                onOpenFolder = { folder = it },
                onEditCalendar = { widgetEdit = 0 },
                onEditClock = { widgetEdit = 1 },
            )
        }

        if (blur > 0.01f) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.22f * blur)))
        }

        AnimatedVisibility(
            visible = controlOpen,
            enter = fadeIn(tween(200)) + slideInVertically(tween(300)) { -it / 8 },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(200)) { -it / 8 },
        ) {
            ControlCenter(
                onClose = { controlOpen = false },
                onOpenSettings = {
                    controlOpen = false
                    settingsOpen = true
                },
            )
        }

        AnimatedVisibility(
            visible = notifOpen,
            enter = fadeIn(tween(200)) + slideInVertically(tween(300)) { -it / 8 },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(200)) { -it / 8 },
        ) {
            NotificationCenter(resumeTick = resumeTick, onClose = { notifOpen = false })
        }

        AnimatedVisibility(
            visible = searchOpen,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150)),
        ) {
            SpotlightOverlay(apps = apps, onClose = { searchOpen = false })
        }

        val openFolder = folder
        val lastFolder = remember { arrayOf("") }
        if (openFolder != null) lastFolder[0] = openFolder
        AnimatedVisibility(
            visible = openFolder != null,
            enter = fadeIn(tween(180)) + scaleIn(spring(dampingRatio = 0.7f, stiffness = 400f), initialScale = 0.85f),
            exit = fadeOut(tween(140)) + scaleOut(tween(160), targetScale = 0.9f),
        ) {
            FolderOverlay(
                title = lastFolder[0],
                apps = apps.filter { it.category == lastFolder[0] },
                pinned = pinned,
                onTogglePin = togglePin,
                onClose = { folder = null },
            )
        }

        AnimatedVisibility(
            visible = settingsOpen,
            enter = fadeIn(tween(200)) + slideInVertically(tween(300)) { it / 8 },
            exit = fadeOut(tween(150)),
        ) {
            SettingsScreen(resumeTick = resumeTick, onClose = { settingsOpen = false })
        }

        if (widgetEdit >= 0) {
            WidgetEditor(kind = widgetEdit, onClose = { widgetEdit = -1 })
        }

        if (S.customStatusBar) IosStatusBar(battery)
        if (S.islandEnabled) DynamicIsland(battery, media, activity)
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
    onOpenNotifications: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onEditCalendar: () -> Unit,
    onEditClock: () -> Unit,
) {
    val topSpace = if (S.customStatusBar || S.islandEnabled) {
        (S.islandTop + S.islandHeight + 18).dp
    } else {
        12.dp
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(gesturesEnabled) {
                var total = 0f
                var startX = 0f
                var startY = 0f
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        total = 0f
                        startX = offset.x
                        startY = offset.y
                    },
                    onDragEnd = {
                        if (gesturesEnabled && total > 140f) {
                            // Upper part of the screen: left half = Notifications,
                            // right half = Control Center. Lower part = Spotlight search.
                            when {
                                startY > size.height * 0.6f -> onOpenSearch()
                                startX > size.width * 0.5f -> onOpenControl()
                                else -> onOpenNotifications()
                            }
                        }
                    },
                    onDragCancel = {},
                    onVerticalDrag = { _, dy -> total += dy },
                )
            }
    ) {
        Spacer(Modifier.height(topSpace))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page ->
            if (page < pages.size) {
                AppGridPage(
                    apps = pages[page],
                    showWidgets = page == 0 && S.showWidgets,
                    pinned = pinned,
                    onTogglePin = onTogglePin,
                    onEditCalendar = onEditCalendar,
                    onEditClock = onEditClock,
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
    showWidgets: Boolean,
    pinned: List<String>,
    onTogglePin: (String) -> Unit,
    onEditCalendar: () -> Unit,
    onEditClock: () -> Unit,
) {
    val columns = S.columns
    val appRows = if (showWidgets) Cfg.HOME_ROWS - 2 else Cfg.HOME_ROWS
    BoxWithConstraints(Modifier.fillMaxSize()) {
    // Keep icons inside their cell when many columns are used.
    val cellIcon = minOf(S.iconSize.dp, (maxWidth - 28.dp) / columns - 6.dp)
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        if (showWidgets) {
            WidgetsRow(
                modifier = Modifier.fillMaxWidth().weight(S.widgetHeight).padding(bottom = 14.dp),
                onEditCalendar = onEditCalendar,
                onEditClock = onEditClock,
            )
        }
        for (r in 0 until appRows) {
            Row(Modifier.fillMaxWidth().weight(1f)) {
                for (c in 0 until columns) {
                    val app = apps.getOrNull(r * columns + c)
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
                                iconSize = cellIcon,
                            )
                        }
                    }
                }
            }
        }
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
            .padding(horizontal = 22.dp)
            .fillMaxWidth()
            .glass(RoundedCornerShape(38.dp), 1.25f)
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .heightIn(min = S.iconSize.dp),
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
// App icon with gloss, press animation, zoom-launch and long-press menu
// ------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(
    app: AppItem,
    pinned: Boolean,
    canPin: Boolean,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = S.iconSize.dp,
    showLabel: Boolean = S.showLabels,
    onLaunched: () -> Unit = {},
) {
    val context = LocalContext.current
    val view = LocalView.current
    val haptics = LocalHapticFeedback.current
    val shape = iconShape()
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
                    .shadow(6.dp, shape, clip = false)
                    .clip(shape)
                    .border(
                        0.7.dp,
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.04f))
                        ),
                        shape,
                    )
            ) {
                Image(
                    bitmap = app.icon,
                    contentDescription = app.label,
                    modifier = Modifier.fillMaxSize(),
                )
                // Soft glossy highlight across the top half, like iOS icons.
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.White.copy(alpha = 0.20f),
                                0.5f to Color.Transparent,
                            )
                        )
                )
            }
            if (showLabel) {
                Text(
                    text = app.label,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
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
// Battery
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

package com.hexyn.launcher

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*

/** Last home page: apps grouped into glass category cards, like the iOS App Library. */
@Composable
fun AppLibraryPage(
    apps: List<AppItem>,
    onSearch: () -> Unit,
    onOpenFolder: (String) -> Unit,
) {
    val groups = remember(apps) {
        apps.groupBy { it.category }
            .toList()
            .sortedWith(
                compareBy<Pair<String, List<AppItem>>>({ it.first == "Other" }, { it.first })
            )
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .glass(RoundedCornerShape(50), 0.9f)
                .clickable(onClick = onSearch)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "App Library",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 15.sp,
            )
        }
        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(groups, key = { it.first }) { group ->
                FolderCard(
                    title = group.first,
                    apps = group.second,
                    onClick = { onOpenFolder(group.first) },
                )
            }
        }
    }
}

@Composable
fun FolderCard(title: String, apps: List<AppItem>, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .glass(RoundedCornerShape(28.dp))
                .clickable(onClick = onClick)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            for (r in 0..1) {
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (c in 0..1) {
                        val app = apps.getOrNull(r * 2 + c)
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            if (app != null) {
                                Image(
                                    bitmap = app.icon,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(IconShape),
                                )
                            }
                        }
                    }
                }
            }
        }
        Text(
            text = title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = ShadowText,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** Opens a category as a glass sheet in the middle of the screen. */
@Composable
fun FolderOverlay(
    title: String,
    apps: List<AppItem>,
    pinned: List<String>,
    onTogglePin: (String) -> Unit,
    onClose: () -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { onClose() } },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = shown,
            enter = fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.8f),
        ) {
            Column(
                Modifier
                    .padding(horizontal = 18.dp)
                    .fillMaxWidth()
                    .glass(RoundedCornerShape(34.dp), 1.2f)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .padding(18.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 6.dp, bottom = 12.dp),
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 420.dp),
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        AppIcon(
                            app = app,
                            pinned = app.packageName in pinned,
                            canPin = pinned.size < Cfg.MAX_DOCK,
                            onTogglePin = { onTogglePin(app.packageName) },
                            onLaunched = onClose,
                        )
                    }
                }
            }
        }
    }
}

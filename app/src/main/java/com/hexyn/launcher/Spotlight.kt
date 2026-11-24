package com.hexyn.launcher

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*

/** Pull down on the left side of the home screen to search all your apps. */
@Composable
fun SpotlightOverlay(apps: List<AppItem>, onClose: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }

    val results = remember(apps, query) {
        if (query.isBlank()) {
            emptyList<AppItem>()
        } else {
            apps.filter { it.label.contains(query, ignoreCase = true) }
                .sortedBy { !it.label.startsWith(query, ignoreCase = true) }
        }
    }

    LaunchedEffect(Unit) {
        try {
            focus.requestFocus()
        } catch (e: Exception) {
            // Keyboard will open when the field is tapped.
        }
    }

    val open: (AppItem) -> Unit = { app ->
        launchApp(context, view, app.packageName)
        onClose()
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { onClose() } }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .padding(top = 68.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.fillMaxWidth().glass(RoundedCornerShape(50), 1.1f)) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                        )
                    },
                    placeholder = { Text("Search", color = Color.White.copy(alpha = 0.6f)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        results.firstOrNull()?.let { open(it) }
                    }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                    ),
                )
            }

            if (results.isNotEmpty()) {
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .glass(RoundedCornerShape(28.dp), 1.1f)
                        .pointerInput(Unit) { detectTapGestures { } }
                ) {
                    items(results, key = { it.packageName }) { app ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { open(app) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                bitmap = app.icon,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(iconShape()),
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(
                                text = app.label,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

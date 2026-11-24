package com.hexyn.launcher

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*

/**
 * Pull down on the right side of the home screen.
 * Real controls: flashlight, brightness, volume.
 * The rest open the matching system screen (Android does not let apps flip those switches).
 */
@Composable
fun ControlCenter(onClose: () -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current

    // --- Flashlight ---
    val cameraManager = remember { context.getSystemService(CameraManager::class.java) }
    val torchId = remember {
        try {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            null
        }
    }
    var torchOn by remember { mutableStateOf(false) }
    DisposableEffect(cameraManager) {
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                if (cameraId == torchId) torchOn = enabled
            }
        }
        cameraManager.registerTorchCallback(callback, null)
        onDispose { cameraManager.unregisterTorchCallback(callback) }
    }

    // --- Volume ---
    val audio = remember { context.getSystemService(AudioManager::class.java) }
    val maxVolume = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var volume by remember {
        mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC) / maxVolume.toFloat())
    }

    // --- Brightness ---
    var brightness by remember {
        mutableFloatStateOf(
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f
        )
    }
    var askedWritePermission by remember { mutableStateOf(false) }

    val setBrightness: (Float) -> Unit = { value ->
        brightness = value
        if (Settings.System.canWrite(context)) {
            try {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                )
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    (value * 255f).toInt().coerceIn(1, 255),
                )
            } catch (e: Exception) {
                // Ignore: brightness could not be changed.
            }
        } else if (!askedWritePermission) {
            askedWritePermission = true
            startSafely(
                context,
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")),
            )
            onClose()
        }
    }
    val setVolume: (Float) -> Unit = { value ->
        volume = value
        try {
            audio.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                Math.round(value * maxVolume),
                0,
            )
        } catch (e: Exception) {
            // Ignore: volume could not be changed.
        }
    }

    val open: (Intent) -> Unit = { intent ->
        startSafely(context, intent)
        onClose()
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.12f))
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
        val sidePadding = 20.dp
        val gap = 12.dp
        val cell = (maxWidth - sidePadding * 2 - gap * 3) / 4

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 66.dp, start = sidePadding, end = sidePadding),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                // Connectivity block (2 x 2)
                Column(
                    Modifier
                        .size(cell * 2 + gap, cell * 2 + gap)
                        .glass(RoundedCornerShape(30.dp), 1.1f)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        RoundButton(
                            Icons.Rounded.AirplanemodeActive, cell - 10.dp,
                        ) { open(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)) }
                        RoundButton(
                            Icons.Rounded.SignalCellularAlt, cell - 10.dp,
                        ) { open(Intent(Settings.ACTION_WIRELESS_SETTINGS)) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        RoundButton(
                            Icons.Rounded.Wifi, cell - 10.dp,
                        ) {
                            open(
                                Intent(
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        Settings.Panel.ACTION_INTERNET_CONNECTIVITY
                                    } else {
                                        Settings.ACTION_WIFI_SETTINGS
                                    }
                                )
                            )
                        }
                        RoundButton(
                            Icons.Rounded.Bluetooth, cell - 10.dp,
                        ) { open(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
                    }
                }

                GlassSlider(
                    value = brightness,
                    icon = Icons.Rounded.WbSunny,
                    modifier = Modifier.size(cell, cell * 2 + gap),
                    onValueChange = setBrightness,
                )
                GlassSlider(
                    value = volume,
                    icon = Icons.Rounded.VolumeUp,
                    modifier = Modifier.size(cell, cell * 2 + gap),
                    onValueChange = setVolume,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                ToggleTile(Icons.Rounded.FlashlightOn, torchOn, Modifier.size(cell)) {
                    val id = torchId
                    if (id != null) {
                        try {
                            cameraManager.setTorchMode(id, !torchOn)
                        } catch (e: Exception) {
                            // Flashlight busy or unavailable.
                        }
                    }
                }
                ToggleTile(Icons.Rounded.NotificationsOff, false, Modifier.size(cell)) {
                    open(Intent(Settings.ACTION_SOUND_SETTINGS))
                }
                ToggleTile(Icons.Rounded.CameraAlt, false, Modifier.size(cell)) {
                    open(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
                }
                ToggleTile(Icons.Rounded.Calculate, false, Modifier.size(cell)) {
                    open(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALCULATOR))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                ToggleTile(Icons.Rounded.Timer, false, Modifier.size(cell)) {
                    open(Intent(AlarmClock.ACTION_SHOW_TIMERS))
                }
                ToggleTile(Icons.Rounded.Alarm, false, Modifier.size(cell)) {
                    open(Intent(AlarmClock.ACTION_SHOW_ALARMS))
                }
                ToggleTile(Icons.Rounded.Smartphone, false, Modifier.size(cell)) {
                    open(Intent(Settings.ACTION_DISPLAY_SETTINGS))
                }
                ToggleTile(Icons.Rounded.Settings, false, Modifier.size(cell)) {
                    onOpenSettings() // Hexyn Launcher settings
                }
            }
        }
    }
}

@Composable
fun RoundButton(icon: ImageVector, diameter: Dp, onClick: () -> Unit) {
    Box(
        Modifier
            .size(diameter)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.20f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

@Composable
fun ToggleTile(icon: ImageVector, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .then(
                if (active) {
                    Modifier.background(Color.White.copy(alpha = 0.92f))
                } else {
                    Modifier.glass(RoundedCornerShape(24.dp), 1.1f)
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) Color(0xFF2C2C2E) else Color.White,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
fun GlassSlider(
    value: Float,
    icon: ImageVector,
    modifier: Modifier,
    onValueChange: (Float) -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(30.dp))
            .background(Color.White.copy(alpha = 0.20f))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onValueChange((1f - offset.y / size.height).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    onValueChange((1f - change.position.y / size.height).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(value.coerceIn(0f, 1f))
                .background(Color.White.copy(alpha = 0.92f))
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (value > 0.12f) Color(0xFF48484A) else Color.White,
            modifier = Modifier.padding(bottom = 16.dp).size(26.dp),
        )
    }
}

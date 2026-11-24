package com.hexyn.launcher

import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

data class ActivityState(
    val onCall: Boolean = false,
    val callSeconds: Int = 0,
    val camera: Boolean = false,
    val mic: Boolean = false,
    val audio: Boolean = false,
)

/**
 * Watches for a phone/voice call, camera in use, microphone in use and sound playing.
 * None of this needs a permission: Android lets any app see that these are active
 * (but not which app is using them).
 */
@Composable
fun rememberActivity(enabled: Boolean): ActivityState {
    val context = LocalContext.current
    var camera by remember { mutableStateOf(false) }
    var mic by remember { mutableStateOf(false) }
    var onCall by remember { mutableStateOf(false) }
    var audio by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }

    // Camera and microphone: Android tells us when they are opened or closed.
    DisposableEffect(enabled) {
        if (!enabled) {
            camera = false
            mic = false
            return@DisposableEffect onDispose { }
        }

        val handler = Handler(Looper.getMainLooper())
        val audioManager = context.getSystemService(AudioManager::class.java)
        val cameraManager = context.getSystemService(CameraManager::class.java)

        val busy = mutableSetOf<String>()
        val baseline = mutableSetOf<String>()
        var ready = false
        handler.postDelayed({ ready = true }, 800)

        val cameraCallback = object : CameraManager.AvailabilityCallback() {
            override fun onCameraUnavailable(cameraId: String) {
                if (!ready) {
                    baseline.add(cameraId) // already unavailable at start: ignore it
                } else if (cameraId !in baseline) {
                    busy.add(cameraId)
                    camera = true
                }
            }

            override fun onCameraAvailable(cameraId: String) {
                baseline.remove(cameraId)
                busy.remove(cameraId)
                camera = busy.isNotEmpty()
            }
        }

        fun anyRecording(configs: List<AudioRecordingConfiguration>?): Boolean =
            configs != null && configs.any {
                Build.VERSION.SDK_INT < 29 || !it.isClientSilenced
            }

        val micCallback = object : AudioManager.AudioRecordingCallback() {
            override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>?) {
                mic = anyRecording(configs)
            }
        }

        try {
            cameraManager.registerAvailabilityCallback(cameraCallback, handler)
        } catch (e: Exception) {
            // Camera status not available on this phone.
        }
        try {
            audioManager.registerAudioRecordingCallback(micCallback, handler)
            mic = anyRecording(audioManager.activeRecordingConfigurations)
        } catch (e: Exception) {
            // Microphone status not available on this phone.
        }

        onDispose {
            handler.removeCallbacksAndMessages(null)
            try {
                cameraManager.unregisterAvailabilityCallback(cameraCallback)
            } catch (e: Exception) {
                // Nothing to clean up.
            }
            try {
                audioManager.unregisterAudioRecordingCallback(micCallback)
            } catch (e: Exception) {
                // Nothing to clean up.
            }
        }
    }

    // Calls and playing sound: checked once a second.
    LaunchedEffect(enabled) {
        val audioManager = context.getSystemService(AudioManager::class.java)
        var start = 0L
        while (true) {
            if (enabled) {
                val mode = audioManager.mode
                val inCall = mode == AudioManager.MODE_IN_CALL ||
                    mode == AudioManager.MODE_IN_COMMUNICATION
                if (inCall && start == 0L) start = System.currentTimeMillis()
                if (!inCall) start = 0L
                onCall = inCall
                seconds = if (inCall) ((System.currentTimeMillis() - start) / 1000L).toInt() else 0
                audio = audioManager.isMusicActive
            } else {
                onCall = false
                audio = false
                start = 0L
            }
            delay(1_000)
        }
    }

    return ActivityState(
        onCall = onCall,
        callSeconds = seconds,
        camera = camera,
        mic = mic,
        audio = audio,
    )
}

package com.hexyn.launcher

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat

/**
 * Android only lets an app read "now playing" info if it is an enabled notification listener.
 * This service does nothing itself. It only exists so you can grant Notification access once.
 */
class MediaListenerService : NotificationListenerService()

data class MediaState(
    val title: String,
    val artist: String,
    val art: ImageBitmap?,
    val playing: Boolean,
    val controller: MediaController,
)

fun hasListenerAccess(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

/** Returns what is playing right now (Spotify, YouTube Music, etc.), or null. */
@Composable
fun rememberNowPlaying(enabled: Boolean, refreshKey: Int): MediaState? {
    val context = LocalContext.current
    var state by remember { mutableStateOf<MediaState?>(null) }

    DisposableEffect(enabled, refreshKey) {
        if (!enabled || !hasListenerAccess(context)) {
            state = null
            return@DisposableEffect onDispose { }
        }

        val manager = context.getSystemService(MediaSessionManager::class.java)
        val component = ComponentName(context, MediaListenerService::class.java)
        var current: MediaController? = null

        fun refresh() {
            val c = current
            if (c == null) {
                state = null
                return
            }
            val md = c.metadata
            val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE)
            if (md == null || title.isNullOrBlank()) {
                state = null
                return
            }
            val artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: ""
            val bitmap = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: md.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            val playing = c.playbackState?.state == PlaybackState.STATE_PLAYING
            state = MediaState(title, artist, bitmap?.asImageBitmap(), playing, c)
        }

        val callback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(s: PlaybackState?) {
                refresh()
            }

            override fun onMetadataChanged(m: MediaMetadata?) {
                refresh()
            }
        }

        fun pick(list: List<MediaController>?) {
            current?.unregisterCallback(callback)
            current = list?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: list?.firstOrNull()
            current?.registerCallback(callback)
            refresh()
        }

        val listener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            pick(controllers)
        }

        try {
            manager.addOnActiveSessionsChangedListener(listener, component)
            pick(manager.getActiveSessions(component))
        } catch (e: SecurityException) {
            state = null
        }

        onDispose {
            try {
                manager.removeOnActiveSessionsChangedListener(listener)
            } catch (e: Exception) {
                // Nothing to clean up.
            }
            current?.unregisterCallback(callback)
        }
    }

    return state
}

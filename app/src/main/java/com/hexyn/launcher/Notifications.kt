package com.hexyn.launcher

import android.app.Notification
import android.app.PendingIntent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap

data class NotifItem(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val whenMs: Long,
    val icon: ImageBitmap?,
    val intent: PendingIntent?,
    val ongoing: Boolean,
)

/** Everything the Notification Center and the Dynamic Island need to know about notifications. */
object NotificationStore {
    val items = mutableStateListOf<NotifItem>()
    var banner by mutableStateOf<NotifItem?>(null)
    var bannerId by mutableIntStateOf(0)
    var service: NotificationListenerService? = null

    fun replaceAll(list: List<NotifItem>) {
        items.clear()
        items.addAll(list.sortedByDescending { it.whenMs })
    }

    fun post(item: NotifItem) {
        val old = items.firstOrNull { it.key == item.key }
        val changed = old == null || old.title != item.title || old.text != item.text
        val rest = items.filter { it.key != item.key }
        items.clear()
        items.addAll((rest + item).sortedByDescending { it.whenMs })
        if (changed && !item.ongoing) {
            banner = item
            bannerId++
        }
    }

    fun remove(key: String) {
        items.removeAll { it.key == key }
    }

    fun dismiss(key: String) {
        try {
            service?.cancelNotification(key)
        } catch (e: Exception) {
            // Not connected right now.
        }
        remove(key)
    }

    fun clearAll() {
        try {
            service?.cancelAllNotifications()
        } catch (e: Exception) {
            // Not connected right now.
        }
        items.clear()
    }
}

/**
 * Android only lets an app read notifications and "now playing" info if it is an enabled
 * notification listener. You grant Notification access once in Launcher Settings.
 */
class MediaListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        NotificationStore.service = this
        refreshAll()
    }

    override fun onListenerDisconnected() {
        if (NotificationStore.service === this) NotificationStore.service = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        toItem(sbn)?.let { NotificationStore.post(it) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        NotificationStore.remove(sbn.key)
    }

    private fun refreshAll() {
        val active = try {
            activeNotifications
        } catch (e: Exception) {
            null
        }
        NotificationStore.replaceAll(active?.mapNotNull { toItem(it) } ?: emptyList())
    }

    private fun toItem(sbn: StatusBarNotification): NotifItem? {
        if (sbn.packageName == packageName) return null
        val n = sbn.notification ?: return null
        if ((n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return null

        val extras = n.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras?.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return null

        val pm = packageManager
        val appName = try {
            pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
        } catch (e: Exception) {
            sbn.packageName
        }

        return NotifItem(
            key = sbn.key,
            packageName = sbn.packageName,
            appName = appName,
            title = title,
            text = text,
            whenMs = sbn.postTime,
            icon = loadAppIcon(this, sbn.packageName),
            intent = n.contentIntent,
            ongoing = sbn.isOngoing,
        )
    }
}

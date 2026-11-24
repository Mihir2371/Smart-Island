package com.example.liveactivity

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.state.CapsuleStateHolder

class CapsuleNotificationListener : NotificationListenerService() {

  private var mediaTracker: MediaSessionTracker? = null

  override fun onListenerConnected() {
    super.onListenerConnected()
    CapsuleStateHolder.updatePermissions(notification = true)
    mediaTracker =
      MediaSessionTracker(applicationContext).apply {
        start(ComponentName(applicationContext, CapsuleNotificationListener::class.java))
      }
    refreshActiveNotifications()
  }

  override fun onListenerDisconnected() {
    super.onListenerDisconnected()
    mediaTracker?.stop()
    mediaTracker = null
  }

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    refreshActiveNotifications()
  }

  override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    refreshActiveNotifications()
  }

  private fun refreshActiveNotifications() {
    runCatching {
      val parsed = NotificationParser.parseAll(activeNotifications)
      CapsuleStateHolder.replaceNotificationActivities(parsed)
    }
  }
}

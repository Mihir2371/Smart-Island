package com.example.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.SettingsRepository
import com.example.draggo.DragAndGoProcessor
import com.example.hardware.HardwareController
import com.example.hardware.SystemEventMonitor
import com.example.liveactivity.CapsuleNotificationListener
import com.example.liveactivity.MediaSessionTracker
import com.example.state.CapsuleStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OverlayService : Service() {

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  private lateinit var settingsRepository: SettingsRepository
  private lateinit var hardwareController: HardwareController
  private lateinit var systemEventMonitor: SystemEventMonitor
  private lateinit var mediaSessionTracker: MediaSessionTracker
  private lateinit var dragAndGoProcessor: DragAndGoProcessor
  private var capsuleWindowManager: CapsuleWindowManager? = null

  override fun onCreate() {
    super.onCreate()
    settingsRepository = SettingsRepository(applicationContext)
    hardwareController = HardwareController(applicationContext)
    systemEventMonitor = SystemEventMonitor(applicationContext)
    mediaSessionTracker = MediaSessionTracker(applicationContext)
    dragAndGoProcessor = DragAndGoProcessor(applicationContext, settingsRepository)

    createNotificationChannel()
    startForegroundCompat()

    capsuleWindowManager =
      CapsuleWindowManager(
        context = applicationContext,
        hardwareController = hardwareController,
        mediaTracker = mediaSessionTracker,
        dragAndGoProcessor = dragAndGoProcessor,
        onStopSatellite = { systemEventMonitor.stopSatelliteAlignment() },
      )

    if (Settings.canDrawOverlays(this)) {
      capsuleWindowManager?.attach()
    }

    systemEventMonitor.start()
    runCatching {
      mediaSessionTracker.start(
        ComponentName(applicationContext, CapsuleNotificationListener::class.java)
      )
    }

    CapsuleStateHolder.setOverlayServiceRunning(true)

    serviceScope.launch {
      settingsRepository.cutoutConfigFlow.collectLatest { config ->
        CapsuleStateHolder.updateCutoutConfig(config)
        capsuleWindowManager?.updateBoundsFromState()
      }
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP) {
      stopSelf()
      return START_NOT_STICKY
    }
    if (Settings.canDrawOverlays(this)) {
      capsuleWindowManager?.attach()
    }
    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()
    CapsuleStateHolder.setOverlayServiceRunning(false)
    systemEventMonitor.stop()
    mediaSessionTracker.stop()
    hardwareController.stopVoiceMemoRecording(dismissPopup = false)
    capsuleWindowManager?.detach()
    serviceScope.cancel()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun startForegroundCompat() {
    val openIntent =
      PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
      )

    val notification: Notification =
      NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_menu_compass)
        .setContentTitle(getString(R.string.overlay_service_notif_title))
        .setContentText(getString(R.string.overlay_service_notif_text))
        .setContentIntent(openIntent)
        .setOngoing(true)
        .setSilent(true)
        .setPriority(NotificationCompat.PRIORITY_MIN)
        .build()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      startForeground(
        NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
      )
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val channel =
        NotificationChannel(
            CHANNEL_ID,
            getString(R.string.overlay_service_channel_name),
            NotificationManager.IMPORTANCE_MIN,
          )
          .apply {
            description = getString(R.string.overlay_service_channel_desc)
            setShowBadge(false)
          }
      manager.createNotificationChannel(channel)
    }
  }

  companion object {
    private const val CHANNEL_ID = "capsule_overlay_engine"
    private const val NOTIFICATION_ID = 4102
    const val ACTION_STOP = "com.example.overlay.ACTION_STOP"

    fun start(context: Context) {
      val intent = Intent(context, OverlayService::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun stop(context: Context) {
      context.stopService(Intent(context, OverlayService::class.java))
    }
  }
}

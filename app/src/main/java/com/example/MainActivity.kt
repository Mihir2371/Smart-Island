package com.example

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.data.SettingsRepository
import com.example.draggo.DragAndGoProcessor
import com.example.hardware.HardwareController
import com.example.hardware.SystemEventMonitor
import com.example.liveactivity.CapsuleNotificationListener
import com.example.liveactivity.MediaSessionTracker
import com.example.overlay.OverlayService
import com.example.state.CapsuleStateHolder
import com.example.ui.screens.CapsuleStudioApp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  private lateinit var settingsRepository: SettingsRepository
  private lateinit var hardwareController: HardwareController
  private lateinit var systemEventMonitor: SystemEventMonitor
  private lateinit var mediaSessionTracker: MediaSessionTracker
  private lateinit var dragAndGoProcessor: DragAndGoProcessor

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
    )

    settingsRepository = SettingsRepository(applicationContext)
    hardwareController = HardwareController(applicationContext)
    systemEventMonitor = SystemEventMonitor(applicationContext)
    mediaSessionTracker = MediaSessionTracker(applicationContext)
    dragAndGoProcessor = DragAndGoProcessor(applicationContext, settingsRepository)

    lifecycleScope.launch {
      settingsRepository.cutoutConfigFlow.collectLatest { cfg ->
        CapsuleStateHolder.updateCutoutConfig(cfg)
      }
    }
    lifecycleScope.launch {
      settingsRepository.quickNotesFlow.collectLatest { notes ->
        CapsuleStateHolder.updateQuickNotes(notes)
      }
    }

    handleIncomingIntent(intent)

    setContent {
      val uiState by CapsuleStateHolder.uiState.collectAsState()

      val photoPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
          if (uri != null) {
            dragAndGoProcessor.processImageUri(uri, sourceLabel = "Photo Picker OCR")
          }
        }

      val audioPermLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
          CapsuleStateHolder.updatePermissions(audio = granted)
          hardwareController.startVoiceMemoRecording(isScreenRecording = false)
        }

      val runtimePermLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
          refreshPermissionState()
        }

      LaunchedEffect(Unit) {
        refreshPermissionState()
      }

      CapsuleStudioApp(
        uiState = uiState,
        onUpdateConfig = { next ->
          CapsuleStateHolder.updateCutoutConfig(next)
          lifecycleScope.launch { settingsRepository.saveCutoutConfig(next) }
        },
        onToggleOverlayService = { enable ->
          if (enable) {
            if (Settings.canDrawOverlays(this)) {
              OverlayService.start(this)
            } else {
              openOverlaySettings()
            }
          } else {
            OverlayService.stop(this)
          }
        },
        onRequestOverlayPermission = { openOverlaySettings() },
        onRequestNotificationListener = { openNotificationListenerSettings() },
        onRequestRuntimePermissions = {
          val perms = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
              add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              add(Manifest.permission.POST_NOTIFICATIONS)
            }
          }
          if (perms.isNotEmpty()) {
            runtimePermLauncher.launch(perms.toTypedArray())
          }
        },
        onOpenFlashlight = { hardwareController.openFlashlightControls(forceOn = true) },
        onStartVoiceMemo = {
          val hasAudio =
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
              PackageManager.PERMISSION_GRANTED
          if (hasAudio) {
            hardwareController.startVoiceMemoRecording(isScreenRecording = false)
          } else {
            audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
          }
        },
        onStartSatellite = { systemEventMonitor.startSatelliteAlignment() },
        onCycleRinger = { hardwareController.cycleRingerMode() },
        onStopRecording = { hardwareController.stopVoiceMemoRecording() },
        onStopSatellite = { systemEventMonitor.stopSatelliteAlignment() },
        onFlashlightDrag = { b, s -> hardwareController.updateFlashlightBeam(b, s) },
        onFlashlightOff = { hardwareController.toggleFlashlightOff() },
        onToggleMute = { id -> hardwareController.toggleCallMute(id) },
        onToggleSpeaker = { id -> hardwareController.toggleCallSpeaker(id) },
        onMediaPlayPause = { pkg -> mediaSessionTracker.togglePlayPause(pkg) },
        onMediaNext = { pkg -> mediaSessionTracker.skipNext(pkg) },
        onMediaPrev = { pkg -> mediaSessionTracker.skipPrevious(pkg) },
        onDragAction = { action -> dragAndGoProcessor.executeAction(action) },
        onPickImageForOcr = {
          photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          )
        },
        onDropClipData = { clip, label -> dragAndGoProcessor.processClipData(clip, label) },
        onDropTextPayload = { text, label -> dragAndGoProcessor.processPlainText(text, label) },
        onDeleteQuickNote = { id ->
          lifecycleScope.launch { settingsRepository.deleteQuickNote(id) }
        },
        onCopyQuickNote = { content ->
          val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
          cm.setPrimaryClip(ClipData.newPlainText("Quick Note", content))
        },
      )
    }
  }

  override fun onStart() {
    super.onStart()
    systemEventMonitor.start()
    refreshPermissionState()
  }

  override fun onStop() {
    super.onStop()
    if (!CapsuleStateHolder.uiState.value.overlayServiceRunning) {
      systemEventMonitor.stop()
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIncomingIntent(intent)
  }

  private fun handleIncomingIntent(intent: Intent?) {
    if (intent?.action != Intent.ACTION_SEND) return
    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
    val sharedUri: Uri? =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
      } else {
        @Suppress("DEPRECATION")
        intent.getParcelableExtra(Intent.EXTRA_STREAM)
      }
    when {
      sharedUri != null -> dragAndGoProcessor.processImageUri(sharedUri, "Shared Image OCR")
      !sharedText.isNullOrBlank() -> dragAndGoProcessor.processPlainText(sharedText, "Shared Text")
    }
  }

  private fun refreshPermissionState() {
    val overlay = Settings.canDrawOverlays(this)
    val enabledListeners =
      Settings.Secure.getString(contentResolver, "enabled_notification_listeners").orEmpty()
    val listenerName =
      ComponentName(this, CapsuleNotificationListener::class.java).flattenToString()
    val notifListener = enabledListeners.contains(listenerName)
    val btGranted =
      Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
          PackageManager.PERMISSION_GRANTED
    val audioGranted =
      ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    val postNotifGranted =
      Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED

    CapsuleStateHolder.updatePermissions(
      overlay = overlay,
      notification = notifListener,
      bluetooth = btGranted,
      audio = audioGranted,
      postNotification = postNotifGranted,
    )
  }

  private fun openOverlaySettings() {
    val intent =
      Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:$packageName"),
      )
    runCatching { startActivity(intent) }
  }

  private fun openNotificationListenerSettings() {
    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    runCatching { startActivity(intent) }
  }
}

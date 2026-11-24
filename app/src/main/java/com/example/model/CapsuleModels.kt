package com.example.model

import android.graphics.Bitmap

enum class CutoutShapeMode {
  FLOATING_PILL,
  TOP_DOCKED_NOTCH,
}

data class CutoutConfig(
  val mode: CutoutShapeMode = CutoutShapeMode.FLOATING_PILL,
  val offsetXdp: Float = 0f,
  val offsetYdp: Float = 8f,
  val widthDp: Float = 128f,
  val heightDp: Float = 34f,
  val cornerRadiusDp: Float = 20f,
  val showPrivacyDots: Boolean = true,
  val autoCollapseSeconds: Int = 6,
)

data class PrivacyStatus(
  val cameraActive: Boolean = false,
  val micActive: Boolean = false,
  val simulatedCamera: Boolean = false,
  val simulatedMic: Boolean = false,
) {
  val effectiveCamera: Boolean get() = cameraActive || simulatedCamera
  val effectiveMic: Boolean get() = micActive || simulatedMic
}

enum class SystemToggleType {
  SILENT,
  VIBRATE,
  RING,
  DND,
  AIRPLANE,
  HOTSPOT,
}

enum class TurnDirection {
  STRAIGHT,
  LEFT,
  RIGHT,
  SLIGHT_LEFT,
  SLIGHT_RIGHT,
  U_TURN,
  ROUNDABOUT,
}

sealed interface HardwarePopup {
  val id: String
  val timestampMs: Long

  data class Flashlight(
    override val id: String = "flashlight",
    override val timestampMs: Long = System.currentTimeMillis(),
    val isOn: Boolean,
    val brightness: Float,
    val beamSpread: Float,
  ) : HardwarePopup

  data class Charging(
    override val id: String = "charging",
    override val timestampMs: Long = System.currentTimeMillis(),
    val level: Int,
    val isFastCharging: Boolean,
    val wattageEstimate: Int,
  ) : HardwarePopup

  data class LowBattery(
    override val id: String = "low_battery",
    override val timestampMs: Long = System.currentTimeMillis(),
    val level: Int,
  ) : HardwarePopup

  data class BluetoothConnected(
    override val id: String = "bluetooth",
    override val timestampMs: Long = System.currentTimeMillis(),
    val deviceName: String,
    val batteryPercent: Int,
    val isAudioDevice: Boolean,
  ) : HardwarePopup

  data class SystemToggle(
    override val id: String = "toggle",
    override val timestampMs: Long = System.currentTimeMillis(),
    val type: SystemToggleType,
    val label: String,
    val subtitle: String,
    val isActive: Boolean,
  ) : HardwarePopup

  data class Recording(
    override val id: String = "recording",
    override val timestampMs: Long = System.currentTimeMillis(),
    val isScreenRecording: Boolean,
    val startedAtMs: Long,
    val amplitudes: List<Float>,
  ) : HardwarePopup

  data class NfcScan(
    override val id: String = "nfc",
    override val timestampMs: Long = System.currentTimeMillis(),
    val tagSummary: String,
  ) : HardwarePopup

  data class SatelliteAlignment(
    override val id: String = "satellite",
    override val timestampMs: Long = System.currentTimeMillis(),
    val azimuthDelta: Float,
    val pitchDelta: Float,
    val isLocked: Boolean,
    val signalBars: Int,
  ) : HardwarePopup
}

sealed interface LiveActivity {
  val id: String
  val packageName: String
  val priority: Int

  data class PhoneCall(
    override val id: String,
    override val packageName: String,
    override val priority: Int = 100,
    val callerName: String,
    val appLabel: String,
    val startedAtMs: Long,
    val isMuted: Boolean = false,
    val isSpeaker: Boolean = false,
    val isVoip: Boolean = false,
  ) : LiveActivity

  data class MediaPlayback(
    override val id: String,
    override val packageName: String,
    override val priority: Int = 60,
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val albumArt: Bitmap? = null,
  ) : LiveActivity

  data class Navigation(
    override val id: String,
    override val packageName: String,
    override val priority: Int = 80,
    val direction: TurnDirection,
    val distanceText: String,
    val streetName: String,
    val etaText: String,
  ) : LiveActivity

  data class Rideshare(
    override val id: String,
    override val packageName: String,
    override val priority: Int = 75,
    val provider: String,
    val statusText: String,
    val etaMinutes: Int,
    val vehicleInfo: String,
  ) : LiveActivity

  data class Delivery(
    override val id: String,
    override val packageName: String,
    override val priority: Int = 70,
    val provider: String,
    val statusText: String,
    val etaMinutes: Int,
    val progress: Float,
  ) : LiveActivity

  data class Flight(
    override val id: String,
    override val packageName: String,
    override val priority: Int = 72,
    val flightCode: String,
    val gate: String,
    val statusText: String,
    val boardingMinutes: Int,
  ) : LiveActivity

  data class Sports(
    override val id: String,
    override val packageName: String,
    override val priority: Int = 65,
    val league: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: Int,
    val awayScore: Int,
    val clockText: String,
  ) : LiveActivity

  data class TimerActivity(
    override val id: String,
    override val packageName: String,
    override val priority: Int = 85,
    val label: String,
    val endTimeMs: Long,
    val totalMs: Long,
    val isPaused: Boolean = false,
  ) : LiveActivity

  data class DownloadActivity(
    override val id: String,
    override val packageName: String,
    override val priority: Int = 55,
    val fileName: String,
    val progress: Float,
    val speedText: String,
  ) : LiveActivity
}

enum class DragActionType {
  COPY,
  SHARE,
  SEARCH_WEB,
  OPEN_MAPS,
  DIAL_PHONE,
  QUICK_NOTE,
}

data class DragAction(
  val type: DragActionType,
  val label: String,
  val value: String,
)

data class DragDropResult(
  val id: String = System.currentTimeMillis().toString(),
  val sourceLabel: String,
  val extractedText: String,
  val detectedUrl: String? = null,
  val detectedAddress: String? = null,
  val detectedPhone: String? = null,
  val actions: List<DragAction> = emptyList(),
  val isProcessing: Boolean = false,
  val confirmationBanner: String? = null,
)

data class QuickNote(
  val id: String,
  val content: String,
  val createdAtMs: Long,
)

enum class CapsuleDisplayMode {
  COLLAPSED,
  EXPANDED,
}

data class CapsuleUiState(
  val cutoutConfig: CutoutConfig = CutoutConfig(),
  val displayMode: CapsuleDisplayMode = CapsuleDisplayMode.COLLAPSED,
  val privacyStatus: PrivacyStatus = PrivacyStatus(),
  val activeHardwarePopup: HardwarePopup? = null,
  val liveActivities: List<LiveActivity> = emptyList(),
  val selectedActivityIndex: Int = 0,
  val isDragHovering: Boolean = false,
  val dragDropResult: DragDropResult? = null,
  val quickNotes: List<QuickNote> = emptyList(),
  val overlayServiceRunning: Boolean = false,
  val hasOverlayPermission: Boolean = false,
  val hasNotificationPermission: Boolean = false,
  val hasBluetoothPermission: Boolean = false,
  val hasAudioPermission: Boolean = false,
  val hasPostNotificationPermission: Boolean = false,
  val tickerMs: Long = System.currentTimeMillis(),
) {
  val activeLiveActivity: LiveActivity?
    get() =
      if (liveActivities.isEmpty()) null
      else liveActivities[selectedActivityIndex.coerceIn(0, liveActivities.lastIndex)]
}

package com.example.state

import com.example.model.CapsuleDisplayMode
import com.example.model.CapsuleUiState
import com.example.model.CutoutConfig
import com.example.model.DragDropResult
import com.example.model.HardwarePopup
import com.example.model.LiveActivity
import com.example.model.PrivacyStatus
import com.example.model.QuickNote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object CapsuleStateHolder {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val _uiState = MutableStateFlow(CapsuleUiState())
  val uiState: StateFlow<CapsuleUiState> = _uiState.asStateFlow()

  private var autoCollapseJob: Job? = null
  private var hardwareDismissJob: Job? = null
  private var tickerJob: Job? = null

  init {
    startTickerIfNeeded()
  }

  private fun startTickerIfNeeded() {
    if (tickerJob?.isActive == true) return
    tickerJob =
      scope.launch {
        while (true) {
          delay(250L)
          val state = _uiState.value
          val needsTick =
            state.activeHardwarePopup is HardwarePopup.Recording ||
              state.liveActivities.any {
                it is LiveActivity.PhoneCall ||
                  it is LiveActivity.TimerActivity ||
                  it is LiveActivity.MediaPlayback
              }
          if (needsTick) {
            _uiState.update { it.copy(tickerMs = System.currentTimeMillis()) }
          }
        }
      }
  }

  fun updateCutoutConfig(config: CutoutConfig) {
    _uiState.update { it.copy(cutoutConfig = config) }
  }

  fun updateQuickNotes(notes: List<QuickNote>) {
    _uiState.update { it.copy(quickNotes = notes) }
  }

  fun updatePermissions(
    overlay: Boolean = _uiState.value.hasOverlayPermission,
    notification: Boolean = _uiState.value.hasNotificationPermission,
    bluetooth: Boolean = _uiState.value.hasBluetoothPermission,
    audio: Boolean = _uiState.value.hasAudioPermission,
    postNotification: Boolean = _uiState.value.hasPostNotificationPermission,
  ) {
    _uiState.update {
      it.copy(
        hasOverlayPermission = overlay,
        hasNotificationPermission = notification,
        hasBluetoothPermission = bluetooth,
        hasAudioPermission = audio,
        hasPostNotificationPermission = postNotification,
      )
    }
  }

  fun setOverlayServiceRunning(running: Boolean) {
    _uiState.update { it.copy(overlayServiceRunning = running) }
  }

  fun setDisplayMode(mode: CapsuleDisplayMode) {
    _uiState.update { it.copy(displayMode = mode) }
    if (mode == CapsuleDisplayMode.EXPANDED) {
      scheduleAutoCollapse()
    } else {
      autoCollapseJob?.cancel()
    }
  }

  fun toggleExpanded() {
    val current = _uiState.value.displayMode
    if (current == CapsuleDisplayMode.COLLAPSED) {
      setDisplayMode(CapsuleDisplayMode.EXPANDED)
    } else {
      collapseNow()
    }
  }

  fun collapseNow() {
    autoCollapseJob?.cancel()
    _uiState.update {
      it.copy(
        displayMode = CapsuleDisplayMode.COLLAPSED,
        dragDropResult = null,
        isDragHovering = false,
      )
    }
  }

  private fun scheduleAutoCollapse() {
    autoCollapseJob?.cancel()
    val state = _uiState.value
    // Never auto-collapse interactive Drag & Go results, Flashlight beam, Satellite, or active Recording
    if (
      state.dragDropResult != null ||
        state.activeHardwarePopup is HardwarePopup.Flashlight ||
        state.activeHardwarePopup is HardwarePopup.SatelliteAlignment ||
        state.activeHardwarePopup is HardwarePopup.Recording
    ) {
      return
    }
    val seconds = state.cutoutConfig.autoCollapseSeconds.coerceIn(3, 20)
    autoCollapseJob =
      scope.launch {
        delay(seconds * 1000L)
        _uiState.update {
          if (it.dragDropResult == null && !it.isDragHovering) {
            it.copy(displayMode = CapsuleDisplayMode.COLLAPSED)
          } else {
            it
          }
        }
      }
  }

  fun updateHardwarePrivacy(cameraActive: Boolean? = null, micActive: Boolean? = null) {
    _uiState.update { state ->
      state.copy(
        privacyStatus =
          state.privacyStatus.copy(
            cameraActive = cameraActive ?: state.privacyStatus.cameraActive,
            micActive = micActive ?: state.privacyStatus.micActive,
          )
      )
    }
  }

  fun toggleSimulatedPrivacy(camera: Boolean? = null, mic: Boolean? = null) {
    _uiState.update { state ->
      state.copy(
        privacyStatus =
          state.privacyStatus.copy(
            simulatedCamera = camera ?: state.privacyStatus.simulatedCamera,
            simulatedMic = mic ?: state.privacyStatus.simulatedMic,
          )
      )
    }
  }

  fun showHardwarePopup(
    popup: HardwarePopup,
    expand: Boolean = true,
    autoDismissMs: Long? = 4500L,
  ) {
    hardwareDismissJob?.cancel()
    _uiState.update {
      it.copy(
        activeHardwarePopup = popup,
        displayMode = if (expand) CapsuleDisplayMode.EXPANDED else it.displayMode,
      )
    }
    if (expand) {
      scheduleAutoCollapse()
    }
    if (autoDismissMs != null) {
      hardwareDismissJob =
        scope.launch {
          delay(autoDismissMs)
          _uiState.update { state ->
            if (state.activeHardwarePopup?.id == popup.id) {
              state.copy(
                activeHardwarePopup = null,
                displayMode =
                  if (state.liveActivities.isEmpty() && state.dragDropResult == null) {
                    CapsuleDisplayMode.COLLAPSED
                  } else {
                    state.displayMode
                  },
              )
            } else {
              state
            }
          }
        }
    }
  }

  fun updateRecordingAmplitudes(amplitudes: List<Float>) {
    _uiState.update { state ->
      val current = state.activeHardwarePopup as? HardwarePopup.Recording ?: return@update state
      state.copy(activeHardwarePopup = current.copy(amplitudes = amplitudes))
    }
  }

  fun dismissHardwarePopup(id: String? = null) {
    hardwareDismissJob?.cancel()
    _uiState.update { state ->
      if (id == null || state.activeHardwarePopup?.id == id) {
        state.copy(
          activeHardwarePopup = null,
          displayMode = CapsuleDisplayMode.COLLAPSED,
        )
      } else {
        state
      }
    }
  }

  fun upsertLiveActivity(activity: LiveActivity) {
    _uiState.update { state ->
      val mutable = state.liveActivities.filterNot { it.id == activity.id }.toMutableList()
      mutable.add(activity)
      mutable.sortByDescending { it.priority }
      val newIdx = mutable.indexOfFirst { it.id == activity.id }.coerceAtLeast(0)
      state.copy(liveActivities = mutable, selectedActivityIndex = newIdx)
    }
  }

  fun replaceMediaActivities(mediaList: List<LiveActivity.MediaPlayback>) {
    _uiState.update { state ->
      val nonMedia = state.liveActivities.filterNot { it is LiveActivity.MediaPlayback }
      val combined = (nonMedia + mediaList).sortedByDescending { it.priority }
      val safeIdx =
        if (combined.isEmpty()) 0 else state.selectedActivityIndex.coerceIn(0, combined.lastIndex)
      state.copy(liveActivities = combined, selectedActivityIndex = safeIdx)
    }
  }

  fun replaceNotificationActivities(parsed: List<LiveActivity>) {
    _uiState.update { state ->
      val mediaAndSim =
        state.liveActivities.filter {
          it is LiveActivity.MediaPlayback || it.id.startsWith("sim_")
        }
      val combined = (mediaAndSim + parsed).sortedByDescending { it.priority }
      val safeIdx =
        if (combined.isEmpty()) 0 else state.selectedActivityIndex.coerceIn(0, combined.lastIndex)
      state.copy(liveActivities = combined, selectedActivityIndex = safeIdx)
    }
  }

  fun removeLiveActivity(id: String) {
    _uiState.update { state ->
      val remaining = state.liveActivities.filterNot { it.id == id }
      val safeIdx =
        if (remaining.isEmpty()) 0 else state.selectedActivityIndex.coerceIn(0, remaining.lastIndex)
      state.copy(liveActivities = remaining, selectedActivityIndex = safeIdx)
    }
  }

  fun cycleLiveActivity() {
    _uiState.update { state ->
      if (state.liveActivities.size <= 1) state
      else state.copy(selectedActivityIndex = (state.selectedActivityIndex + 1) % state.liveActivities.size)
    }
  }

  fun setDragHovering(hovering: Boolean) {
    _uiState.update { state ->
      state.copy(
        isDragHovering = hovering,
        displayMode = if (hovering) CapsuleDisplayMode.EXPANDED else state.displayMode,
      )
    }
  }

  fun setDragDropResult(result: DragDropResult?) {
    autoCollapseJob?.cancel()
    _uiState.update {
      it.copy(
        isDragHovering = false,
        dragDropResult = result,
        displayMode = if (result != null) CapsuleDisplayMode.EXPANDED else CapsuleDisplayMode.COLLAPSED,
      )
    }
  }
}

package com.example.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.draggo.DragAndGoProcessor
import com.example.hardware.HardwareController
import com.example.liveactivity.MediaSessionTracker
import com.example.model.CapsuleDisplayMode
import com.example.model.CutoutShapeMode
import com.example.state.CapsuleStateHolder
import com.example.ui.capsule.CapsuleOverlayRoot
import kotlin.math.roundToInt

class CapsuleWindowManager(
  private val context: Context,
  private val hardwareController: HardwareController,
  private val mediaTracker: MediaSessionTracker,
  private val dragAndGoProcessor: DragAndGoProcessor,
  private val onStopSatellite: () -> Unit,
) {

  private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
  private val lifecycleOwner = OverlayLifecycleOwner()
  private var rootContainer: FrameLayout? = null
  private var layoutParams: WindowManager.LayoutParams? = null
  private var isAttached = false

  fun attach() {
    if (isAttached) return
    lifecycleOwner.onCreate()
    lifecycleOwner.onResume()

    val density = context.resources.displayMetrics.density
    val params = createLayoutParams(density)
    layoutParams = params

    val container =
      FrameLayout(context).apply {
        setViewTreeLifecycleOwner(lifecycleOwner)
        setViewTreeViewModelStoreOwner(lifecycleOwner)
        setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        setOnDragListener { _, event -> handleSystemDragEvent(event) }
      }

    val composeView =
      ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
          val uiState by CapsuleStateHolder.uiState.collectAsState()
          UpdateWindowLayoutForState(
            density = density,
            mode = uiState.cutoutConfig.mode,
            displayMode = uiState.displayMode,
            offsetXdp = uiState.cutoutConfig.offsetXdp,
            offsetYdp = uiState.cutoutConfig.offsetYdp,
            hasExpandedContent =
              uiState.activeHardwarePopup != null ||
                uiState.dragDropResult != null ||
                uiState.isDragHovering ||
                uiState.activeLiveActivity != null,
          )
          CapsuleOverlayRoot(
            uiState = uiState,
            onToggleExpand = {
              hardwareController.performHapticTap()
              CapsuleStateHolder.toggleExpanded()
            },
            onCollapse = { CapsuleStateHolder.collapseNow() },
            onCycleActivity = { CapsuleStateHolder.cycleLiveActivity() },
            onFlashlightDrag = { brightness, spread ->
              hardwareController.updateFlashlightBeam(brightness, spread)
            },
            onFlashlightOff = { hardwareController.toggleFlashlightOff() },
            onStopRecording = { hardwareController.stopVoiceMemoRecording() },
            onStopSatellite = onStopSatellite,
            onToggleMute = { id -> hardwareController.toggleCallMute(id) },
            onToggleSpeaker = { id -> hardwareController.toggleCallSpeaker(id) },
            onMediaPlayPause = { pkg -> mediaTracker.togglePlayPause(pkg) },
            onMediaNext = { pkg -> mediaTracker.skipNext(pkg) },
            onMediaPrev = { pkg -> mediaTracker.skipPrevious(pkg) },
            onDragAction = { action -> dragAndGoProcessor.executeAction(action) },
            onDismissPopup = { id -> CapsuleStateHolder.dismissHardwarePopup(id) },
          )
        }
      }

    container.addView(composeView)
    runCatching {
      windowManager.addView(container, params)
      rootContainer = container
      isAttached = true
    }
  }

  fun detach() {
    if (!isAttached) return
    runCatching {
      rootContainer?.let { windowManager.removeViewImmediate(it) }
    }
    rootContainer = null
    layoutParams = null
    isAttached = false
    lifecycleOwner.onDestroy()
  }

  fun updateBoundsFromState() {
    val container = rootContainer ?: return
    val params = layoutParams ?: return
    val state = CapsuleStateHolder.uiState.value
    val density = context.resources.displayMetrics.density
    val cfg = state.cutoutConfig

    val isExpanded = state.displayMode == CapsuleDisplayMode.EXPANDED || state.isDragHovering
    val targetWidthDp =
      when {
        isExpanded -> 356f
        state.activeLiveActivity != null || state.activeHardwarePopup != null ->
          (cfg.widthDp + 84f).coerceAtLeast(196f)
        else -> cfg.widthDp + 24f
      }
    val targetHeightDp =
      when {
        state.dragDropResult != null -> 230f
        isExpanded -> 178f
        else -> (cfg.heightDp + 16f).coerceAtLeast(52f)
      }

    val newW = (targetWidthDp * density).roundToInt()
    val newH = (targetHeightDp * density).roundToInt()
    val newX = (cfg.offsetXdp * density).roundToInt()
    val newY =
      if (cfg.mode == CutoutShapeMode.TOP_DOCKED_NOTCH) 0
      else (cfg.offsetYdp * density).roundToInt()

    if (params.width != newW || params.height != newH || params.x != newX || params.y != newY) {
      params.width = newW
      params.height = newH
      params.x = newX
      params.y = newY
      runCatching { windowManager.updateViewLayout(container, params) }
    }
  }

  @androidx.compose.runtime.Composable
  private fun UpdateWindowLayoutForState(
    density: Float,
    mode: CutoutShapeMode,
    displayMode: CapsuleDisplayMode,
    offsetXdp: Float,
    offsetYdp: Float,
    hasExpandedContent: Boolean,
  ) {
    androidx.compose.runtime.SideEffect {
      updateBoundsFromState()
    }
  }

  private fun createLayoutParams(density: Float): WindowManager.LayoutParams {
    val windowType =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
      } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
      }

    val flags =
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

    return WindowManager.LayoutParams(
      (180f * density).roundToInt(),
      (56f * density).roundToInt(),
      windowType,
      flags,
      PixelFormat.TRANSLUCENT,
    ).apply {
      gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
      x = 0
      y = (8f * density).roundToInt()
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        layoutInDisplayCutoutMode =
          WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
      }
    }
  }

  private fun handleSystemDragEvent(event: DragEvent): Boolean {
    return when (event.action) {
      DragEvent.ACTION_DRAG_STARTED -> true
      DragEvent.ACTION_DRAG_ENTERED -> {
        CapsuleStateHolder.setDragHovering(true)
        true
      }
      DragEvent.ACTION_DRAG_EXITED -> {
        CapsuleStateHolder.setDragHovering(false)
        true
      }
      DragEvent.ACTION_DROP -> {
        val clip = event.clipData
        if (clip != null) {
          dragAndGoProcessor.processClipData(clip, sourceLabel = "System Drag & Go")
        } else {
          CapsuleStateHolder.setDragHovering(false)
        }
        true
      }
      DragEvent.ACTION_DRAG_ENDED -> {
        CapsuleStateHolder.setDragHovering(false)
        true
      }
      else -> false
    }
  }
}

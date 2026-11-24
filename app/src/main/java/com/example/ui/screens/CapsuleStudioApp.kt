package com.example.ui.screens

import android.content.ClipDescription
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CapsuleUiState
import com.example.model.CutoutConfig
import com.example.model.CutoutShapeMode
import com.example.model.DragAction
import com.example.state.CapsuleStateHolder
import com.example.theme.CapsuleColors
import com.example.theme.CapsuleTypography
import com.example.theme.OledText
import com.example.ui.capsule.CapsuleOverlayRoot
import com.example.ui.icons.CutoutGlyph
import com.example.ui.icons.DropZoneGlyph
import com.example.ui.icons.SparklePulseGlyph

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CapsuleStudioApp(
  uiState: CapsuleUiState,
  onUpdateConfig: (CutoutConfig) -> Unit,
  onToggleOverlayService: (Boolean) -> Unit,
  onRequestOverlayPermission: () -> Unit,
  onRequestNotificationListener: () -> Unit,
  onRequestRuntimePermissions: () -> Unit,
  onOpenFlashlight: () -> Unit,
  onStartVoiceMemo: () -> Unit,
  onStartSatellite: () -> Unit,
  onCycleRinger: () -> Unit,
  onStopRecording: () -> Unit,
  onStopSatellite: () -> Unit,
  onFlashlightDrag: (Float, Float) -> Unit,
  onFlashlightOff: () -> Unit,
  onToggleMute: (String) -> Unit,
  onToggleSpeaker: (String) -> Unit,
  onMediaPlayPause: (String) -> Unit,
  onMediaNext: (String) -> Unit,
  onMediaPrev: (String) -> Unit,
  onDragAction: (DragAction) -> Unit,
  onPickImageForOcr: () -> Unit,
  onDropClipData: (android.content.ClipData, String) -> Unit,
  onDropTextPayload: (String, String) -> Unit,
  onDeleteQuickNote: (String) -> Unit,
  onCopyQuickNote: (String) -> Unit,
) {
  var selectedTab by remember { mutableIntStateOf(0) }

  val dropTarget =
    remember {
      object : DragAndDropTarget {
        override fun onEntered(event: DragAndDropEvent) {
          CapsuleStateHolder.setDragHovering(true)
        }

        override fun onExited(event: DragAndDropEvent) {
          CapsuleStateHolder.setDragHovering(false)
        }

        override fun onEnded(event: DragAndDropEvent) {
          CapsuleStateHolder.setDragHovering(false)
        }

        override fun onDrop(event: DragAndDropEvent): Boolean {
          val clipData = event.toAndroidDragEvent().clipData
          if (clipData != null) {
            onDropClipData(clipData, "In-App Drag & Go")
          } else {
            CapsuleStateHolder.setDragHovering(false)
          }
          return true
        }
      }
    }

  Box(
    modifier = Modifier.fillMaxSize().background(CapsuleColors.SurfaceDark),
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top Interactive Cutout & Capsule Drop Stage
      val cfg = uiState.cutoutConfig
      val stageTopPadding =
        if (cfg.mode == CutoutShapeMode.TOP_DOCKED_NOTCH) Modifier
        else Modifier.windowInsetsPadding(WindowInsets.statusBars)

      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .then(stageTopPadding)
            .background(CapsuleColors.SurfaceDark)
            .dragAndDropTarget(
              shouldStartDragAndDrop = { event ->
                event.mimeTypes().any {
                  it == ClipDescription.MIMETYPE_TEXT_PLAIN || it.startsWith("image/")
                }
              },
              target = dropTarget,
            )
            .padding(bottom = 10.dp),
      ) {
        val yOffset =
          if (cfg.mode == CutoutShapeMode.TOP_DOCKED_NOTCH) 0.dp else cfg.offsetYdp.dp
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .offset(x = cfg.offsetXdp.dp, y = yOffset),
          contentAlignment = Alignment.TopCenter,
        ) {
          CapsuleOverlayRoot(
            uiState = uiState,
            onToggleExpand = { CapsuleStateHolder.toggleExpanded() },
            onCollapse = { CapsuleStateHolder.collapseNow() },
            onCycleActivity = { CapsuleStateHolder.cycleLiveActivity() },
            onFlashlightDrag = onFlashlightDrag,
            onFlashlightOff = onFlashlightOff,
            onStopRecording = onStopRecording,
            onStopSatellite = onStopSatellite,
            onToggleMute = onToggleMute,
            onToggleSpeaker = onToggleSpeaker,
            onMediaPlayPause = onMediaPlayPause,
            onMediaNext = onMediaNext,
            onMediaPrev = onMediaPrev,
            onDragAction = onDragAction,
            onDismissPopup = { id -> CapsuleStateHolder.dismissHardwarePopup(id) },
          )
        }
      }

      // Header Row
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          OledText("Capsule", style = CapsuleTypography.Title.copy(fontSize = 22.sp))
          OledText(
            text =
              when (selectedTab) {
                0 -> "Cutout & Notch Studio"
                1 -> "Hardware & Live Activities"
                else -> "Drag & Go Lab"
              },
            style = CapsuleTypography.Caption,
          )
        }
        Box(
          modifier =
            Modifier
              .clip(RoundedCornerShape(50))
              .background(CapsuleColors.PitchBlack)
              .border(0.5.dp, CapsuleColors.Hairline, RoundedCornerShape(50))
              .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
          OledText(
            text = if (uiState.overlayServiceRunning) "LIVE" else "STAGE",
            style = CapsuleTypography.TabularNumeric.copy(fontSize = 11.sp),
            color = if (uiState.overlayServiceRunning) CapsuleColors.Emerald else CapsuleColors.TextSecondary,
          )
        }
      }

      // Active Tab Content
      Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        when (selectedTab) {
          0 ->
            CutoutCustomizerTab(
              uiState = uiState,
              onUpdateConfig = onUpdateConfig,
              onToggleOverlayService = onToggleOverlayService,
              onRequestOverlayPermission = onRequestOverlayPermission,
              onRequestNotificationListener = onRequestNotificationListener,
              onRequestRuntimePermissions = onRequestRuntimePermissions,
            )
          1 ->
            ControlDeckTab(
              uiState = uiState,
              onOpenFlashlight = onOpenFlashlight,
              onStartVoiceMemo = onStartVoiceMemo,
              onStartSatellite = onStartSatellite,
              onCycleRinger = onCycleRinger,
            )
          else ->
            DragAndGoLabTab(
              uiState = uiState,
              onPickImageForOcr = onPickImageForOcr,
              onDropTextPayload = onDropTextPayload,
              onDeleteQuickNote = onDeleteQuickNote,
              onCopyQuickNote = onCopyQuickNote,
            )
        }
      }
    }

    // Floating OLED Glass Bottom Pill Switcher (No Material NavigationBar)
    Box(
      modifier =
        Modifier
          .align(Alignment.BottomCenter)
          .windowInsetsPadding(WindowInsets.navigationBars)
          .padding(horizontal = 24.dp, vertical = 14.dp),
    ) {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(CapsuleColors.PitchBlack)
            .border(0.5.dp, CapsuleColors.HairlineStrong, RoundedCornerShape(50))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        BottomPillTab(
          label = "Cutout",
          selected = selectedTab == 0,
          icon = { tint -> CutoutGlyph(tint = tint, size = 16.dp) },
          onClick = { selectedTab = 0 },
          modifier = Modifier.weight(1f).testTag("tab_cutout"),
        )
        BottomPillTab(
          label = "Activities",
          selected = selectedTab == 1,
          icon = { tint -> SparklePulseGlyph(tint = tint, size = 16.dp) },
          onClick = { selectedTab = 1 },
          modifier = Modifier.weight(1f).testTag("tab_activities"),
        )
        BottomPillTab(
          label = "Drag & Go",
          selected = selectedTab == 2,
          icon = { tint -> DropZoneGlyph(tint = tint, size = 16.dp) },
          onClick = { selectedTab = 2 },
          modifier = Modifier.weight(1f).testTag("tab_drag_go"),
        )
      }
    }
  }
}

@Composable
private fun BottomPillTab(
  label: String,
  selected: Boolean,
  icon: @Composable (Color) -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val tint = if (selected) CapsuleColors.TextPrimary else CapsuleColors.TextSecondary
  Row(
    modifier =
      modifier
        .height(48.dp)
        .clip(RoundedCornerShape(50))
        .background(if (selected) CapsuleColors.SurfaceElevated else Color.Transparent)
        .border(
          0.5.dp,
          if (selected) CapsuleColors.Hairline else Color.Transparent,
          RoundedCornerShape(50),
        )
        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    icon(tint)
    Spacer(Modifier.padding(horizontal = 3.dp))
    OledText(
      text = label,
      style =
        CapsuleTypography.Caption.copy(
          fontSize = 12.sp,
          fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        ),
      color = tint,
    )
  }
}

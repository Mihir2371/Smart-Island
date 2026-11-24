package com.example.ui.capsule

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CapsuleDisplayMode
import com.example.model.CapsuleUiState
import com.example.model.CutoutShapeMode
import com.example.model.DragAction
import com.example.model.HardwarePopup
import com.example.model.LiveActivity
import com.example.model.SystemToggleType
import com.example.theme.CapsuleColors
import com.example.theme.CapsuleSprings
import com.example.theme.CapsuleTypography
import com.example.theme.OledText
import com.example.ui.icons.AirplaneGlyph
import com.example.ui.icons.AudioVisualizerBars
import com.example.ui.icons.BellGlyph
import com.example.ui.icons.DownloadGlyph
import com.example.ui.icons.DropZoneGlyph
import com.example.ui.icons.FlashlightGlyph
import com.example.ui.icons.HeadphonesGlyph
import com.example.ui.icons.HotspotGlyph
import com.example.ui.icons.LightningBoltGlyph
import com.example.ui.icons.MoonGlyph
import com.example.ui.icons.NfcGlyph
import com.example.ui.icons.PhoneGlyph
import com.example.ui.icons.SatelliteGlyph
import com.example.ui.icons.TimerGlyph
import com.example.ui.icons.TurnArrowGlyph
import kotlin.math.roundToInt

@Composable
fun CapsuleOverlayRoot(
  uiState: CapsuleUiState,
  onToggleExpand: () -> Unit,
  onCollapse: () -> Unit,
  onCycleActivity: () -> Unit,
  onFlashlightDrag: (Float, Float) -> Unit,
  onFlashlightOff: () -> Unit,
  onStopRecording: () -> Unit,
  onStopSatellite: () -> Unit,
  onToggleMute: (String) -> Unit,
  onToggleSpeaker: (String) -> Unit,
  onMediaPlayPause: (String) -> Unit,
  onMediaNext: (String) -> Unit,
  onMediaPrev: (String) -> Unit,
  onDragAction: (DragAction) -> Unit,
  onDismissPopup: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val cfg = uiState.cutoutConfig
  val isExpanded =
    uiState.displayMode == CapsuleDisplayMode.EXPANDED ||
      uiState.isDragHovering ||
      uiState.dragDropResult != null
  val hasCompactContent =
    uiState.activeHardwarePopup != null || uiState.activeLiveActivity != null

  val targetWidth by
    animateDpAsState(
      targetValue =
        when {
          isExpanded -> 344.dp
          hasCompactContent -> (cfg.widthDp + 76f).coerceAtLeast(188f).dp
          else -> cfg.widthDp.dp
        },
      animationSpec = CapsuleSprings.fluid(),
      label = "capsule_width",
    )

  val targetHeight by
    animateDpAsState(
      targetValue =
        when {
          uiState.dragDropResult != null -> 212.dp
          uiState.isDragHovering -> 132.dp
          isExpanded -> 152.dp
          else -> cfg.heightDp.dp
        },
      animationSpec = CapsuleSprings.fluid(),
      label = "capsule_height",
    )

  val isTopNotch = cfg.mode == CutoutShapeMode.TOP_DOCKED_NOTCH && !isExpanded
  val topRadius by
    animateDpAsState(
      targetValue = if (isTopNotch) 0.dp else if (isExpanded) 28.dp else cfg.cornerRadiusDp.dp,
      animationSpec = CapsuleSprings.fluid(),
      label = "capsule_top_radius",
    )
  val bottomRadius by
    animateDpAsState(
      targetValue = if (isExpanded) 28.dp else cfg.cornerRadiusDp.dp,
      animationSpec = CapsuleSprings.fluid(),
      label = "capsule_bottom_radius",
    )

  val shape =
    RoundedCornerShape(
      topStart = topRadius,
      topEnd = topRadius,
      bottomStart = bottomRadius,
      bottomEnd = bottomRadius,
    )

  val borderColor =
    if (uiState.isDragHovering) CapsuleColors.Cyan.copy(alpha = 0.65f)
    else CapsuleColors.Hairline

  val interactionSource = remember { MutableInteractionSource() }

  Box(
    modifier = modifier.fillMaxWidth(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Row(verticalAlignment = Alignment.Top) {
      // Waterdrop concave left ear fillet when docked flush at top
      if (isTopNotch) {
        WaterdropCornerFillet(isLeft = true, sizeDp = 12.dp)
      }

      Box(
        modifier =
          Modifier
            .width(targetWidth)
            .height(targetHeight)
            .clip(shape)
            .background(CapsuleColors.PitchBlack)
            .border(0.5.dp, borderColor, shape)
            .clickable(
              interactionSource = interactionSource,
              indication = null,
              onClick = {
                if (uiState.dragDropResult != null) onCollapse() else onToggleExpand()
              },
            ),
        contentAlignment = Alignment.Center,
      ) {
        when {
          uiState.isDragHovering || uiState.dragDropResult != null -> {
            ExpandedDragAndGoContent(
              isHovering = uiState.isDragHovering,
              result = uiState.dragDropResult,
              onActionClick = onDragAction,
              onDismiss = onCollapse,
            )
          }
          isExpanded && uiState.activeHardwarePopup != null -> {
            ExpandedHardwareContent(
              popup = uiState.activeHardwarePopup,
              tickerMs = uiState.tickerMs,
              onFlashlightDrag = onFlashlightDrag,
              onFlashlightOff = onFlashlightOff,
              onStopRecording = onStopRecording,
              onStopSatellite = onStopSatellite,
              onDismiss = onDismissPopup,
            )
          }
          else -> {
            val activeActivity = uiState.activeLiveActivity
            if (isExpanded && activeActivity != null) {
              ExpandedLiveActivityContent(
                activity = activeActivity,
                tickerMs = uiState.tickerMs,
                hasMultiple = uiState.liveActivities.size > 1,
                onCycleActivity = onCycleActivity,
                onToggleMute = onToggleMute,
                onToggleSpeaker = onToggleSpeaker,
                onMediaPlayPause = onMediaPlayPause,
                onMediaNext = onMediaNext,
                onMediaPrev = onMediaPrev,
              )
            } else if (isExpanded) {
              ExpandedIdleContent(onCollapse = onCollapse)
            } else {
              CompactCapsuleContent(uiState = uiState)
            }
          }
        }
      }

      if (isTopNotch) {
        WaterdropCornerFillet(isLeft = false, sizeDp = 12.dp)
      }
    }
  }
}

@Composable
private fun WaterdropCornerFillet(isLeft: Boolean, sizeDp: androidx.compose.ui.unit.Dp) {
  Canvas(modifier = Modifier.size(sizeDp)) {
    val w = size.width
    val h = size.height
    val path =
      Path().apply {
        if (isLeft) {
          moveTo(w, 0f)
          lineTo(0f, 0f)
          quadraticTo(w, 0f, w, h)
          close()
        } else {
          moveTo(0f, 0f)
          lineTo(w, 0f)
          quadraticTo(0f, 0f, 0f, h)
          close()
        }
      }
    drawPath(path, color = CapsuleColors.PitchBlack)
  }
}

@Composable
private fun ExpandedIdleContent(onCollapse: () -> Unit) {
  Row(
    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      DropZoneGlyph(tint = CapsuleColors.Cyan, size = 20.dp)
      Spacer(Modifier.width(10.dp))
      OledText(
        text = "Capsule Ready · Drop or Trigger",
        style = CapsuleTypography.Body.copy(fontSize = 13.sp),
        color = CapsuleColors.TextSecondary,
      )
    }
    CapsuleActionChip(label = "Hide", accent = CapsuleColors.TextSecondary, onClick = onCollapse)
  }
}

@Composable
private fun CompactCapsuleContent(uiState: CapsuleUiState) {
  val privacy = uiState.privacyStatus
  val showPrivacy = uiState.cutoutConfig.showPrivacyDots
  val infiniteTransition = rememberInfiniteTransition(label = "compact_viz")
  val phase by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 6.283f,
      animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
      label = "compact_phase",
    )

  Row(
    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    // Left Wing
    AnimatedContent(
      targetState = uiState.activeHardwarePopup ?: uiState.activeLiveActivity,
      transitionSpec = { fadeIn() togetherWith fadeOut() },
      label = "compact_left",
    ) { target ->
      when (target) {
        is HardwarePopup.Flashlight -> FlashlightGlyph(isOn = target.isOn, size = 15.dp)
        is HardwarePopup.Charging -> LightningBoltGlyph(tint = CapsuleColors.Emerald, size = 15.dp)
        is HardwarePopup.LowBattery ->
          OledText("!", style = CapsuleTypography.TabularNumeric, color = CapsuleColors.Crimson)
        is HardwarePopup.BluetoothConnected -> HeadphonesGlyph(tint = CapsuleColors.Cyan, size = 15.dp)
        is HardwarePopup.SystemToggle ->
          when (target.type) {
            SystemToggleType.SILENT -> BellGlyph(slashed = true, tint = CapsuleColors.Crimson, size = 14.dp)
            SystemToggleType.VIBRATE, SystemToggleType.RING -> BellGlyph(slashed = false, tint = CapsuleColors.Amber, size = 14.dp)
            SystemToggleType.DND -> MoonGlyph(tint = CapsuleColors.Indigo, size = 14.dp)
            SystemToggleType.AIRPLANE -> AirplaneGlyph(tint = CapsuleColors.Amber, size = 14.dp)
            SystemToggleType.HOTSPOT -> HotspotGlyph(tint = CapsuleColors.Sapphire, size = 14.dp)
          }
        is HardwarePopup.Recording ->
          Box(Modifier.size(9.dp).clip(CircleShape).background(CapsuleColors.Crimson))
        is HardwarePopup.NfcScan -> NfcGlyph(tint = CapsuleColors.Cyan, size = 15.dp)
        is HardwarePopup.SatelliteAlignment ->
          SatelliteGlyph(
            tint = if (target.isLocked) CapsuleColors.Emerald else CapsuleColors.Amber,
            size = 15.dp,
          )
        is LiveActivity.PhoneCall -> PhoneGlyph(tint = CapsuleColors.Emerald, size = 14.dp)
        is LiveActivity.MediaPlayback ->
          AudioVisualizerBars(isPlaying = target.isPlaying, phase = phase, tint = CapsuleColors.Cyan, size = 15.dp)
        is LiveActivity.Navigation -> TurnArrowGlyph(target.direction, tint = CapsuleColors.Sapphire, size = 15.dp)
        is LiveActivity.Rideshare ->
          OledText(target.provider, style = CapsuleTypography.Caption.copy(fontWeight = FontWeight.Bold), color = CapsuleColors.Cyan)
        is LiveActivity.Delivery ->
          OledText("DEL", style = CapsuleTypography.Caption.copy(fontWeight = FontWeight.Bold), color = CapsuleColors.Amber)
        is LiveActivity.Flight ->
          OledText(target.flightCode, style = CapsuleTypography.TabularNumeric.copy(fontSize = 11.sp), color = CapsuleColors.Cyan)
        is LiveActivity.Sports ->
          OledText(target.homeTeam, style = CapsuleTypography.Caption.copy(fontWeight = FontWeight.Bold))
        is LiveActivity.TimerActivity -> TimerGlyph(tint = CapsuleColors.Amber, size = 14.dp)
        is LiveActivity.DownloadActivity -> DownloadGlyph(tint = CapsuleColors.Sapphire, size = 14.dp)
        else -> Spacer(Modifier.width(4.dp))
      }
    }

    // Center Camera Cutout Spacer + Right Wing + Embedded Privacy Dots
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      val target = uiState.activeHardwarePopup ?: uiState.activeLiveActivity
      when (target) {
        is HardwarePopup.Flashlight ->
          OledText("${(target.brightness * 100).roundToInt()}%", style = CapsuleTypography.TabularNumeric.copy(fontSize = 11.sp), color = CapsuleColors.Amber)
        is HardwarePopup.Charging ->
          OledText("${target.level}%", style = CapsuleTypography.TabularNumeric.copy(fontSize = 11.sp), color = CapsuleColors.Emerald)
        is HardwarePopup.LowBattery ->
          OledText("${target.level}%", style = CapsuleTypography.TabularNumeric.copy(fontSize = 11.sp), color = CapsuleColors.Crimson)
        is HardwarePopup.BluetoothConnected ->
          OledText("${target.batteryPercent}%", style = CapsuleTypography.TabularNumeric.copy(fontSize = 11.sp), color = CapsuleColors.Emerald)
        is HardwarePopup.SystemToggle ->
          OledText(target.label, style = CapsuleTypography.Caption.copy(fontSize = 11.sp), color = CapsuleColors.TextPrimary)
        is HardwarePopup.Recording -> {
          val sec = ((uiState.tickerMs - target.startedAtMs).coerceAtLeast(0L) / 1000L)
          OledText(formatDuration(sec), style = CapsuleTypography.TabularNumeric.copy(fontSize = 11.sp), color = CapsuleColors.Crimson)
        }
        is LiveActivity.PhoneCall -> {
          val sec = ((uiState.tickerMs - target.startedAtMs).coerceAtLeast(0L) / 1000L)
          OledText(formatDuration(sec), style = CapsuleTypography.TabularNumeric.copy(fontSize = 11.sp), color = CapsuleColors.Emerald)
        }
        is LiveActivity.Navigation ->
          OledText(target.distanceText, style = CapsuleTypography.TabularNumeric.copy(fontSize = 11.sp), color = CapsuleColors.Sapphire)
        is LiveActivity.Rideshare ->
          OledText("${target.etaMinutes}m", style = CapsuleTypography.TabularNumeric.copy(fontSize = 11.sp), color = CapsuleColors.Emerald)
        is LiveActivity.Delivery ->
          OledText("${target.etaMinutes}m", style = CapsuleTypography.TabularNumeric.copy(fontSize = 11.sp), color = CapsuleColors.Amber)
        is LiveActivity.Flight ->
          OledText(target.gate, style = CapsuleTypography.TabularNumeric.copy(fontSize = 11.sp), color = CapsuleColors.Emerald)
        is LiveActivity.Sports ->
          OledText("${target.homeScore}:${target.awayScore}", style = CapsuleTypography.TabularNumeric.copy(fontSize = 11.sp), color = CapsuleColors.Emerald)
        is LiveActivity.TimerActivity -> {
          val sec = ((target.endTimeMs - uiState.tickerMs).coerceAtLeast(0L) / 1000L)
          OledText(formatDuration(sec), style = CapsuleTypography.TabularNumeric.copy(fontSize = 11.sp), color = CapsuleColors.Amber)
        }
        is LiveActivity.DownloadActivity ->
          OledText("${(target.progress * 100).roundToInt()}%", style = CapsuleTypography.TabularNumeric.copy(fontSize = 11.sp), color = CapsuleColors.Sapphire)
        else -> Unit
      }

      if (showPrivacy) {
        if (privacy.effectiveCamera) {
          Box(Modifier.size(7.dp).clip(CircleShape).background(CapsuleColors.Emerald))
        }
        if (privacy.effectiveMic) {
          Box(Modifier.size(7.dp).clip(CircleShape).background(CapsuleColors.Amber))
        }
      }
    }
  }
}

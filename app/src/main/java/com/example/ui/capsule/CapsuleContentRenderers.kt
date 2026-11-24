package com.example.ui.capsule

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DragAction
import com.example.model.DragActionType
import com.example.model.DragDropResult
import com.example.model.HardwarePopup
import com.example.model.LiveActivity
import com.example.model.SystemToggleType
import com.example.theme.CapsuleColors
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
import com.example.ui.icons.MediaControlGlyph
import com.example.ui.icons.MoonGlyph
import com.example.ui.icons.NfcGlyph
import com.example.ui.icons.PhoneGlyph
import com.example.ui.icons.SatelliteGlyph
import com.example.ui.icons.TimerGlyph
import com.example.ui.icons.TurnArrowGlyph
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ExpandedHardwareContent(
  popup: HardwarePopup,
  tickerMs: Long,
  onFlashlightDrag: (Float, Float) -> Unit,
  onFlashlightOff: () -> Unit,
  onStopRecording: () -> Unit,
  onStopSatellite: () -> Unit,
  onDismiss: (String) -> Unit,
) {
  when (popup) {
    is HardwarePopup.Flashlight -> {
      Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Interactive Touch-Drag Beam Canvas
        Box(
          modifier =
            Modifier
              .weight(1f)
              .fillMaxHeight()
              .clip(RoundedCornerShape(16.dp))
              .background(CapsuleColors.SurfaceDark)
              .border(0.5.dp, CapsuleColors.Hairline, RoundedCornerShape(16.dp))
              .pointerInput(popup.brightness, popup.beamSpread) {
                detectDragGestures { change, dragAmount ->
                  change.consume()
                  val nextBrightness = (popup.brightness - dragAmount.y / 220f).coerceIn(0.08f, 1f)
                  val nextSpread = (popup.beamSpread + dragAmount.x / 220f).coerceIn(0.2f, 1f)
                  onFlashlightDrag(nextBrightness, nextSpread)
                }
              },
          contentAlignment = Alignment.Center,
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val origin = Offset(w * 0.5f, h * 0.86f)
            val halfSpread = w * (0.14f + 0.34f * popup.beamSpread)
            val topY = h * (0.55f - 0.45f * popup.brightness)
            val beamPath =
              Path().apply {
                moveTo(origin.x - w * 0.06f, origin.y)
                lineTo(origin.x - halfSpread, topY)
                lineTo(origin.x + halfSpread, topY)
                lineTo(origin.x + w * 0.06f, origin.y)
                close()
              }
            if (popup.isOn) {
              drawPath(
                path = beamPath,
                brush =
                  Brush.verticalGradient(
                    colors =
                      listOf(
                        CapsuleColors.Amber.copy(alpha = 0.04f),
                        CapsuleColors.Amber.copy(alpha = 0.55f * popup.brightness),
                      ),
                    startY = topY,
                    endY = origin.y,
                  ),
              )
            }
            drawRoundRect(
              color = if (popup.isOn) CapsuleColors.Amber else CapsuleColors.TextTertiary,
              topLeft = Offset(origin.x - 10f, origin.y - 6f),
              size = Size(20f, 10f),
              cornerRadius = CornerRadius(4f),
            )
          }
          Column(
            modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
          ) {
            OledText(
              text = "Drag Beam",
              style = CapsuleTypography.Caption.copy(fontSize = 11.sp),
              color = CapsuleColors.TextSecondary,
            )
            OledText(
              text = "${(popup.brightness * 100).roundToInt()}% · ${(popup.beamSpread * 100).roundToInt()}°",
              style = CapsuleTypography.TabularNumeric.copy(fontSize = 12.sp),
              color = CapsuleColors.Amber,
            )
          }
        }
        Spacer(Modifier.width(12.dp))
        Column(
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxHeight(),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            FlashlightGlyph(isOn = popup.isOn, size = 20.dp)
            Spacer(Modifier.width(6.dp))
            OledText(
              text = if (popup.isOn) "Torch On" else "Torch Off",
              style = CapsuleTypography.Body.copy(fontWeight = FontWeight.SemiBold),
            )
          }
          CapsuleActionChip(
            label = if (popup.isOn) "Turn Off" else "Close",
            accent = if (popup.isOn) CapsuleColors.Amber else CapsuleColors.TextSecondary,
            onClick = onFlashlightOff,
          )
        }
      }
    }

    is HardwarePopup.Charging -> {
      Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier =
              Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(CapsuleColors.Emerald.copy(alpha = 0.14f))
                .border(0.5.dp, CapsuleColors.Emerald.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            LightningBoltGlyph(tint = CapsuleColors.Emerald, size = 22.dp)
          }
          Spacer(Modifier.width(12.dp))
          Column {
            OledText(
              text = if (popup.isFastCharging) "Fast Charging" else "Charging",
              style = CapsuleTypography.Body.copy(fontWeight = FontWeight.SemiBold),
            )
            OledText(
              text = "${popup.wattageEstimate}W Power Delivery",
              style = CapsuleTypography.Caption,
              color = CapsuleColors.Emerald,
            )
          }
        }
        OledText(
          text = "${popup.level}%",
          style = CapsuleTypography.TabularLarge,
          color = CapsuleColors.Emerald,
        )
      }
    }

    is HardwarePopup.LowBattery -> {
      Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier =
              Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(CapsuleColors.Crimson.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
          ) {
            OledText(
              text = "!",
              style = CapsuleTypography.TabularLarge,
              color = CapsuleColors.Crimson,
            )
          }
          Spacer(Modifier.width(12.dp))
          Column {
            OledText("Low Battery", style = CapsuleTypography.Body.copy(fontWeight = FontWeight.SemiBold))
            OledText("Connect charger soon", style = CapsuleTypography.Caption, color = CapsuleColors.TextSecondary)
          }
        }
        OledText("${popup.level}%", style = CapsuleTypography.TabularLarge, color = CapsuleColors.Crimson)
      }
    }

    is HardwarePopup.BluetoothConnected -> {
      Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Box(
            modifier =
              Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(CapsuleColors.Cyan.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
          ) {
            HeadphonesGlyph(tint = CapsuleColors.Cyan, size = 22.dp)
          }
          Spacer(Modifier.width(12.dp))
          Column {
            OledText(popup.deviceName, style = CapsuleTypography.Body.copy(fontWeight = FontWeight.SemiBold))
            OledText("Audio Connected", style = CapsuleTypography.Caption, color = CapsuleColors.Cyan)
          }
        }
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
              color = CapsuleColors.SurfaceGlass,
              startAngle = 0f,
              sweepAngle = 360f,
              useCenter = false,
              style = Stroke(width = 3.dp.toPx()),
            )
            drawArc(
              color = CapsuleColors.Emerald,
              startAngle = -90f,
              sweepAngle = 360f * (popup.batteryPercent / 100f),
              useCenter = false,
              style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )
          }
          OledText(
            text = "${popup.batteryPercent}%",
            style = CapsuleTypography.TabularNumeric.copy(fontSize = 11.sp),
          )
        }
      }
    }

    is HardwarePopup.SystemToggle -> {
      Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          val tint =
            when (popup.type) {
              SystemToggleType.SILENT -> CapsuleColors.Crimson
              SystemToggleType.VIBRATE -> CapsuleColors.Amber
              SystemToggleType.RING -> CapsuleColors.Emerald
              SystemToggleType.DND -> CapsuleColors.Indigo
              SystemToggleType.AIRPLANE -> CapsuleColors.Amber
              SystemToggleType.HOTSPOT -> CapsuleColors.Sapphire
            }
          Box(
            modifier =
              Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
          ) {
            when (popup.type) {
              SystemToggleType.SILENT -> BellGlyph(slashed = true, tint = tint, size = 20.dp)
              SystemToggleType.VIBRATE, SystemToggleType.RING -> BellGlyph(slashed = false, tint = tint, size = 20.dp)
              SystemToggleType.DND -> MoonGlyph(tint = tint, size = 20.dp)
              SystemToggleType.AIRPLANE -> AirplaneGlyph(tint = tint, size = 20.dp)
              SystemToggleType.HOTSPOT -> HotspotGlyph(tint = tint, size = 20.dp)
            }
          }
          Spacer(Modifier.width(12.dp))
          Column {
            OledText(popup.label, style = CapsuleTypography.Body.copy(fontWeight = FontWeight.SemiBold))
            OledText(popup.subtitle, style = CapsuleTypography.Caption, color = CapsuleColors.TextSecondary)
          }
        }
        OledText(
          text = if (popup.isActive) "ON" else "OFF",
          style = CapsuleTypography.TabularNumeric,
          color = if (popup.isActive) CapsuleColors.Emerald else CapsuleColors.TextSecondary,
        )
      }
    }

    is HardwarePopup.Recording -> {
      val elapsedSec = ((tickerMs - popup.startedAtMs).coerceAtLeast(0L) / 1000L)
      Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier.size(10.dp).clip(CircleShape).background(CapsuleColors.Crimson)
            )
            Spacer(Modifier.width(8.dp))
            OledText(
              text = if (popup.isScreenRecording) "Screen Recording" else "Voice Memo",
              style = CapsuleTypography.Body.copy(fontWeight = FontWeight.SemiBold),
            )
          }
          OledText(
            text = formatDuration(elapsedSec),
            style = CapsuleTypography.TabularNumeric.copy(fontSize = 15.sp),
            color = CapsuleColors.Crimson,
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          // Real-time 16-bar audio waveform
          Row(
            modifier = Modifier.weight(1f).height(34.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            popup.amplitudes.forEach { amp ->
              Box(
                modifier =
                  Modifier
                    .width(5.dp)
                    .fillMaxHeight(amp.coerceIn(0.14f, 1f))
                    .clip(RoundedCornerShape(50))
                    .background(CapsuleColors.Crimson.copy(alpha = 0.85f))
              )
            }
          }
          Spacer(Modifier.width(12.dp))
          CapsuleActionChip(
            label = "Stop",
            accent = CapsuleColors.Crimson,
            onClick = onStopRecording,
          )
        }
      }
    }

    is HardwarePopup.NfcScan -> {
      Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier =
              Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(CapsuleColors.Cyan.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
          ) {
            NfcGlyph(tint = CapsuleColors.Cyan, size = 22.dp)
          }
          Spacer(Modifier.width(12.dp))
          Column {
            OledText("NFC Verified", style = CapsuleTypography.Body.copy(fontWeight = FontWeight.SemiBold))
            OledText(popup.tagSummary, style = CapsuleTypography.Caption, color = CapsuleColors.Cyan)
          }
        }
        CapsuleActionChip(label = "Done", accent = CapsuleColors.TextSecondary, onClick = { onDismiss(popup.id) })
      }
    }

    is HardwarePopup.SatelliteAlignment -> {
      Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            SatelliteGlyph(
              tint = if (popup.isLocked) CapsuleColors.Emerald else CapsuleColors.Amber,
              size = 18.dp,
            )
            Spacer(Modifier.width(8.dp))
            OledText(
              text = if (popup.isLocked) "Satellite Connected" else "Align to Satellite",
              style = CapsuleTypography.Body.copy(fontWeight = FontWeight.SemiBold),
            )
          }
          OledText(
            text = "${popup.signalBars}/4",
            style = CapsuleTypography.TabularNumeric,
            color = if (popup.isLocked) CapsuleColors.Emerald else CapsuleColors.Amber,
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Canvas(modifier = Modifier.weight(1f).height(44.dp)) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.5f
            drawCircle(
              color = CapsuleColors.HairlineStrong,
              radius = 18.dp.toPx(),
              center = Offset(cx, cy),
              style = Stroke(width = 1.dp.toPx()),
            )
            val targetX = (cx + (popup.azimuthDelta / 90f) * (size.width * 0.35f)).coerceIn(16f, size.width - 16f)
            val targetY = (cy + (popup.pitchDelta / 45f) * 14.dp.toPx()).coerceIn(10f, size.height - 10f)
            drawCircle(
              color = if (popup.isLocked) CapsuleColors.Emerald else CapsuleColors.Amber,
              radius = 6.dp.toPx(),
              center = Offset(targetX, targetY),
            )
          }
          Spacer(Modifier.width(12.dp))
          CapsuleActionChip(label = "Exit", accent = CapsuleColors.TextSecondary, onClick = onStopSatellite)
        }
      }
    }
  }
}

@Composable
fun ExpandedLiveActivityContent(
  activity: LiveActivity,
  tickerMs: Long,
  hasMultiple: Boolean,
  onCycleActivity: () -> Unit,
  onToggleMute: (String) -> Unit,
  onToggleSpeaker: (String) -> Unit,
  onMediaPlayPause: (String) -> Unit,
  onMediaNext: (String) -> Unit,
  onMediaPrev: (String) -> Unit,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "live_viz")
  val phase by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 6.283f,
      animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
      label = "viz_phase",
    )

  when (activity) {
    is LiveActivity.PhoneCall -> {
      val elapsed = ((tickerMs - activity.startedAtMs).coerceAtLeast(0L) / 1000L)
      Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
              modifier = Modifier.size(40.dp).clip(CircleShape).background(CapsuleColors.Emerald.copy(alpha = 0.16f)),
              contentAlignment = Alignment.Center,
            ) {
              PhoneGlyph(tint = CapsuleColors.Emerald, size = 20.dp)
            }
            Spacer(Modifier.width(10.dp))
            Column {
              OledText(activity.appLabel, style = CapsuleTypography.Caption, color = CapsuleColors.Emerald)
              OledText(activity.callerName, style = CapsuleTypography.Body.copy(fontWeight = FontWeight.SemiBold))
            }
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            AudioVisualizerBars(isPlaying = true, phase = phase, tint = CapsuleColors.Emerald, size = 18.dp)
            Spacer(Modifier.width(8.dp))
            OledText(formatDuration(elapsed), style = CapsuleTypography.TabularNumeric, color = CapsuleColors.Emerald)
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          CapsuleActionChip(
            label = if (activity.isMuted) "Unmute" else "Mute",
            accent = if (activity.isMuted) CapsuleColors.Crimson else CapsuleColors.TextPrimary,
            onClick = { onToggleMute(activity.id) },
            modifier = Modifier.weight(1f),
          )
          CapsuleActionChip(
            label = if (activity.isSpeaker) "Speaker On" else "Speaker",
            accent = if (activity.isSpeaker) CapsuleColors.Sapphire else CapsuleColors.TextPrimary,
            onClick = { onToggleSpeaker(activity.id) },
            modifier = Modifier.weight(1f),
          )
          if (hasMultiple) {
            CapsuleActionChip(label = "Next", accent = CapsuleColors.TextSecondary, onClick = onCycleActivity)
          }
        }
      }
    }

    is LiveActivity.MediaPlayback -> {
      val frac = (activity.positionMs.toFloat() / activity.durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
      Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            if (activity.albumArt != null) {
              Image(
                bitmap = activity.albumArt.asImageBitmap(),
                contentDescription = "Album art",
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop,
              )
            } else {
              Box(
                modifier =
                  Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                      Brush.linearGradient(listOf(CapsuleColors.Indigo, CapsuleColors.Sapphire))
                    ),
                contentAlignment = Alignment.Center,
              ) {
                AudioVisualizerBars(isPlaying = activity.isPlaying, phase = phase, tint = Color.White, size = 20.dp)
              }
            }
            Spacer(Modifier.width(12.dp))
            Column {
              OledText(activity.title, style = CapsuleTypography.Body.copy(fontWeight = FontWeight.SemiBold))
              OledText(activity.artist, style = CapsuleTypography.Caption, color = CapsuleColors.TextSecondary)
            }
          }
          AudioVisualizerBars(isPlaying = activity.isPlaying, phase = phase, tint = CapsuleColors.Cyan, size = 20.dp)
        }
        // Thin progress bar
        Box(
          modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(50)).background(CapsuleColors.SurfaceGlass)
        ) {
          Box(
            modifier = Modifier.fillMaxWidth(frac).fillMaxHeight().background(CapsuleColors.TextPrimary)
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OledText(
            text = formatDuration(activity.positionMs / 1000L),
            style = CapsuleTypography.TabularNumeric.copy(fontSize = 11.sp),
            color = CapsuleColors.TextSecondary,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            CapsuleIconHitTarget(onClick = { onMediaPrev(activity.packageName) }) {
              MediaControlGlyph("prev", size = 20.dp)
            }
            CapsuleIconHitTarget(onClick = { onMediaPlayPause(activity.packageName) }) {
              MediaControlGlyph(if (activity.isPlaying) "pause" else "play", size = 22.dp)
            }
            CapsuleIconHitTarget(onClick = { onMediaNext(activity.packageName) }) {
              MediaControlGlyph("next", size = 20.dp)
            }
          }
          OledText(
            text = formatDuration(activity.durationMs / 1000L),
            style = CapsuleTypography.TabularNumeric.copy(fontSize = 11.sp),
            color = CapsuleColors.TextSecondary,
          )
        }
      }
    }

    is LiveActivity.Navigation -> {
      Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Box(
            modifier =
              Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CapsuleColors.Sapphire.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center,
          ) {
            TurnArrowGlyph(activity.direction, tint = CapsuleColors.Sapphire, size = 24.dp)
          }
          Spacer(Modifier.width(12.dp))
          Column {
            OledText(activity.distanceText, style = CapsuleTypography.TabularLarge)
            OledText(activity.streetName, style = CapsuleTypography.Caption, color = CapsuleColors.TextSecondary)
          }
        }
        Column(horizontalAlignment = Alignment.End) {
          OledText("ETA", style = CapsuleTypography.Caption, color = CapsuleColors.TextTertiary)
          OledText(activity.etaText, style = CapsuleTypography.TabularNumeric, color = CapsuleColors.Emerald)
        }
      }
    }

    is LiveActivity.Rideshare -> {
      Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          OledText(activity.provider.uppercase(), style = CapsuleTypography.Caption, color = CapsuleColors.Cyan)
          OledText(activity.statusText, style = CapsuleTypography.Body.copy(fontWeight = FontWeight.SemiBold))
          OledText(activity.vehicleInfo, style = CapsuleTypography.Caption, color = CapsuleColors.TextSecondary)
        }
        Column(horizontalAlignment = Alignment.End) {
          OledText("${activity.etaMinutes} min", style = CapsuleTypography.TabularLarge, color = CapsuleColors.Emerald)
          OledText("Arrival", style = CapsuleTypography.Caption, color = CapsuleColors.TextSecondary)
        }
      }
    }

    is LiveActivity.Delivery -> {
      Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            OledText(activity.provider, style = CapsuleTypography.Body.copy(fontWeight = FontWeight.SemiBold))
            OledText(activity.statusText, style = CapsuleTypography.Caption, color = CapsuleColors.Amber)
          }
          OledText("${activity.etaMinutes} min", style = CapsuleTypography.TabularLarge, color = CapsuleColors.Amber)
        }
        Box(
          modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(CapsuleColors.SurfaceGlass)
        ) {
          Box(
            modifier = Modifier.fillMaxWidth(activity.progress.coerceIn(0.05f, 1f)).fillMaxHeight().background(CapsuleColors.Amber)
          )
        }
      }
    }

    is LiveActivity.Flight -> {
      Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column {
          OledText(activity.flightCode, style = CapsuleTypography.TabularLarge, color = CapsuleColors.Cyan)
          OledText(activity.statusText, style = CapsuleTypography.Caption, color = CapsuleColors.TextSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
          Column(horizontalAlignment = Alignment.End) {
            OledText("GATE", style = CapsuleTypography.Caption, color = CapsuleColors.TextTertiary)
            OledText(activity.gate, style = CapsuleTypography.TabularNumeric.copy(fontSize = 18.sp))
          }
          Column(horizontalAlignment = Alignment.End) {
            OledText("BOARD", style = CapsuleTypography.Caption, color = CapsuleColors.TextTertiary)
            OledText("${activity.boardingMinutes}m", style = CapsuleTypography.TabularNumeric.copy(fontSize = 18.sp), color = CapsuleColors.Emerald)
          }
        }
      }
    }

    is LiveActivity.Sports -> {
      Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(horizontalAlignment = Alignment.Start) {
          OledText(activity.league, style = CapsuleTypography.Caption, color = CapsuleColors.TextTertiary)
          OledText(activity.homeTeam, style = CapsuleTypography.Body.copy(fontWeight = FontWeight.Bold))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          OledText(
            text = "${activity.homeScore} : ${activity.awayScore}",
            style = CapsuleTypography.TabularLarge,
          )
          OledText(activity.clockText, style = CapsuleTypography.TabularNumeric.copy(fontSize = 11.sp), color = CapsuleColors.Emerald)
        }
        Column(horizontalAlignment = Alignment.End) {
          OledText("LIVE", style = CapsuleTypography.Caption, color = CapsuleColors.Crimson)
          OledText(activity.awayTeam, style = CapsuleTypography.Body.copy(fontWeight = FontWeight.Bold))
        }
      }
    }

    is LiveActivity.TimerActivity -> {
      val remainingSec = ((activity.endTimeMs - tickerMs).coerceAtLeast(0L) / 1000L)
      val frac = ((activity.endTimeMs - tickerMs).toFloat() / activity.totalMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
      Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            TimerGlyph(tint = CapsuleColors.Amber, size = 20.dp)
            Spacer(Modifier.width(8.dp))
            OledText(activity.label, style = CapsuleTypography.Body.copy(fontWeight = FontWeight.SemiBold))
          }
          OledText(
            text = formatDuration(remainingSec),
            style = CapsuleTypography.TabularLarge,
            color = CapsuleColors.Amber,
          )
        }
        Box(
          modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(CapsuleColors.SurfaceGlass)
        ) {
          Box(
            modifier = Modifier.fillMaxWidth(frac).fillMaxHeight().background(CapsuleColors.Amber)
          )
        }
      }
    }

    is LiveActivity.DownloadActivity -> {
      Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            DownloadGlyph(tint = CapsuleColors.Sapphire, size = 18.dp)
            Spacer(Modifier.width(8.dp))
            OledText(activity.fileName, style = CapsuleTypography.Body.copy(fontWeight = FontWeight.SemiBold))
          }
          OledText(
            text = "${(activity.progress * 100).roundToInt()}%",
            style = CapsuleTypography.TabularNumeric,
            color = CapsuleColors.Sapphire,
          )
        }
        Box(
          modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(CapsuleColors.SurfaceGlass)
        ) {
          Box(
            modifier = Modifier.fillMaxWidth(activity.progress.coerceIn(0.03f, 1f)).fillMaxHeight().background(CapsuleColors.Sapphire)
          )
        }
        OledText(activity.speedText, style = CapsuleTypography.Caption, color = CapsuleColors.TextSecondary)
      }
    }
  }
}

@Composable
fun ExpandedDragAndGoContent(
  isHovering: Boolean,
  result: DragDropResult?,
  onActionClick: (DragAction) -> Unit,
  onDismiss: () -> Unit,
) {
  if (isHovering && result == null) {
    Column(
      modifier = Modifier.fillMaxSize().padding(18.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      DropZoneGlyph(tint = CapsuleColors.Cyan, size = 28.dp)
      Spacer(Modifier.height(8.dp))
      OledText("Drop to Drag & Go", style = CapsuleTypography.Body.copy(fontWeight = FontWeight.SemiBold), color = CapsuleColors.Cyan)
      OledText("Text, links, or images for instant OCR", style = CapsuleTypography.Caption, color = CapsuleColors.TextSecondary)
    }
    return
  }

  result ?: return
  Column(
    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        DropZoneGlyph(tint = CapsuleColors.Cyan, size = 16.dp)
        Spacer(Modifier.width(6.dp))
        OledText(
          text = result.confirmationBanner ?: result.sourceLabel,
          style = CapsuleTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
          color = if (result.confirmationBanner != null) CapsuleColors.Emerald else CapsuleColors.Cyan,
        )
      }
      CapsuleActionChip(label = "Close", accent = CapsuleColors.TextSecondary, onClick = onDismiss)
    }

    OledText(
      text = result.extractedText,
      style = CapsuleTypography.Body.copy(fontSize = 13.sp, lineHeight = 17.sp),
      color = CapsuleColors.TextPrimary,
      maxLines = 3,
      modifier =
        Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(CapsuleColors.SurfaceDark)
          .border(0.5.dp, CapsuleColors.Hairline, RoundedCornerShape(12.dp))
          .padding(10.dp),
    )

    if (!result.isProcessing && result.actions.isNotEmpty()) {
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        result.actions.forEach { action ->
          val accent =
            when (action.type) {
              DragActionType.OPEN_MAPS -> CapsuleColors.Emerald
              DragActionType.SEARCH_WEB -> CapsuleColors.Sapphire
              DragActionType.QUICK_NOTE -> CapsuleColors.Amber
              DragActionType.DIAL_PHONE -> CapsuleColors.Cyan
              else -> CapsuleColors.TextPrimary
            }
          CapsuleActionChip(
            label = action.label,
            accent = accent,
            onClick = { onActionClick(action) },
          )
        }
      }
    }
  }
}

@Composable
fun CapsuleActionChip(
  label: String,
  accent: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  Box(
    modifier =
      modifier
        .defaultMinSize(minHeight = 36.dp)
        .clip(RoundedCornerShape(50))
        .background(accent.copy(alpha = 0.14f))
        .border(0.5.dp, accent.copy(alpha = 0.40f), RoundedCornerShape(50))
        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 7.dp),
    contentAlignment = Alignment.Center,
  ) {
    OledText(
      text = label,
      style = CapsuleTypography.Caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
      color = accent,
    )
  }
}

@Composable
private fun CapsuleIconHitTarget(
  onClick: () -> Unit,
  content: @Composable () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  Box(
    modifier =
      Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(CapsuleColors.SurfaceGlass)
        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    content()
  }
}

fun formatDuration(totalSeconds: Long): String {
  val mins = totalSeconds / 60L
  val secs = totalSeconds % 60L
  return "%02d:%02d".format(mins, secs)
}

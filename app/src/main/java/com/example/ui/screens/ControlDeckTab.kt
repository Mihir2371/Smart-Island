package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CapsuleDisplayMode
import com.example.model.CapsuleUiState
import com.example.model.HardwarePopup
import com.example.model.LiveActivity
import com.example.model.SystemToggleType
import com.example.model.TurnDirection
import com.example.state.CapsuleStateHolder
import com.example.theme.CapsuleColors
import com.example.theme.CapsuleTypography
import com.example.theme.HairlinePanel
import com.example.theme.OledText
import com.example.ui.icons.AirplaneGlyph
import com.example.ui.icons.BellGlyph
import com.example.ui.icons.DownloadGlyph
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

@Composable
fun ControlDeckTab(
  uiState: CapsuleUiState,
  onOpenFlashlight: () -> Unit,
  onStartVoiceMemo: () -> Unit,
  onStartSatellite: () -> Unit,
  onCycleRinger: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    item {
      HairlinePanel(modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          OledText("HARDWARE & SYSTEM POPUPS", style = CapsuleTypography.SectionHeader)

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeckTile(
              label = "Flashlight",
              subtitle = "Drag beam",
              accent = CapsuleColors.Amber,
              icon = { FlashlightGlyph(isOn = true, size = 18.dp) },
              onClick = onOpenFlashlight,
              modifier = Modifier.weight(1f).testTag("tile_flashlight"),
            )
            DeckTile(
              label = "Voice Memo",
              subtitle = "Live mic",
              accent = CapsuleColors.Crimson,
              icon = { Box(Modifier.size(12.dp).clip(CircleShape).background(CapsuleColors.Crimson)) },
              onClick = onStartVoiceMemo,
              modifier = Modifier.weight(1f).testTag("tile_voice_memo"),
            )
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeckTile(
              label = "Charging",
              subtitle = "30W PD",
              accent = CapsuleColors.Emerald,
              icon = { LightningBoltGlyph(size = 18.dp) },
              onClick = {
                CapsuleStateHolder.showHardwarePopup(
                  HardwarePopup.Charging(level = 84, isFastCharging = true, wattageEstimate = 30)
                )
              },
              modifier = Modifier.weight(1f).testTag("tile_charging"),
            )
            DeckTile(
              label = "Low Battery",
              subtitle = "12% alert",
              accent = CapsuleColors.Crimson,
              icon = { OledText("!", style = CapsuleTypography.TabularNumeric, color = CapsuleColors.Crimson) },
              onClick = {
                CapsuleStateHolder.showHardwarePopup(HardwarePopup.LowBattery(level = 12))
              },
              modifier = Modifier.weight(1f).testTag("tile_low_battery"),
            )
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeckTile(
              label = "Bluetooth",
              subtitle = "AirPods 94%",
              accent = CapsuleColors.Cyan,
              icon = { HeadphonesGlyph(size = 18.dp) },
              onClick = {
                CapsuleStateHolder.showHardwarePopup(
                  HardwarePopup.BluetoothConnected(
                    deviceName = "Studio Buds Pro",
                    batteryPercent = 94,
                    isAudioDevice = true,
                  )
                )
              },
              modifier = Modifier.weight(1f).testTag("tile_bluetooth"),
            )
            DeckTile(
              label = "Screen Rec",
              subtitle = "Timer + wave",
              accent = CapsuleColors.Crimson,
              icon = { Box(Modifier.size(12.dp).clip(CircleShape).background(CapsuleColors.Crimson)) },
              onClick = {
                CapsuleStateHolder.showHardwarePopup(
                  HardwarePopup.Recording(
                    isScreenRecording = true,
                    startedAtMs = System.currentTimeMillis(),
                    amplitudes = listOf(0.3f, 0.7f, 0.5f, 0.9f, 0.4f, 0.8f, 0.6f, 0.5f, 0.9f, 0.3f, 0.7f, 0.6f),
                  ),
                  autoDismissMs = null,
                )
              },
              modifier = Modifier.weight(1f).testTag("tile_screen_rec"),
            )
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeckTile(
              label = "NFC Scan",
              subtitle = "Tag verified",
              accent = CapsuleColors.Cyan,
              icon = { NfcGlyph(size = 18.dp) },
              onClick = {
                CapsuleStateHolder.showHardwarePopup(
                  HardwarePopup.NfcScan(tagSummary = "Transit Pass · Verified")
                )
              },
              modifier = Modifier.weight(1f).testTag("tile_nfc"),
            )
            DeckTile(
              label = "Satellite",
              subtitle = "Sensor align",
              accent = CapsuleColors.Emerald,
              icon = { SatelliteGlyph(size = 18.dp) },
              onClick = onStartSatellite,
              modifier = Modifier.weight(1f).testTag("tile_satellite"),
            )
          }

          // System Toggles & Privacy Dots Row
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniPillTrigger(
              label = "Ringer",
              icon = { BellGlyph(slashed = true, tint = CapsuleColors.Crimson, size = 14.dp) },
              onClick = onCycleRinger,
              modifier = Modifier.weight(1f),
            )
            MiniPillTrigger(
              label = "Focus",
              icon = { MoonGlyph(size = 14.dp) },
              onClick = {
                CapsuleStateHolder.showHardwarePopup(
                  HardwarePopup.SystemToggle(
                    type = SystemToggleType.DND,
                    label = "Do Not Disturb",
                    subtitle = "Focus active",
                    isActive = true,
                  )
                )
              },
              modifier = Modifier.weight(1f),
            )
            MiniPillTrigger(
              label = "Airplane",
              icon = { AirplaneGlyph(size = 14.dp) },
              onClick = {
                CapsuleStateHolder.showHardwarePopup(
                  HardwarePopup.SystemToggle(
                    type = SystemToggleType.AIRPLANE,
                    label = "Airplane Mode",
                    subtitle = "Radios paused",
                    isActive = true,
                  )
                )
              },
              modifier = Modifier.weight(1f),
            )
            MiniPillTrigger(
              label = "Hotspot",
              icon = { HotspotGlyph(size = 14.dp) },
              onClick = {
                CapsuleStateHolder.showHardwarePopup(
                  HardwarePopup.SystemToggle(
                    type = SystemToggleType.HOTSPOT,
                    label = "Hotspot",
                    subtitle = "2 devices connected",
                    isActive = true,
                  )
                )
              },
              modifier = Modifier.weight(1f),
            )
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val camSim = uiState.privacyStatus.simulatedCamera
            val micSim = uiState.privacyStatus.simulatedMic
            MiniPillTrigger(
              label = if (camSim) "Cam Dot On" else "Test Cam Dot",
              icon = { Box(Modifier.size(8.dp).clip(CircleShape).background(CapsuleColors.Emerald)) },
              onClick = { CapsuleStateHolder.toggleSimulatedPrivacy(camera = !camSim) },
              modifier = Modifier.weight(1f).testTag("tile_cam_dot"),
            )
            MiniPillTrigger(
              label = if (micSim) "Mic Dot On" else "Test Mic Dot",
              icon = { Box(Modifier.size(8.dp).clip(CircleShape).background(CapsuleColors.Amber)) },
              onClick = { CapsuleStateHolder.toggleSimulatedPrivacy(mic = !micSim) },
              modifier = Modifier.weight(1f).testTag("tile_mic_dot"),
            )
          }
        }
      }
    }

    item {
      HairlinePanel(modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OledText("LIVE ACTIVITIES", style = CapsuleTypography.SectionHeader)
            if (uiState.liveActivities.isNotEmpty()) {
              OledText(
                text = "Clear All",
                style = CapsuleTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                color = CapsuleColors.Crimson,
                modifier =
                  Modifier
                    .clickable(
                      interactionSource = remember { MutableInteractionSource() },
                      indication = null,
                    ) {
                      uiState.liveActivities.map { it.id }.forEach {
                        CapsuleStateHolder.removeLiveActivity(it)
                      }
                      CapsuleStateHolder.collapseNow()
                    }
                    .padding(4.dp),
              )
            }
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeckTile(
              label = "VoIP Call",
              subtitle = "WhatsApp",
              accent = CapsuleColors.Emerald,
              icon = { PhoneGlyph(size = 18.dp) },
              onClick = {
                triggerLiveActivity(
                  LiveActivity.PhoneCall(
                    id = "sim_call",
                    packageName = "com.whatsapp",
                    callerName = "Elena Vance",
                    appLabel = "WhatsApp Audio",
                    startedAtMs = System.currentTimeMillis() - 42_000L,
                    isVoip = true,
                  )
                )
              },
              modifier = Modifier.weight(1f).testTag("tile_call"),
            )
            DeckTile(
              label = "Media Player",
              subtitle = "Visualizer",
              accent = CapsuleColors.Cyan,
              icon = { Box(Modifier.size(12.dp).clip(CircleShape).background(CapsuleColors.Cyan)) },
              onClick = {
                triggerLiveActivity(
                  LiveActivity.MediaPlayback(
                    id = "sim_media",
                    packageName = "com.spotify.music",
                    title = "Midnight City",
                    artist = "M83 · Hurry Up, We're Dreaming",
                    isPlaying = true,
                    positionMs = 74_000L,
                    durationMs = 243_000L,
                  )
                )
              },
              modifier = Modifier.weight(1f).testTag("tile_media"),
            )
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeckTile(
              label = "Navigation",
              subtitle = "Turn arrow",
              accent = CapsuleColors.Sapphire,
              icon = { TurnArrowGlyph(TurnDirection.RIGHT, tint = CapsuleColors.Sapphire, size = 18.dp) },
              onClick = {
                triggerLiveActivity(
                  LiveActivity.Navigation(
                    id = "sim_nav",
                    packageName = "com.google.android.apps.maps",
                    direction = TurnDirection.RIGHT,
                    distanceText = "240 m",
                    streetName = "Castro St · Mountain View",
                    etaText = "6 min",
                  )
                )
              },
              modifier = Modifier.weight(1f).testTag("tile_nav"),
            )
            DeckTile(
              label = "Rideshare",
              subtitle = "Uber ETA",
              accent = CapsuleColors.Cyan,
              icon = { OledText("U", style = CapsuleTypography.Body, color = CapsuleColors.Cyan) },
              onClick = {
                triggerLiveActivity(
                  LiveActivity.Rideshare(
                    id = "sim_ride",
                    packageName = "com.ubercab",
                    provider = "Uber Black",
                    statusText = "Arriving in 3 min",
                    etaMinutes = 3,
                    vehicleInfo = "Model S · 8XLP904",
                  )
                )
              },
              modifier = Modifier.weight(1f).testTag("tile_ride"),
            )
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeckTile(
              label = "Delivery",
              subtitle = "Courier live",
              accent = CapsuleColors.Amber,
              icon = { OledText("D", style = CapsuleTypography.Body, color = CapsuleColors.Amber) },
              onClick = {
                triggerLiveActivity(
                  LiveActivity.Delivery(
                    id = "sim_delivery",
                    packageName = "com.dd.doordash",
                    provider = "DoorDash",
                    statusText = "Courier 2 blocks away",
                    etaMinutes = 4,
                    progress = 0.82f,
                  )
                )
              },
              modifier = Modifier.weight(1f).testTag("tile_delivery"),
            )
            DeckTile(
              label = "Flight",
              subtitle = "Gate B14",
              accent = CapsuleColors.Cyan,
              icon = { AirplaneGlyph(tint = CapsuleColors.Cyan, size = 18.dp) },
              onClick = {
                triggerLiveActivity(
                  LiveActivity.Flight(
                    id = "sim_flight",
                    packageName = "com.united.mobile",
                    flightCode = "UA 412",
                    gate = "B14",
                    statusText = "Boarding Group 2",
                    boardingMinutes = 14,
                  )
                )
              },
              modifier = Modifier.weight(1f).testTag("tile_flight"),
            )
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeckTile(
              label = "Sports",
              subtitle = "Live score",
              accent = CapsuleColors.Emerald,
              icon = { OledText("2:1", style = CapsuleTypography.TabularNumeric, color = CapsuleColors.Emerald) },
              onClick = {
                triggerLiveActivity(
                  LiveActivity.Sports(
                    id = "sim_sports",
                    packageName = "com.espn.score_center",
                    league = "UCL",
                    homeTeam = "ARS",
                    awayTeam = "FCB",
                    homeScore = 2,
                    awayScore = 1,
                    clockText = "68'",
                  )
                )
              },
              modifier = Modifier.weight(1f).testTag("tile_sports"),
            )
            DeckTile(
              label = "Timer",
              subtitle = "Countdown",
              accent = CapsuleColors.Amber,
              icon = { TimerGlyph(size = 18.dp) },
              onClick = {
                triggerLiveActivity(
                  LiveActivity.TimerActivity(
                    id = "sim_timer",
                    packageName = "com.google.android.deskclock",
                    label = "Espresso Pull",
                    endTimeMs = System.currentTimeMillis() + 185_000L,
                    totalMs = 185_000L,
                  )
                )
              },
              modifier = Modifier.weight(1f).testTag("tile_timer"),
            )
          }

          DeckTile(
            label = "File Download",
            subtitle = "Linear transfer progress · 18.4 MB/s",
            accent = CapsuleColors.Sapphire,
            icon = { DownloadGlyph(size = 18.dp) },
            onClick = {
              triggerLiveActivity(
                LiveActivity.DownloadActivity(
                  id = "sim_download",
                  packageName = "com.android.providers.downloads",
                  fileName = "capsule-firmware-v2.zip",
                  progress = 0.68f,
                  speedText = "18.4 MB/s · 4s left",
                )
              )
            },
            modifier = Modifier.fillMaxWidth().testTag("tile_download"),
          )
        }
      }
    }
  }
}

private fun triggerLiveActivity(activity: LiveActivity) {
  CapsuleStateHolder.dismissHardwarePopup(null)
  CapsuleStateHolder.upsertLiveActivity(activity)
  CapsuleStateHolder.setDisplayMode(CapsuleDisplayMode.EXPANDED)
}

@Composable
private fun DeckTile(
  label: String,
  subtitle: String,
  accent: Color,
  icon: @Composable () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  Row(
    modifier =
      modifier
        .height(62.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(CapsuleColors.SurfaceDark)
        .border(0.5.dp, CapsuleColors.Hairline, RoundedCornerShape(16.dp))
        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
        .padding(horizontal = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier =
        Modifier
          .size(34.dp)
          .clip(CircleShape)
          .background(accent.copy(alpha = 0.14f)),
      contentAlignment = Alignment.Center,
    ) {
      icon()
    }
    Spacer(Modifier.width(10.dp))
    Column {
      OledText(label, style = CapsuleTypography.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
      OledText(subtitle, style = CapsuleTypography.Caption.copy(fontSize = 11.sp), color = CapsuleColors.TextSecondary)
    }
  }
}

@Composable
private fun MiniPillTrigger(
  label: String,
  icon: @Composable () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  Row(
    modifier =
      modifier
        .height(44.dp)
        .clip(RoundedCornerShape(50))
        .background(CapsuleColors.SurfaceDark)
        .border(0.5.dp, CapsuleColors.Hairline, RoundedCornerShape(50))
        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
        .padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    icon()
    Spacer(Modifier.width(5.dp))
    OledText(label, style = CapsuleTypography.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium), color = CapsuleColors.TextPrimary)
  }
}

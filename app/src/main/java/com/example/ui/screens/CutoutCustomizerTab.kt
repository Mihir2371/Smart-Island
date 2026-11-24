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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CapsuleUiState
import com.example.model.CutoutConfig
import com.example.model.CutoutShapeMode
import com.example.theme.CapsuleColors
import com.example.theme.CapsuleTypography
import com.example.theme.HairlinePanel
import com.example.theme.OledSlider
import com.example.theme.OledText
import com.example.theme.OledToggleSwitch
import com.example.theme.SegmentedCapsuleBar
import com.example.theme.TactilePillButton
import kotlin.math.roundToInt

@Composable
fun CutoutCustomizerTab(
  uiState: CapsuleUiState,
  onUpdateConfig: (CutoutConfig) -> Unit,
  onToggleOverlayService: (Boolean) -> Unit,
  onRequestOverlayPermission: () -> Unit,
  onRequestNotificationListener: () -> Unit,
  onRequestRuntimePermissions: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val cfg = uiState.cutoutConfig

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    item {
      HairlinePanel(modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              OledText("System Overlay Engine", style = CapsuleTypography.Title.copy(fontSize = 16.sp))
              OledText(
                text =
                  if (uiState.overlayServiceRunning) "Floating across all apps"
                  else if (uiState.hasOverlayPermission) "Ready to start floating window"
                  else "Grant overlay access to float over apps",
                style = CapsuleTypography.Caption,
                color = if (uiState.overlayServiceRunning) CapsuleColors.Emerald else CapsuleColors.TextSecondary,
              )
            }
            OledToggleSwitch(
              checked = uiState.overlayServiceRunning,
              onCheckedChange = onToggleOverlayService,
              modifier = Modifier.testTag("overlay_service_toggle"),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            PermissionStatusChip(
              title = if (uiState.hasOverlayPermission) "Overlay On" else "Overlay Off",
              granted = uiState.hasOverlayPermission,
              onClick = onRequestOverlayPermission,
              modifier = Modifier.weight(1f).testTag("perm_overlay_button"),
            )
            PermissionStatusChip(
              title = if (uiState.hasNotificationPermission) "Live Sync On" else "Live Sync Off",
              granted = uiState.hasNotificationPermission,
              onClick = onRequestNotificationListener,
              modifier = Modifier.weight(1f).testTag("perm_notif_button"),
            )
          }

          if (!uiState.hasBluetoothPermission || !uiState.hasPostNotificationPermission) {
            TactilePillButton(
              label = "Grant Bluetooth & Status Permissions",
              onClick = onRequestRuntimePermissions,
              accentColor = CapsuleColors.Sapphire,
              modifier = Modifier.fillMaxWidth().testTag("perm_runtime_button"),
            )
          }
        }
      }
    }

    item {
      HairlinePanel(modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          OledText("CUTOUT GEOMETRY", style = CapsuleTypography.SectionHeader)

          SegmentedCapsuleBar(
            options = listOf("Floating Pill", "Waterdrop Notch"),
            selectedIndex = if (cfg.mode == CutoutShapeMode.FLOATING_PILL) 0 else 1,
            onSelect = { idx ->
              val nextMode =
                if (idx == 0) CutoutShapeMode.FLOATING_PILL else CutoutShapeMode.TOP_DOCKED_NOTCH
              onUpdateConfig(
                cfg.copy(
                  mode = nextMode,
                  offsetYdp = if (nextMode == CutoutShapeMode.TOP_DOCKED_NOTCH) 0f else 8f,
                )
              )
            },
            modifier = Modifier.testTag("cutout_mode_segment"),
          )

          // Presets
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            PresetChip("Center", Modifier.weight(1f)) {
              onUpdateConfig(
                cfg.copy(
                  mode = CutoutShapeMode.FLOATING_PILL,
                  offsetXdp = 0f,
                  offsetYdp = 8f,
                  widthDp = 128f,
                  heightDp = 34f,
                  cornerRadiusDp = 20f,
                )
              )
            }
            PresetChip("Left Punch", Modifier.weight(1f)) {
              onUpdateConfig(
                cfg.copy(
                  mode = CutoutShapeMode.FLOATING_PILL,
                  offsetXdp = -84f,
                  offsetYdp = 8f,
                  widthDp = 112f,
                  heightDp = 32f,
                  cornerRadiusDp = 18f,
                )
              )
            }
            PresetChip("Notch 13", Modifier.weight(1f)) {
              onUpdateConfig(
                cfg.copy(
                  mode = CutoutShapeMode.TOP_DOCKED_NOTCH,
                  offsetXdp = 0f,
                  offsetYdp = 0f,
                  widthDp = 156f,
                  heightDp = 36f,
                  cornerRadiusDp = 18f,
                )
              )
            }
          }

          OledSlider(
            label = "X Position",
            value = cfg.offsetXdp,
            valueRange = -120f..120f,
            valueDisplay = "${cfg.offsetXdp.roundToInt()} dp",
            onValueChange = { onUpdateConfig(cfg.copy(offsetXdp = it)) },
            modifier = Modifier.testTag("slider_offset_x"),
          )

          OledSlider(
            label = "Y Position",
            value = cfg.offsetYdp,
            valueRange = 0f..56f,
            valueDisplay =
              if (cfg.mode == CutoutShapeMode.TOP_DOCKED_NOTCH) "Docked (0 dp)"
              else "${cfg.offsetYdp.roundToInt()} dp",
            onValueChange = {
              if (cfg.mode == CutoutShapeMode.FLOATING_PILL) {
                onUpdateConfig(cfg.copy(offsetYdp = it))
              }
            },
            modifier = Modifier.testTag("slider_offset_y"),
          )

          OledSlider(
            label = "Capsule Width",
            value = cfg.widthDp,
            valueRange = 96f..240f,
            valueDisplay = "${cfg.widthDp.roundToInt()} dp",
            onValueChange = { onUpdateConfig(cfg.copy(widthDp = it)) },
            modifier = Modifier.testTag("slider_width"),
          )

          OledSlider(
            label = "Capsule Height",
            value = cfg.heightDp,
            valueRange = 24f..52f,
            valueDisplay = "${cfg.heightDp.roundToInt()} dp",
            onValueChange = { onUpdateConfig(cfg.copy(heightDp = it)) },
            modifier = Modifier.testTag("slider_height"),
          )

          OledSlider(
            label = "Corner Radius",
            value = cfg.cornerRadiusDp,
            valueRange = 8f..28f,
            valueDisplay = "${cfg.cornerRadiusDp.roundToInt()} dp",
            onValueChange = { onUpdateConfig(cfg.copy(cornerRadiusDp = it)) },
            modifier = Modifier.testTag("slider_radius"),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              OledText("Privacy Indicator Dots", style = CapsuleTypography.Body)
              OledText("Embed camera/mic dots inside cutout", style = CapsuleTypography.Caption)
            }
            OledToggleSwitch(
              checked = cfg.showPrivacyDots,
              onCheckedChange = { onUpdateConfig(cfg.copy(showPrivacyDots = it)) },
              modifier = Modifier.testTag("privacy_dots_toggle"),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PermissionStatusChip(
  title: String,
  granted: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val accent = if (granted) CapsuleColors.Emerald else CapsuleColors.Amber
  val interactionSource = remember { MutableInteractionSource() }
  Box(
    modifier =
      modifier
        .height(48.dp)
        .clip(RoundedCornerShape(14.dp))
        .background(accent.copy(alpha = 0.12f))
        .border(0.5.dp, accent.copy(alpha = 0.36f), RoundedCornerShape(14.dp))
        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    OledText(
      text = title,
      style = CapsuleTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
      color = accent,
    )
  }
}

@Composable
private fun PresetChip(
  label: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  Box(
    modifier =
      modifier
        .height(40.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(CapsuleColors.SurfaceDark)
        .border(0.5.dp, CapsuleColors.Hairline, RoundedCornerShape(12.dp))
        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    OledText(
      text = label,
      style = CapsuleTypography.Caption.copy(fontWeight = FontWeight.Medium),
      color = CapsuleColors.TextPrimary,
    )
  }
}

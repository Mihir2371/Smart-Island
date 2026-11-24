package com.example.ui.screens

import android.content.ClipData
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CapsuleUiState
import com.example.theme.CapsuleColors
import com.example.theme.CapsuleTypography
import com.example.theme.HairlinePanel
import com.example.theme.OledText
import com.example.theme.TactilePillButton
import com.example.ui.icons.DropZoneGlyph

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DragAndGoLabTab(
  uiState: CapsuleUiState,
  onPickImageForOcr: () -> Unit,
  onDropTextPayload: (String, String) -> Unit,
  onDeleteQuickNote: (String) -> Unit,
  onCopyQuickNote: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val samplePayloads =
    remember {
      listOf(
        "Address" to "1600 Amphitheatre Parkway, Mountain View, CA",
        "Web Link" to "https://developer.android.com/develop/ui/compose",
        "Contact" to "Call Alex Chen at +1 (650) 555-0199 before 4 PM",
        "Flight Info" to "United UA 412 · Gate B14 · Boarding 14:20",
      )
    }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    item {
      HairlinePanel(modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            DropZoneGlyph(tint = CapsuleColors.Cyan, size = 20.dp)
            Spacer(Modifier.padding(horizontal = 4.dp))
            OledText("ORIGINOS DRAG & GO", style = CapsuleTypography.SectionHeader, color = CapsuleColors.Cyan)
          }
          OledText(
            text = "Long-press & drag any card below up into the Capsule stage, or pick an image for instant on-device ML Kit OCR.",
            style = CapsuleTypography.Caption,
            maxLines = 3,
          )
          TactilePillButton(
            label = "Pick Photo for On-Device OCR",
            onClick = onPickImageForOcr,
            accentColor = CapsuleColors.Cyan,
            modifier = Modifier.fillMaxWidth().testTag("pick_ocr_photo_button"),
          )
        }
      }
    }

    item {
      HairlinePanel(modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OledText("DRAGGABLE SNIPPETS (HOLD & DRAG UP OR TAP)", style = CapsuleTypography.SectionHeader)

          samplePayloads.forEachIndexed { idx, (tag, content) ->
            Box(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .background(CapsuleColors.SurfaceDark)
                  .border(0.5.dp, CapsuleColors.Hairline, RoundedCornerShape(14.dp))
                  .dragAndDropSource {
                    detectTapGestures(
                      onTap = { onDropTextPayload(content, "Drag & Go · $tag") },
                      onLongPress = {
                        startTransfer(
                          DragAndDropTransferData(
                            clipData = ClipData.newPlainText(tag, content)
                          )
                        )
                      },
                    )
                  }
                  .padding(12.dp)
                  .testTag("drag_sample_$idx"),
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  OledText(tag.uppercase(), style = CapsuleTypography.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold), color = CapsuleColors.Cyan)
                  OledText("Hold & Drag / Tap", style = CapsuleTypography.Caption.copy(fontSize = 11.sp), color = CapsuleColors.TextTertiary)
                }
                OledText(content, style = CapsuleTypography.Body.copy(fontSize = 13.sp), maxLines = 2)
              }
            }
          }
        }
      }
    }

    item {
      OledText(
        text = "SAVED QUICK NOTES (${uiState.quickNotes.size})",
        style = CapsuleTypography.SectionHeader,
        modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp).padding(horizontal = 4.dp),
      )
    }

    if (uiState.quickNotes.isEmpty()) {
      item {
        HairlinePanel(modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)) {
          OledText(
            text = "Drop content into the capsule and tap Quick Note to save snippets here.",
            style = CapsuleTypography.Caption,
            maxLines = 2,
            modifier = Modifier.padding(16.dp),
          )
        }
      }
    } else {
      items(uiState.quickNotes, key = { it.id }) { note ->
        HairlinePanel(modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            OledText(
              text = note.content,
              style = CapsuleTypography.Body.copy(fontSize = 13.sp),
              maxLines = 3,
              modifier = Modifier.weight(1f).padding(end = 10.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              NoteActionText("Copy", CapsuleColors.Cyan) { onCopyQuickNote(note.content) }
              NoteActionText("Del", CapsuleColors.Crimson) { onDeleteQuickNote(note.id) }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun NoteActionText(
  label: String,
  color: androidx.compose.ui.graphics.Color,
  onClick: () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  Box(
    modifier =
      Modifier
        .clip(RoundedCornerShape(8.dp))
        .background(color.copy(alpha = 0.12f))
        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
        .padding(horizontal = 10.dp, vertical = 6.dp),
  ) {
    OledText(label, style = CapsuleTypography.Caption.copy(fontWeight = FontWeight.SemiBold), color = color)
  }
}

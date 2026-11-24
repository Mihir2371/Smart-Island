package com.example.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

object CapsuleColors {
  val PitchBlack = Color(0xFF000000)
  val SurfaceDark = Color(0xFF0D0D0F)
  val SurfaceElevated = Color(0xFF161618)
  val SurfaceGlass = Color(0xFF1C1C1F)
  val Hairline = Color(0x1FFFFFFF) // rgba(255,255,255,0.12)
  val HairlineStrong = Color(0x38FFFFFF)
  val TextPrimary = Color(0xFFFFFFFF)
  val TextSecondary = Color(0xFF9A9AA3)
  val TextTertiary = Color(0xFF5E5E66)
  val Emerald = Color(0xFF30D158)
  val Amber = Color(0xFFFF9F0A)
  val Crimson = Color(0xFFFF453A)
  val Sapphire = Color(0xFF0A84FF)
  val Cyan = Color(0xFF64D2FF)
  val Indigo = Color(0xFF5E5CE6)
}

object CapsuleTypography {
  val Title =
    TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.SemiBold,
      fontSize = 18.sp,
      lineHeight = 22.sp,
      letterSpacing = (-0.3).sp,
      color = CapsuleColors.TextPrimary,
    )

  val SectionHeader =
    TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.SemiBold,
      fontSize = 13.sp,
      lineHeight = 16.sp,
      letterSpacing = 0.4.sp,
      color = CapsuleColors.TextSecondary,
    )

  val Body =
    TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Medium,
      fontSize = 14.sp,
      lineHeight = 18.sp,
      color = CapsuleColors.TextPrimary,
    )

  val Caption =
    TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Normal,
      fontSize = 12.sp,
      lineHeight = 16.sp,
      color = CapsuleColors.TextSecondary,
    )

  val TabularNumeric =
    TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.SemiBold,
      fontSize = 13.sp,
      lineHeight = 16.sp,
      fontFeatureSettings = "tnum",
      color = CapsuleColors.TextPrimary,
    )

  val TabularLarge =
    TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Bold,
      fontSize = 22.sp,
      lineHeight = 26.sp,
      fontFeatureSettings = "tnum",
      color = CapsuleColors.TextPrimary,
    )
}

object CapsuleSprings {
  fun <T> fluid(): AnimationSpec<T> =
    spring(dampingRatio = 0.82f, stiffness = 360f)

  fun <T> snappy(): AnimationSpec<T> =
    spring(dampingRatio = 0.86f, stiffness = 520f)

  fun <T> gentle(): AnimationSpec<T> =
    spring(dampingRatio = 0.88f, stiffness = 260f)
}

@Composable
fun OledText(
  text: String,
  modifier: Modifier = Modifier,
  style: TextStyle = CapsuleTypography.Body,
  color: Color = style.color,
  maxLines: Int = 1,
) {
  BasicText(
    text = text,
    modifier = modifier,
    style = style.copy(color = color),
    maxLines = maxLines,
    overflow = TextOverflow.Ellipsis,
  )
}

@Composable
fun HairlinePanel(
  modifier: Modifier = Modifier,
  backgroundColor: Color = CapsuleColors.SurfaceElevated,
  borderColor: Color = CapsuleColors.Hairline,
  shape: Shape = RoundedCornerShape(18.dp),
  content: @Composable BoxScope.() -> Unit,
) {
  Box(
    modifier =
      modifier
        .clip(shape)
        .background(backgroundColor)
        .border(0.5.dp, borderColor, shape),
    content = content,
  )
}

@Composable
fun TactilePillButton(
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  accentColor: Color = CapsuleColors.TextPrimary,
  containerColor: Color = CapsuleColors.SurfaceGlass,
  borderColor: Color = CapsuleColors.Hairline,
  leadingIcon: (@Composable () -> Unit)? = null,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by
    animateFloatAsState(
      targetValue = if (isPressed) 0.95f else 1f,
      animationSpec = CapsuleSprings.snappy(),
      label = "pill_press_scale",
    )

  Row(
    modifier =
      modifier
        .scale(scale)
        .defaultMinSize(minHeight = 48.dp)
        .clip(RoundedCornerShape(50))
        .background(containerColor)
        .border(0.5.dp, borderColor, RoundedCornerShape(50))
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = onClick,
        )
        .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    if (leadingIcon != null) {
      leadingIcon()
      Spacer(Modifier.width(8.dp))
    }
    OledText(
      text = label,
      style = CapsuleTypography.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
      color = accentColor,
    )
  }
}

@Composable
fun OledToggleSwitch(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  activeColor: Color = CapsuleColors.Emerald,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val thumbOffset by
    animateDpAsState(
      targetValue = if (checked) 22.dp else 2.dp,
      animationSpec = CapsuleSprings.snappy(),
      label = "toggle_thumb",
    )

  Box(
    modifier =
      modifier
        .defaultMinSize(minWidth = 52.dp, minHeight = 48.dp)
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = { onCheckedChange(!checked) },
        ),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
        Modifier
          .width(48.dp)
          .height(28.dp)
          .clip(RoundedCornerShape(50))
          .background(if (checked) activeColor else CapsuleColors.SurfaceGlass)
          .border(
            0.5.dp,
            if (checked) activeColor.copy(alpha = 0.6f) else CapsuleColors.Hairline,
            RoundedCornerShape(50),
          )
    ) {
      Box(
        modifier =
          Modifier
            .offset(x = thumbOffset, y = 2.dp)
            .size(24.dp)
            .clip(CircleShape)
            .background(Color.White)
      )
    }
  }
}

@Composable
fun OledSlider(
  label: String,
  value: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  valueDisplay: String,
  onValueChange: (Float) -> Unit,
  modifier: Modifier = Modifier,
  accentColor: Color = CapsuleColors.Sapphire,
) {
  val density = LocalDensity.current
  var trackWidthPx by remember { mutableFloatStateOf(1f) }
  val fraction =
    ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      OledText(
        text = label,
        style = CapsuleTypography.Caption.copy(color = CapsuleColors.TextSecondary),
      )
      OledText(
        text = valueDisplay,
        style = CapsuleTypography.TabularNumeric.copy(fontSize = 12.sp),
        color = CapsuleColors.TextPrimary,
      )
    }
    Spacer(Modifier.height(6.dp))
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(48.dp)
          .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
          .pointerInput(valueRange) {
            detectTapGestures { offset ->
              val nextFrac = (offset.x / trackWidthPx).coerceIn(0f, 1f)
              val nextVal =
                valueRange.start + nextFrac * (valueRange.endInclusive - valueRange.start)
              onValueChange(nextVal)
            }
          }
          .pointerInput(valueRange) {
            detectHorizontalDragGestures { change, _ ->
              change.consume()
              val nextFrac = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
              val nextVal =
                valueRange.start + nextFrac * (valueRange.endInclusive - valueRange.start)
              onValueChange(nextVal)
            }
          },
      contentAlignment = Alignment.CenterStart,
    ) {
      Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
        val radius = size.height / 2f
        drawRoundRect(
          color = CapsuleColors.SurfaceGlass,
          topLeft = Offset.Zero,
          size = size,
          cornerRadius = CornerRadius(radius, radius),
        )
        val fillWidth = (size.width * fraction).coerceAtLeast(size.height)
        drawRoundRect(
          color = accentColor,
          topLeft = Offset.Zero,
          size = Size(fillWidth, size.height),
          cornerRadius = CornerRadius(radius, radius),
        )
      }
      val thumbSizePx = with(density) { 20.dp.toPx() }
      val thumbX = ((trackWidthPx - thumbSizePx) * fraction).roundToInt()
      Box(
        modifier =
          Modifier
            .offset { IntOffset(thumbX, 0) }
            .size(20.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(0.5.dp, Color.Black.copy(alpha = 0.4f), CircleShape)
      )
    }
  }
}

@Composable
fun SegmentedCapsuleBar(
  options: List<String>,
  selectedIndex: Int,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  HairlinePanel(
    modifier = modifier.fillMaxWidth(),
    backgroundColor = CapsuleColors.PitchBlack,
    borderColor = CapsuleColors.Hairline,
    shape = RoundedCornerShape(50),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      options.forEachIndexed { index, title ->
        val selected = index == selectedIndex
        val interactionSource = remember { MutableInteractionSource() }
        Box(
          modifier =
            Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 40.dp)
              .clip(RoundedCornerShape(50))
              .background(if (selected) CapsuleColors.SurfaceElevated else Color.Transparent)
              .border(
                0.5.dp,
                if (selected) CapsuleColors.HairlineStrong else Color.Transparent,
                RoundedCornerShape(50),
              )
              .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onSelect(index) },
              ),
          contentAlignment = Alignment.Center,
        ) {
          OledText(
            text = title,
            style =
              CapsuleTypography.Body.copy(
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
              ),
            color = if (selected) CapsuleColors.TextPrimary else CapsuleColors.TextSecondary,
          )
        }
      }
    }
  }
}

package com.example.ui.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.TurnDirection
import com.example.theme.CapsuleColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FlashlightGlyph(
  isOn: Boolean,
  tint: Color = if (isOn) CapsuleColors.Amber else CapsuleColors.TextSecondary,
  size: Dp = 18.dp,
) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = w * 0.09f
    // Handle
    drawRoundRect(
      color = tint,
      topLeft = Offset(w * 0.36f, h * 0.45f),
      size = Size(w * 0.28f, h * 0.46f),
      cornerRadius = CornerRadius(w * 0.08f),
    )
    // Head
    val head = Path().apply {
      moveTo(w * 0.25f, h * 0.26f)
      lineTo(w * 0.75f, h * 0.26f)
      lineTo(w * 0.64f, h * 0.45f)
      lineTo(w * 0.36f, h * 0.45f)
      close()
    }
    drawPath(head, color = tint)
    if (isOn) {
      drawLine(tint, Offset(w * 0.5f, h * 0.05f), Offset(w * 0.5f, h * 0.16f), strokeWidth = stroke, cap = StrokeCap.Round)
      drawLine(tint, Offset(w * 0.24f, h * 0.1f), Offset(w * 0.32f, h * 0.18f), strokeWidth = stroke, cap = StrokeCap.Round)
      drawLine(tint, Offset(w * 0.76f, h * 0.1f), Offset(w * 0.68f, h * 0.18f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
  }
}

@Composable
fun LightningBoltGlyph(
  tint: Color = CapsuleColors.Emerald,
  size: Dp = 18.dp,
) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val path = Path().apply {
      moveTo(w * 0.56f, h * 0.08f)
      lineTo(w * 0.22f, h * 0.54f)
      lineTo(w * 0.48f, h * 0.54f)
      lineTo(w * 0.42f, h * 0.92f)
      lineTo(w * 0.78f, h * 0.45f)
      lineTo(w * 0.52f, h * 0.45f)
      close()
    }
    drawPath(path, color = tint)
  }
}

@Composable
fun HeadphonesGlyph(
  tint: Color = CapsuleColors.Cyan,
  size: Dp = 18.dp,
) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = w * 0.11f
    drawArc(
      color = tint,
      startAngle = 180f,
      sweepAngle = 180f,
      useCenter = false,
      topLeft = Offset(w * 0.16f, h * 0.18f),
      size = Size(w * 0.68f, h * 0.68f),
      style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
    drawRoundRect(
      color = tint,
      topLeft = Offset(w * 0.12f, h * 0.50f),
      size = Size(w * 0.18f, h * 0.34f),
      cornerRadius = CornerRadius(w * 0.08f),
    )
    drawRoundRect(
      color = tint,
      topLeft = Offset(w * 0.70f, h * 0.50f),
      size = Size(w * 0.18f, h * 0.34f),
      cornerRadius = CornerRadius(w * 0.08f),
    )
  }
}

@Composable
fun BellGlyph(
  slashed: Boolean = false,
  tint: Color = CapsuleColors.Crimson,
  size: Dp = 18.dp,
) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val path = Path().apply {
      moveTo(w * 0.5f, h * 0.14f)
      cubicTo(w * 0.30f, h * 0.14f, w * 0.24f, h * 0.36f, w * 0.24f, h * 0.58f)
      lineTo(w * 0.15f, h * 0.73f)
      lineTo(w * 0.85f, h * 0.73f)
      lineTo(w * 0.76f, h * 0.58f)
      cubicTo(w * 0.76f, h * 0.36f, w * 0.70f, h * 0.14f, w * 0.5f, h * 0.14f)
      close()
    }
    drawPath(path, color = tint)
    drawCircle(tint, radius = w * 0.09f, center = Offset(w * 0.5f, h * 0.83f))
    if (slashed) {
      drawLine(
        color = CapsuleColors.PitchBlack,
        start = Offset(w * 0.18f, h * 0.18f),
        end = Offset(w * 0.82f, h * 0.82f),
        strokeWidth = w * 0.14f,
        cap = StrokeCap.Round,
      )
      drawLine(
        color = tint,
        start = Offset(w * 0.18f, h * 0.18f),
        end = Offset(w * 0.82f, h * 0.82f),
        strokeWidth = w * 0.08f,
        cap = StrokeCap.Round,
      )
    }
  }
}

@Composable
fun MoonGlyph(tint: Color = CapsuleColors.Indigo, size: Dp = 18.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val path = Path().apply {
      moveTo(w * 0.62f, h * 0.15f)
      cubicTo(w * 0.25f, h * 0.18f, w * 0.16f, h * 0.68f, w * 0.55f, h * 0.85f)
      cubicTo(w * 0.70f, h * 0.88f, w * 0.84f, h * 0.78f, w * 0.86f, h * 0.66f)
      cubicTo(w * 0.60f, h * 0.72f, w * 0.42f, h * 0.46f, w * 0.62f, h * 0.15f)
      close()
    }
    drawPath(path, color = tint)
  }
}

@Composable
fun AirplaneGlyph(tint: Color = CapsuleColors.Amber, size: Dp = 18.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val path = Path().apply {
      moveTo(w * 0.50f, h * 0.10f)
      lineTo(w * 0.58f, h * 0.38f)
      lineTo(w * 0.90f, h * 0.55f)
      lineTo(w * 0.90f, h * 0.65f)
      lineTo(w * 0.58f, h * 0.56f)
      lineTo(w * 0.56f, h * 0.78f)
      lineTo(w * 0.70f, h * 0.88f)
      lineTo(w * 0.70f, h * 0.93f)
      lineTo(w * 0.50f, h * 0.87f)
      lineTo(w * 0.30f, h * 0.93f)
      lineTo(w * 0.30f, h * 0.88f)
      lineTo(w * 0.44f, h * 0.78f)
      lineTo(w * 0.42f, h * 0.56f)
      lineTo(w * 0.10f, h * 0.65f)
      lineTo(w * 0.10f, h * 0.55f)
      lineTo(w * 0.42f, h * 0.38f)
      close()
    }
    drawPath(path, color = tint)
  }
}

@Composable
fun HotspotGlyph(tint: Color = CapsuleColors.Sapphire, size: Dp = 18.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = w * 0.1f
    drawArc(
      color = tint,
      startAngle = 125f,
      sweepAngle = 110f,
      useCenter = false,
      topLeft = Offset(w * 0.14f, h * 0.25f),
      size = Size(w * 0.50f, h * 0.50f),
      style = Stroke(stroke, cap = StrokeCap.Round),
    )
    drawArc(
      color = tint,
      startAngle = 305f,
      sweepAngle = 110f,
      useCenter = false,
      topLeft = Offset(w * 0.36f, h * 0.25f),
      size = Size(w * 0.50f, h * 0.50f),
      style = Stroke(stroke, cap = StrokeCap.Round),
    )
    drawCircle(tint, radius = w * 0.08f, center = Offset(w * 0.5f, h * 0.5f))
  }
}

@Composable
fun NfcGlyph(tint: Color = CapsuleColors.Cyan, size: Dp = 18.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = w * 0.1f
    drawCircle(tint, radius = w * 0.42f, style = Stroke(stroke))
    drawArc(
      color = tint,
      startAngle = -55f,
      sweepAngle = 110f,
      useCenter = false,
      topLeft = Offset(w * 0.22f, h * 0.28f),
      size = Size(w * 0.44f, h * 0.44f),
      style = Stroke(stroke, cap = StrokeCap.Round),
    )
    drawArc(
      color = tint,
      startAngle = -55f,
      sweepAngle = 110f,
      useCenter = false,
      topLeft = Offset(w * 0.35f, h * 0.36f),
      size = Size(w * 0.28f, h * 0.28f),
      style = Stroke(stroke, cap = StrokeCap.Round),
    )
  }
}

@Composable
fun SatelliteGlyph(tint: Color = CapsuleColors.Emerald, size: Dp = 18.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = w * 0.09f
    // Dish + signal arcs
    drawLine(tint, Offset(w * 0.22f, h * 0.78f), Offset(w * 0.52f, h * 0.48f), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.34f, h * 0.34f), Offset(w * 0.66f, h * 0.66f), strokeWidth = stroke * 1.3f, cap = StrokeCap.Round)
    drawArc(
      color = tint,
      startAngle = -80f,
      sweepAngle = 70f,
      useCenter = false,
      topLeft = Offset(w * 0.36f, h * 0.14f),
      size = Size(w * 0.50f, h * 0.50f),
      style = Stroke(stroke, cap = StrokeCap.Round),
    )
    drawArc(
      color = tint,
      startAngle = -80f,
      sweepAngle = 70f,
      useCenter = false,
      topLeft = Offset(w * 0.46f, h * 0.24f),
      size = Size(w * 0.30f, h * 0.30f),
      style = Stroke(stroke, cap = StrokeCap.Round),
    )
  }
}

@Composable
fun PhoneGlyph(tint: Color = CapsuleColors.Emerald, size: Dp = 18.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val path = Path().apply {
      moveTo(w * 0.25f, h * 0.18f)
      lineTo(w * 0.38f, h * 0.18f)
      lineTo(w * 0.43f, h * 0.36f)
      lineTo(w * 0.34f, h * 0.43f)
      cubicTo(w * 0.40f, h * 0.55f, w * 0.45f, h * 0.60f, w * 0.57f, h * 0.66f)
      lineTo(w * 0.64f, h * 0.57f)
      lineTo(w * 0.82f, h * 0.62f)
      lineTo(w * 0.82f, h * 0.75f)
      cubicTo(w * 0.82f, h * 0.84f, w * 0.72f, h * 0.86f, w * 0.62f, h * 0.82f)
      cubicTo(w * 0.34f, h * 0.72f, w * 0.28f, h * 0.66f, w * 0.18f, h * 0.38f)
      cubicTo(w * 0.14f, h * 0.28f, w * 0.16f, h * 0.18f, w * 0.25f, h * 0.18f)
      close()
    }
    drawPath(path, color = tint)
  }
}

@Composable
fun MediaControlGlyph(
  type: String, // "play", "pause", "prev", "next"
  tint: Color = CapsuleColors.TextPrimary,
  size: Dp = 18.dp,
) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    when (type) {
      "play" -> {
        val p = Path().apply {
          moveTo(w * 0.28f, h * 0.18f)
          lineTo(w * 0.82f, h * 0.50f)
          lineTo(w * 0.28f, h * 0.82f)
          close()
        }
        drawPath(p, color = tint)
      }
      "pause" -> {
        drawRoundRect(tint, Offset(w * 0.24f, h * 0.20f), Size(w * 0.18f, h * 0.60f), CornerRadius(w * 0.05f))
        drawRoundRect(tint, Offset(w * 0.58f, h * 0.20f), Size(w * 0.18f, h * 0.60f), CornerRadius(w * 0.05f))
      }
      "next" -> {
        val p1 = Path().apply {
          moveTo(w * 0.16f, h * 0.24f)
          lineTo(w * 0.50f, h * 0.50f)
          lineTo(w * 0.16f, h * 0.76f)
          close()
        }
        val p2 = Path().apply {
          moveTo(w * 0.50f, h * 0.24f)
          lineTo(w * 0.84f, h * 0.50f)
          lineTo(w * 0.50f, h * 0.76f)
          close()
        }
        drawPath(p1, color = tint)
        drawPath(p2, color = tint)
      }
      else -> {
        val p1 = Path().apply {
          moveTo(w * 0.50f, h * 0.24f)
          lineTo(w * 0.16f, h * 0.50f)
          lineTo(w * 0.50f, h * 0.76f)
          close()
        }
        val p2 = Path().apply {
          moveTo(w * 0.84f, h * 0.24f)
          lineTo(w * 0.50f, h * 0.50f)
          lineTo(w * 0.84f, h * 0.76f)
          close()
        }
        drawPath(p1, color = tint)
        drawPath(p2, color = tint)
      }
    }
  }
}

@Composable
fun TurnArrowGlyph(
  direction: TurnDirection,
  tint: Color = CapsuleColors.TextPrimary,
  size: Dp = 18.dp,
) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = Stroke(width = w * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val path = Path()
    when (direction) {
      TurnDirection.RIGHT, TurnDirection.SLIGHT_RIGHT -> {
        path.moveTo(w * 0.30f, h * 0.85f)
        path.lineTo(w * 0.30f, h * 0.35f)
        path.lineTo(w * 0.75f, h * 0.35f)
        drawPath(path, color = tint, style = stroke)
        drawLine(tint, Offset(w * 0.58f, h * 0.20f), Offset(w * 0.76f, h * 0.35f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.58f, h * 0.50f), Offset(w * 0.76f, h * 0.35f), strokeWidth = stroke.width, cap = StrokeCap.Round)
      }
      TurnDirection.LEFT, TurnDirection.SLIGHT_LEFT -> {
        path.moveTo(w * 0.70f, h * 0.85f)
        path.lineTo(w * 0.70f, h * 0.35f)
        path.lineTo(w * 0.25f, h * 0.35f)
        drawPath(path, color = tint, style = stroke)
        drawLine(tint, Offset(w * 0.42f, h * 0.20f), Offset(w * 0.24f, h * 0.35f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.42f, h * 0.50f), Offset(w * 0.24f, h * 0.35f), strokeWidth = stroke.width, cap = StrokeCap.Round)
      }
      TurnDirection.U_TURN -> {
        path.moveTo(w * 0.68f, h * 0.82f)
        path.lineTo(w * 0.68f, h * 0.38f)
        path.cubicTo(w * 0.68f, h * 0.16f, w * 0.32f, h * 0.16f, w * 0.32f, h * 0.38f)
        path.lineTo(w * 0.32f, h * 0.72f)
        drawPath(path, color = tint, style = stroke)
        drawLine(tint, Offset(w * 0.18f, h * 0.56f), Offset(w * 0.32f, h * 0.74f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.46f, h * 0.56f), Offset(w * 0.32f, h * 0.74f), strokeWidth = stroke.width, cap = StrokeCap.Round)
      }
      else -> {
        drawLine(tint, Offset(w * 0.50f, h * 0.84f), Offset(w * 0.50f, h * 0.20f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.30f, h * 0.38f), Offset(w * 0.50f, h * 0.18f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.70f, h * 0.38f), Offset(w * 0.50f, h * 0.18f), strokeWidth = stroke.width, cap = StrokeCap.Round)
      }
    }
  }
}

@Composable
fun TimerGlyph(tint: Color = CapsuleColors.Amber, size: Dp = 18.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = w * 0.10f
    drawCircle(tint, radius = w * 0.36f, center = Offset(w * 0.5f, h * 0.55f), style = Stroke(stroke))
    drawLine(tint, Offset(w * 0.40f, h * 0.12f), Offset(w * 0.60f, h * 0.12f), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.50f, h * 0.55f), Offset(w * 0.50f, h * 0.34f), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.50f, h * 0.55f), Offset(w * 0.66f, h * 0.62f), strokeWidth = stroke, cap = StrokeCap.Round)
  }
}

@Composable
fun DownloadGlyph(tint: Color = CapsuleColors.Sapphire, size: Dp = 18.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = w * 0.11f
    drawLine(tint, Offset(w * 0.50f, h * 0.16f), Offset(w * 0.50f, h * 0.66f), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.32f, h * 0.48f), Offset(w * 0.50f, h * 0.68f), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.68f, h * 0.48f), Offset(w * 0.50f, h * 0.68f), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.22f, h * 0.84f), Offset(w * 0.78f, h * 0.84f), strokeWidth = stroke, cap = StrokeCap.Round)
  }
}

@Composable
fun AudioVisualizerBars(
  isPlaying: Boolean,
  phase: Float,
  tint: Color = CapsuleColors.Emerald,
  size: Dp = 18.dp,
) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val barCount = 4
    val barWidth = w * 0.14f
    val gap = (w - barCount * barWidth) / (barCount + 1)
    for (i in 0 until barCount) {
      val amp =
        if (isPlaying) {
          0.30f + 0.65f * kotlin.math.abs(sin((phase + i * 0.85f).toDouble())).toFloat()
        } else {
          0.22f
        }
      val barH = h * amp
      val x = gap + i * (barWidth + gap)
      val y = (h - barH) / 2f
      drawRoundRect(
        color = tint,
        topLeft = Offset(x, y),
        size = Size(barWidth, barH),
        cornerRadius = CornerRadius(barWidth / 2f),
      )
    }
  }
}

@Composable
fun DropZoneGlyph(tint: Color = CapsuleColors.Cyan, size: Dp = 20.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = w * 0.10f
    drawRoundRect(
      color = tint,
      topLeft = Offset(w * 0.16f, h * 0.16f),
      size = Size(w * 0.68f, h * 0.68f),
      cornerRadius = CornerRadius(w * 0.18f),
      style = Stroke(stroke),
    )
    drawLine(tint, Offset(w * 0.50f, h * 0.32f), Offset(w * 0.50f, h * 0.68f), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.32f, h * 0.50f), Offset(w * 0.68f, h * 0.50f), strokeWidth = stroke, cap = StrokeCap.Round)
  }
}

@Composable
fun CutoutGlyph(tint: Color = CapsuleColors.TextPrimary, size: Dp = 18.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    drawRoundRect(
      color = tint,
      topLeft = Offset(w * 0.10f, h * 0.30f),
      size = Size(w * 0.80f, h * 0.40f),
      cornerRadius = CornerRadius(h * 0.20f),
      style = Stroke(w * 0.10f),
    )
    drawCircle(tint, radius = w * 0.08f, center = Offset(w * 0.72f, h * 0.50f))
  }
}

@Composable
fun SparklePulseGlyph(tint: Color = CapsuleColors.TextPrimary, size: Dp = 18.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = w * 0.10f
    val path = Path().apply {
      moveTo(w * 0.10f, h * 0.52f)
      lineTo(w * 0.28f, h * 0.52f)
      lineTo(w * 0.40f, h * 0.22f)
      lineTo(w * 0.56f, h * 0.78f)
      lineTo(w * 0.70f, h * 0.42f)
      lineTo(w * 0.78f, h * 0.52f)
      lineTo(w * 0.90f, h * 0.52f)
    }
    drawPath(path, color = tint, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
  }
}

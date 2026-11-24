package com.example.draggo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Patterns
import com.example.data.SettingsRepository
import com.example.model.DragAction
import com.example.model.DragActionType
import com.example.model.DragDropResult
import com.example.state.CapsuleStateHolder
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import kotlin.coroutines.resume

class DragAndGoProcessor(
  private val context: Context,
  private val settingsRepository: SettingsRepository,
) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val textRecognizer by lazy {
    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
  }

  private val addressRegex =
    Regex(
      """\b\d{1,5}\s+[A-Za-z0-9.\s]{2,32}\b(?:St|Street|Ave|Avenue|Blvd|Boulevard|Rd|Road|Dr|Drive|Ln|Lane|Way|Pkwy|Parkway|Pl|Place)\b\.?(?:\s*,?\s*[A-Za-z\s]{2,20})?""",
      RegexOption.IGNORE_CASE,
    )

  fun processClipData(clipData: ClipData, sourceLabel: String = "Dropped Content") {
    val itemCount = clipData.itemCount
    if (itemCount == 0) return

    val textParts = mutableListOf<String>()
    var firstUri: Uri? = null

    for (i in 0 until itemCount) {
      val item = clipData.getItemAt(i)
      val text = item.coerceToText(context)?.toString()?.trim()
      if (!text.isNullOrBlank()) {
        textParts.add(text)
      }
      if (firstUri == null && item.uri != null) {
        firstUri = item.uri
      }
    }

    val combinedText = textParts.joinToString("\n").trim()
    val mime = clipData.description?.getMimeType(0).orEmpty()

    if (firstUri != null && (mime.startsWith("image/") || combinedText.isBlank())) {
      processImageUri(firstUri, sourceLabel)
    } else if (combinedText.isNotBlank()) {
      publishTextResult(combinedText, sourceLabel)
    }
  }

  fun processPlainText(text: String, sourceLabel: String = "Dragged Snippet") {
    if (text.isBlank()) return
    publishTextResult(text.trim(), sourceLabel)
  }

  fun processImageUri(uri: Uri, sourceLabel: String = "Image OCR") {
    CapsuleStateHolder.setDragDropResult(
      DragDropResult(
        sourceLabel = sourceLabel,
        extractedText = "Scanning text on-device...",
        isProcessing = true,
      )
    )
    scope.launch {
      val extracted = runOcrOnUri(uri)
      if (extracted.isBlank()) {
        val fallbackText = uri.lastPathSegment ?: uri.toString()
        publishTextResult(
          text = "No readable text detected ($fallbackText)",
          sourceLabel = sourceLabel,
          allowSmartLinks = false,
        )
      } else {
        publishTextResult(extracted, sourceLabel)
      }
    }
  }

  private suspend fun runOcrOnUri(uri: Uri): String =
    withContext(Dispatchers.IO) {
      val bitmap = decodeSampledBitmap(uri, maxDimension = 1280) ?: return@withContext ""
      suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        textRecognizer
          .process(image)
          .addOnSuccessListener { visionText ->
            bitmap.recycle()
            if (cont.isActive) cont.resume(visionText.text.trim())
          }
          .addOnFailureListener {
            bitmap.recycle()
            if (cont.isActive) cont.resume("")
          }
      }
    }

  private fun decodeSampledBitmap(uri: Uri, maxDimension: Int): Bitmap? =
    runCatching {
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
      }
      var sampleSize = 1
      val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
      while (maxSide / sampleSize > maxDimension) {
        sampleSize *= 2
      }
      val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, opts)
      }
    }.getOrNull()

  private fun publishTextResult(
    text: String,
    sourceLabel: String,
    allowSmartLinks: Boolean = true,
  ) {
    val url = if (allowSmartLinks) Patterns.WEB_URL.matcher(text).let { if (it.find()) it.group() else null } else null
    val address = if (allowSmartLinks) addressRegex.find(text)?.value else null
    val phone =
      if (allowSmartLinks) {
        Patterns.PHONE.matcher(text).let { m ->
          if (m.find() && m.group().count { c -> c.isDigit() } >= 7) m.group() else null
        }
      } else {
        null
      }

    val actions = buildList {
      add(DragAction(DragActionType.COPY, "Copy", text))
      add(DragAction(DragActionType.SHARE, "Share", text))
      if (address != null) {
        add(DragAction(DragActionType.OPEN_MAPS, "Open Maps", address))
      }
      if (phone != null) {
        add(DragAction(DragActionType.DIAL_PHONE, "Dial", phone))
      }
      add(
        DragAction(
          DragActionType.SEARCH_WEB,
          if (url != null) "Open Link" else "Search Web",
          url ?: text.take(120),
        )
      )
      add(DragAction(DragActionType.QUICK_NOTE, "Quick Note", text))
    }

    CapsuleStateHolder.setDragDropResult(
      DragDropResult(
        sourceLabel = sourceLabel,
        extractedText = text,
        detectedUrl = url,
        detectedAddress = address,
        detectedPhone = phone,
        actions = actions,
        isProcessing = false,
      )
    )
  }

  fun executeAction(action: DragAction) {
    when (action.type) {
      DragActionType.COPY -> {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Capsule Drag & Go", action.value))
        showBanner("Copied to clipboard")
      }
      DragActionType.SHARE -> {
        val intent =
          Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, action.value)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
        val chooser =
          Intent.createChooser(intent, "Share via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
        runCatching { context.startActivity(chooser) }
      }
      DragActionType.SEARCH_WEB -> {
        val targetUrl =
          if (action.value.startsWith("http://") || action.value.startsWith("https://")) {
            action.value
          } else {
            val encoded = URLEncoder.encode(action.value, "UTF-8")
            "https://www.google.com/search?q=$encoded"
          }
        val intent =
          Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
        runCatching { context.startActivity(intent) }
        CapsuleStateHolder.collapseNow()
      }
      DragActionType.OPEN_MAPS -> {
        val encoded = URLEncoder.encode(action.value, "UTF-8")
        val intent =
          Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encoded")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
        runCatching { context.startActivity(intent) }
        CapsuleStateHolder.collapseNow()
      }
      DragActionType.DIAL_PHONE -> {
        val intent =
          Intent(Intent.ACTION_DIAL, Uri.parse("tel:${action.value}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
        runCatching { context.startActivity(intent) }
        CapsuleStateHolder.collapseNow()
      }
      DragActionType.QUICK_NOTE -> {
        scope.launch {
          settingsRepository.addQuickNote(action.value)
          showBanner("Saved to Quick Notes")
        }
      }
    }
  }

  private fun showBanner(message: String) {
    val current = CapsuleStateHolder.uiState.value.dragDropResult ?: return
    CapsuleStateHolder.setDragDropResult(current.copy(confirmationBanner = message))
  }
}

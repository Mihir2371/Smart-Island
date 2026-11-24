package com.example.liveactivity

import android.app.Notification
import android.service.notification.StatusBarNotification
import com.example.model.LiveActivity
import com.example.model.TurnDirection
import java.util.Locale

object NotificationParser {

  private val scoreRegex = Regex("""(\b\d{1,3}\s*[-:]\s*\d{1,3}\b)""")
  private val distanceRegex = Regex("""(\b\d+(?:\.\d+)?\s?(?:m|km|ft|mi|yd)\b)""", RegexOption.IGNORE_CASE)
  private val etaMinRegex = Regex("""(\b\d{1,3}\s?min(?:s|utes?)?\b)""", RegexOption.IGNORE_CASE)
  private val gateRegex = Regex("""(?:gate|g)\s*([A-Z]?\d{1,3}[A-Z]?)""", RegexOption.IGNORE_CASE)
  private val flightCodeRegex = Regex("""\b([A-Z]{2}\s?\d{2,4})\b""")

  fun parseAll(notifications: Array<StatusBarNotification>?): List<LiveActivity> {
    if (notifications.isNullOrEmpty()) return emptyList()
    val results = mutableListOf<LiveActivity>()
    for (sbn in notifications) {
      parseOne(sbn)?.let { results.add(it) }
    }
    return results.sortedByDescending { it.priority }
  }

  fun parseOne(sbn: StatusBarNotification): LiveActivity? {
    val notif = sbn.notification ?: return null
    val extras = notif.extras ?: return null
    val pkg = sbn.packageName.orEmpty()
    val pkgLower = pkg.lowercase(Locale.US)

    // Ignore our own foreground service notification
    if (pkg == "com.aistudio.capsule.qvrtxm" || pkg == "com.example") return null

    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
    val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()
    val combined = "$title $text $subText".trim()
    if (combined.isBlank()) return null

    val category = notif.category.orEmpty()

    // 1. Phone & VoIP Calls
    if (
      category == Notification.CATEGORY_CALL ||
        (sbn.isOngoing &&
          (pkgLower.contains("dialer") ||
            pkgLower.contains("telecom") ||
            pkgLower.contains("whatsapp") ||
            pkgLower.contains("telegram") ||
            pkgLower.contains("discord")) &&
          (combined.contains("call", ignoreCase = true) ||
            combined.contains("ongoing", ignoreCase = true) ||
            notif.usesChronometer()))
    ) {
      val startedAt = if (notif.`when` > 0L) notif.`when` else System.currentTimeMillis()
      val isVoip = pkgLower.contains("whatsapp") || pkgLower.contains("telegram") || pkgLower.contains("discord")
      val appLabel =
        when {
          pkgLower.contains("whatsapp") -> "WhatsApp"
          pkgLower.contains("telegram") -> "Telegram"
          pkgLower.contains("discord") -> "Discord"
          else -> "Cellular"
        }
      return LiveActivity.PhoneCall(
        id = "notif_${sbn.key}",
        packageName = pkg,
        callerName = title.ifBlank { "Active Call" },
        appLabel = appLabel,
        startedAtMs = startedAt,
        isVoip = isVoip,
      )
    }

    // 2. Turn-by-turn Navigation
    if (
      category == Notification.CATEGORY_NAVIGATION ||
        (sbn.isOngoing &&
          (pkgLower.contains("maps") || pkgLower.contains("waze") || pkgLower.contains("osmand")))
    ) {
      val dir =
        when {
          combined.contains("u-turn", ignoreCase = true) -> TurnDirection.U_TURN
          combined.contains("slight left", ignoreCase = true) -> TurnDirection.SLIGHT_LEFT
          combined.contains("slight right", ignoreCase = true) -> TurnDirection.SLIGHT_RIGHT
          combined.contains("left", ignoreCase = true) -> TurnDirection.LEFT
          combined.contains("right", ignoreCase = true) -> TurnDirection.RIGHT
          combined.contains("roundabout", ignoreCase = true) -> TurnDirection.ROUNDABOUT
          else -> TurnDirection.STRAIGHT
        }
      val distance = distanceRegex.find(combined)?.value ?: "Ahead"
      val eta = etaMinRegex.find(combined)?.value ?: subText.ifBlank { "Active" }
      return LiveActivity.Navigation(
        id = "notif_${sbn.key}",
        packageName = pkg,
        direction = dir,
        distanceText = distance,
        streetName = text.ifBlank { title },
        etaText = eta,
      )
    }

    // 3. Timers & Alarms
    if (
      (category == Notification.CATEGORY_ALARM || pkgLower.contains("deskclock") || pkgLower.contains("clock")) &&
        sbn.isOngoing
    ) {
      val isCountdown = extras.getBoolean("android.chronometerCountDown", false)
      val baseTime = if (notif.`when` > System.currentTimeMillis()) notif.`when` else System.currentTimeMillis() + 60_000L
      if (isCountdown || combined.contains("timer", ignoreCase = true)) {
        return LiveActivity.TimerActivity(
          id = "notif_${sbn.key}",
          packageName = pkg,
          label = title.ifBlank { "Timer" },
          endTimeMs = baseTime,
          totalMs = (baseTime - System.currentTimeMillis()).coerceAtLeast(60_000L),
        )
      }
    }

    // 4. Downloads & File Transfers
    val maxProgress = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
    val progress = extras.getInt(Notification.EXTRA_PROGRESS, 0)
    val indeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)
    if (
      (maxProgress > 0 && progress in 0..maxProgress) ||
        (sbn.isOngoing && (category == Notification.CATEGORY_PROGRESS || pkgLower.contains("downloads")))
    ) {
      val frac = if (maxProgress > 0) (progress.toFloat() / maxProgress).coerceIn(0f, 1f) else if (indeterminate) 0.45f else 0f
      return LiveActivity.DownloadActivity(
        id = "notif_${sbn.key}",
        packageName = pkg,
        fileName = title.ifBlank { "Downloading file" },
        progress = frac,
        speedText = text.ifBlank { "${(frac * 100).toInt()}%" },
      )
    }

    // 5. Rideshare (Uber, Lyft, Grab, Bolt)
    if (
      sbn.isOngoing &&
        (pkgLower.contains("uber") ||
          pkgLower.contains("lyft") ||
          pkgLower.contains("bolt") ||
          pkgLower.contains("grab") ||
          combined.contains("driver", ignoreCase = true))
    ) {
      val etaMatch = etaMinRegex.find(combined)?.value?.filter { it.isDigit() }?.toIntOrNull() ?: 4
      return LiveActivity.Rideshare(
        id = "notif_${sbn.key}",
        packageName = pkg,
        provider = if (pkgLower.contains("lyft")) "Lyft" else "Uber",
        statusText = title.ifBlank { "Driver arriving" },
        etaMinutes = etaMatch,
        vehicleInfo = text.ifBlank { "Arriving soon" },
      )
    }

    // 6. Food / Package Delivery
    if (
      sbn.isOngoing &&
        (pkgLower.contains("doordash") ||
          pkgLower.contains("ubereats") ||
          pkgLower.contains("instacart") ||
          pkgLower.contains("grubhub") ||
          pkgLower.contains("zomato") ||
          combined.contains("deliver", ignoreCase = true))
    ) {
      val etaMatch = etaMinRegex.find(combined)?.value?.filter { it.isDigit() }?.toIntOrNull() ?: 12
      val frac = if (maxProgress > 0) (progress.toFloat() / maxProgress).coerceIn(0.1f, 1f) else 0.65f
      return LiveActivity.Delivery(
        id = "notif_${sbn.key}",
        packageName = pkg,
        provider = title.ifBlank { "Delivery" },
        statusText = text.ifBlank { "On the way" },
        etaMinutes = etaMatch,
        progress = frac,
      )
    }

    // 7. Flights
    if (sbn.isOngoing && (combined.contains("boarding", ignoreCase = true) || gateRegex.containsMatchIn(combined))) {
      val code = flightCodeRegex.find(combined)?.value ?: "FL 204"
      val gate = gateRegex.find(combined)?.groupValues?.getOrNull(1) ?: "B12"
      val mins = etaMinRegex.find(combined)?.value?.filter { it.isDigit() }?.toIntOrNull() ?: 18
      return LiveActivity.Flight(
        id = "notif_${sbn.key}",
        packageName = pkg,
        flightCode = code,
        gate = gate,
        statusText = text.ifBlank { "Boarding soon" },
        boardingMinutes = mins,
      )
    }

    // 8. Live Sports Scores
    val scoreMatch = scoreRegex.find(combined)
    if (sbn.isOngoing && scoreMatch != null && !combined.contains(":00")) {
      val parts = scoreMatch.value.split('-', ':').mapNotNull { it.trim().toIntOrNull() }
      if (parts.size == 2) {
        return LiveActivity.Sports(
          id = "notif_${sbn.key}",
          packageName = pkg,
          league = subText.ifBlank { "LIVE" },
          homeTeam = "HOM",
          awayTeam = "AWY",
          homeScore = parts[0],
          awayScore = parts[1],
          clockText = title.take(14),
        )
      }
    }

    return null
  }

  private fun Notification.usesChronometer(): Boolean =
    extras?.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) == true
}

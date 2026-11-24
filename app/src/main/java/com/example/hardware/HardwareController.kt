package com.example.hardware

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import com.example.model.HardwarePopup
import com.example.model.LiveActivity
import com.example.model.SystemToggleType
import com.example.state.CapsuleStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

class HardwareController(private val context: Context) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  private val notificationManager =
    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

  private var torchCameraId: String? = null
  private var maxTorchLevel: Int = 1
  private var audioRecordJob: Job? = null
  private var currentBrightness: Float = 0.7f
  private var currentBeamSpread: Float = 0.55f

  init {
    resolveTorchCamera()
  }

  private fun resolveTorchCamera() {
    runCatching {
      for (id in cameraManager.cameraIdList) {
        val chars = cameraManager.getCameraCharacteristics(id)
        val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        val facingBack =
          chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        if (hasFlash && facingBack) {
          torchCameraId = id
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            maxTorchLevel =
              chars.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
          }
          break
        }
      }
    }
  }

  fun performHapticTap() {
    runCatching {
      val vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
            .defaultVibrator
        } else {
          @Suppress("DEPRECATION")
          context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(14L, 90))
      } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(14L)
      }
    }
  }

  fun openFlashlightControls(forceOn: Boolean? = null) {
    val current = CapsuleStateHolder.uiState.value.activeHardwarePopup as? HardwarePopup.Flashlight
    val targetOn = forceOn ?: !(current?.isOn ?: false)
    applyTorchState(targetOn, currentBrightness, currentBeamSpread)
  }

  fun updateFlashlightBeam(brightness: Float, beamSpread: Float) {
    currentBrightness = brightness.coerceIn(0.08f, 1f)
    currentBeamSpread = beamSpread.coerceIn(0.2f, 1f)
    applyTorchState(isOn = true, brightness = currentBrightness, beamSpread = currentBeamSpread)
  }

  fun toggleFlashlightOff() {
    applyTorchState(isOn = false, brightness = currentBrightness, beamSpread = currentBeamSpread)
    CapsuleStateHolder.dismissHardwarePopup("flashlight")
  }

  private fun applyTorchState(isOn: Boolean, brightness: Float, beamSpread: Float) {
    val camId = torchCameraId
    if (camId != null) {
      runCatching {
        if (!isOn) {
          cameraManager.setTorchMode(camId, false)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && maxTorchLevel > 1) {
          val level = (brightness * maxTorchLevel).roundToInt().coerceIn(1, maxTorchLevel)
          cameraManager.turnOnTorchWithStrengthLevel(camId, level)
        } else {
          cameraManager.setTorchMode(camId, true)
        }
      }
    }
    CapsuleStateHolder.showHardwarePopup(
      popup =
        HardwarePopup.Flashlight(
          isOn = isOn,
          brightness = brightness,
          beamSpread = beamSpread,
        ),
      expand = true,
      autoDismissMs = if (isOn) null else 2200L,
    )
  }

  fun toggleCallMute(activityId: String) {
    runCatching {
      val nextMute = !audioManager.isMicrophoneMute
      audioManager.isMicrophoneMute = nextMute
      val existing =
        CapsuleStateHolder.uiState.value.liveActivities.find { it.id == activityId }
          as? LiveActivity.PhoneCall
      if (existing != null) {
        CapsuleStateHolder.upsertLiveActivity(existing.copy(isMuted = nextMute))
      }
    }
  }

  fun toggleCallSpeaker(activityId: String) {
    runCatching {
      val existing =
        CapsuleStateHolder.uiState.value.liveActivities.find { it.id == activityId }
          as? LiveActivity.PhoneCall
      val nextSpeaker = !(existing?.isSpeaker ?: false)
      @Suppress("DEPRECATION")
      audioManager.isSpeakerphoneOn = nextSpeaker
      if (existing != null) {
        CapsuleStateHolder.upsertLiveActivity(existing.copy(isSpeaker = nextSpeaker))
      }
    }
  }

  fun cycleRingerMode() {
    runCatching {
      val nextMode =
        when (audioManager.ringerMode) {
          AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
          AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
          else -> AudioManager.RINGER_MODE_NORMAL
        }
      // Note: RINGER_MODE_SILENT may require DND policy access on some devices; fall back gracefully to UI feedback
      runCatching { audioManager.ringerMode = nextMode }
      val popup =
        when (nextMode) {
          AudioManager.RINGER_MODE_SILENT ->
            HardwarePopup.SystemToggle(
              type = SystemToggleType.SILENT,
              label = "Silent",
              subtitle = "Alerts muted",
              isActive = true,
            )
          AudioManager.RINGER_MODE_VIBRATE ->
            HardwarePopup.SystemToggle(
              type = SystemToggleType.VIBRATE,
              label = "Vibrate",
              subtitle = "Haptic alerts active",
              isActive = true,
            )
          else ->
            HardwarePopup.SystemToggle(
              type = SystemToggleType.RING,
              label = "Ring",
              subtitle = "Sound on",
              isActive = false,
            )
        }
      CapsuleStateHolder.showHardwarePopup(popup, expand = true, autoDismissMs = 3000L)
    }
  }

  fun showDndStatusPopup() {
    val active =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
      } else {
        false
      }
    CapsuleStateHolder.showHardwarePopup(
      HardwarePopup.SystemToggle(
        type = SystemToggleType.DND,
        label = if (active) "Focus Active" else "Do Not Disturb",
        subtitle = if (active) "Notifications silenced" else "Tap system shade to toggle",
        isActive = active,
      ),
      expand = true,
      autoDismissMs = 3200L,
    )
  }

  @SuppressLint("MissingPermission")
  fun startVoiceMemoRecording(isScreenRecording: Boolean = false) {
    stopVoiceMemoRecording(dismissPopup = false)
    val startedAt = System.currentTimeMillis()
    val hasMicPermission =
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

    CapsuleStateHolder.showHardwarePopup(
      popup =
        HardwarePopup.Recording(
          isScreenRecording = isScreenRecording,
          startedAtMs = startedAt,
          amplitudes = List(16) { 0.18f },
        ),
      expand = true,
      autoDismissMs = null,
    )

    audioRecordJob =
      scope.launch(Dispatchers.IO) {
        var recorder: AudioRecord? = null
        val sampleRate = 16000
        val bufferSize =
          AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
          )
        if (hasMicPermission && !isScreenRecording && bufferSize > 0) {
          runCatching {
            recorder =
              AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
              )
            if (recorder?.state == AudioRecord.STATE_INITIALIZED) {
              recorder?.startRecording()
            } else {
              recorder?.release()
              recorder = null
            }
          }
        }

        val buffer = ShortArray((bufferSize.coerceAtLeast(1024)) / 2)
        val bars = ArrayDeque<Float>(16).apply { repeat(16) { addLast(0.15f) } }
        var step = 0
        try {
          while (isActive) {
            val normalizedAmp =
              if (recorder != null) {
                val read = recorder?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                  var peak = 0
                  for (i in 0 until read) {
                    val v = abs(buffer[i].toInt())
                    if (v > peak) peak = v
                  }
                  (peak / 14000f).coerceIn(0.12f, 1f)
                } else {
                  0.15f
                }
              } else {
                // Screen recording or permission-pending gentle pulse
                (0.22f + 0.55f * abs(kotlin.math.sin(step * 0.45))).toFloat()
              }
            step++
            bars.removeFirst()
            bars.addLast(normalizedAmp)
            CapsuleStateHolder.updateRecordingAmplitudes(bars.toList())
            delay(85L)
          }
        } finally {
          runCatching {
            recorder?.stop()
            recorder?.release()
          }
        }
      }
  }

  fun stopVoiceMemoRecording(dismissPopup: Boolean = true) {
    audioRecordJob?.cancel()
    audioRecordJob = null
    if (dismissPopup) {
      CapsuleStateHolder.dismissHardwarePopup("recording")
    }
  }
}

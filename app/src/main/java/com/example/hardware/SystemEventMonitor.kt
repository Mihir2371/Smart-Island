package com.example.hardware

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.model.HardwarePopup
import com.example.model.SystemToggleType
import com.example.state.CapsuleStateHolder
import kotlin.math.abs

class SystemEventMonitor(private val context: Context) {

  private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
  private val notificationManager =
    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  private val mainHandler = Handler(Looper.getMainLooper())

  private var started = false
  private var lastWasCharging: Boolean? = null
  private var lastLowBatteryAlertedLevel: Int = 100
  private var satelliteActive = false
  private val activeCameraIds = mutableSetOf<String>()

  private val gravity = FloatArray(3)
  private val geomagnetic = FloatArray(3)
  private var hasGravity = false
  private var hasGeo = false

  private val cameraAvailabilityCallback =
    object : CameraManager.AvailabilityCallback() {
      override fun onCameraUnavailable(cameraId: String) {
        activeCameraIds.add(cameraId)
        CapsuleStateHolder.updateHardwarePrivacy(cameraActive = activeCameraIds.isNotEmpty())
      }

      override fun onCameraAvailable(cameraId: String) {
        activeCameraIds.remove(cameraId)
        CapsuleStateHolder.updateHardwarePrivacy(cameraActive = activeCameraIds.isNotEmpty())
      }
    }

  private val audioRecordingCallback =
    object : AudioManager.AudioRecordingCallback() {
      override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>) {
        CapsuleStateHolder.updateHardwarePrivacy(micActive = configs.isNotEmpty())
      }
    }

  private val sensorListener =
    object : SensorEventListener {
      override fun onSensorChanged(event: SensorEvent) {
        if (!satelliteActive) return
        when (event.sensor.type) {
          Sensor.TYPE_ACCELEROMETER -> {
            System.arraycopy(event.values, 0, gravity, 0, 3)
            hasGravity = true
          }
          Sensor.TYPE_MAGNETIC_FIELD -> {
            System.arraycopy(event.values, 0, geomagnetic, 0, 3)
            hasGeo = true
          }
        }
        if (hasGravity && hasGeo) {
          val r = FloatArray(9)
          val i = FloatArray(9)
          if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
            val orientation = FloatArray(3)
            SensorManager.getOrientation(r, orientation)
            val azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
            val pitchDeg = Math.toDegrees(orientation[1].toDouble()).toFloat()
            // Target satellite window: azimuth ~ 15 deg, pitch ~ -40 deg (phone tilted up)
            val azimuthDelta = ((azimuthDeg - 15f + 540f) % 360f) - 180f
            val pitchDelta = pitchDeg - (-40f)
            val error = abs(azimuthDelta) + abs(pitchDelta)
            val locked = error < 24f
            val bars =
              when {
                error < 18f -> 4
                error < 35f -> 3
                error < 60f -> 2
                else -> 1
              }
            CapsuleStateHolder.showHardwarePopup(
              popup =
                HardwarePopup.SatelliteAlignment(
                  azimuthDelta = azimuthDelta.coerceIn(-90f, 90f),
                  pitchDelta = pitchDelta.coerceIn(-45f, 45f),
                  isLocked = locked,
                  signalBars = bars,
                ),
              expand = true,
              autoDismissMs = null,
            )
          }
        }
      }

      override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

  private val systemReceiver =
    object : BroadcastReceiver() {
      @SuppressLint("MissingPermission")
      override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
          Intent.ACTION_POWER_CONNECTED,
          Intent.ACTION_BATTERY_CHANGED -> handleBatteryIntent(intent)
          Intent.ACTION_POWER_DISCONNECTED -> lastWasCharging = false
          AudioManager.RINGER_MODE_CHANGED_ACTION -> handleRingerMode()
          NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> handleDndChanged()
          Intent.ACTION_AIRPLANE_MODE_CHANGED -> handleAirplaneChanged()
          "android.net.wifi.WIFI_AP_STATE_CHANGED" -> handleHotspotChanged(intent)
          BluetoothDevice.ACTION_ACL_CONNECTED -> handleBluetoothConnected(intent)
          "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED" -> handleBluetoothConnected(intent)
          NfcAdapter.ACTION_ADAPTER_STATE_CHANGED -> handleNfcState(intent)
        }
      }
    }

  fun start() {
    if (started) return
    started = true
    runCatching {
      cameraManager.registerAvailabilityCallback(cameraAvailabilityCallback, mainHandler)
    }
    runCatching {
      audioManager.registerAudioRecordingCallback(audioRecordingCallback, mainHandler)
    }
    val filter =
      IntentFilter().apply {
        addAction(Intent.ACTION_POWER_CONNECTED)
        addAction(Intent.ACTION_POWER_DISCONNECTED)
        addAction(Intent.ACTION_BATTERY_CHANGED)
        addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
        addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        addAction("android.net.wifi.WIFI_AP_STATE_CHANGED")
        addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
        addAction(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
      }
    ContextCompat.registerReceiver(
      context,
      systemReceiver,
      filter,
      ContextCompat.RECEIVER_EXPORTED,
    )
  }

  fun stop() {
    if (!started) return
    started = false
    stopSatelliteAlignment()
    runCatching { cameraManager.unregisterAvailabilityCallback(cameraAvailabilityCallback) }
    runCatching { audioManager.unregisterAudioRecordingCallback(audioRecordingCallback) }
    runCatching { context.unregisterReceiver(systemReceiver) }
  }

  fun startSatelliteAlignment() {
    satelliteActive = true
    val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    if (accel != null) {
      sensorManager.registerListener(sensorListener, accel, SensorManager.SENSOR_DELAY_UI)
    }
    if (mag != null) {
      sensorManager.registerListener(sensorListener, mag, SensorManager.SENSOR_DELAY_UI)
    }
    CapsuleStateHolder.showHardwarePopup(
      popup =
        HardwarePopup.SatelliteAlignment(
          azimuthDelta = 28f,
          pitchDelta = -12f,
          isLocked = false,
          signalBars = 2,
        ),
      expand = true,
      autoDismissMs = null,
    )
  }

  fun stopSatelliteAlignment() {
    satelliteActive = false
    runCatching { sensorManager.unregisterListener(sensorListener) }
    CapsuleStateHolder.dismissHardwarePopup("satellite")
  }

  private fun handleBatteryIntent(intent: Intent) {
    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val isCharging =
      status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
    val pct = if (level >= 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else return

    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
    val isFast = plugged == BatteryManager.BATTERY_PLUGGED_AC

    if (isCharging && lastWasCharging == false) {
      CapsuleStateHolder.showHardwarePopup(
        popup =
          HardwarePopup.Charging(
            level = pct,
            isFastCharging = isFast,
            wattageEstimate = if (isFast) 30 else 10,
          ),
        expand = true,
        autoDismissMs = 4000L,
      )
    } else if (!isCharging && pct <= 15 && pct < lastLowBatteryAlertedLevel) {
      lastLowBatteryAlertedLevel = pct
      CapsuleStateHolder.showHardwarePopup(
        popup = HardwarePopup.LowBattery(level = pct),
        expand = true,
        autoDismissMs = 5000L,
      )
    }
    lastWasCharging = isCharging
    if (pct > 20) lastLowBatteryAlertedLevel = 100
  }

  private fun handleRingerMode() {
    val popup =
      when (audioManager.ringerMode) {
        AudioManager.RINGER_MODE_SILENT ->
          HardwarePopup.SystemToggle(
            type = SystemToggleType.SILENT,
            label = "Silent",
            subtitle = "Ringtone muted",
            isActive = true,
          )
        AudioManager.RINGER_MODE_VIBRATE ->
          HardwarePopup.SystemToggle(
            type = SystemToggleType.VIBRATE,
            label = "Vibrate",
            subtitle = "Haptic alerts",
            isActive = true,
          )
        else ->
          HardwarePopup.SystemToggle(
            type = SystemToggleType.RING,
            label = "Ring",
            subtitle = "Alerts audible",
            isActive = false,
          )
      }
    CapsuleStateHolder.showHardwarePopup(popup, expand = true, autoDismissMs = 3000L)
  }

  private fun handleDndChanged() {
    val active =
      notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
    CapsuleStateHolder.showHardwarePopup(
      popup =
        HardwarePopup.SystemToggle(
          type = SystemToggleType.DND,
          label = if (active) "Do Not Disturb" else "Focus Off",
          subtitle = if (active) "Calls & alerts silenced" else "Notifications resumed",
          isActive = active,
        ),
      expand = true,
      autoDismissMs = 3200L,
    )
  }

  private fun handleAirplaneChanged() {
    val isAirplane =
      Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
    CapsuleStateHolder.showHardwarePopup(
      popup =
        HardwarePopup.SystemToggle(
          type = SystemToggleType.AIRPLANE,
          label = if (isAirplane) "Airplane Mode On" else "Radios Restored",
          subtitle = if (isAirplane) "Cellular & Wi-Fi disabled" else "Connectivity active",
          isActive = isAirplane,
        ),
      expand = true,
      autoDismissMs = 3200L,
    )
  }

  private fun handleHotspotChanged(intent: Intent) {
    val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1)
    // WIFI_AP_STATE_ENABLED is 13, DISABLED is 11
    if (state == 13 || state == 11) {
      val active = state == 13
      CapsuleStateHolder.showHardwarePopup(
        popup =
          HardwarePopup.SystemToggle(
            type = SystemToggleType.HOTSPOT,
            label = if (active) "Personal Hotspot" else "Hotspot Off",
            subtitle = if (active) "Tethering active" else "Sharing ended",
            isActive = active,
          ),
        expand = true,
        autoDismissMs = 3500L,
      )
    }
  }

  @SuppressLint("MissingPermission")
  private fun handleBluetoothConnected(intent: Intent) {
    val canReadBt =
      Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
          PackageManager.PERMISSION_GRANTED
    if (!canReadBt) return

    val device: BluetoothDevice? =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
      } else {
        @Suppress("DEPRECATION")
        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
      }
    device ?: return
    val name = device.name ?: "Audio Accessory"
    val battery = intent.getIntExtra("android.bluetooth.device.extra.BATTERY_LEVEL", -1)
    val isAudio =
      device.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO

    CapsuleStateHolder.showHardwarePopup(
      popup =
        HardwarePopup.BluetoothConnected(
          deviceName = name,
          batteryPercent = if (battery in 0..100) battery else 92,
          isAudioDevice = isAudio,
        ),
      expand = true,
      autoDismissMs = 4500L,
    )
  }

  private fun handleNfcState(intent: Intent) {
    val state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF)
    if (state == NfcAdapter.STATE_ON) {
      CapsuleStateHolder.showHardwarePopup(
        popup = HardwarePopup.NfcScan(tagSummary = "NFC Reader Ready"),
        expand = true,
        autoDismissMs = 2800L,
      )
    }
  }
}

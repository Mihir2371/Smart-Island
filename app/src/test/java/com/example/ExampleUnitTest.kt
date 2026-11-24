package com.example

import com.example.model.CapsuleDisplayMode
import com.example.model.CutoutConfig
import com.example.model.CutoutShapeMode
import com.example.model.HardwarePopup
import com.example.model.LiveActivity
import com.example.state.CapsuleStateHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExampleUnitTest {

  private val dispatcher = UnconfinedTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `cutout config switches between floating pill and top docked notch`() {
    val notch =
      CutoutConfig(
        mode = CutoutShapeMode.TOP_DOCKED_NOTCH,
        offsetXdp = 0f,
        offsetYdp = 0f,
        widthDp = 156f,
        heightDp = 36f,
        cornerRadiusDp = 18f,
      )
    CapsuleStateHolder.updateCutoutConfig(notch)
    assertEquals(CutoutShapeMode.TOP_DOCKED_NOTCH, CapsuleStateHolder.uiState.value.cutoutConfig.mode)
    assertEquals(0f, CapsuleStateHolder.uiState.value.cutoutConfig.offsetYdp)
  }

  @Test
  fun `live activities sort by descending priority and cycle cleanly`() {
    val media =
      LiveActivity.MediaPlayback(
        id = "m1",
        packageName = "com.spotify.music",
        title = "Track",
        artist = "Artist",
        isPlaying = true,
        positionMs = 1000L,
        durationMs = 60000L,
      )
    val call =
      LiveActivity.PhoneCall(
        id = "c1",
        packageName = "com.whatsapp",
        callerName = "Alex",
        appLabel = "WhatsApp",
        startedAtMs = 1000L,
      )
    CapsuleStateHolder.upsertLiveActivity(media)
    CapsuleStateHolder.upsertLiveActivity(call)
    assertTrue(CapsuleStateHolder.uiState.value.liveActivities.first() is LiveActivity.PhoneCall)
    CapsuleStateHolder.cycleLiveActivity()
    assertTrue(CapsuleStateHolder.uiState.value.activeLiveActivity is LiveActivity.MediaPlayback)
  }

  @Test
  fun `hardware popup dismissal restores collapsed state`() {
    CapsuleStateHolder.showHardwarePopup(
      HardwarePopup.NfcScan(tagSummary = "Transit Pass"),
      expand = true,
      autoDismissMs = null,
    )
    assertEquals(CapsuleDisplayMode.EXPANDED, CapsuleStateHolder.uiState.value.displayMode)
    CapsuleStateHolder.dismissHardwarePopup("nfc")
    assertNull(CapsuleStateHolder.uiState.value.activeHardwarePopup)
    assertEquals(CapsuleDisplayMode.COLLAPSED, CapsuleStateHolder.uiState.value.displayMode)
  }
}

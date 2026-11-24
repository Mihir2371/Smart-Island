package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SettingsRepository
import com.example.draggo.DragAndGoProcessor
import com.example.model.CapsuleDisplayMode
import com.example.model.DragActionType
import com.example.state.CapsuleStateHolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Capsule", appName)
  }

  @Test
  fun `drag and go extracts address and generates open maps pill`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val processor = DragAndGoProcessor(context, SettingsRepository(context))
    processor.processPlainText("Meet at 1600 Amphitheatre Parkway, Mountain View")
    val state = CapsuleStateHolder.uiState.value
    assertEquals(CapsuleDisplayMode.EXPANDED, state.displayMode)
    val result = state.dragDropResult!!
    assertTrue(result.actions.any { it.type == DragActionType.OPEN_MAPS })
    assertTrue(result.actions.any { it.type == DragActionType.COPY })
    assertTrue(result.actions.any { it.type == DragActionType.QUICK_NOTE })
  }
}

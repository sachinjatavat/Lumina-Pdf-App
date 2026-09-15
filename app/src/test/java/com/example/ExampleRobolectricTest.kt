package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.LuminaTab
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
    assertEquals("Lumina Convert", appName)
  }

  @Test
  fun `verify LuminaTab contains CONNECT and no AI_STUDIO`() {
    val tabs = LuminaTab.values().map { it.name }
    assertTrue(tabs.contains("CONNECT"))
    assertTrue(tabs.contains("VIEWER_EDITOR"))
    assertTrue(!tabs.contains("AI_STUDIO"))
  }

  @Test
  fun `verify DocumentType extensions`() {
    val pdf = com.example.domain.document.DocumentType.PDF
    assertEquals("pdf", pdf.extension)
  }
}


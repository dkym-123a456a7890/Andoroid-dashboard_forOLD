package com.example

import com.example.data.AppReleaseNote
import com.example.data.AppUpdateState
import com.example.data.UpdateCheckStatus
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for data models and update management logic.
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun updateState_defaultValues() {
    val state = AppUpdateState()
    assertEquals("2.5.0", state.currentVersion)
    assertEquals("2.6.0", state.latestVersion)
    assertEquals(UpdateCheckStatus.IDLE, state.status)
    assertEquals(30, state.autoRefreshIntervalMinutes)
    assertTrue(state.autoCheckEnabled)
    assertTrue(state.releaseNotes.isEmpty())
  }

  @Test
  fun updateState_statusTransitions() {
    var state = AppUpdateState(status = UpdateCheckStatus.CHECKING)
    assertEquals(UpdateCheckStatus.CHECKING, state.status)

    state = state.copy(status = UpdateCheckStatus.UPDATE_AVAILABLE)
    assertEquals(UpdateCheckStatus.UPDATE_AVAILABLE, state.status)

    state = state.copy(status = UpdateCheckStatus.DOWNLOADING, downloadProgress = 0.5f)
    assertEquals(0.5f, state.downloadProgress, 0.01f)

    state = state.copy(status = UpdateCheckStatus.COMPLETED, currentVersion = "2.6.0")
    assertEquals("2.6.0", state.currentVersion)
  }
}

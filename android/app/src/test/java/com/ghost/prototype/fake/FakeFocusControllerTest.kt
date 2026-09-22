package com.ghost.prototype.fake

import com.ghost.prototype.contract.FocusController
import com.ghost.prototype.contract.FocusControllerContract
import com.ghost.prototype.contract.FocusError
import com.ghost.prototype.floating.FloatingStateStore
import com.ghost.prototype.floating.OverlayPosition
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakeFocusControllerTest : FocusControllerContract() {
    private val floating = FloatingStateStore()
    private var overlayGranted = true
    private var starts = 0
    private var stops = 0

    /** 플로팅 명령이 성공하는 경우. 권한이 없으면 실제 FloatingLauncher처럼 오류를 남긴다. */
    override fun TestScope.create(): FocusController = FakeFocusController(
        startFloating = {
            starts++
            if (overlayGranted) floating.shown(OverlayPosition(0, 0)) else floating.failed("권한 없음")
        },
        stopFloating = { stops++; floating.stopped() },
        floating = floating.state,
        overlayGranted = { overlayGranted },
        scope = backgroundScope,
    )

    @Test
    fun startAndStopDriveFloating() = runTest {
        val controller = create()
        controller.startTask("t1")
        settle()
        assertTrue(controller.state.value.floatingVisible)
        controller.stop()
        settle()
        assertFalse(controller.state.value.floatingVisible)
        assertEquals(1, starts)
        assertEquals(1, stops)
    }

    @Test
    fun missingOverlayPermissionIsReportedAsCode() = runTest {
        overlayGranted = false
        val controller = create()
        controller.startTask("t1")
        settle()
        assertEquals(FocusError.OVERLAY_PERMISSION_MISSING, controller.state.value.error)
    }

    @Test
    fun failureWithPermissionIsAttachFailure() = runTest {
        val controller = create()
        floating.failed("창 연결 실패")
        settle()
        assertEquals(FocusError.OVERLAY_ATTACH_FAILED, controller.state.value.error)
    }

    @Test
    fun successfulStartClearsPreviousError() = runTest {
        overlayGranted = false
        val controller = create()
        controller.startTask("t1")
        settle()
        overlayGranted = true
        controller.startTask("t1")
        settle()
        assertNull(controller.state.value.error)
    }
}

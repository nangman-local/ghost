package com.ghost.prototype.floating

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.view.MotionEvent
import android.view.inspector.WindowInspector
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Requires the user to have granted GHOST's overlay permission on the device. */
@RunWith(AndroidJUnit4::class)
class OverlayControllerDeviceTest {
    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun tapGreetsButDragAndCancelDoNot() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assertTrue("Grant GHOST overlay permission first", Settings.canDrawOverlays(context))
        instrumentation.runOnMainSync {
            val store = FloatingStateStore()
            val controller = OverlayController(TestServiceContext(context), store) { error("Unexpected detach") }
            try {
                controller.show()
                val character = WindowInspector.getGlobalWindowViews().filterIsInstance<CharacterView>().single()
                fun send(action: Int, x: Float, y: Float) {
                    val time = SystemClock.uptimeMillis()
                    val event = MotionEvent.obtain(time, time, action, x, y, 0)
                    try { character.dispatchTouchEvent(event) } finally { event.recycle() }
                }
                send(MotionEvent.ACTION_DOWN, 100f, 100f)
                send(MotionEvent.ACTION_MOVE, 20f, 20f)
                send(MotionEvent.ACTION_UP, 20f, 20f)
                assertFalse(store.state.value.greetingVisible)
                send(MotionEvent.ACTION_DOWN, 100f, 100f)
                send(MotionEvent.ACTION_CANCEL, 100f, 100f)
                assertFalse(store.state.value.greetingVisible)
                send(MotionEvent.ACTION_DOWN, 100f, 100f)
                send(MotionEvent.ACTION_UP, 100f, 100f)
                assertTrue(store.state.value.greetingVisible)
                val greeted = store.state.value.position
                character.performClick()
                assertEquals(greeted, store.state.value.position)
                controller.hide()
                assertFalse(store.state.value.greetingVisible)
                controller.show()
                assertFalse(store.state.value.greetingVisible)
            } finally {
                controller.hide()
            }
        }
    }

    @Test fun nonVisualServiceContextCanShowMoveAndRemoveWindow() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assertTrue("Grant GHOST overlay permission on the device first", Settings.canDrawOverlays(context))
        instrumentation.runOnMainSync {
            val store = FloatingStateStore()
            val service = TestServiceContext(context)
            val controller = OverlayController(service, store) { error("Unexpected window detach") }
            try {
                controller.show()
                assertTrue(store.state.value.visible)
                val initial = store.state.value.position
                controller.show() // Idempotent: must not create a second window or reset position.
                assertEquals(initial, store.state.value.position)
                controller.repositionWithinScreen()
                assertEquals(initial, store.state.value.position)
                controller.hide()
                controller.hide()
                assertFalse(store.state.value.visible)
                assertNull(store.state.value.position)
            } finally {
                controller.hide()
            }
        }
    }

    private class TestServiceContext(context: Context) : Service() {
        init { attachBaseContext(context) }
        override fun onBind(intent: Intent?): IBinder? = null
    }
}

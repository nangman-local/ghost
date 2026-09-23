package com.ghost.prototype.floating

import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import kotlin.math.hypot
import kotlin.math.roundToInt

class OverlayController(
    context: Context,
    private val state: FloatingStateStore,
    private val onWindowLost: () -> Unit,
) {
    // A Service is non-visual: associate it with the phone display before making
    // a window context. Calling createWindowContext directly on it crashes.
    private val displayContext = context.createDisplayContext(
        checkNotNull(context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)),
    )
    private val windowContext = if (Build.VERSION.SDK_INT >= 30) {
        displayContext.createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
    } else displayContext
    private val windowManager = windowContext.getSystemService(WindowManager::class.java)
    private val density = windowContext.resources.displayMetrics.density
    private val width = (80 * density).roundToInt()
    private val height = (88 * density).roundToInt()
    private var view: CharacterView? = null
    private var params: WindowManager.LayoutParams? = null

    fun show() {
        if (view != null) return
        val position = bounds().initial(width, height, (16 * density).roundToInt())
        val layout = WindowManager.LayoutParams(
            width, height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            @Suppress("RtlHardcoded")
            gravity = Gravity.TOP or Gravity.LEFT
            x = position.x
            y = position.y
            title = "GHOST floating character"
            if (Build.VERSION.SDK_INT >= 30) {
                setFitInsetsTypes(0)
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
        val character = CharacterView(windowContext)
        character.setOnClickListener { showGreeting() }
        installDrag(character, position)
        character.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                if (view === v) onWindowLost()
            }
        })
        // Publish only after WindowManager has actually accepted the window.
        windowManager.addView(character, layout)
        view = character
        params = layout
        state.shown(position)
    }

    fun repositionWithinScreen() {
        val position = state.state.value.position ?: return
        moveTo(position)
    }

    /**
     * 개입 단계에 맞춰 캐릭터 표현을 바꾼다(#77). 레벨이 올라가면 창도 같이 커진다.
     *
     * **창은 캐릭터 크기 그대로 둔다.** 전체 화면 투명 창을 만들지 않는다
     * (`android/AGENTS.md` 오버레이 규칙) — 그러면 화면 전체의 터치를 먹는다.
     */
    fun applyAppearance(appearance: CharacterAppearance) {
        val character = view ?: return
        val layout = params ?: return
        if (character.appearance == appearance) return
        character.appearance = appearance

        val scaledWidth = (width * appearance.scale).roundToInt()
        val scaledHeight = (height * appearance.scale).roundToInt()
        val grewBy = scaledHeight - layout.height
        layout.width = scaledWidth
        layout.height = scaledHeight + greetingExtraHeight()
        // 커질 때 아래로 자라면 화면 밖으로 밀리기 쉽다. 위로 자라게 해서 제자리에 머물게 한다.
        val position = state.state.value.position ?: return
        moveTo(position.copy(y = position.y - grewBy.coerceAtLeast(0)))
    }

    private fun greetingExtraHeight(): Int =
        if (state.state.value.greetingVisible) (CharacterView.GREETING_HEIGHT_DP * density).roundToInt() else 0

    private fun showGreeting() {
        if (state.state.value.greetingVisible) return
        val character = view ?: return
        val layout = params ?: return
        val position = state.state.value.position ?: return
        val extraHeight = (CharacterView.GREETING_HEIGHT_DP * density).roundToInt()
        layout.height = height + extraHeight
        character.greetingVisible = true
        // Expand upward so tapping doesn't move the ghost, unless a screen edge
        // requires clamping. There is no invisible speech-bubble window before a tap.
        moveTo(position.copy(y = position.y - extraHeight))
        state.greet()
    }

    private fun moveTo(position: OverlayPosition) {
        val character = view ?: return
        val layout = params ?: return
        // 개입 단계에 따라 창이 커지므로(#77) 고정 width 가 아니라 현재 창 크기로 가둔다.
        val bounded = bounds().clamp(position, layout.width, layout.height)
        layout.x = bounded.x
        layout.y = bounded.y
        try {
            windowManager.updateViewLayout(character, layout)
            state.moved(bounded)
        } catch (_: SecurityException) {
            onWindowLost()
        } catch (_: IllegalArgumentException) {
            onWindowLost()
        }
    }

    fun hide() {
        val character = view
        view = null
        params = null
        if (character != null) {
            try {
                windowManager.removeViewImmediate(character)
            } catch (_: IllegalArgumentException) {
                // The system may already have removed it after permission revocation.
            }
        }
        state.stopped()
    }

    private fun installDrag(character: View, initial: OverlayPosition) {
        var downX = 0f
        var downY = 0f
        var origin = initial
        var dragging = false
        val touchSlop = ViewConfiguration.get(windowContext).scaledTouchSlop
        character.setOnTouchListener { touched, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    origin = state.state.value.position ?: initial
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (hypot(dx, dy) > touchSlop) dragging = true
                    if (dragging) moveTo(OverlayPosition(origin.x + dx.roundToInt(), origin.y + dy.roundToInt()))
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val distance = hypot(event.rawX - downX, event.rawY - downY)
                    if (!dragging && distance <= touchSlop) touched.performClick()
                    dragging = false
                    true
                }
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> {
                    // A cancelled/multi-touch gesture must not speak on release.
                    dragging = true
                    true
                }
                else -> true
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun bounds(): OverlayBounds {
        if (Build.VERSION.SDK_INT >= 30) {
            val metrics = windowManager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
            )
            return OverlayBounds(
                insets.left, insets.top,
                metrics.bounds.width() - insets.right,
                metrics.bounds.height() - insets.bottom,
            )
        }
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        // The OS also constrains application overlays to its available display frame.
        return OverlayBounds(0, 0, metrics.widthPixels, metrics.heightPixels)
    }
}

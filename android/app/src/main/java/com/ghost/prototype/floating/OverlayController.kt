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
    /** 협상 말풍선(#63)의 버튼을 눌렀다. `FloatingService`가 `NegotiationController`로 넘긴다. */
    private val onNegotiationAction: (NegotiationAction) -> Unit = {},
    /**
     * 유령을 탭했다(드래그·협상 버튼 아님). **레벨 2 제안은 자동으로 안 뜨고 이 탭으로 연다(#63).**
     * `true`를 반환하면 제안을 연 것이므로 "안녕?"은 건너뛴다.
     */
    private val onCharacterTapped: () -> Boolean = { false },
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

    /** 협상(#63) 시작 직전, 유령이 실제로 있던 자리. 협상이 끝나면 여기로 되돌린다. */
    private var positionBeforeNegotiation: OverlayPosition? = null

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
        // 제안이 열려 있으면(#63) "안녕?"을 건너뛴다. TalkBack 등 접근성 클릭도 이 경로를 탄다.
        character.setOnClickListener { if (!onCharacterTapped()) showGreeting() }
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
        if (character.appearance == appearance) return
        character.appearance = appearance
        applyLayout()
    }

    /**
     * 2분 협상(#63) 표시를 바꾼다. `Hidden`이 아니면 창이 [CharacterView.NEGOTIATION_WIDTH_DP]까지
     * 옆으로, [CharacterView.NEGOTIATION_HEIGHT_DP]만큼 위로 넓어져 말풍선·버튼을 담는다.
     *
     * **협상 중에는 화면 하단 중앙의 고정된 자리로 옮긴다.** 유령이 드래그돼 있던 자리에서
     * 그대로 커지면 화면 아무 데서나(하필 중요한 내용 위일 수도 있게) 뜬다 — 예측할 수 없다.
     * 대신 항상 같은 안전한 자리에서 뜨게 하고, 끝나면(수락 완료·거절·복귀) 유령을 원래
     * 있던 자리로 되돌린다.
     */
    fun applyNegotiation(negotiation: NegotiationState) {
        val character = view ?: return
        if (character.negotiation == negotiation) return
        val wasNegotiating = character.negotiation != NegotiationState.Hidden
        val entering = negotiation != NegotiationState.Hidden && !wasNegotiating
        val exiting = negotiation == NegotiationState.Hidden && wasNegotiating
        if (entering) positionBeforeNegotiation = state.state.value.position

        character.negotiation = negotiation
        applyLayout()

        if (exiting) {
            positionBeforeNegotiation?.let { moveTo(it) }
            positionBeforeNegotiation = null
        }
    }

    /**
     * 표현·협상 상태를 반영해 창 크기를 다시 잰다.
     *
     * **협상 중이 아니면** 캐릭터는 항상 창의 오른쪽 아래에 고정된다 — 커질 때 왼쪽·위로
     * 자라게 해서, 개입 레벨에 따라 커지고 작아져도 화면 위치는 거의 그대로 유지된다.
     *
     * **협상 중이면** 크기와 무관하게 [OverlayBounds.centered]의 고정된 자리(화면 정중앙)로
     * 간다. `Offer`·`Counting`·`Celebrate` 사이에는 크기가 안 바뀌므로 사실상 자리를 다시
     * 확인만 하는 셈이다.
     */
    private fun applyLayout() {
        val character = view ?: return
        val layout = params ?: return
        val negotiating = character.negotiation != NegotiationState.Hidden

        val scaledWidth = (width * character.appearance.scale).roundToInt()
        val scaledHeight = (height * character.appearance.scale).roundToInt()
        val targetWidth = if (negotiating) {
            (CharacterView.NEGOTIATION_WIDTH_DP * density).roundToInt().coerceAtLeast(scaledWidth)
        } else scaledWidth
        // 협상 중에는 "안녕?" 말풍선을 그리지 않는다(CharacterView.onDraw) — 그런데 greetingVisible이
        // 켜진 채로 레벨 2에 도달하면 그 여백만 남아 말풍선과 캐릭터 사이가 벌어진다. 둘은 배타적이다.
        val extraHeight = if (negotiating) {
            (CharacterView.NEGOTIATION_HEIGHT_DP * density).roundToInt()
        } else {
            greetingExtraHeight()
        }
        val targetHeight = scaledHeight + extraHeight

        if (negotiating) {
            layout.width = targetWidth
            layout.height = targetHeight
            moveTo(bounds().centered(targetWidth, targetHeight))
            return
        }

        val grewWidthBy = targetWidth - layout.width
        val grewHeightBy = targetHeight - layout.height
        layout.width = targetWidth
        layout.height = targetHeight
        val position = state.state.value.position ?: return
        moveTo(
            position.copy(
                x = position.x - grewWidthBy.coerceAtLeast(0),
                y = position.y - grewHeightBy.coerceAtLeast(0),
            ),
        )
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

    private fun installDrag(character: CharacterView, initial: OverlayPosition) {
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
                    if (!dragging && distance <= touchSlop) {
                        val action = character.negotiationActionAt(event.x, event.y)
                        when {
                            action != null -> onNegotiationAction(action)
                            character.negotiation == NegotiationState.Hidden -> touched.performClick()
                            // 협상 중 캐릭터 몸을 한 번 더 탭하면(#63) 거절 선택지를 연다.
                            // 말풍선 여백처럼 캐릭터도 버튼도 아닌 곳은 무시한다 — 인사로 새지 않는다
                            // (showGreeting()의 창 크기 계산은 협상용 크기와 다른 가정을 쓴다).
                            character.negotiationCharacterAreaAt(event.x, event.y) -> onCharacterTapped()
                        }
                    }
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

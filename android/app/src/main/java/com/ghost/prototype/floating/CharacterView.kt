package com.ghost.prototype.floating

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import androidx.core.graphics.withSave
import androidx.core.graphics.withTranslation
import com.ghost.prototype.R

/** 협상 말풍선(#63)의 버튼. 화면에 실제로 뭐가 있었는지는 [CharacterView.negotiationActionAt]이 안다. */
enum class NegotiationAction { ACCEPT, REJECT, CONTINUE, REST }

/** Temporary code-drawn character. No interaction policy or window ownership here. */
class CharacterView(context: Context) : View(context) {
    private val displayDensity = resources.displayMetrics.density

    var greetingVisible: Boolean = false
        set(value) {
            field = value
            updateDescription()
            invalidate()
        }

    /**
     * 2분 협상(#63)의 현재 표시. `Hidden`이 아니면 창이 옆으로 넓어져 말풍선을 그린다
     * (`negotiationWidthDp`). 그리기는 [OverlayController]가 창 크기를 맞춘 뒤 호출한다.
     */
    var negotiation: NegotiationState = NegotiationState.Hidden
        set(value) {
            if (field == value) return
            field = value
            updateDescription()
            invalidate()
        }

    /**
     * 개입 단계에 따른 표현. `shared/states.md`의 `characterState`를 따른다.
     * **Rive(`.riv`)가 나오면 이 View 안의 그리기만 교체한다(#16).** 매핑은 [CharacterAppearance]에 있다.
     */
    var appearance: CharacterAppearance = CharacterAppearance.forLevel(0)
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    /** 직전 그리기에서 버튼이 놓인 자리(View 픽셀 좌표). 터치 판정에 쓴다. */
    private var buttonHitRects: Map<NegotiationAction, RectF> = emptyMap()

    private val body = Path().apply {
        moveTo(10f, 72f)
        lineTo(10f, 37f)
        cubicTo(10f, 0f, 70f, 0f, 70f, 37f)
        lineTo(70f, 72f)
        quadTo(65f, 83f, 58f, 72f)
        quadTo(49f, 86f, 40f, 73f)
        quadTo(31f, 86f, 22f, 72f)
        quadTo(15f, 83f, 10f, 72f)
        close()
    }

    init {
        contentDescription = context.getString(R.string.ghost_description)
    }

    // `OverlayController.installDrag`가 setOnTouchListener로 클릭을 가로채므로,
    // 접근성 서비스(TalkBack 등)가 이 View를 클릭 가능하다고 인식하려면 명시적으로 있어야 한다.
    override fun performClick(): Boolean = super.performClick()

    private fun updateDescription() {
        contentDescription = context.getString(
            when {
                negotiation != NegotiationState.Hidden -> R.string.ghost_negotiation_description
                greetingVisible -> R.string.ghost_greeting_description
                else -> R.string.ghost_description
            },
        )
    }

    /** [x], [y](View 픽셀 좌표)에 놓인 협상 버튼. 없으면 null. `OverlayController`의 터치 판정이 부른다. */
    fun negotiationActionAt(x: Float, y: Float): NegotiationAction? =
        buttonHitRects.entries.firstOrNull { it.value.contains(x, y) }?.key

    /**
     * [x], [y]가 협상 중인 캐릭터 그림 자체(오른쪽 아래 80×88dp) 위인지. 버튼이 아니라
     * **유령 몸을 한 번 더 탭했을 때**(예: 거절 선택지 열기, #63) 쓴다.
     */
    fun negotiationCharacterAreaAt(x: Float, y: Float): Boolean {
        if (negotiation == NegotiationState.Hidden) return false
        val left = width - CHARACTER_SIZE_DP * displayDensity
        val top = height - CHARACTER_HEIGHT_DP * displayDensity
        return x >= left && y >= top
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.withSave {
            val negotiating = negotiation
            if (negotiating != NegotiationState.Hidden) {
                buttonHitRects = drawNegotiationBubble(this, negotiating)
                // 캐릭터 자체는 항상 80×88dp로 그린다 — 말풍선이 옆으로 넓어져도 크기는 그대로다.
                // 창은 오른쪽 아래에 캐릭터를 고정하고 왼쪽·위로 넓어지므로 여기서도 오른쪽 아래에 맞춘다.
                // 세로를 CHARACTER_SIZE_DP(너비 기준, 80)로 잘못 맞추면 그림 아래쪽(80~88 구간,
                // 유령 발 부분)이 창 밖으로 잘린다 — 반드시 CHARACTER_HEIGHT_DP(88)를 써야 한다.
                translate(width - CHARACTER_SIZE_DP * displayDensity, height - CHARACTER_HEIGHT_DP * displayDensity)
                scale(displayDensity, displayDensity)
            } else {
                buttonHitRects = emptyMap()
                scale(width / CHARACTER_SIZE_DP, width / CHARACTER_SIZE_DP)
                if (greetingVisible) {
                    drawGreeting(this)
                    translate(0f, GREETING_HEIGHT_DP)
                }
            }
            drawBody(this)
        }
    }

    private fun drawBody(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(246, 242, 255)
        canvas.drawPath(body, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.color = Color.rgb(99, 84, 181)
        canvas.drawPath(body, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(48, 39, 77)
        // 개입 단계가 올라갈수록 눈을 키워 시선을 끈다. Rive 전까지의 임시 표현이다(#16).
        val eye = when (appearance.state) {
            CharacterState.IDLE -> 0f
            CharacterState.ALERT -> 1.5f
            CharacterState.TALK -> 2.5f
            CharacterState.BLOCK -> 3.5f
        }
        canvas.drawOval(27f - eye, 34f - eye, 33f + eye, 44f + eye, paint)
        canvas.drawOval(47f - eye, 34f - eye, 53f + eye, 44f + eye, paint)
        paint.color = Color.rgb(236, 190, 207)
        canvas.drawOval(19f, 46f, 30f, 51f, paint)
        canvas.drawOval(50f, 46f, 61f, 51f, paint)
    }

    private fun drawGreeting(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(246, 242, 255)
        canvas.drawRoundRect(1.5f, 1.5f, 78.5f, 31f, 10f, 10f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = Color.rgb(99, 84, 181)
        canvas.drawRoundRect(1.5f, 1.5f, 78.5f, 31f, 10f, 10f, paint)
        canvas.drawLine(36f, 31f, 40f, 36f, paint)
        canvas.drawLine(40f, 36f, 44f, 31f, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(48, 39, 77)
        paint.textSize = 14f * resources.configuration.fontScale.coerceAtMost(1.5f)
        paint.textAlign = Paint.Align.CENTER
        val metrics = paint.fontMetrics
        canvas.drawText(context.getString(R.string.greeting), 40f, 16f - (metrics.ascent + metrics.descent) / 2f, paint)
    }

    /**
     * 협상 말풍선. 캐릭터 왼쪽 위 넓은 영역에 그린다(View 픽셀 좌표, dp 스케일은 여기서 직접 곱한다 —
     * 캐릭터 본체와 달리 이 영역은 [CHARACTER_SIZE_DP] 기준 스케일을 쓰지 않는다).
     *
     * @return 이번에 그린 버튼들의 히트 영역(View 픽셀 좌표)
     */
    private fun drawNegotiationBubble(canvas: Canvas, state: NegotiationState): Map<NegotiationAction, RectF> {
        val d = displayDensity
        val bubble = RectF(4f * d, 4f * d, width - 4f * d, (NEGOTIATION_HEIGHT_DP - 12f) * d)

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(30, 27, 75)
        canvas.drawRoundRect(bubble, 16f * d, 16f * d, paint)

        val message = when (state) {
            is NegotiationState.Offer -> context.getString(R.string.negotiation_offer, state.taskTitle)
            is NegotiationState.Counting -> context.getString(
                R.string.negotiation_counting,
                state.taskTitle,
                state.remainingSeconds / 60,
                state.remainingSeconds % 60,
            )
            is NegotiationState.Celebrate -> context.getString(R.string.negotiation_celebrate)
            NegotiationState.Hidden -> return emptyMap()
        }

        textPaint.color = Color.WHITE
        textPaint.textSize = 13f * d * resources.configuration.fontScale.coerceAtMost(1.3f)
        val textWidth = (bubble.width() - 24f * d).toInt().coerceAtLeast(1)
        val layout = StaticLayout.Builder
            .obtain(message, 0, message.length, textPaint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()
        canvas.withTranslation(bubble.left + 12f * d, bubble.top + 12f * d) {
            layout.draw(this)
        }
        val textBottom = bubble.top + 12f * d + layout.height

        // 주 동작(수락/더 할래)은 꽉 찬 버튼으로 강조한다. 보조 동작(거절/쉴래)은 버튼이 아니라
        // 작은 텍스트 링크로 낮춘다 — 거부할 수단 자체는 남기되(#64: "출구가 없으면 앱을 지운다"),
        // 화면에서 두 선택지가 똑같이 경쟁하지 않게 한다.
        val primary = when (state) {
            is NegotiationState.Offer -> NegotiationAction.ACCEPT to context.getString(R.string.negotiation_accept)
            is NegotiationState.Celebrate ->
                NegotiationAction.CONTINUE to context.getString(R.string.negotiation_continue)
            is NegotiationState.Counting, NegotiationState.Hidden -> null
        } ?: return emptyMap()
        val secondary = when {
            // 제안 직후에는 "해볼래"만 보인다. 유령을 한 번 더 탭해 rejectVisible이 되면 그제서야
            // "아니, 됐어"가 나온다(#63) — 처음부터 두 선택지를 같이 들이밀지 않는다.
            state is NegotiationState.Offer && state.rejectVisible ->
                NegotiationAction.REJECT to context.getString(R.string.negotiation_reject)
            state is NegotiationState.Celebrate ->
                NegotiationAction.REST to context.getString(R.string.negotiation_rest)
            else -> null
        }

        val hitRects = mutableMapOf<NegotiationAction, RectF>()

        val buttonTop = textBottom + 10f * d
        val buttonHeight = 36f * d
        val buttonRect = RectF(bubble.left + 8f * d, buttonTop, bubble.right - 8f * d, buttonTop + buttonHeight)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(129, 111, 217)
        canvas.drawRoundRect(buttonRect, 10f * d, 10f * d, paint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 13f * d
        textPaint.color = Color.WHITE
        val buttonMetrics = textPaint.fontMetrics
        canvas.drawText(
            primary.second,
            buttonRect.centerX(),
            buttonRect.centerY() - (buttonMetrics.ascent + buttonMetrics.descent) / 2f,
            textPaint,
        )
        hitRects[primary.first] = buttonRect

        if (secondary != null) {
            val linkTop = buttonRect.bottom + 6f * d
            textPaint.textSize = 11f * d
            // 배경(진한 남색)과는 구분되지만 주 버튼보다는 은은한 톤 — 눈에 덜 띄되 안 보이진 않는다.
            textPaint.color = Color.rgb(160, 155, 190)
            val linkMetrics = textPaint.fontMetrics
            canvas.drawText(secondary.second, bubble.centerX(), linkTop - linkMetrics.ascent, textPaint)
            // 글자는 작아도 누르기는 쉽게 — 히트 영역은 최소 32dp 높이를 보장한다.
            val textHeight = linkMetrics.descent - linkMetrics.ascent
            val hitHeight = maxOf(textHeight + 16f * d, 32f * d)
            hitRects[secondary.first] = RectF(bubble.left, linkTop - 8f * d, bubble.right, linkTop - 8f * d + hitHeight)
        }
        return hitRects
    }

    companion object {
        const val GREETING_HEIGHT_DP = 38f
        const val CHARACTER_SIZE_DP = 80f

        /**
         * 캐릭터 그림의 실제 세로 크기(dp). 가로(80)와 다르다 — 발 부분이 아래로 더 나온다
         * (`body` Path가 y=86까지 그린다). 협상 중 캐릭터를 오른쪽 아래에 고정할 때
         * [CHARACTER_SIZE_DP]를 세로에도 쓰면 발이 창 밖으로 잘린다.
         */
        const val CHARACTER_HEIGHT_DP = 88f

        /** 협상 중 창 너비(dp). 캐릭터(80dp)보다 넓게 잡아야 말풍선·버튼이 들어간다. */
        const val NEGOTIATION_WIDTH_DP = 240f

        /** 협상 중 캐릭터 위에 추가되는 높이(dp). 상태별로 버튼 유무가 달라 넉넉하게 고정폭을 쓴다. */
        const val NEGOTIATION_HEIGHT_DP = 128f
    }
}

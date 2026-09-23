package com.ghost.prototype.floating

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** `shared/states.md`의 `interventionLevel` → `characterState` 매핑을 고정한다. */
class CharacterAppearanceTest {

    @Test
    fun `레벨 0은 평상시다`() {
        assertEquals(CharacterState.IDLE, CharacterAppearance.forLevel(0).state)
    }

    @Test
    fun `레벨마다 다른 상태를 쓴다`() {
        assertEquals(CharacterState.ALERT, CharacterAppearance.forLevel(1).state)
        assertEquals(CharacterState.TALK, CharacterAppearance.forLevel(2).state)
        assertEquals(CharacterState.BLOCK, CharacterAppearance.forLevel(3).state)
    }

    @Test
    fun `레벨이 올라가면 커진다`() {
        val scales = (0..3).map { CharacterAppearance.forLevel(it).scale }
        assertEquals("오름차순이어야 눈에 띈다", scales.sorted(), scales)
        assertEquals("평상시는 원래 크기", 1.0f, scales[0], 0.001f)
    }

    @Test
    fun `범위를 벗어난 값도 안전하다`() {
        // 서버 세션(#14)에서 온 값이 어긋날 수 있다. 크래시 대신 가장 가까운 단계로 맞춘다.
        assertEquals(CharacterState.IDLE, CharacterAppearance.forLevel(-1).state)
        assertEquals(CharacterState.BLOCK, CharacterAppearance.forLevel(99).state)
    }

    @Test
    fun `같은 레벨은 같은 표현이다`() {
        // OverlayController 가 이 동등성으로 불필요한 다시 그리기를 막는다.
        assertEquals(CharacterAppearance.forLevel(1), CharacterAppearance.forLevel(1))
    }

    @Test
    fun `모든 레벨의 배율이 양수다`() {
        (0..3).forEach { assertTrue(CharacterAppearance.forLevel(it).scale > 0f) }
    }
}

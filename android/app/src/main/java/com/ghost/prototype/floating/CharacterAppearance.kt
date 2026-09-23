package com.ghost.prototype.floating

/**
 * `shared/states.md`의 캐릭터 상태 `characterState`. 이름을 그대로 쓴다.
 * Rive `.riv`(#16)의 상태머신 입력값도 같은 이름을 쓴다.
 */
enum class CharacterState { IDLE, ALERT, TALK, BLOCK }

/**
 * 개입 레벨에 따른 캐릭터 표현. Android 의존이 없어 단위 테스트로 검증한다
 * (`android/AGENTS.md`: Android 독립 로직은 순수 Kotlin으로 분리한다).
 *
 * @param scale 평상시 크기 대비 배율. 레벨이 올라가면 커져서 눈에 띈다.
 */
data class CharacterAppearance(
    val state: CharacterState,
    val scale: Float,
) {
    companion object {
        /**
         * `shared/states.md`의 `interventionLevel` → `characterState` 매핑.
         *
         * | 레벨 | 상태 | 의미 |
         * | --- | --- | --- |
         * | 0 | `IDLE` | 평상시 대기 |
         * | 1 | `ALERT` | 시선 끌기 |
         * | 2 | `TALK` | 말풍선으로 할 일 제시 (문구는 #63) |
         * | 3 | `BLOCK` | 화면 개입 (MVP 제외 — #22) |
         *
         * 범위를 벗어난 값은 가장 가까운 단계로 맞춘다. 상태머신이 0~3만 내보내지만
         * 서버 세션(#14)에서 온 값이 어긋날 수 있어 방어한다.
         */
        fun forLevel(level: Int): CharacterAppearance = when (level.coerceIn(0, 3)) {
            0 -> CharacterAppearance(CharacterState.IDLE, 1.0f)
            1 -> CharacterAppearance(CharacterState.ALERT, 1.15f)
            2 -> CharacterAppearance(CharacterState.TALK, 1.3f)
            else -> CharacterAppearance(CharacterState.BLOCK, 1.5f)
        }
    }
}

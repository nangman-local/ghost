package com.ghost.prototype.focus

/**
 * 개입 임계값. `shared/states.md`의 "개입 전이 규칙" 표와 같은 값이다.
 *
 * **이 값들은 임시값이다.** 예소팀 도그푸딩(2026-09-24~) 후 #10에서 조정한다.
 * 조정할 때는 `shared/states.md`도 같이 고친다. Android와 PC가 같은 값을 써야 한다.
 *
 * 하드코딩하지 않고 주입 가능한 형태로 두는 이유: 데모 모드(#65)가 같은 로직을 초 단위로 돌린다.
 */
data class InterventionPolicy(
    /** 앱·제목이 이 시간 이상 유지돼야 판정한다. `distract-rules.json`의 `minHoldSeconds`와 같다. */
    val minHoldSeconds: Int = 20,

    /** 딴짓 누적이 이 시간을 넘으면 `FOCUS → DISTRACT`. */
    val distractEntrySeconds: Int = 60,

    /** 레벨 1·2·3 승급에 필요한 딴짓 누적 시간. 오름차순이어야 한다. */
    val levelSeconds: List<Int> = listOf(10 * 60, 15 * 60, 20 * 60),

    /** 이 시간 이상 `FOCUS`가 유지되면 누적을 리셋한다(진짜 복귀로 본다). */
    val recoveryGraceSeconds: Int = 30,

    /**
     * 구현된 최대 개입 레벨. MVP는 2단계까지다(3단계 화면 가리기는 #22).
     * 이 값을 넘는 레벨로는 올라가지 않는다.
     */
    val maxLevel: Int = 2,
) {
    init {
        require(levelSeconds == levelSeconds.sorted()) { "levelSeconds는 오름차순이어야 한다: $levelSeconds" }
        require(maxLevel in 0..levelSeconds.size) { "maxLevel은 0..${levelSeconds.size} 범위여야 한다: $maxLevel" }
    }

    /** 누적 [seconds]에 해당하는 개입 레벨. [maxLevel]을 넘지 않는다. */
    fun levelFor(seconds: Int): Int = levelSeconds.count { seconds >= it }.coerceAtMost(maxLevel)

    companion object {
        /** 발표 시연용(#65). 같은 로직을 초 단위로 돌린다. */
        val Demo = InterventionPolicy(
            minHoldSeconds = 2,
            distractEntrySeconds = 5,
            levelSeconds = listOf(30, 60, 90),
            recoveryGraceSeconds = 5,
        )
    }
}

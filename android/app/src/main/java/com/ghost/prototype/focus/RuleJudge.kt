package com.ghost.prototype.focus

/**
 * 로컬 1차 규칙 판단. `shared/distract-rules.json`의 `android` 섹션을 따른다.
 *
 * **AI 없이 동작해야 한다.** 사전에 없는 앱은 `DISTRACT`가 아니라 [Judgement.AMBIGUOUS]다 —
 * 오탐이 미탐보다 훨씬 치명적이기 때문이다(루트 `AGENTS.md`).
 *
 * 서버 `/judge`(#12) 연동 전까지 이것만으로 판정한다.
 */
class RuleJudge(
    private val distract: Set<String>,
    private val focus: Set<String>,
    /** 사전에 명시적으로 AMBIGUOUS로 분류된 앱. 판정 결과는 미분류와 같지만 의도를 구분해 둔다. */
    private val ambiguous: Set<String> = emptySet(),
    /** 판정에서 제외할 패키지(런처·시스템UI·자기 자신). 관측 자체를 무시한다. */
    private val ignored: Set<String> = emptySet(),
) {
    /** [packageName]의 판정. 사전에 없으면 [Judgement.AMBIGUOUS]. */
    fun judge(packageName: String): Judgement? = when {
        packageName.isBlank() || packageName in ignored -> null
        packageName in distract -> Judgement.DISTRACT
        packageName in focus -> Judgement.FOCUS
        else -> Judgement.AMBIGUOUS // ambiguous 목록과 미분류를 같게 다룬다
    }

    companion object {
        /**
         * `shared/distract-rules.json`의 `android` 섹션 값.
         *
         * 지금은 상수로 둔다. 자산 파일을 읽어 오는 방식은 서버가 규칙을 내려주는 #12 때 같이 정한다.
         * **값을 고칠 때는 `shared/distract-rules.json`을 먼저 고치고 여기에 반영한다.**
         */
        fun fromSharedRules(ownPackage: String): RuleJudge = RuleJudge(
            distract = setOf(
                "com.instagram.android",
                "com.zhiliaoapp.musically",
                "com.twitter.android",
                "com.facebook.katana",
                "com.netflix.mediaclient",
                "com.nhn.android.webtoon",
                "com.kakao.page",
                "com.coupang.mobile",
                "com.towneers.www",
                "com.supercell.clashofclans",
                "com.riotgames.league.wildrift",
            ),
            focus = setOf(
                "com.google.android.apps.docs.editors.docs",
                "com.google.android.calendar",
                "com.google.android.keep",
                "com.google.android.apps.classroom",
                "com.notion.id",
                "com.microsoft.office.word",
                "com.adobe.reader",
            ),
            ambiguous = setOf(
                "com.android.chrome",
                "com.sec.android.app.sbrowser",
                "com.google.android.youtube",
                "com.google.android.gm",
                "com.kakao.talk",
                "com.discord",
                "com.Slack",
                "com.nhn.android.search",
            ),
            ignored = setOf(ownPackage),
        )
    }
}

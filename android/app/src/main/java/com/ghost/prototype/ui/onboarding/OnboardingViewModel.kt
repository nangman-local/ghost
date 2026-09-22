package com.ghost.prototype.ui.onboarding

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import com.ghost.prototype.GhostApplication
import com.ghost.prototype.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingSteps.WELCOME,
    val canGoBack: Boolean = false,
    /** 인디케이터 위치. 권한 없는 안내 단계는 -1(인디케이터를 숨긴다). */
    val indicatorIndex: Int = -1,
    val indicatorCount: Int = 0,
    val finished: Boolean = false,
    val error: String? = null,
)

/** 온보딩 완료 여부 등 UI 전용 로컬 플래그. 서버 세션과 무관하다. */
class OnboardingPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("ghost_ui", Context.MODE_PRIVATE)

    var done: Boolean
        get() = prefs.getBoolean("onboarding_done", false)
        set(value) = prefs.edit { putBoolean("onboarding_done", value) }

    /** 런타임 권한 팝업을 한 번이라도 띄웠는지. 거부가 굳었는지 판단할 때 쓴다. */
    fun wasAsked(permission: String): Boolean = prefs.getBoolean("asked:$permission", false)

    fun markAsked(permission: String) = prefs.edit { putBoolean("asked:$permission", true) }
}

class OnboardingViewModel(application: Application, private val saved: SavedStateHandle) :
    AndroidViewModel(application) {
    private val app = application as GhostApplication
    val prefs = OnboardingPrefs(application)
    private val flow = OnboardingFlow(OnboardingSteps.all, app.permissionStatus::isGranted, Build.VERSION.SDK_INT)
    private val mutableState = MutableStateFlow(stateFor(restoredStep()))
    val state: StateFlow<OnboardingUiState> = mutableState.asStateFlow()

    /** 안내 단계의 버튼(예: "Ghost 시작하기"): 다음 단계로 간다. */
    fun next() = moveTo(flow.next(state.value.step))

    /** "나중에": 권한 없이 다음 단계로 간다. */
    fun skip() = moveTo(flow.next(state.value.step))

    fun back() {
        flow.previous(state.value.step)?.let(::moveTo)
    }

    /** onResume·권한 팝업 결과 후 호출. 지금 단계가 허용됐으면 넘어간다. 재요청은 하지 않는다. */
    fun refresh() = moveTo(flow.afterResume(state.value.step))

    fun settingsUnavailable() {
        mutableState.update { it.copy(error = app.getString(R.string.error_settings)) }
    }

    private fun moveTo(step: OnboardingStep?) {
        if (step == null) {
            prefs.done = true
            mutableState.update { it.copy(finished = true) }
            return
        }
        saved[KEY_STEP] = step.key
        mutableState.value = stateFor(step)
    }

    private fun stateFor(step: OnboardingStep) = OnboardingUiState(
        step = step,
        canGoBack = flow.previous(step) != null,
        indicatorIndex = flow.indicatorIndex(step),
        indicatorCount = flow.indicatorCount,
    )

    private fun restoredStep() = flow.stepFor(saved.get<String>(KEY_STEP)) ?: flow.first

    private companion object {
        const val KEY_STEP = "onboarding_step"
    }
}

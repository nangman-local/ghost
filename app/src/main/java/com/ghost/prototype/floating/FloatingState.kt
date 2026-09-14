package com.ghost.prototype.floating

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class OverlayPosition(val x: Int, val y: Int)

data class FloatingState(
    val visible: Boolean = false,
    val position: OverlayPosition? = null,
    val greetingVisible: Boolean = false,
    val error: String? = null,
)

class FloatingStateStore {
    private val mutableState = MutableStateFlow(FloatingState())
    val state = mutableState.asStateFlow()

    fun shown(position: OverlayPosition) {
        mutableState.value = FloatingState(visible = true, position = position)
    }

    fun moved(position: OverlayPosition) {
        mutableState.update { if (it.visible) it.copy(position = position) else it }
    }

    fun greet() {
        mutableState.update { if (it.visible) it.copy(greetingVisible = true) else it }
    }

    fun stopped() {
        mutableState.update { FloatingState(error = it.error) }
    }

    fun failed(message: String) {
        mutableState.value = FloatingState(error = message)
    }

    fun reportError(message: String) {
        mutableState.update { it.copy(error = message) }
    }
}

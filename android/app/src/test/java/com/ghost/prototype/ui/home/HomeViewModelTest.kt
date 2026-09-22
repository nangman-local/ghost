package com.ghost.prototype.ui.home

import com.ghost.prototype.contract.FocusError
import com.ghost.prototype.fake.FakeFocusController
import com.ghost.prototype.fake.FakeTaskRepository
import com.ghost.prototype.floating.FloatingStateStore
import com.ghost.prototype.floating.OverlayPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val floating = FloatingStateStore()
    private var overlayGranted = true

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** Fake 두 개를 붙인 ViewModel. 상태를 계속 구독해 두어야 `state`가 갱신된다. */
    private fun TestScope.viewModel(): HomeViewModel {
        val focus = FakeFocusController(
            startFloating = { if (overlayGranted) floating.shown(OverlayPosition(0, 0)) else floating.failed("권한 없음") },
            stopFloating = { floating.stopped() },
            floating = floating.state,
            overlayGranted = { overlayGranted },
            scope = backgroundScope,
        )
        return HomeViewModel(FakeTaskRepository(), focus).also { vm ->
            backgroundScope.launch { vm.state.collect {} }
        }
    }

    @Test
    fun startsWithNoTaskAndKeepsInput() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onInputChange("기말")
        assertEquals(HomeContent.NoTask("기말"), vm.state.value.content)
    }

    @Test
    fun blankInputIsNotAdded() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onInputChange("   ")
        vm.addTask()
        assertTrue(vm.state.value.content is HomeContent.NoTask)
    }

    @Test
    fun addingTaskMakesItTodaysTask() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onInputChange("  기말 과제 마감 ")
        vm.addTask()
        val content = vm.state.value.content
        assertTrue(content is HomeContent.TaskReady)
        assertEquals("기말 과제 마감", (content as HomeContent.TaskReady).task.title)
        vm.openPicker()
        assertEquals("", vm.state.value.picker?.input)
    }

    @Test
    fun startThenStopReturnsToTodaysTask() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onInputChange("기말 과제 마감")
        vm.addTask()
        vm.startTask()
        assertTrue(vm.state.value.content is HomeContent.InProgress)
        vm.stopTask()
        assertTrue(vm.state.value.content is HomeContent.TaskReady)
    }

    @Test
    fun startWithoutOverlayShowsPermissionErrorWithoutRetry() = runTest(dispatcher) {
        overlayGranted = false
        val vm = viewModel()
        vm.onInputChange("기말 과제 마감")
        vm.addTask()
        vm.startTask()
        assertEquals(HomeError.Focus(FocusError.OVERLAY_PERMISSION_MISSING), vm.state.value.error)
    }

    @Test
    fun settingsUnavailableIsShownUntilNextStart() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onInputChange("기말 과제 마감")
        vm.addTask()
        vm.settingsUnavailable()
        assertEquals(HomeError.SettingsUnavailable, vm.state.value.error)
        vm.startTask()
        assertNull(vm.state.value.error)
    }

    @Test
    fun pickerSelectsAnotherTaskAndCloses() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onInputChange("자료구조 복습")
        vm.addTask()
        vm.onInputChange("기말 과제 마감")
        vm.addTask()
        vm.openPicker()
        val picker = vm.state.value.picker!!
        assertEquals(2, picker.tasks.size)
        val first = picker.tasks.first { it.title == "자료구조 복습" }
        vm.selectTask(first.id)
        assertNull(vm.state.value.picker)
        assertEquals(first, (vm.state.value.content as HomeContent.TaskReady).task)
    }

    @Test
    fun pickerAddMakesNewTaskCurrent() = runTest(dispatcher) {
        val vm = viewModel()
        vm.openPicker()
        vm.onPickerInputChange("새 할 일")
        vm.addTaskFromPicker()
        assertNull(vm.state.value.picker)
        assertEquals("새 할 일", (vm.state.value.content as HomeContent.TaskReady).task.title)
    }
}

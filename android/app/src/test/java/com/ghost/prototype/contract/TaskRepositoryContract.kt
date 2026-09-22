package com.ghost.prototype.contract

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 모든 [TaskRepository] 구현이 지켜야 할 동작.
 * 구현 담당(서버 담당)은 이 클래스를 상속한 테스트를 만들어 자기 구현으로 돌린다. 예: `FakeTaskRepositoryTest`.
 */
abstract class TaskRepositoryContract {
    /** 할 일이 하나도 없는 상태의 구현을 만든다. 네트워크는 가짜 서버로 채운다. */
    protected abstract fun createEmpty(): TaskRepository

    @Test
    fun emptyRepositoryHasNoCurrentTask() = runTest {
        val repository = createEmpty()
        assertTrue(repository.tasks.first().isEmpty())
        assertNull(repository.currentTask.first())
    }

    @Test
    fun addedTaskIsListedAndBecomesCurrent() = runTest {
        val repository = createEmpty()
        val added = repository.addTask("기말 과제 마감")
        assertEquals("기말 과제 마감", added.title)
        assertTrue(repository.tasks.first().any { it.id == added.id })
        assertEquals(added.id, repository.currentTask.first()?.id)
    }

    @Test
    fun selectTaskChangesCurrent() = runTest {
        val repository = createEmpty()
        val first = repository.addTask("자료구조 복습")
        repository.addTask("기말 과제 마감")
        repository.selectTask(first.id)
        assertEquals(first.id, repository.currentTask.first()?.id)
    }

    @Test
    fun todayStatsAreNeverNegative() = runTest {
        val stats = createEmpty().todayStats.first()
        assertTrue(stats.focusSeconds >= 0 && stats.distractSeconds >= 0)
    }
}

package com.ghost.prototype.fake

import com.ghost.prototype.contract.Task
import com.ghost.prototype.contract.TaskRepository
import com.ghost.prototype.contract.TaskRepositoryContract
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeTaskRepositoryTest : TaskRepositoryContract() {
    override fun createEmpty(): TaskRepository = FakeTaskRepository()

    @Test
    fun keepsAddOrder() = runTest {
        val repository = FakeTaskRepository(listOf(Task("a", "기존 할 일")))
        repository.addTask("기말 과제 마감")
        assertEquals(listOf("기존 할 일", "기말 과제 마감"), repository.tasks.value.map { it.title })
    }

    @Test
    fun selectingUnknownTaskKeepsCurrent() = runTest {
        val repository = FakeTaskRepository(listOf(Task("a", "할 일")))
        repository.selectTask("missing")
        assertEquals("a", repository.currentTask.first()?.id)
    }
}

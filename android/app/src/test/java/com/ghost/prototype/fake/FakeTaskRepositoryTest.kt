package com.ghost.prototype.fake

import com.ghost.prototype.contract.Task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FakeTaskRepositoryTest {
    @Test
    fun addedTaskBecomesCurrent() = runBlocking {
        val repository = FakeTaskRepository(listOf(Task("a", "기존 할 일")))

        val added = repository.addTask("기말 과제 마감")

        assertEquals(added, repository.currentTask.first())
        assertEquals(listOf("기존 할 일", "기말 과제 마감"), repository.tasks.value.map { it.title })
    }

    @Test
    fun selectingUnknownTaskKeepsCurrent() = runBlocking {
        val repository = FakeTaskRepository(listOf(Task("a", "할 일")))

        repository.selectTask("missing")

        assertEquals("a", repository.currentTask.first()?.id)
    }

    @Test
    fun emptyRepositoryHasNoCurrentTask() = runBlocking {
        assertNull(FakeTaskRepository().currentTask.first())
    }
}

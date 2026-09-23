package com.ghost.prototype.data

import com.ghost.prototype.contract.TaskRepository
import com.ghost.prototype.contract.TaskRepositoryContract
import com.ghost.prototype.contract.TaskSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 메모리에만 두는 가짜 저장소. 재시작은 같은 인스턴스로 새 Repository를 만들어 흉내 낸다. */
private class InMemoryStorage(var value: StoredTasks = StoredTasks()) : TaskStorage {
    override fun load(): StoredTasks = value
    override fun save(value: StoredTasks) { this.value = value }
}

class LocalTaskRepositoryTest : TaskRepositoryContract() {

    override fun createEmpty(): TaskRepository =
        LocalTaskRepository(InMemoryStorage()) { TODAY }

    @Test
    fun `앱을 다시 켜도 할 일이 남는다`() = runTest {
        val storage = InMemoryStorage()

        LocalTaskRepository(storage) { TODAY }.addTask("자료구조 과제")

        val restarted = LocalTaskRepository(storage) { TODAY }
        assertEquals(listOf("자료구조 과제"), restarted.tasks.first().map { it.title })
    }

    @Test
    fun `앱을 다시 켜도 현재 할 일이 유지된다`() = runTest {
        val storage = InMemoryStorage()
        val first = LocalTaskRepository(storage) { TODAY }
        first.addTask("첫 번째")
        val second = first.addTask("두 번째")

        val restarted = LocalTaskRepository(storage) { TODAY }
        assertEquals(second.id, restarted.currentTask.first()?.id)
    }

    @Test
    fun `날짜가 바뀌면 오늘 누적이 초기화된다`() = runTest {
        val storage = InMemoryStorage(
            StoredTasks(focusSeconds = 3600, distractSeconds = 1800, statsDate = TODAY),
        )

        val tomorrow = LocalTaskRepository(storage) { "2026-09-24" }

        val stats = tomorrow.todayStats.first()
        assertEquals(0, stats.focusSeconds)
        assertEquals(0, stats.distractSeconds)
    }

    @Test
    fun `날짜가 바뀌어도 할 일은 남는다`() = runTest {
        val storage = InMemoryStorage()
        LocalTaskRepository(storage) { TODAY }.addTask("어제 만든 할 일")

        val tomorrow = LocalTaskRepository(storage) { "2026-09-24" }

        assertEquals("누적만 버리고 할 일은 남긴다", 1, tomorrow.tasks.first().size)
    }

    @Test
    fun `같은 날이면 누적이 이어진다`() = runTest {
        val storage = InMemoryStorage(
            StoredTasks(focusSeconds = 3600, distractSeconds = 1800, statsDate = TODAY),
        )

        val stats = LocalTaskRepository(storage) { TODAY }.todayStats.first()

        assertEquals(3600, stats.focusSeconds)
        assertEquals(1800, stats.distractSeconds)
    }

    @Test
    fun `저장값이 깨져 있어도 앱이 열린다`() = runTest {
        // 없는 할 일을 현재 할 일로 가리키고, nextId가 목록보다 작다.
        val storage = InMemoryStorage(
            StoredTasks(
                tasks = listOf(localTask("local_0", "정상"), localTask("", "id 없음")),
                currentId = "사라진_할일",
                nextId = 0,
                statsDate = TODAY,
            ),
        )

        val repository = LocalTaskRepository(storage) { TODAY }

        assertEquals("빈 id는 버린다", 1, repository.tasks.first().size)
        assertNull("없는 현재 할 일은 비운다", repository.currentTask.first())

        // nextId가 0이었지만 local_0이 이미 있으므로 새 id가 부딪히면 안 된다.
        val added = repository.addTask("새 할 일")
        assertEquals("local_1", added.id)
    }

    @Test
    fun `로컬 할 일은 MANUAL 이다`() = runTest {
        val added = LocalTaskRepository(InMemoryStorage()) { TODAY }.addTask("직접 입력")
        assertEquals(TaskSource.MANUAL, added.source)
    }

    @Test
    fun `저장 형식 자체를 왕복한다`() {
        // PreferencesTaskStorage 는 Android 의존이라 단위 테스트에서 못 돌린다.
        // 대신 StoredTasks 의 날짜 정리 규칙만 여기서 고정한다.
        val stored = StoredTasks(
            tasks = listOf(localTask("local_0", "과제")),
            currentId = "local_0",
            nextId = 1,
            focusSeconds = 100,
            distractSeconds = 200,
            statsDate = TODAY,
        )

        assertEquals("같은 날이면 그대로", stored, stored.forDate(TODAY))

        val next = stored.forDate("2026-09-24")
        assertEquals("누적만 0", 0, next.focusSeconds)
        assertEquals(0, next.distractSeconds)
        assertEquals("할 일은 그대로", stored.tasks, next.tasks)
        assertEquals("현재 할 일도 그대로", stored.currentId, next.currentId)
    }

    private companion object { const val TODAY = "2026-09-23" }
}

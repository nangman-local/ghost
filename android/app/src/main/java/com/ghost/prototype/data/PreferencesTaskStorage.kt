package com.ghost.prototype.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.ghost.prototype.contract.Task
import com.ghost.prototype.contract.TaskSource
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * `SharedPreferences`에 JSON으로 저장한다(#53).
 *
 * 할 일 수가 수십 개 수준이라 DB를 쓰지 않는다. 서버 연동(#14) 때 이 구현만 바꾸면 된다.
 *
 * **앱 전용 저장소에만 쓴다.** 외부 저장소·공유 영역에 쓰지 않는다.
 */
class PreferencesTaskStorage(context: Context) : TaskStorage {

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override fun load(): StoredTasks {
        val raw = prefs.getString(KEY, null) ?: return StoredTasks()
        return try {
            val json = JSONObject(raw)
            StoredTasks(
                tasks = json.optJSONArray("tasks").toTasks(),
                currentId = json.optString("currentId").ifBlank { null },
                nextId = json.optInt("nextId", 0),
                focusSeconds = json.optLong("focusSeconds", 0),
                distractSeconds = json.optLong("distractSeconds", 0),
                statsDate = json.optString("statsDate"),
            )
        } catch (error: JSONException) {
            // 저장값이 깨졌다. 빈 상태로 시작한다 — 앱이 못 열리는 것보다 낫다.
            Log.w(TAG, "Stored tasks are unreadable; starting empty", error)
            StoredTasks()
        }
    }

    override fun save(value: StoredTasks) {
        val tasks = JSONArray()
        value.tasks.forEach { task ->
            tasks.put(
                JSONObject()
                    .put("id", task.id)
                    .put("title", task.title)
                    .put("source", task.source.name),
            )
        }
        val json = JSONObject()
            .put("tasks", tasks)
            .put("currentId", value.currentId ?: "")
            .put("nextId", value.nextId)
            .put("focusSeconds", value.focusSeconds)
            .put("distractSeconds", value.distractSeconds)
            .put("statsDate", value.statsDate)
        prefs.edit { putString(KEY, json.toString()) }
    }

    private fun JSONArray?.toTasks(): List<Task> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { index ->
            val item = optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("id")
            val title = item.optString("title")
            if (id.isBlank() || title.isBlank()) return@mapNotNull null
            Task(id, title, source(item.optString("source")))
        }
    }

    /** 모르는 값이 오면 `MANUAL`로 둔다. 앱 버전이 내려갔을 때 죽지 않게 한다. */
    private fun source(name: String): TaskSource =
        TaskSource.entries.firstOrNull { it.name == name } ?: TaskSource.MANUAL

    private companion object {
        const val FILE = "ghost_tasks"
        const val KEY = "tasks_v1"
        const val TAG = "GhostTaskStorage"
    }
}

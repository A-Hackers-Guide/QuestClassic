package io.github.questclassic.app.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-app notification bus. **Not** the Android system notifications —
 * those surface outside the app; this is the in-process feed the Home
 * screen bell icon and the Dev Super-Mode panel subscribe to.
 *
 * Any module can post:
 *   container.notifications.post(Notice(severity, title, body))
 *
 * Every notice carries a severity so the UI can tint it, and an
 * optional category so filters can hide classes the user doesn't want
 * (e.g. "AI chatter" vs "security events"). Notices are time-stamped
 * and capped at 64 — oldest drops first.
 *
 * Thread-safe via StateFlow; readers collect it as any other flow.
 */
class NotificationCenter(
    val capacity: Int = 64,
    private val now: () -> Long = System::currentTimeMillis,
) {
    init { require(capacity in 1..1024) }

    enum class Severity { INFO, WARN, ERROR }
    enum class Category { SYSTEM, AI, SECURITY, UPDATE, COMPANION, HAPTICS, OTHER }

    data class Notice(
        val id: String,
        val severity: Severity,
        val category: Category,
        val title: String,
        val body: String,
        val postedAtMs: Long,
        val read: Boolean = false,
    )

    private val _notices = MutableStateFlow<List<Notice>>(emptyList())
    val notices: StateFlow<List<Notice>> = _notices.asStateFlow()

    @Synchronized
    fun post(severity: Severity, category: Category, title: String, body: String = ""): Notice {
        val n = Notice(
            id = "n${now()}-${(_notices.value.size + 1)}",
            severity = severity,
            category = category,
            title = title,
            body = body,
            postedAtMs = now(),
        )
        val next = (_notices.value + n).takeLast(capacity)
        _notices.value = next
        return n
    }

    @Synchronized
    fun markAllRead() {
        _notices.value = _notices.value.map { it.copy(read = true) }
    }

    @Synchronized
    fun markRead(id: String) {
        _notices.value = _notices.value.map { if (it.id == id) it.copy(read = true) else it }
    }

    @Synchronized
    fun clear(category: Category? = null) {
        _notices.value = if (category == null) {
            emptyList()
        } else {
            _notices.value.filter { it.category != category }
        }
    }

    /** How many unread notices, optionally filtered by severity. */
    fun unreadCount(atLeast: Severity? = null): Int = notices.value.count {
        !it.read && (atLeast == null || it.severity.ordinal >= atLeast.ordinal)
    }
}

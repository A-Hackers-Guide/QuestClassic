package io.github.questclassic.app.core

/**
 * Compact 'time ago' formatter for the notifications panel, device
 * registry, update panel.
 *
 * - '<1m'  → 'just now'
 * - <60m   → 'Nm ago'
 * - <24h   → 'Nh ago'
 * - <30d   → 'Nd ago'
 * - older  → 'Nmo ago'  (months ≈ 30 days; months >12 → 'Ny ago').
 *
 * Future times (clock skew, injected clock) return 'soon'. Pure
 * function; injectable clock.
 */
class TimeAgo(private val now: () -> Long = System::currentTimeMillis) {

    fun format(eventMs: Long): String {
        val delta = now() - eventMs
        if (delta < 0) return "soon"
        val sec = delta / 1000
        return when {
            sec < 60 -> "just now"
            sec < 3600 -> "${sec / 60}m ago"
            sec < 86_400 -> "${sec / 3600}h ago"
            sec < 86_400L * 30 -> "${sec / 86_400}d ago"
            sec < 86_400L * 365 -> "${sec / (86_400L * 30)}mo ago"
            else -> "${sec / (86_400L * 365)}y ago"
        }
    }
}

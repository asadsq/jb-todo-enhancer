/*
 * Enum of TODO priority levels (P1, P2, P3, NONE) with a numeric rank used for sorting,
 * where NONE sorts last. The `fromToken` factory maps user-written tokens such as
 * `p1`, `high`, or `low` to the matching level.
 */
package com.github.asadsq.jbtodoenhancer.model

/**
 * Priority of a TODO, extracted from a `!p1` / `!high` style token.
 * [rank] is used for sorting (lower = more urgent); [NONE] sorts last.
 */
enum class TodoPriority(val label: String, val rank: Int) {
    P1("P1", 1),
    P2("P2", 2),
    P3("P3", 3),
    NONE("—", 99);

    companion object {
        /** Maps a bare priority token (already stripped of a leading `!`) to a [TodoPriority]. */
        fun fromToken(token: String): TodoPriority = when (token.trim().lowercase()) {
            "p1", "1", "high", "urgent", "critical" -> P1
            "p2", "2", "medium", "med", "normal" -> P2
            "p3", "3", "low", "minor" -> P3
            else -> NONE
        }
    }
}

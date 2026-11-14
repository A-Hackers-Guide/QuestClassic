package io.github.questclassic.app.ai

/**
 * 'Did you mean /help?' — suggests the closest known slash command
 * keyword when the user types one that [SlashCommands] didn't match.
 *
 * Uses a bounded Damerau-Levenshtein-ish distance with transpositions
 * so '/hleb' suggests '/help'. Max edit distance = 2 so a wildly
 * different word doesn't get matched against a 4-letter command.
 *
 * Pure function; no I/O.
 */
object FuzzyCommandMatcher {

    /** @return the closest command keyword (with leading '/') if any
     *  is within distance 2 of the input; null otherwise. */
    fun suggest(unknownKeyword: String): String? {
        val k = unknownKeyword.trim().removePrefix("/").lowercase()
        if (k.isEmpty()) return null
        var bestKw: String? = null
        var bestDist = Int.MAX_VALUE
        for (cmd in SlashCommands.Command.entries) {
            val d = distance(k, cmd.keyword)
            if (d < bestDist) {
                bestDist = d
                bestKw = cmd.keyword
            }
        }
        return if (bestKw != null && bestDist <= MAX_DISTANCE) "/$bestKw" else null
    }

    /** Damerau-Levenshtein with a max of 2. */
    private fun distance(a: String, b: String): Int {
        if (a == b) return 0
        val la = a.length
        val lb = b.length
        if (kotlin.math.abs(la - lb) > MAX_DISTANCE) return MAX_DISTANCE + 1
        val prev2 = IntArray(lb + 1)
        val prev = IntArray(lb + 1) { it }
        val curr = IntArray(lb + 1)
        for (i in 1..la) {
            curr[0] = i
            for (j in 1..lb) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(
                    curr[j - 1] + 1,
                    prev[j] + 1,
                    prev[j - 1] + cost,
                )
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    curr[j] = minOf(curr[j], prev2[j - 2] + cost)
                }
            }
            System.arraycopy(prev, 0, prev2, 0, prev.size)
            System.arraycopy(curr, 0, prev, 0, curr.size)
        }
        return prev[lb]
    }

    private const val MAX_DISTANCE = 2
}

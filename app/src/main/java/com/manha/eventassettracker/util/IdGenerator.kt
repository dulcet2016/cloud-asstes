package com.manha.eventassettracker.util

/** Derives a short readable prefix (2-6 letters) from an item's full name, e.g.
 *  "COB Light" -> "COB", "Backdrop Pot" -> "BFPOT". */
fun derivePrefix(name: String): String {
    val words = name.uppercase()
        .replace(Regex("[^A-Z0-9 ]"), " ")
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    if (words.isEmpty()) return ""
    val first = words[0]
    if (first.length in 2..6) return first
    if (first.length > 6) return first.take(6)
    // First word too short (e.g. "LED Wall") — combine initials of first letters instead.
    val initials = words.joinToString("") { it.take(1) }
    return if (initials.length in 2..6) initials else (first + initials).take(6).ifEmpty { "AST" }
}

/** Generates [count] new sequential, zero-padded IDs like PREFIX + 001, 002... starting right
 *  after [currentMax] (the highest existing numeric suffix already used for this prefix). */
fun nextSequentialIds(prefix: String, count: Int, currentMax: Int): List<String> {
    val safePrefix = prefix.ifBlank { "AST" }
    val start = currentMax + 1
    return (start until start + count).map { n ->
        safePrefix + n.toString().padStart(3, '0')
    }
}

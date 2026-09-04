package com.manha.eventassettracker.util

object ReportParser {

    private val NUMBERED_LINE = Regex(
        "^\\s*\\d+\\.\\s*(.+?)\\s*[—–-]\\s*([A-Z0-9]{2,10}\\d{2,})\\s*$",
        RegexOption.MULTILINE
    )
    private val BARE_ID = Regex("\\b([A-Z]{2,10}\\d{2,})\\b")

    data class ParsedLine(val name: String, val assetId: String)

    /** Extracts (name, assetId) pairs from a pasted report. Understands the app's own
     *  "1. Name — ASSETID" format, and falls back to picking out any bare Asset-ID-looking
     *  tokens (e.g. if someone forwarded a reformatted or partially-edited WhatsApp message). */
    fun parse(text: String): List<ParsedLine> {
        val results = mutableListOf<ParsedLine>()
        val matchedIds = mutableSetOf<String>()

        for (match in NUMBERED_LINE.findAll(text)) {
            val name = match.groupValues[1].trim()
            val id = match.groupValues[2].trim().uppercase()
            if (matchedIds.add(id)) results.add(ParsedLine(name, id))
        }

        // Fallback: any line not already captured above, but containing a bare ID token.
        for (line in text.lines()) {
            if (NUMBERED_LINE.containsMatchIn(line)) continue
            val idMatch = BARE_ID.find(line.uppercase()) ?: continue
            val id = idMatch.value
            if (matchedIds.add(id)) {
                val name = line.replace(idMatch.value, "").replace(Regex("[—–\\-.\\d]"), " ").trim()
                results.add(ParsedLine(name, id))
            }
        }
        return results
    }

    /** IDs present in [outText] but missing from [returnText] — i.e. still out with the event. */
    fun findMissing(outText: String, returnText: String): List<ParsedLine> {
        val outItems = parse(outText)
        val returnedIds = parse(returnText).map { it.assetId }.toSet()
        return outItems.filter { it.assetId !in returnedIds }
    }
}

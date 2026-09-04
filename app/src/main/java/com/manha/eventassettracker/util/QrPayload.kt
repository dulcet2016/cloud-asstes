package com.manha.eventassettracker.util

/** What we encode into every generated QR label, and what the scanner expects to read back. */
data class QrPayload(
    val assetId: String,
    val name: String,
    val category: String
) {
    fun encode(): String =
        "EAT1|${assetId}|${name.replace('|', ' ')}|${category.replace('|', ' ')}"

    companion object {
        private const val PREFIX = "EAT1|"

        /** Parses a scanned QR value. Falls back gracefully: if it's not our own format
         *  (e.g. an old plain "COB001" style label, or someone else's QR code), the raw
         *  text is still treated as a bare Asset ID so old/foreign labels keep working. */
        fun parse(raw: String): QrPayload {
            val text = raw.trim()
            if (text.startsWith(PREFIX)) {
                val parts = text.removePrefix(PREFIX).split("|")
                val assetId = parts.getOrNull(0)?.trim().orEmpty()
                val name = parts.getOrNull(1)?.trim().orEmpty()
                val category = parts.getOrNull(2)?.trim().orEmpty()
                if (assetId.isNotEmpty()) return QrPayload(assetId, name, category)
            }
            return QrPayload(assetId = text, name = "", category = "")
        }
    }
}

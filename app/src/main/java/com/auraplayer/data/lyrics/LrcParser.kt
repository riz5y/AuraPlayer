package com.auraplayer.data.lyrics

data class LyricLine(
    val timeMs: Long,
    val text: String
)

object LrcParser {
    private val lineRegex = Regex("""\[(\d{2}):(\d{2})(?:\.(\d{2,3}))?](.*)""")

    fun parse(lrcContent: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        lrcContent.lineSequence().forEach { line ->
            lineRegex.matchEntire(line.trim())?.let { match ->
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val msPart = match.groupValues[3]
                val ms = when (msPart.length) {
                    2 -> (msPart.toLongOrNull() ?: 0L) * 10
                    3 -> msPart.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val totalTime = min * 60000 + sec * 1000 + ms
                val text = match.groupValues[4].trim()
                if (text.isNotEmpty()) {
                    lines.add(LyricLine(totalTime, text))
                }
            }
        }
        return lines.sortedBy { it.timeMs }
    }
}

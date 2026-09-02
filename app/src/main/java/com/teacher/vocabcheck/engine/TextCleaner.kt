package com.teacher.vocabcheck.engine

/**
 * 把 OCR 出来的原始文本修成可判定的形态。课本拍照的噪声主要三类：
 * 行末断词连字符、全角/花体字符、以及页码和中文标点。
 */
object TextCleaner {

    private val LIGATURES = mapOf(
        "ﬁ" to "fi", "ﬂ" to "fl", "ﬃ" to "ffi",
        "ﬄ" to "ffl", "ﬅ" to "st", "ﬆ" to "st"
    )

    private val CHINESE_PUNCT = "，。、；：！？（）【】《》“”‘’…—～·".toSet()

    private val PAGE_FURNITURE = Regex(
        """^[\s]*([0-9]{1,4}|[一二三四五六七八九十]{1,4}\s*页|第\s*[0-9一二三四五六七八九十]+\s*页|Page\s*[0-9]+)[\s]*$""",
        RegexOption.IGNORE_CASE
    )

    private val HYPHEN_BREAK = Regex("""([A-Za-z])-{1,2}[ \t]*\n[ \t]*([A-Za-z])""")

    fun normalize(src: String): String {
        var t = src.replace("\r\n", "\n").replace('\r', '\n')
        t = t.replace("\u00AD", "").replace("\uFEFF", "").replace('\u00A0', ' ')
        for ((from, to) in LIGATURES) t = t.replace(from, to)
        t = t.replace('\u2019', '\'').replace('\u02BC', '\'')
            .replace('\u2018', '\'').replace('\u201C', '"').replace('\u201D', '"')
        t = toHalfWidth(t)
        t = t.map { if (it in CHINESE_PUNCT) ' ' else it }.joinToString("")
        var guard = 0
        while (HYPHEN_BREAK.containsMatchIn(t) && guard++ < 12) {
            t = HYPHEN_BREAK.replace(t) { m -> m.groupValues[1] + m.groupValues[2] }
        }
        return t.lines()
            .filterNot { PAGE_FURNITURE.matches(it) }
            .joinToString("\n")
            .trim('\n')
    }

    private fun toHalfWidth(t: String): String = buildString(t.length) {
        for (ch in t) {
            val c = ch.code
            append(if (c in 0xFF01..0xFF5E) (c - 0xFEE0).toChar() else if (c == 0x3000) ' ' else ch)
        }
    }
}

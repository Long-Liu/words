package com.teacher.vocabcheck.engine

/** 词表档位。一个词同时属于多档时，按声明顺序取靠前的那档着色。 */
enum class WordTier {
    /** 高考核心 688 */
    CORE_EXAM,

    /** 中考核心 688 */
    CORE_JUNIOR,

    /** 高考词汇基线 */
    BASE
}

data class WordEntry(
    val base: String,
    val pos: String,
    val meaning: String,
    val tiers: Set<WordTier>
)

/** tier 为 null 表示该词不在任何词表里，正文中不着色 */
data class Token(
    val surface: String,
    val start: Int,
    val end: Int,
    val lemma: String,
    val tier: WordTier?
)

data class HitWord(
    val lemma: String,
    val tier: WordTier,
    val count: Int,
    val surfaces: List<String>,
    val meaning: String
)

data class Report(
    val text: String,
    val tokens: List<Token>,
    val hits: List<HitWord>
) {
    val matched: Int get() = tokens.count { it.tier != null }

    fun countOf(tier: WordTier): Int = hits.count { it.tier == tier }
}

package com.teacher.vocabcheck.engine

/** 词表档位。声明顺序既是优先级（正文着色取靠前的那档），也是清单竖线的段序。 */
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

/** tier 为 null 表示该词不在任何词表里，正文中不着色；正文只能有一种颜色，故这里保留最高档单值 */
data class Token(
    val surface: String,
    val start: Int,
    val end: Int,
    val lemma: String,
    val tier: WordTier?
)

/** tiers 按 WordTier 声明顺序排列，首元素即正文着色所用的最高档，一个词属于几张表就有几项 */
data class HitWord(
    val lemma: String,
    val tiers: List<WordTier>,
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

    /** 各表独立计数：一个词同时属于几张表就在几格里各计一次，故三格之和可大于命中词数 */
    fun countOf(tier: WordTier): Int = hits.count { tier in it.tiers }
}

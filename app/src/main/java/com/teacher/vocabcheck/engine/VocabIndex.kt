package com.teacher.vocabcheck.engine

/**
 * 变形 -> 原形的倒排索引。
 *
 * 一个变形可以对应多个原形（wanted 同时来自 want），所以候选是列表而非单值：
 * 任一候选落在词表里即算命中，这样只会少报，不会把表里的词漏掉。
 */
class VocabIndex private constructor(
    private val entries: Map<String, WordEntry>,
    private val forms: Map<String, List<String>>
) {

    val baseWords: Set<String> get() = entries.keys

    fun candidates(form: String): List<String> = forms[form] ?: emptyList()

    fun tiers(base: String): Set<WordTier> = entries[base]?.tiers ?: emptySet()

    fun meaning(base: String): String = entries[base]?.meaning.orEmpty()

    fun lists(): List<Pair<WordTier, Int>> =
        WordTier.entries
            .map { tier -> tier to entries.values.count { tier in it.tiers } }
            .filter { it.second > 0 }

    companion object {
        fun build(words: List<WordEntry>): VocabIndex {
            val merged = LinkedHashMap<String, WordEntry>()
            for (entry in words) {
                val existing = merged[entry.base]
                merged[entry.base] = if (existing == null) entry else existing.copy(
                    tiers = existing.tiers + entry.tiers,
                    meaning = existing.meaning.ifEmpty { entry.meaning },
                    pos = existing.pos.ifEmpty { entry.pos }
                )
            }

            val index = HashMap<String, MutableList<String>>(merged.size * 5)
            for ((base, entry) in merged) {
                for (form in Inflection.forms(base, entry.pos)) {
                    val bucket = index.getOrPut(form) { ArrayList(2) }
                    if (base !in bucket) bucket += base
                }
            }
            return VocabIndex(merged, index)
        }
    }
}

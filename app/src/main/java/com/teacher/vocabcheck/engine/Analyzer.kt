package com.teacher.vocabcheck.engine

class Analyzer(private val index: VocabIndex) {

    fun analyze(raw: String): Report {
        val text = TextCleaner.normalize(raw)
        val segments = Tokenizer.split(text)
        val tokens = ArrayList<Token>(segments.size)
        val grouped = LinkedHashMap<String, MutableHit>()

        for (segment in segments) {
            val lemmas = segment.atoms.flatMap { index.candidates(it) }.distinct()
            val top = TIER_ORDER.firstOrNull { t -> lemmas.any { t in index.tiers(it) } }
            if (top == null) {
                tokens += Token(segment.surface, segment.start, segment.end, "", null)
                continue
            }
            val lemma = lemmas.first { top in index.tiers(it) }
            val tiers = TIER_ORDER.filter { it in index.tiers(lemma) }
            tokens += Token(segment.surface, segment.start, segment.end, lemma, top)
            val hit = grouped.getOrPut(lemma) { MutableHit(lemma, tiers) }
            hit.count++
            hit.surfaces += segment.surface
        }

        return Report(
            text = text,
            tokens = tokens,
            hits = grouped.values
                .map { HitWord(it.lemma, it.tiers, it.count, it.surfaces.distinctBy { s -> s.lowercase() }, index.meaning(it.lemma)) }
                .sortedWith(compareBy({ it.tiers.first().ordinal }, { -it.count }, { it.lemma }))
        )
    }

    private class MutableHit(val lemma: String, val tiers: List<WordTier>) {
        var count: Int = 0
        val surfaces = ArrayList<String>()
    }

    private companion object {
        /** 声明顺序即优先级：正文着色取靠前的那档，高考核心最需要被看到；清单竖线也按此顺序分段 */
        val TIER_ORDER = listOf(WordTier.CORE_EXAM, WordTier.CORE_JUNIOR, WordTier.BASE)
    }
}

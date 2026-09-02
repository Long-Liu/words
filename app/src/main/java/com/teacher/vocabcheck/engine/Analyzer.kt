package com.teacher.vocabcheck.engine

class Analyzer(private val index: VocabIndex) {

    fun analyze(raw: String): Report {
        val text = TextCleaner.normalize(raw)
        val segments = Tokenizer.split(text)
        val tokens = ArrayList<Token>(segments.size)
        val grouped = LinkedHashMap<String, MutableHit>()

        for (segment in segments) {
            val lemmas = segment.atoms.flatMap { index.candidates(it) }.distinct()
            val tier = TIER_ORDER.firstOrNull { t -> lemmas.any { t in index.tiers(it) } }
            if (tier == null) {
                tokens += Token(segment.surface, segment.start, segment.end, "", null)
                continue
            }
            val lemma = lemmas.first { tier in index.tiers(it) }
            tokens += Token(segment.surface, segment.start, segment.end, lemma, tier)
            val hit = grouped.getOrPut(lemma) { MutableHit(lemma, tier) }
            hit.count++
            hit.surfaces += segment.surface
        }

        return Report(
            text = text,
            tokens = tokens,
            hits = grouped.values
                .map { HitWord(it.lemma, it.tier, it.count, it.surfaces.distinctBy { s -> s.lowercase() }, index.meaning(it.lemma)) }
                .sortedWith(compareBy({ it.tier.ordinal }, { -it.count }, { it.lemma }))
        )
    }

    private class MutableHit(val lemma: String, val tier: WordTier) {
        var count: Int = 0
        val surfaces = ArrayList<String>()
    }

    private companion object {
        /** 同一词命中多档时取靠前的档位，高考核心最需要被看到 */
        val TIER_ORDER = listOf(WordTier.CORE_EXAM, WordTier.CORE_JUNIOR, WordTier.BASE)
    }
}

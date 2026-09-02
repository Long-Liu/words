package com.teacher.vocabcheck.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyzerTest {

    private val analyzer = Analyzer(
        VocabIndex.build(
            listOf(
                WordEntry("go", "v", "去", setOf(WordTier.CORE_EXAM)),
                WordEntry("child", "n", "孩子", setOf(WordTier.CORE_EXAM)),
                WordEntry("improve", "v", "改善", setOf(WordTier.CORE_EXAM)),
                WordEntry("study", "v", "学习", setOf(WordTier.CORE_JUNIOR)),
                WordEntry("park", "n", "公园", setOf(WordTier.CORE_JUNIOR)),
                WordEntry("volunteer", "n", "志愿者", setOf(WordTier.BASE)),
                WordEntry("water", "n", "水", setOf(WordTier.BASE)),
                WordEntry("the", "", "这", setOf(WordTier.BASE)),
                WordEntry("do", "v", "做", setOf(WordTier.BASE)),
                WordEntry("not", "", "不", setOf(WordTier.BASE)),
                WordEntry("look", "v", "看", setOf(WordTier.BASE)),
                WordEntry("good", "a", "好的", setOf(WordTier.BASE))
            )
        )
    )

    private fun tierOf(text: String, surface: String) =
        analyzer.analyze(text).tokens.first { it.surface == surface }.tier

    private fun hit(text: String, lemma: String) =
        analyzer.analyze(text).hits.first { it.lemma == lemma }

    @Test
    fun `规则屈折形式命中原形并带上所属档位`() {
        assertEquals(WordTier.CORE_EXAM, tierOf("The children went.", "children"))
        assertEquals(WordTier.CORE_JUNIOR, tierOf("She studies hard.", "studies"))
        assertEquals(WordTier.BASE, tierOf("Good.", "Good"))
    }

    @Test
    fun `不规则变化命中`() {
        assertEquals(WordTier.CORE_EXAM, tierOf("He went home.", "went"))
        assertEquals(WordTier.BASE, tierOf("Best.", "Best"))
    }

    @Test
    fun `不在任何词表里的词不参与标注`() {
        assertNull(tierOf("A zebra ran.", "zebra"))
        assertTrue(analyzer.analyze("A zebra ran.").hits.none { it.lemma == "zebra" })
    }

    @Test
    fun `同时属于多档时取靠前档位`() {
        val both = Analyzer(
            VocabIndex.build(
                listOf(
                    WordEntry("water", "n", "水", setOf(WordTier.BASE)),
                    WordEntry("water", "n", "水源", setOf(WordTier.CORE_JUNIOR))
                )
            )
        )
        assertEquals(WordTier.CORE_JUNIOR, both.analyze("The water.").tokens.first { it.surface == "water" }.tier)
    }

    @Test
    fun `同属三张词表时保留完整归属并各计一次`() {
        val report = Analyzer(
            VocabIndex.build(
                listOf(
                    WordEntry("water", "n", "水", setOf(WordTier.BASE)),
                    WordEntry("water", "n", "水", setOf(WordTier.CORE_JUNIOR)),
                    WordEntry("water", "n", "水源", setOf(WordTier.CORE_EXAM))
                )
            )
        ).analyze("The water.")
        assertEquals(
            listOf(WordTier.CORE_EXAM, WordTier.CORE_JUNIOR, WordTier.BASE),
            report.hits.first().tiers
        )
        assertEquals(WordTier.CORE_EXAM, report.tokens.first { it.surface == "water" }.tier)
        WordTier.entries.forEach { assertEquals(1, report.countOf(it)) }
    }

    @Test
    fun `缩写拆开后各部分独立判定`() {
        val token = analyzer.analyze("They don't study.").tokens.first { it.surface == "don't" }
        assertEquals(WordTier.BASE, token.tier)
    }

    @Test
    fun `所有格不产生多余词元`() {
        val report = analyzer.analyze("The children's parks.")
        assertEquals(WordTier.CORE_EXAM, report.tokens.first { it.surface == "children's" }.tier)
        assertEquals(WordTier.CORE_JUNIOR, report.tokens.first { it.surface == "parks" }.tier)
    }

    @Test
    fun `连字符复合词按两部分判定`() {
        assertEquals(WordTier.BASE, tierOf("A good-looking child.", "good-looking"))
    }

    @Test
    fun `拍照常见的断词与页码不影响判定`() {
        val report = analyzer.analyze("17\nThe chil-\ndren study.")
        assertEquals(WordTier.CORE_EXAM, report.tokens.first { it.surface == "children" }.tier)
        assertEquals(WordTier.CORE_JUNIOR, report.tokens.first { it.surface == "study" }.tier)
    }

    @Test
    fun `命中词按原形合并并统计出现次数`() {
        val hit = hit("Improve and improved and improve.", "improve")
        assertEquals(3, hit.count)
        assertEquals(listOf("Improve", "improved"), hit.surfaces)
        assertEquals("改善", hit.meaning)
    }

    @Test
    fun `清单先按档位再按出现次数排序`() {
        val tiers = analyzer.analyze("Water and parks and children and children.").hits.map { it.tiers.first() }
        assertEquals(listOf(WordTier.CORE_EXAM, WordTier.CORE_JUNIOR, WordTier.BASE), tiers)
    }

    @Test
    fun `统计给出处数与档位分布`() {
        val report = analyzer.analyze("The children study in the park.")
        assertEquals(5, report.matched)
        assertEquals(1, report.countOf(WordTier.CORE_EXAM))
        assertEquals(2, report.countOf(WordTier.CORE_JUNIOR))
        assertEquals(1, report.countOf(WordTier.BASE))
    }
}

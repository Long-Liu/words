package com.teacher.vocabcheck.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InflectionTest {

    private fun forms(word: String, pos: String = "") = Inflection.forms(word, pos)

    @Test
    fun `规则动词覆盖三单过去式和现在分词`() {
        val study = forms("study")
        assertTrue(study.containsAll(listOf("studies", "studied", "studying")))
    }

    @Test
    fun `双写与不写两种形态都产出`() {
        val stop = forms("stop")
        assertTrue(stop.containsAll(listOf("stops", "stopped", "stopping")))
    }

    @Test
    fun `以 e 结尾只加 d 或去 e 加 ing`() {
        val make = forms("make")
        assertTrue(make.containsAll(listOf("makes", "made", "making")))
        assertFalse("e 结尾不应再加完整 ed", make.contains("makeed"))
    }

    @Test
    fun `不规则动词走表而不靠规则`() {
        assertTrue(forms("go").containsAll(listOf("goes", "went", "gone", "going")))
        assertTrue(forms("be").containsAll(listOf("am", "is", "are", "was", "were", "been", "being")))
        assertTrue(forms("good", "a").containsAll(listOf("better", "best")))
        assertTrue(forms("child", "n").contains("children"))
    }

    @Test
    fun `f 和 fe 结尾变 ves`() {
        assertTrue(forms("leaf", "n").contains("leaves"))
        assertTrue(forms("knife", "n").contains("knives"))
    }

    @Test
    fun `比较级含 y 变 i`() {
        assertTrue(forms("happy", "a").containsAll(listOf("happier", "happiest")))
    }

    @Test
    fun `词性标记限制生成范围`() {
        val nounOnly = forms("book", "n")
        assertTrue(nounOnly.contains("books"))
        assertFalse("名词不应产出动词变形 booking", nounOnly.contains("booking"))
    }

    @Test
    fun `不标词性时三类全部生成`() {
        val loose = forms("answer")
        assertTrue(loose.containsAll(listOf("answers", "answered", "answering")))
    }

    @Test
    fun `已含空格的词组不做变形`() {
        assertTrue(forms("in advance").contains("in advance"))
    }
}

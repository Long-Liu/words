package com.teacher.vocabcheck.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextCleanerTest {

    @Test
    fun `行末断词连字符合并`() {
        val out = TextCleaner.normalize("The vo-\ncabulary list is long.")
        assertTrue(out.contains("vocabulary"))
        assertFalse(out.contains("vo-"))
    }

    @Test
    fun `全角字母和中文标点归一`() {
        val out = TextCleaner.normalize("Ｈｅｌｌｏ，ｗｏｒｌｄ！")
        assertTrue(out.contains("Hello"))
        assertFalse(out.any { it.code in 0xFF01..0xFF5E })
    }

    @Test
    fun `花体连字拆开`() {
        assertTrue(TextCleaner.normalize("ﬁnger").contains("finger"))
    }

    @Test
    fun `软连字符和零宽字符删除`() {
        assertEquals("vocab", TextCleaner.normalize("vo\u00ADcab").trim())
    }

    @Test
    fun `页码行丢弃但正文保留`() {
        val out = TextCleaner.normalize("17\nThis is the text.\n第 18 页")
        assertEquals("This is the text.", out.trim())
    }

    @Test
    fun `弯引号转直引号`() {
        assertTrue(TextCleaner.normalize("don’t").contains("don't"))
    }
}

package com.teacher.vocabcheck.data

import android.content.res.AssetManager
import com.teacher.vocabcheck.engine.VocabIndex
import com.teacher.vocabcheck.engine.WordEntry
import com.teacher.vocabcheck.engine.WordTier

data class VocabCatalog(
    val index: VocabIndex,
    val counts: Map<WordTier, Int>
)

/**
 * 词表就是 assets/vocab 下的纯文本，一行一词，格式 `word` 或 `word|词性|中文`。
 * 子目录即档位：core-exam 高考核心、core-junior 中考核心、base 高考基线。
 * 同一目录可放多个文件并取并集，改词表不需要改代码。
 */
class VocabRepository(private val assets: AssetManager) {

    fun load(): VocabCatalog {
        val entries = ArrayList<WordEntry>(8000)
        for ((tier, dir) in DIRS) {
            safeList(dir).forEach { entries += parseFile("$dir/$it", tier) }
        }
        val index = VocabIndex.build(entries)
        return VocabCatalog(index, index.lists().toMap())
    }

    private fun safeList(dir: String): List<String> =
        runCatching { assets.list(dir)?.toList().orEmpty() }
            .getOrDefault(emptyList())
            .filter { it.endsWith(".txt") }
            .sorted()

    private fun parseFile(path: String, tier: WordTier): List<WordEntry> =
        runCatching { assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readLines() } }
            .getOrDefault(emptyList())
            .mapNotNull { line ->
                val raw = line.trim().removePrefix("﻿").trim()
                if (raw.isEmpty() || raw.startsWith("#") || raw.startsWith("//")) return@mapNotNull null
                val cols = raw.split('|').map { it.trim() }
                val word = cols.firstOrNull()?.lowercase().orEmpty()
                if (word.isEmpty() || !word.all { it.isLetter() || it == '-' || it == '\'' || it == ' ' }) {
                    return@mapNotNull null
                }
                WordEntry(
                    base = word,
                    pos = cols.getOrNull(1).orEmpty(),
                    meaning = cols.getOrNull(2).orEmpty(),
                    tiers = setOf(tier)
                )
            }

    private companion object {
        val DIRS = linkedMapOf(
            WordTier.CORE_EXAM to "vocab/core-exam",
            WordTier.CORE_JUNIOR to "vocab/core-junior",
            WordTier.BASE to "vocab/base"
        )
    }
}

package com.teacher.vocabcheck.engine

/**
 * 把一个词条展开成它可能出现的所有屈折形式。
 *
 * 有意做"宽松生成"：双写与不双写两种形态都会产出。多余的伪形态不会命中任何真实词，
 * 而漏生成会把学过的词误报成超纲词——后者才是不可接受的。
 */
object Inflection {

    private const val CONSONANTS = "bcdfghjklmnpqrstvwxyz"
    private const val VOWELS = "aeiou"
    private val ES_ENDINGS = arrayOf("s", "ss", "sh", "ch", "x", "z", "o")

    private fun needsEs(w: String) = ES_ENDINGS.any { w.endsWith(it) }

    fun forms(wordRaw: String, pos: String = ""): Set<String> {
        val w = wordRaw.trim().lowercase()
        if (w.isEmpty() || w.contains(' ')) return setOf(w)
        if (w in Irregular.defective) return setOf(w)

        val out = LinkedHashSet<String>()
        out += w

        val tagged = pos.lowercase()
        val noPos = tagged.isEmpty()
        if (noPos || tagged.contains('n')) plural(w, out)
        if (noPos || tagged.contains('v')) verb(w, out)
        if (noPos || tagged.contains('a') || tagged.contains('d')) degree(w, out)

        Irregular.nouns[w]?.let { out += it }
        Irregular.verbs[w]?.let { out += it }
        Irregular.adjectives[w]?.let { out += it }
        return out
    }

    private fun plural(w: String, out: MutableSet<String>) {
        if (w.endsWith("fe")) out += w.dropLast(2) + "ves"
        if (w.endsWith("f") && !w.endsWith("ff")) out += w.dropLast(1) + "ves"
        out += when {
            w.endsWith("y") && consonantAt(w, w.length - 2) -> w.dropLast(1) + "ies"
            needsEs(w) -> w + "es"
            else -> w + "s"
        }
    }

    private fun verb(w: String, out: MutableSet<String>) {
        out += when {
            w.endsWith("ie") -> w.dropLast(2) + "ies"
            w.endsWith("y") && consonantAt(w, w.length - 2) -> w.dropLast(1) + "ies"
            needsEs(w) -> w + "es"
            else -> w + "s"
        }

        out += when {
            w.endsWith("e") -> w + "d"
            w.endsWith("y") && consonantAt(w, w.length - 2) -> w.dropLast(1) + "ied"
            else -> w + "ed"
        }

        out += when {
            w.endsWith("ie") -> w.dropLast(2) + "ying"
            w.endsWith("ee") || w.endsWith("oe") || w.endsWith("ye") -> w + "ing"
            w.endsWith("e") -> w.dropLast(1) + "ing"
            else -> w + "ing"
        }

        if (doublingCandidate(w)) {
            val d = w + w.last()
            out += d + "ed"
            out += d + "ing"
        }
    }

    private fun degree(w: String, out: MutableSet<String>) {
        when {
            w.endsWith("e") -> { out += w + "r"; out += w + "st" }
            w.endsWith("y") && consonantAt(w, w.length - 2) -> {
                out += w.dropLast(1) + "ier"
                out += w.dropLast(1) + "iest"
            }
            else -> { out += w + "er"; out += w + "est" }
        }
        if (doublingCandidate(w)) {
            val d = w + w.last()
            out += d + "er"
            out += d + "est"
        }
    }

    private fun consonantAt(w: String, i: Int) = i in w.indices && w[i] in CONSONANTS

    private fun doublingCandidate(w: String): Boolean {
        if (w in Irregular.MULTI_SYLLABLE_DOUBLE) return true
        val n = w.length
        if (n < 3) return false
        val last = w[n - 1]
        if (last !in CONSONANTS || last in "wxyh") return false
        if (w[n - 2] !in VOWELS) return false
        if (w[n - 3] !in CONSONANTS) return false
        return vowelGroups(w) <= 1 || w in Irregular.MULTI_SYLLABLE_DOUBLE
    }

    private fun vowelGroups(w: String): Int {
        var count = 0
        var inGroup = false
        for (ch in w) {
            val v = ch in VOWELS
            if (v && !inGroup) count++
            inGroup = v
        }
        return count
    }
}

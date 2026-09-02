package com.teacher.vocabcheck.engine

data class Segment(
    val surface: String,
    val start: Int,
    val end: Int,
    /** 拆分缩写和连字符后得到的待查词，全部小写 */
    val atoms: List<String>
)

object Tokenizer {

    private val WORD = Regex("[A-Za-z]+(?:['-](?=[A-Za-z])[A-Za-z]+)*")

    private val CONTRACTIONS = mapOf(
        "isn't" to listOf("is", "not"), "aren't" to listOf("are", "not"),
        "wasn't" to listOf("was", "not"), "weren't" to listOf("were", "not"),
        "don't" to listOf("do", "not"), "doesn't" to listOf("does", "not"),
        "didn't" to listOf("did", "not"), "can't" to listOf("can", "not"),
        "couldn't" to listOf("could", "not"), "shouldn't" to listOf("should", "not"),
        "wouldn't" to listOf("would", "not"), "won't" to listOf("will", "not"),
        "mustn't" to listOf("must", "not"), "needn't" to listOf("need", "not"),
        "haven't" to listOf("have", "not"), "hasn't" to listOf("has", "not"),
        "hadn't" to listOf("had", "not"), "ain't" to listOf("is", "not"),
        "i'm" to listOf("i", "am"), "you're" to listOf("you", "are"),
        "we're" to listOf("we", "are"), "they're" to listOf("they", "are"),
        "he's" to listOf("he", "is"), "she's" to listOf("she", "is"),
        "it's" to listOf("it", "is"), "that's" to listOf("that", "is"),
        "there's" to listOf("there", "is"), "what's" to listOf("what", "is"),
        "who's" to listOf("who", "is"), "here's" to listOf("here", "is"),
        "let's" to listOf("let", "us"), "i've" to listOf("i", "have"),
        "we've" to listOf("we", "have"), "you've" to listOf("you", "have"),
        "they've" to listOf("they", "have"), "i'd" to listOf("i", "would"),
        "you'd" to listOf("you", "would"), "he'd" to listOf("he", "would"),
        "she'd" to listOf("she", "would"), "we'd" to listOf("we", "would"),
        "they'd" to listOf("they", "would"), "i'll" to listOf("i", "will"),
        "you'll" to listOf("you", "will"), "he'll" to listOf("he", "will"),
        "she'll" to listOf("she", "will"), "we'll" to listOf("we", "will"),
        "they'll" to listOf("they", "will"), "o'clock" to listOf("clock")
    )

    private val KEEP_SINGLE = setOf("i", "a")

    fun split(text: String): List<Segment> {
        val out = ArrayList<Segment>()
        for (m in WORD.findAll(text)) {
            val surface = m.value
            out += Segment(
                surface = surface,
                start = m.range.first,
                end = m.range.last + 1,
                atoms = atomsOf(surface)
            )
        }
        return out
    }

    private fun atomsOf(surface: String): List<String> {
        val full = surface.lowercase()
        CONTRACTIONS[full]?.let { return it }
        val atoms = ArrayList<String>(4)
        for (part in full.split('-', '\'')) {
            if (part.isNotEmpty()) atoms += expandPart(part)
        }
        return atoms.filter { it.length > 1 || it in KEEP_SINGLE }.distinct()
    }

    private fun expandPart(part: String): List<String> {
        CONTRACTIONS[part]?.let { return it }
        val contraction = Regex("""(.+)n't""").matchEntire(part)
        if (contraction != null) return listOf(contraction.groupValues[1], "not")
        return listOf(part)
    }
}

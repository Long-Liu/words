package com.teacher.vocabcheck.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.teacher.vocabcheck.data.VocabCatalog
import com.teacher.vocabcheck.data.VocabRepository
import com.teacher.vocabcheck.engine.Analyzer
import com.teacher.vocabcheck.engine.Report
import com.teacher.vocabcheck.engine.WordTier
import com.teacher.vocabcheck.ocr.TextScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ViewState(
    val input: String = "",
    val report: Report? = null,
    val counts: Map<WordTier, Int> = emptyMap(),
    val busy: Boolean = false,
    val status: String? = null,
    val error: String? = null
)

class VocabViewModel(app: Application) : AndroidViewModel(app) {

    private val scanner = TextScanner()
    private val _state = MutableStateFlow(ViewState())
    val state: StateFlow<ViewState> = _state

    private var catalog: VocabCatalog? = null
    private var generation = 0
    private var clearedSnapshot: String? = null

    init {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                runCatching { VocabRepository(getApplication<Application>().assets).load() }
            }
            loaded.onSuccess {
                catalog = it
                _state.update { s -> s.copy(counts = it.counts, status = "已加载 ${it.index.baseWords.size} 个词条") }
                reanalyze()
            }.onFailure {
                _state.update { s -> s.copy(error = "词表加载失败：${it.message}") }
            }
        }
    }

    fun onInput(text: String) {
        _state.update { it.copy(input = text) }
        reanalyze()
    }

    fun clear() {
        clearedSnapshot = _state.value.input
        generation++
        _state.update { it.copy(input = "", report = null, error = null, status = null) }
    }

    fun undoClear() {
        val restored = clearedSnapshot ?: return
        clearedSnapshot = null
        _state.update { it.copy(input = restored) }
        reanalyze()
    }

    fun onBitmap(bitmap: Bitmap?) {
        if (bitmap == null) return
        _state.update { it.copy(busy = true, error = null, status = "正在识别文字…") }
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.Default) { scanner.recognize(bitmap) } }
                .onSuccess { text ->
                    _state.update { it.copy(busy = false, status = "识别完成，共 ${text.length} 字符") }
                    onInput(text)
                }
                .onFailure { e ->
                    _state.update { it.copy(busy = false, error = e.message ?: "识别失败") }
                }
        }
    }

    fun exportText(): String {
        val report = _state.value.report ?: return _state.value.input
        return buildString {
            appendLine("共 ${report.matched} 处用词命中核心词表")
            for (tier in WordTier.entries) {
                val words = report.hits.filter { it.tier == tier }
                if (words.isEmpty()) continue
                appendLine()
                appendLine("【${tier.label()}】${words.size} 个")
                for (word in words) {
                    append("  ${word.lemma}")
                    if (word.meaning.isNotEmpty()) append("　${word.meaning}")
                    appendLine("　×${word.count}")
                }
            }
            appendLine()
            appendLine("== 原文 ==")
            appendLine(report.text)
        }
    }

    fun exportHits(tier: WordTier): String =
        _state.value.report?.hits?.filter { it.tier == tier }?.joinToString("\n") { it.lemma }.orEmpty()

    private fun reanalyze() {
        val loaded = catalog ?: return
        val input = _state.value.input
        val ticket = ++generation
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) { Analyzer(loaded.index).analyze(input) }
            if (ticket == generation) _state.update { it.copy(report = result) }
        }
    }
}

fun bitmapFrom(context: Context, uri: Uri): Bitmap? = runCatching {
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
}.getOrNull()

package com.teacher.vocabcheck.ocr

import android.graphics.Bitmap
import android.graphics.Matrix
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OcrException(message: String, cause: Throwable?) : Exception(message, cause)

class TextScanner {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(source: Bitmap): String {
        val bitmap = downscale(source)
        return suspendCancellableCoroutine { cont ->
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { text -> cont.resume(joinLines(text)) }
                .addOnFailureListener { e ->
                    cont.resumeWithException(OcrException("文字识别失败：${e.message}", e))
                }
        }
    }

    /**
     * ML Kit 按像素量计时，课本照片动辄 4000px 宽，缩到 1600 长边能把识别耗时砍掉大半，
     * 印刷体准确率不受影响。
     */
    private fun downscale(src: Bitmap): Bitmap {
        val longest = maxOf(src.width, src.height)
        if (longest <= MAX_EDGE) return src
        val ratio = MAX_EDGE.toFloat() / longest
        val matrix = Matrix().apply { postScale(ratio, ratio) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    private fun joinLines(text: com.google.mlkit.vision.text.Text): String =
        text.textBlocks.joinToString("\n") { block ->
            block.lines.joinToString("\n") { it.text.trim() }
        }

    private companion object {
        const val MAX_EDGE = 1600
    }
}

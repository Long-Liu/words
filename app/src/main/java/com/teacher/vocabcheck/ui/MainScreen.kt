package com.teacher.vocabcheck.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teacher.vocabcheck.R
import com.teacher.vocabcheck.engine.HitWord
import com.teacher.vocabcheck.engine.Report
import com.teacher.vocabcheck.engine.WordTier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

private val CardShape = RoundedCornerShape(22.dp)
private val ChipShape = RoundedCornerShape(18.dp)
private val ChipPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)

@Composable
fun MainScreen(
    shortcut: MutableStateFlow<String?> = MutableStateFlow(null),
    vm: VocabViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val action by shortcut.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val pending = remember { arrayOfNulls<File>(1) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val file = pending[0]
        if (ok && file != null) vm.onBitmap(BitmapFactory.decodeFile(file.absolutePath))
        pending[0] = null
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.onBitmap(bitmapFrom(context, uri))
    }

    LaunchedEffect(action) {
        when (action) {
            "camera" -> launchCamera(context, pending, takePicture)
            "paste" -> {
                val text = clipboardText(context)
                if (text.isNullOrBlank()) Toast.makeText(context, "剪贴板里没有文本", Toast.LENGTH_SHORT).show()
                else vm.onInput(text)
            }
        }
        if (action != null) shortcut.value = null
    }

    var undoAt by remember { mutableStateOf(0L) }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { UndoBar(undoAt, onUndo = vm::undoClear, onExpire = { undoAt = 0L }) }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(inner),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("高频词检测", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "标出课文里命中核心词表的词，颜色区分档位",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = state.input,
                onValueChange = vm::onInput,
                modifier = Modifier.fillMaxWidth().heightIn(min = 132.dp),
                placeholder = { Text("粘贴英文段落，或用下方按钮拍课本") },
                shape = CardShape,
                maxLines = 12,
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { launchCamera(context, pending, takePicture) },
                        enabled = !state.busy,
                        shape = ChipShape,
                        contentPadding = ChipPadding,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1.5f).height(52.dp)
                    ) {
                        if (state.busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("识别中…", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        } else {
                            ActionLabel(R.drawable.ic_sc_camera, "拍照识别", iconSize = 18.dp)
                        }
                    }
                    FilledTonalButton(
                        onClick = {
                            pickPhoto.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !state.busy,
                        shape = ChipShape,
                        contentPadding = ChipPadding,
                        modifier = Modifier.weight(1.5f).height(52.dp)
                    ) {
                        ActionLabel(R.drawable.ic_sc_image, "相册选图", iconSize = 18.dp)
                    }
                    TextButton(
                        onClick = {
                            val hadText = state.input.isNotBlank()
                            vm.clear()
                            if (hadText) undoAt = System.nanoTime()
                        },
                        enabled = !state.busy,
                        shape = ChipShape,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = ChipPadding,
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        ActionLabel(R.drawable.ic_sc_clear, "清空", iconSize = 18.dp)
                    }
                }
                state.error?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            val report = state.report
            if (report != null && report.hits.isNotEmpty()) {
                StatRow(report)
                AnnotatedCard(report)
                HitsCard(report.hits)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { copyToClipboard(context, vm.exportText()) },
                        shape = ChipShape,
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) { Text("复制完整报告") }
                    OutlinedButton(
                        onClick = { copyToClipboard(context, report.hits.joinToString("\n") { it.lemma }) },
                        shape = ChipShape,
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) { Text("只复制命中词") }
                }
            } else if (state.input.isBlank()) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = CardShape) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("怎么用", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Text(
                            "拍一页课本或粘贴一段文字，命中核心词表的词会按档位上色：红色高考核心、蓝色中考核心、绿色高考基线，其余保持原样。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun StatRow(report: Report) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = CardShape) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WordTier.entries.forEach { tier ->
                    Stat("${report.countOf(tier)}", tier.shortLabel(), tierColor(tier), Modifier.weight(1f))
                }
            }
            Text(
                "命中 ${report.matched} 处　全文 ${report.tokens.size} 个词",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "各表独立统计，一词同属多表则重复计入",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Stat(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AnnotatedCard(report: Report) {
    OutlinedCard {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("原文标注", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(annotated(report), style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                WordTier.entries.forEach { tier -> Dot(tier.shortLabel(), tierColor(tier)) }
                Dot("未命中", MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun HitsCard(hits: List<HitWord>) {
    OutlinedCard {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("命中词清单", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            hits.forEachIndexed { i, hit ->
                if (i > 0) HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceContainer)
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Max).padding(top = if (i == 0) 0.dp else 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    TierBar(hit.tiers)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            hit.lemma,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = tierColor(hit.tiers.first())
                        )
                        val variants = hit.surfaces.filter { it.lowercase() != hit.lemma }.joinToString(" / ")
                        val detail = listOf(variants, hit.meaning).filter { it.isNotEmpty() }.joinToString("　")
                        if (detail.isNotEmpty()) {
                            Text(
                                detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        "出现 ${hit.count}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceContainer)
            Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WordTier.entries.forEach { Dot(it.shortLabel(), tierColor(it)) }
            }
            Text(
                "竖线自上而下按上述顺序分段，一段代表所属的一张词表",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 行首归属竖线：一个词属于几张词表就分几段，段序与 WordTier 声明顺序一致 */
@Composable
private fun TierBar(tiers: List<WordTier>) {
    Column(
        Modifier.fillMaxHeight().width(4.dp).clip(RoundedCornerShape(2.dp)),
        verticalArrangement = Arrangement.spacedBy(1.5.dp)
    ) {
        tiers.forEach { Box(Modifier.fillMaxWidth().weight(1f).background(tierColor(it))) }
    }
}

@Composable
private fun OutlinedCard(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, CardShape)
    ) { content() }
}

@Composable
private fun ActionLabel(
    iconRes: Int,
    text: String,
    iconSize: Dp = 18.dp,
    style: TextStyle = MaterialTheme.typography.labelLarge,
    tint: Color = Color.Unspecified
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(iconRes), null, Modifier.size(iconSize), tint = tint)
        Text(text, style = style, maxLines = 1)
    }
}

@Composable
private fun Dot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * 不用 SnackbarHost：它在 Scaffold 槽位里既不超时收起也划不走（小米真机实测），只能自己控制显隐。
 * undoAt 为 0 表示不显示，每次清空盖一个新时间戳来重起计时。
 */
@Composable
private fun UndoBar(undoAt: Long, onUndo: () -> Unit, onExpire: () -> Unit) {
    if (undoAt == 0L) return
    LaunchedEffect(undoAt) {
        delay(4000)
        onExpire()
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Snackbar(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 14.dp),
            action = {
                TextButton(onClick = { onUndo(); onExpire() }) { Text("撤销") }
            }
        ) {
            Text("已清空")
        }
    }
}

private fun annotated(report: Report): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    for (token in report.tokens) {
        if (token.start < cursor || token.end > report.text.length) continue
        if (token.start > cursor) append(report.text.substring(cursor, token.start))
        val tier = token.tier
        if (tier == null) append(token.surface)
        else withStyle(SpanStyle(color = tierColor(tier), fontWeight = FontWeight.SemiBold)) { append(token.surface) }
        cursor = token.end
    }
    if (cursor < report.text.length) append(report.text.substring(cursor))
}

private fun launchCamera(context: Context, holder: Array<File?>, launcher: ActivityResultLauncher<Uri>) {
    val file = File(context.cacheDir, "shot_${System.currentTimeMillis()}.jpg")
    runCatching { file.createNewFile() }
    holder[0] = file
    launcher.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
}

private fun clipboardManager(context: Context) =
    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

private fun clipboardText(context: Context): String? =
    clipboardManager(context).primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()

private fun copyToClipboard(context: Context, text: String) {
    clipboardManager(context).setPrimaryClip(ClipData.newPlainText("vocab", text))
}

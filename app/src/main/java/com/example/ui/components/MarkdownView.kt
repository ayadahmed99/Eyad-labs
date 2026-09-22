package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text as CommonMarkText
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser

enum class TableCellAlignment {
    START, CENTER, END
}

sealed class MarkdownElement {
    data class Header(
        val level: Int,
        val text: String,
        val rawNode: Heading? = null
    ) : MarkdownElement()

    data class Paragraph(
        val text: String,
        val rawNode: org.commonmark.node.Paragraph? = null
    ) : MarkdownElement()

    data class CodeBlock(
        val language: String,
        val code: String
    ) : MarkdownElement()

    data class BulletItem(
        val text: String,
        val level: Int = 0,
        val rawNode: ListItem? = null
    ) : MarkdownElement()

    data class NumberedItem(
        val number: String,
        val text: String,
        val level: Int = 0,
        val rawNode: ListItem? = null
    ) : MarkdownElement()

    data class Blockquote(
        val text: String,
        val rawNode: BlockQuote? = null
    ) : MarkdownElement()

    data class Table(
        val headers: List<String>,
        val rows: List<List<String>>,
        val alignments: List<TableCellAlignment> = emptyList()
    ) : MarkdownElement()

    data object ThematicBreak : MarkdownElement()
}

private val commonMarkParser: Parser by lazy {
    Parser.builder()
        .extensions(
            listOf(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                AutolinkExtension.create()
            )
        )
        .build()
}

@Composable
fun MarkdownView(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val elements = remember(markdown) { parseMarkdown(markdown) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (elem in elements) {
            when (elem) {
                is MarkdownElement.Header -> {
                    MarkdownHeader(elem, textColor)
                }
                is MarkdownElement.CodeBlock -> {
                    CodeBlockView(language = elem.language, code = elem.code)
                }
                is MarkdownElement.BulletItem -> {
                    MarkdownBulletItem(elem, textColor)
                }
                is MarkdownElement.NumberedItem -> {
                    MarkdownNumberedItem(elem, textColor)
                }
                is MarkdownElement.Blockquote -> {
                    MarkdownBlockquote(elem, textColor)
                }
                is MarkdownElement.Table -> {
                    MarkdownTableView(elem)
                }
                is MarkdownElement.Paragraph -> {
                    MarkdownParagraph(elem, textColor)
                }
                is MarkdownElement.ThematicBreak -> {
                    MarkdownThematicBreak()
                }
            }
        }
    }
}

@Composable
private fun MarkdownHeader(header: MarkdownElement.Header, textColor: Color) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val (style, topPadding) = when (header.level) {
        1 -> MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            fontSize = 21.sp,
            lineHeight = 27.sp
        ) to 10.dp
        2 -> MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = textColor,
            fontSize = 18.sp,
            lineHeight = 24.sp
        ) to 8.dp
        3 -> MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            fontSize = 16.sp,
            lineHeight = 22.sp
        ) to 6.dp
        else -> MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ) to 4.dp
    }

    val annotated = remember(header, textColor, primaryColor) {
        header.rawNode?.let { node ->
            buildAnnotatedFromNode(node, textColor, primaryColor)
        } ?: buildAnnotatedFromText(header.text, textColor, primaryColor)
    }

    Column(modifier = Modifier.padding(top = topPadding, bottom = 2.dp)) {
        Text(
            text = annotated,
            style = style,
            modifier = Modifier.fillMaxWidth()
        )
        if (header.level == 1) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width(40.dp)
                    .height(3.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@Composable
private fun MarkdownParagraph(paragraph: MarkdownElement.Paragraph, textColor: Color) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val annotated = remember(paragraph, textColor, primaryColor) {
        paragraph.rawNode?.let { node ->
            buildAnnotatedFromNode(node, textColor, primaryColor)
        } ?: buildAnnotatedFromText(paragraph.text, textColor, primaryColor)
    }

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium.copy(
            lineHeight = 23.sp,
            color = textColor
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun MarkdownBulletItem(item: MarkdownElement.BulletItem, textColor: Color) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val annotated = remember(item, textColor, primaryColor) {
        item.rawNode?.let { node ->
            buildAnnotatedFromNode(node, textColor, primaryColor)
        } ?: buildAnnotatedFromText(item.text, textColor, primaryColor)
    }

    val indent = (item.level * 16).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp, end = 8.dp, start = 4.dp)
                .size(if (item.level == 0) 6.dp else 5.dp)
                .background(
                    color = if (item.level == 0) primaryColor else primaryColor.copy(alpha = 0.7f),
                    shape = CircleShape
                )
        )
        Text(
            text = annotated,
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 22.sp,
                color = textColor
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MarkdownNumberedItem(item: MarkdownElement.NumberedItem, textColor: Color) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val annotated = remember(item, textColor, primaryColor) {
        item.rawNode?.let { node ->
            buildAnnotatedFromNode(node, textColor, primaryColor)
        } ?: buildAnnotatedFromText(item.text, textColor, primaryColor)
    }

    val indent = (item.level * 16).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "${item.number}.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = primaryColor
            ),
            modifier = Modifier.padding(end = 8.dp, top = 1.dp)
        )
        Text(
            text = annotated,
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 22.sp,
                color = textColor
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MarkdownBlockquote(quote: MarkdownElement.Blockquote, textColor: Color) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val annotated = remember(quote, textColor, primaryColor) {
        quote.rawNode?.let { node ->
            buildAnnotatedFromNode(node, textColor, primaryColor)
        } ?: buildAnnotatedFromText(quote.text, textColor, primaryColor)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(32.dp)
                .background(primaryColor, shape = RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = annotated,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontStyle = FontStyle.Italic,
                color = textColor.copy(alpha = 0.95f),
                lineHeight = 22.sp
            )
        )
    }
}

@Composable
private fun MarkdownThematicBreak() {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
fun CodeBlockView(language: String, code: String) {
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val displayLang = language.ifBlank { "CODE" }.uppercase()

    val highlightedCode = remember(code, language) {
        buildHighlightedCode(code)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF141622))
            .border(1.dp, Color(0xFF2B3045), RoundedCornerShape(12.dp))
    ) {
        // Top action bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1B1E2E))
                .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF282C40))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = displayLang,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA78BFA)
                    )
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("code", code)
                        clipboard.setPrimaryClip(clip)
                        isCopied = true
                        Toast.makeText(context, "تم نسخ الكود البرمجي", Toast.LENGTH_SHORT).show()
                        scope.launch {
                            delay(2000)
                            isCopied = false
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = "نسخ الكود",
                    modifier = Modifier.size(15.dp),
                    tint = if (isCopied) Color(0xFFA6E3A1) else Color(0xFFA6ADC8)
                )
                Text(
                    text = if (isCopied) "تم النسخ" else "نسخ",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isCopied) Color(0xFFA6E3A1) else Color(0xFFA6ADC8)
                    )
                )
            }
        }

        // Code content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            Text(
                text = highlightedCode,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            )
        }
    }
}

@Composable
private fun MarkdownTableView(table: MarkdownElement.Table) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Table top title / badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.TableChart,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = primaryColor
            )
            Text(
                text = "جدول بيانات",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Text(
                text = "(${table.rows.size} صفوف)",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            )
        }

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

        // Scrollable Table Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Column {
                // Table Header Row
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    table.headers.forEachIndexed { colIndex, headerText ->
                        val alignment = table.alignments.getOrNull(colIndex) ?: TableCellAlignment.START
                        val textAlign = when (alignment) {
                            TableCellAlignment.START -> TextAlign.Start
                            TableCellAlignment.CENTER -> TextAlign.Center
                            TableCellAlignment.END -> TextAlign.End
                        }

                        val annotatedHeader = remember(headerText) {
                            buildAnnotatedFromText(headerText, onSurface, primaryColor)
                        }

                        Text(
                            text = annotatedHeader,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = textAlign
                            ),
                            modifier = Modifier
                                .widthIn(min = 120.dp, max = 260.dp)
                                .padding(horizontal = 8.dp)
                        )
                    }
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Table Data Rows
                table.rows.forEachIndexed { rowIndex, row ->
                    val rowBg = if (rowIndex % 2 == 0) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    }

                    Row(
                        modifier = Modifier
                            .background(rowBg)
                            .padding(vertical = 9.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        table.headers.indices.forEach { colIndex ->
                            val cellText = row.getOrNull(colIndex) ?: ""
                            val alignment = table.alignments.getOrNull(colIndex) ?: TableCellAlignment.START
                            val textAlign = when (alignment) {
                                TableCellAlignment.START -> TextAlign.Start
                                TableCellAlignment.CENTER -> TextAlign.Center
                                TableCellAlignment.END -> TextAlign.End
                            }

                            val annotatedCell = remember(cellText) {
                                buildAnnotatedFromText(cellText, onSurface, primaryColor)
                            }

                            Text(
                                text = annotatedCell,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    textAlign = textAlign
                                ),
                                modifier = Modifier
                                    .widthIn(min = 120.dp, max = 260.dp)
                                    .padding(horizontal = 8.dp)
                            )
                        }
                    }

                    if (rowIndex < table.rows.lastIndex) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

private fun buildHighlightedCode(code: String): AnnotatedString {
    val keywords = setOf(
        "fun", "val", "var", "class", "interface", "object", "return",
        "if", "else", "when", "for", "while", "import", "package",
        "def", "const", "let", "function", "public", "private", "protected",
        "override", "true", "false", "null", "SELECT", "FROM", "WHERE",
        "INSERT", "UPDATE", "DELETE", "type", "struct", "enum", "case",
        "switch", "break", "continue", "throw", "try", "catch", "finally"
    )

    return buildAnnotatedString {
        val lines = code.lines()
        for ((lineIdx, line) in lines.withIndex()) {
            val trimmedStart = line.trimStart()
            if (trimmedStart.startsWith("//") || trimmedStart.startsWith("#")) {
                pushStyle(
                    SpanStyle(
                        color = Color(0xFF6C7086),
                        fontStyle = FontStyle.Italic
                    )
                )
                append(line)
                pop()
            } else {
                var cursor = 0
                val len = line.length
                while (cursor < len) {
                    val ch = line[cursor]
                    // String literal
                    if (ch == '"' || ch == '\'') {
                        val quoteChar = ch
                        val endQuote = line.indexOf(quoteChar, cursor + 1)
                        if (endQuote != -1) {
                            val strContent = line.substring(cursor, endQuote + 1)
                            pushStyle(SpanStyle(color = Color(0xFFA6E3A1)))
                            append(strContent)
                            pop()
                            cursor = endQuote + 1
                            continue
                        }
                    }

                    // Word / Identifier
                    if (ch.isLetter() || ch == '_') {
                        val wordStart = cursor
                        while (cursor < len && (line[cursor].isLetterOrDigit() || line[cursor] == '_')) {
                            cursor++
                        }
                        val word = line.substring(wordStart, cursor)
                        if (keywords.contains(word)) {
                            pushStyle(
                                SpanStyle(
                                    color = Color(0xFFCBA6F7),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            append(word)
                            pop()
                        } else {
                            pushStyle(SpanStyle(color = Color(0xFFCDD6F4)))
                            append(word)
                            pop()
                        }
                        continue
                    }

                    // Number
                    if (ch.isDigit()) {
                        val numStart = cursor
                        while (cursor < len && (line[cursor].isDigit() || line[cursor] == '.')) {
                            cursor++
                        }
                        pushStyle(SpanStyle(color = Color(0xFFFAB387)))
                        append(line.substring(numStart, cursor))
                        pop()
                        continue
                    }

                    // Symbol / default
                    pushStyle(SpanStyle(color = Color(0xFFBAC2DE)))
                    append(ch)
                    pop()
                    cursor++
                }
            }
            if (lineIdx < lines.lastIndex) {
                append("\n")
            }
        }
    }
}

fun parseMarkdown(text: String): List<MarkdownElement> {
    if (text.isBlank()) return emptyList()

    val document = commonMarkParser.parse(text)
    val elements = mutableListOf<MarkdownElement>()

    var child = document.firstChild
    while (child != null) {
        when (child) {
            is Heading -> {
                val plainText = extractPlainText(child).trim()
                elements.add(
                    MarkdownElement.Header(
                        level = child.level,
                        text = plainText,
                        rawNode = child
                    )
                )
            }
            is FencedCodeBlock -> {
                val lang = child.info?.trim() ?: ""
                val code = child.literal?.trimEnd() ?: ""
                elements.add(MarkdownElement.CodeBlock(language = lang, code = code))
            }
            is IndentedCodeBlock -> {
                val code = child.literal?.trimEnd() ?: ""
                elements.add(MarkdownElement.CodeBlock(language = "", code = code))
            }
            is Paragraph -> {
                val plainText = extractPlainText(child).trim()
                if (plainText.isNotEmpty()) {
                    elements.add(
                        MarkdownElement.Paragraph(
                            text = plainText,
                            rawNode = child
                        )
                    )
                }
            }
            is BlockQuote -> {
                val plainText = extractPlainText(child).trim()
                elements.add(
                    MarkdownElement.Blockquote(
                        text = plainText,
                        rawNode = child
                    )
                )
            }
            is BulletList -> {
                flattenBulletList(child, level = 0, elements = elements)
            }
            is OrderedList -> {
                flattenOrderedList(child, level = 0, elements = elements)
            }
            is TableBlock -> {
                parseTableBlock(child)?.let { elements.add(it) }
            }
            is ThematicBreak -> {
                elements.add(MarkdownElement.ThematicBreak)
            }
        }
        child = child.next
    }

    return elements
}

private fun flattenBulletList(
    listNode: BulletList,
    level: Int,
    elements: MutableList<MarkdownElement>
) {
    var item = listNode.firstChild
    while (item != null) {
        if (item is ListItem) {
            var subNode = item.firstChild
            var hasText = false
            while (subNode != null) {
                when (subNode) {
                    is Paragraph -> {
                        val text = extractPlainText(subNode).trim()
                        if (text.isNotEmpty()) {
                            elements.add(
                                MarkdownElement.BulletItem(
                                    text = text,
                                    level = level,
                                    rawNode = item
                                )
                            )
                            hasText = true
                        }
                    }
                    is BulletList -> {
                        flattenBulletList(subNode, level = level + 1, elements = elements)
                    }
                    is OrderedList -> {
                        flattenOrderedList(subNode, level = level + 1, elements = elements)
                    }
                    is FencedCodeBlock -> {
                        val lang = subNode.info?.trim() ?: ""
                        val code = subNode.literal?.trimEnd() ?: ""
                        elements.add(MarkdownElement.CodeBlock(language = lang, code = code))
                    }
                    else -> {
                        val text = extractPlainText(subNode).trim()
                        if (text.isNotEmpty() && !hasText) {
                            elements.add(
                                MarkdownElement.BulletItem(
                                    text = text,
                                    level = level,
                                    rawNode = item
                                )
                            )
                            hasText = true
                        }
                    }
                }
                subNode = subNode.next
            }
        }
        item = item.next
    }
}

private fun flattenOrderedList(
    listNode: OrderedList,
    level: Int,
    elements: MutableList<MarkdownElement>
) {
    var item = listNode.firstChild
    @Suppress("DEPRECATION")
    var currentNumber = listNode.markerStartNumber ?: listNode.startNumber
    while (item != null) {
        if (item is ListItem) {
            var subNode = item.firstChild
            var hasText = false
            while (subNode != null) {
                when (subNode) {
                    is Paragraph -> {
                        val text = extractPlainText(subNode).trim()
                        if (text.isNotEmpty()) {
                            elements.add(
                                MarkdownElement.NumberedItem(
                                    number = currentNumber.toString(),
                                    text = text,
                                    level = level,
                                    rawNode = item
                                )
                            )
                            hasText = true
                        }
                    }
                    is BulletList -> {
                        flattenBulletList(subNode, level = level + 1, elements = elements)
                    }
                    is OrderedList -> {
                        flattenOrderedList(subNode, level = level + 1, elements = elements)
                    }
                    is FencedCodeBlock -> {
                        val lang = subNode.info?.trim() ?: ""
                        val code = subNode.literal?.trimEnd() ?: ""
                        elements.add(MarkdownElement.CodeBlock(language = lang, code = code))
                    }
                    else -> {
                        val text = extractPlainText(subNode).trim()
                        if (text.isNotEmpty() && !hasText) {
                            elements.add(
                                MarkdownElement.NumberedItem(
                                    number = currentNumber.toString(),
                                    text = text,
                                    level = level,
                                    rawNode = item
                                )
                            )
                            hasText = true
                        }
                    }
                }
                subNode = subNode.next
            }
            currentNumber++
        }
        item = item.next
    }
}

private fun parseTableBlock(tableBlock: TableBlock): MarkdownElement.Table? {
    val headers = mutableListOf<String>()
    val alignments = mutableListOf<TableCellAlignment>()
    val rows = mutableListOf<List<String>>()

    var child = tableBlock.firstChild
    while (child != null) {
        when (child) {
            is TableHead -> {
                var row = child.firstChild
                while (row != null) {
                    if (row is TableRow) {
                        var cell = row.firstChild
                        while (cell != null) {
                            if (cell is TableCell) {
                                headers.add(extractPlainText(cell).trim())
                                val align = when (cell.alignment) {
                                    TableCell.Alignment.CENTER -> TableCellAlignment.CENTER
                                    TableCell.Alignment.RIGHT -> TableCellAlignment.END
                                    TableCell.Alignment.LEFT -> TableCellAlignment.START
                                    null -> TableCellAlignment.START
                                }
                                alignments.add(align)
                            }
                            cell = cell.next
                        }
                    }
                    row = row.next
                }
            }
            is TableBody -> {
                var row = child.firstChild
                while (row != null) {
                    if (row is TableRow) {
                        val rowCells = mutableListOf<String>()
                        var cell = row.firstChild
                        while (cell != null) {
                            if (cell is TableCell) {
                                rowCells.add(extractPlainText(cell).trim())
                            }
                            cell = cell.next
                        }
                        if (rowCells.isNotEmpty()) {
                            rows.add(rowCells)
                        }
                    }
                    row = row.next
                }
            }
        }
        child = child.next
    }

    if (headers.isEmpty() && rows.isEmpty()) return null
    return MarkdownElement.Table(headers = headers, rows = rows, alignments = alignments)
}

fun extractPlainText(node: Node): String {
    when (node) {
        is CommonMarkText -> return node.literal ?: ""
        is Code -> return node.literal ?: ""
        is SoftLineBreak -> return " "
        is HardLineBreak -> return "\n"
    }
    val sb = StringBuilder()
    var child = node.firstChild
    while (child != null) {
        sb.append(extractPlainText(child))
        child = child.next
    }
    return sb.toString()
}

fun buildAnnotatedFromText(
    text: String,
    baseColor: Color,
    primaryColor: Color
): AnnotatedString {
    val doc = commonMarkParser.parse(text)
    val annotated = buildAnnotatedFromNode(doc, baseColor, primaryColor)
    return if (annotated.isEmpty()) AnnotatedString(text) else annotated
}

fun buildAnnotatedFromNode(
    node: Node,
    baseColor: Color,
    primaryColor: Color = Color(0xFF7C5CFF),
    codeBgColor: Color = Color(0x33888888),
    codeTextColor: Color = Color(0xFFF59E0B)
): AnnotatedString {
    return buildAnnotatedString {
        renderNodeInlines(node, this, baseColor, primaryColor, codeBgColor, codeTextColor)
    }
}

private fun renderNodeInlines(
    node: Node,
    builder: AnnotatedString.Builder,
    baseColor: Color,
    primaryColor: Color,
    codeBgColor: Color,
    codeTextColor: Color
) {
    var child = node.firstChild
    while (child != null) {
        when (child) {
            is CommonMarkText -> {
                builder.append(child.literal ?: "")
            }
            is Code -> {
                builder.pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        background = codeBgColor,
                        color = codeTextColor,
                        fontSize = 13.sp
                    )
                )
                builder.append(" ${child.literal ?: ""} ")
                builder.pop()
            }
            is StrongEmphasis -> {
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = baseColor))
                renderNodeInlines(child, builder, baseColor, primaryColor, codeBgColor, codeTextColor)
                builder.pop()
            }
            is Emphasis -> {
                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = baseColor))
                renderNodeInlines(child, builder, baseColor, primaryColor, codeBgColor, codeTextColor)
                builder.pop()
            }
            is Strikethrough -> {
                builder.pushStyle(
                    SpanStyle(
                        textDecoration = TextDecoration.LineThrough,
                        color = baseColor.copy(alpha = 0.65f)
                    )
                )
                renderNodeInlines(child, builder, baseColor, primaryColor, codeBgColor, codeTextColor)
                builder.pop()
            }
            is Link -> {
                val url = child.destination ?: ""
                builder.pushLink(
                    LinkAnnotation.Url(
                        url = url,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = primaryColor,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    )
                )
                renderNodeInlines(child, builder, baseColor, primaryColor, codeBgColor, codeTextColor)
                builder.pop()
            }
            is SoftLineBreak -> {
                builder.append(" ")
            }
            is HardLineBreak -> {
                builder.append("\n")
            }
            else -> {
                renderNodeInlines(child, builder, baseColor, primaryColor, codeBgColor, codeTextColor)
            }
        }
        child = child.next
    }
}

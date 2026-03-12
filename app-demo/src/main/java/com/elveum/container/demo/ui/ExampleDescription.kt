package com.elveum.container.demo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.elveum.container.demo.ui.theme.Dimens


@Composable
fun ExampleDescription(text: String) {
    val paragraphs = text.split("\n\n").map { paragraph ->
        paragraph.split("\n").joinToString(" ") { it.trim() }
    }
    Column(
        modifier = Modifier.padding(
            start = Dimens.MediumPadding,
            end = Dimens.MediumPadding,
            top = Dimens.SmallPadding,
            bottom = Dimens.TinyPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.ParagraphSpace),
    ) {
        paragraphs.forEach { paragraph ->
            Text(text = paragraph.parseFormatting(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun String.parseFormatting(): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    for (match in boldPattern.findAll(this@parseFormatting)) {
        append(this@parseFormatting, cursor, match.range.first)
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(match.groupValues[1])
        }
        cursor = match.range.last + 1
    }
    append(this@parseFormatting, cursor, this@parseFormatting.length)
}

private val boldPattern = Regex("""\*\*(.+?)\*\*""")

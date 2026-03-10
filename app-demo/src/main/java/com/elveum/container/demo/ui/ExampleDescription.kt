package com.elveum.container.demo.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.elveum.container.demo.ui.theme.Dimens

@Composable
fun ExampleDescription(text: String) {
    Text(
        text = text.split("\n").joinToString(" ") { it.trim() },
        modifier = Modifier.padding(
            start = Dimens.MediumPadding,
            end = Dimens.MediumPadding,
            top = Dimens.SmallPadding,
            bottom = Dimens.TinyPadding,
        ),
        style = MaterialTheme.typography.bodyMedium,
    )
}

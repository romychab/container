package com.elveum.container.demo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.elveum.container.demo.ui.theme.Dimens

@Composable
fun Heading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onPrimaryFixed,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryFixedDim)
            .padding(horizontal = Dimens.MediumPadding, vertical = Dimens.SmallPadding),
    )
}

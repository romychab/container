package com.elveum.store.demo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elveum.store.demo.ui.theme.Dimens

@Composable
fun Heading(
    text: String,
    actionIcon: ImageVector? = null,
    actionClick: () -> Unit = {},
    actionEnabled: Boolean = true,
    actionInProgress: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryFixedDim),
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.MediumPadding, vertical = Dimens.SmallPadding),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimaryFixed,
            textAlign = TextAlign.Center,
        )
        if (actionIcon != null) {
            if (actionInProgress) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimaryFixed,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(Dimens.SmallIconSize)
                        .align(Alignment.CenterEnd)
                        .padding(end = Dimens.SmallPadding),
                )
            } else {
                IconButton(
                    onClick = actionClick,
                    enabled = actionEnabled,
                    modifier = Modifier
                        .size(Dimens.SmallIconSize)
                        .align(Alignment.CenterEnd)
                        .padding(end = Dimens.SmallPadding),
                ) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryFixedVariant,
                    )
                }
            }
        }
    }
}

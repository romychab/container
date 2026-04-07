package com.elveum.container.demo.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.elveum.container.demo.ui.theme.Dimens

@Composable
fun ProgressIconButton(
    imageVector: ImageVector,
    isLoading: Boolean,
    contentDescription: String,
    tint: Color = Color.Unspecified,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(Dimens.ProgressIconSize),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(Modifier.padding(Dimens.SmallPadding))
        } else {
            IconButton(
                onClick = onClick,
            ) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = contentDescription,
                    tint = tint,
                )
            }
        }
    }
}

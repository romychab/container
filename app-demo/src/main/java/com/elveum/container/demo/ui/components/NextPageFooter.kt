package com.elveum.container.demo.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elveum.container.demo.ui.theme.Dimens
import com.elveum.container.subject.paging.PageState

@Composable
fun NextPageFooter(
    pageState: PageState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.MediumPadding)
            .heightIn(min = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (pageState) {
            PageState.Idle -> Unit
            PageState.Pending -> CircularProgressIndicator()
            is PageState.Error -> {
                OutlinedButton(onClick = pageState.retry) {
                    Text("Retry", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

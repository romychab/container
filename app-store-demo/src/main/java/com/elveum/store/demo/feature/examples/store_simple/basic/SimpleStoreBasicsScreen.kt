package com.elveum.store.demo.feature.examples.store_simple.basic

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.elveum.store.demo.ui.components.DemoScaffold
import com.elveum.store.demo.ui.components.EditableField
import com.elveum.store.load.StoreResult
import com.elveum.store.load.invalidate

@Composable
fun SimpleStoreBasicsScreen() = DemoScaffold(
    title = "SimpleStore Basics",
    description = """
        Load/reload/edit data with **SimpleStore**, display 3 states: loading, success, and error.

        **SimpleStore** converts suspending data sources into reactive Flow. The suspending loader
        function is triggered only upon the first subscription.
    """.trimIndent()
) {

    val viewModel = hiltViewModel<SimpleStoreBasicsViewModel>()
    val state by viewModel.stateFlow.collectAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = state.isErrorsEnabled,
            onCheckedChange = { viewModel.toggleLoadErrors() },
        )
        Text("Enable Errors")
    }
    when (val userProfile = state.userProfile) {
        StoreResult.Loading -> {
            CircularProgressIndicator(Modifier.weight(1f).wrapContentHeight())
        }
        is StoreResult.Loaded -> {
            Spacer(Modifier.weight(1f))
            Text(
                text = userProfile.value.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            EditableField(
                title = "Age",
                value = userProfile.value.age.toString(),
                isNumber = true,
                onNewValue = { newValue ->
                    newValue.toIntOrNull()?.let { viewModel.updateAge(it) }
                }
            )

            EditableField(
                title = "Address",
                value = userProfile.value.address,
                onNewValue = viewModel::updateAddress,
            )

            EditableField(
                title = "Bio",
                value = userProfile.value.shortBio,
                onNewValue = viewModel::updateBio,
            )

            Button(onClick = userProfile::invalidate) {
                Text("Reload")
            }
            Spacer(Modifier.weight(1f))
        }
        is StoreResult.Failed -> {
            Spacer(Modifier.weight(1f))
            Text("${userProfile.exception.message}")
            Button(
                onClick = userProfile::invalidate,
            ) {
                Text("Try Again")
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

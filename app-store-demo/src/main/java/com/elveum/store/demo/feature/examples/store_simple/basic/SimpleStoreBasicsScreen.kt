package com.elveum.store.demo.feature.examples.store_simple.basic

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.elveum.store.demo.ui.components.DemoScaffold
import com.elveum.store.demo.ui.components.EditableField
import com.elveum.store.demo.ui.components.Heading
import com.elveum.store.load.StoreResult

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

            Button(onClick = viewModel::reload) {
                Text("Reload")
            }
            Spacer(Modifier.weight(1f))
        }
        is StoreResult.Failed -> {
            Spacer(Modifier.weight(1f))
            Text("${userProfile.exception.message}")
            Button(
                onClick = viewModel::reload,
            ) {
                Text("Try Again")
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

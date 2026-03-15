package com.elveum.container.demo.feature.examples.subject_basics

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
import com.elveum.container.demo.ui.components.DemoScaffold
import com.elveum.container.demo.ui.components.Heading

@Composable
fun SubjectBasicsScreen() = DemoScaffold(
    title = "LazyFlowSubject Basics",
    description = """
        Make your data loading logic more reactive and efficient with **LazyFlowSubject**.

        All results are automatically wrapped into Container and cached. The loader
        is triggered only upon the first subscription. It can be re-triggered by calling
        the **reload()** function available within fold() branches or on Container<T> itself.
    """.trimIndent()
) {
    val viewModel = hiltViewModel<SubjectBasicsViewModel>()
    val state by viewModel.stateFlow.collectAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = state.isErrorsEnabled,
            onCheckedChange = { viewModel.toggleLoadErrors() },
        )
        Text("Enable Load Errors")
    }
    state.userProfile.fold(
        onPending = {
            CircularProgressIndicator(Modifier.weight(1f).wrapContentHeight())
        },
        onSuccess = { profile ->
            Spacer(Modifier.weight(1f))
            Text(
                text = profile.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Heading("Age")
            Text("${profile.age}")
            Heading("Address")
            Text(profile.address, textAlign = TextAlign.Center)
            Heading("Bio")
            Text(profile.shortBio, textAlign = TextAlign.Center)
            Button(onClick = ::reload) {
                Text("Reload")
            }
            Spacer(Modifier.weight(1f))
        },
        onError = { exception ->
            Spacer(Modifier.weight(1f))
            Text("${exception.message}")
            Button(
                onClick = ::reload,
            ) {
                Text("Try Again")
            }
            Spacer(Modifier.weight(1f))
        }
    )
}

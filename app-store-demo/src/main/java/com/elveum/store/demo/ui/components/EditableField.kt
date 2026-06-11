package com.elveum.store.demo.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign

@Composable
fun EditableField(
    title: String,
    value: String,
    isNumber: Boolean = false,
    isInProgress: Boolean = false,
    onNewValue: (String) -> Unit,
) {
    var isEditMode by rememberSaveable { mutableStateOf(false) }
    var editableValue by rememberSaveable { mutableStateOf(value) }

    Heading(
        text = title,
        actionIcon = if (!isEditMode) Icons.Default.Edit else Icons.Default.Check,
        actionEnabled = editableValue.isNotBlank(),
        actionInProgress = isInProgress,
        actionClick = {
            if (isEditMode) {
                if (editableValue.isNotBlank()) {
                    onNewValue(editableValue)
                    isEditMode = false
                }
            } else {
                editableValue = value
                isEditMode = true
            }
        }
    )

    if (isEditMode) {
        val focusRequester = remember { FocusRequester() }
        OutlinedTextField(
            value = editableValue,
            onValueChange = { editableValue = it },
            modifier = Modifier.focusRequester(focusRequester),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (editableValue.isNotBlank()) {
                        onNewValue(editableValue)
                        isEditMode = false
                    }
                }
            ),
            maxLines = 1,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = if (isNumber) KeyboardType.Number else KeyboardType.Text
            )
        )
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    } else {
        Text(value, textAlign = TextAlign.Center)
    }
}

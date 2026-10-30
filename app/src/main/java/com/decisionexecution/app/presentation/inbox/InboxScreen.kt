package com.decisionexecution.app.presentation.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decisionexecution.app.domain.model.InboxItem
import com.decisionexecution.app.presentation.components.ConfettiBurst
import com.decisionexecution.app.presentation.theme.Spacing
import java.time.Duration

/**
 * Quick capture inbox. With [focusCapture] the keyboard opens straight away, for the widget.
 */
@Composable
fun InboxScreen(
    onBack: () -> Unit,
    onMakeTask: (InboxItem) -> Unit,
    focusCapture: Boolean = false,
    viewModel: InboxViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(focusCapture) { if (focusCapture) focusRequester.requestFocus() }
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    Box {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Inbox") },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
                OutlinedTextField(
                    value = uiState.draft,
                    onValueChange = viewModel::onDraftChange,
                    placeholder = { Text("What's on your mind?") },
                    shape = RoundedCornerShape(28.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.onCapture() }),
                    trailingIcon = {
                        FilledIconButton(
                            onClick = viewModel::onCapture,
                            enabled = uiState.draft.isNotBlank(),
                            shapes = IconButtonDefaults.shapes()
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Capture")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md)
                        .focusRequester(focusRequester)
                )
                Spacer(Modifier.height(Spacing.md))
                if (!uiState.isLoading && uiState.items.isEmpty()) {
                    InboxZero()
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, bottom = Spacing.xl),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        items(uiState.items, key = { it.id }) { item ->
                            InboxCard(
                                item = item,
                                onMakeTask = { onMakeTask(item) },
                                onMakeHabit = { viewModel.onMakeHabit(item) },
                                onDismiss = { viewModel.onDismiss(item) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
        ConfettiBurst(uiState.inboxZeroKey)
    }
}

@Composable
private fun InboxCard(
    item: InboxItem,
    onMakeTask: () -> Unit,
    onMakeHabit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(item.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(
                    age(item.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = onMakeTask, shapes = ButtonDefaults.shapes(), contentPadding = ButtonDefaults.SmallContentPadding) {
                    Icon(Icons.Rounded.TaskAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text("Task")
                }
                FilledTonalButton(onClick = onMakeHabit, shapes = ButtonDefaults.shapes(), contentPadding = ButtonDefaults.SmallContentPadding) {
                    Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text("Habit")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.Close, contentDescription = "Bin it")
                }
            }
        }
    }
}

@Composable
private fun InboxZero() {
    Column(
        Modifier.fillMaxSize().padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(96.dp).clip(MaterialShapes.Puffy.toShape()).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(Spacing.md))
        Text("Inbox zero", style = MaterialTheme.typography.titleLarge)
        Text(
            "Dump ideas here the moment they pop up, then sort them when you have a minute.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun age(createdAt: Long): String {
    val minutes = Duration.ofMillis(System.currentTimeMillis() - createdAt).toMinutes()
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        minutes < 24 * 60 -> "${minutes / 60}h"
        minutes < 7 * 24 * 60 -> "${minutes / (24 * 60)}d"
        else -> "${minutes / (7 * 24 * 60)}w"
    }
}

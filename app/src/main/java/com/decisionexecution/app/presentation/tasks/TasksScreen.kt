package com.decisionexecution.app.presentation.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun TasksScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding()
    ) {
        Text("Tasks Screen - TODO")
    }
}

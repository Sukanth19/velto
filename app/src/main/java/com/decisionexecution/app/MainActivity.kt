package com.decisionexecution.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.decisionexecution.app.presentation.common.CrashReporter
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.decisionexecution.app.domain.model.UserPreferences
import com.decisionexecution.app.domain.repository.PreferencesRepository
import com.decisionexecution.app.presentation.navigation.AppNavHost
import com.decisionexecution.app.presentation.navigation.openDeepLink
import com.decisionexecution.app.presentation.notifications.NotificationHelper
import com.decisionexecution.app.presentation.theme.VeltoTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    // Deep links from notifications and widgets, both at launch and while open (singleTop)
    private val deepLinks = Channel<Uri>(Channel.BUFFERED)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Draw behind the status and navigation bars; screens pad themselves where needed
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // A recreated activity has already handled its launch link
        if (savedInstanceState == null) intent?.data?.let(deepLinks::trySend)
        setContent {
            val preferences by preferencesRepository.getPreferences().collectAsState(initial = UserPreferences())
            val navController = rememberNavController()

            LaunchedEffect(navController) {
                for (uri in deepLinks) navController.openDeepLink(uri)
            }
            RequestNotificationPermission()

            VeltoTheme(themeMode = preferences.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(navController)
                    CrashReportPrompt()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let(deepLinks::trySend)
    }

    /**
     * After a crash, offers to send the saved report. Asked once; either answer clears it.
     */
    @Composable
    private fun CrashReportPrompt() {
        val reporter = remember { CrashReporter(applicationContext) }
        var report by remember { mutableStateOf(reporter.pendingReport()) }
        val current = report ?: return
        fun done() {
            reporter.clear()
            report = null
        }
        AlertDialog(
            onDismissRequest = ::done,
            title = { Text("Velto crashed last time") },
            text = {
                Text("Sorry about that. Sending the report helps get it fixed. It only has the error and your phone model, nothing you've written.")
            },
            confirmButton = {
                TextButton(onClick = {
                    val issue = Uri.parse(ISSUES_URL).buildUpon()
                        .appendQueryParameter("title", "Crash report")
                        .appendQueryParameter("body", "```\n$current\n```")
                        .build()
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, issue)) }
                    done()
                }) { Text("Report on GitHub") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = ::done) { Text("Not now") }
                    TextButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, current)
                        runCatching { startActivity(Intent.createChooser(send, "Send crash report")) }
                        done()
                    }) { Text("Share") }
                }
            }
        )
    }

    /**
     * Asks once on Android 13+. If denied, the timer still works; notifications are just skipped.
     */
    @Composable
    private fun RequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
        LaunchedEffect(Unit) {
            if (!notificationHelper.canPostNotifications()) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

private const val ISSUES_URL = "https://github.com/Sukanth19/velto/issues/new"

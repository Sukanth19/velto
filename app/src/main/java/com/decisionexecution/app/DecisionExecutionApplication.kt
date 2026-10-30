package com.decisionexecution.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.decisionexecution.app.data.work.ReminderKind
import com.decisionexecution.app.data.work.ReminderScheduler
import com.decisionexecution.app.data.work.reminderFor
import com.decisionexecution.app.domain.repository.PreferencesRepository
import com.decisionexecution.app.domain.service.FocusSessionManager
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.decisionexecution.app.presentation.focusmode.FocusAutomation
import com.decisionexecution.app.presentation.widget.WidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DecisionExecutionApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var focusSessionManager: FocusSessionManager

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    @Inject
    lateinit var focusAutomation: FocusAutomation

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Restore a session that was running when the process died
        appScope.launch { runCatching { focusSessionManager.sync() } }
        widgetUpdater.start(appScope)
        focusAutomation.start(appScope)
        appScope.launch {
            // Rebooking on every start also recovers reminders after a time zone or clock change
            preferencesRepository.getPreferences()
                .map { prefs -> ReminderKind.entries.map { prefs.reminderFor(it) } }
                .distinctUntilChanged()
                .collect { reminders ->
                    ReminderKind.entries.zip(reminders).forEach { (kind, reminder) ->
                        runCatching { reminderScheduler.schedule(kind, reminder) }
                    }
                }
        }
    }
}

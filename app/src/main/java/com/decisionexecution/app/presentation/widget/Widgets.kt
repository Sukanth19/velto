package com.decisionexecution.app.presentation.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.wrapContentSize
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.decisionexecution.app.MainActivity
import com.decisionexecution.app.R
import com.decisionexecution.app.domain.service.FocusSessionManager
import com.decisionexecution.app.domain.usecase.GetBestTaskUseCase
import com.decisionexecution.app.presentation.navigation.DeepLinks
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

/** Glance widgets can't be constructor-injected, so they reach the graph through this. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun getBestTaskUseCase(): GetBestTaskUseCase
    fun focusSessionManager(): FocusSessionManager
}

private fun Context.widgetEntryPoint(): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java)

private fun deepLinkAction(context: Context, uri: String): Action =
    actionStartActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(uri), context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    )

/**
 * Best task with a Just Start button. Tapping the task opens its details.
 * Requirements: 8.1, 8.3, 8.4, 8.6, 8.7
 */
class BestTaskWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // A failed or slow load shows the last known task rather than a broken widget (Requirement 8.4)
        val state = loadBestTaskWidgetState(BestTaskWidgetCache(context), LOAD_TIMEOUT_MILLIS) {
            bestTaskWidgetState(context.widgetEntryPoint().getBestTaskUseCase()().first())
        }

        provideContent {
            GlanceTheme { BestTaskContent(context, state) }
        }
    }

    @Composable
    private fun BestTaskContent(context: Context, state: BestTaskWidgetState) {
        val openAction = deepLinkAction(context, state.taskId?.let(DeepLinks::openTask) ?: DeepLinks.HOME)
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(16.dp)
                .padding(12.dp)
                .clickable(openAction)
        ) {
            Text(
                "Next task",
                style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            )
            Text(
                state.title,
                maxLines = 2,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            )
            state.details?.let {
                Text(it, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
            }
            Spacer(GlanceModifier.height(8.dp))
            if (state.taskId != null) {
                Button("Just Start", onClick = deepLinkAction(context, DeepLinks.JUST_START))
            }
        }
    }

    private companion object {
        const val LOAD_TIMEOUT_MILLIS = 5_000L
    }
}

/**
 * Current focus session with a live countdown. Tapping opens the Focus screen.
 * Requirements: 8.2, 8.5, 8.6
 */
class FocusTimerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = runCatching {
            val manager = context.widgetEntryPoint().focusSessionManager()
            manager.sync()
            timerWidgetState(manager.sessionState.value)
        }.getOrElse { TimerWidgetState.Idle }

        provideContent {
            GlanceTheme { TimerContent(context, state) }
        }
    }

    @Composable
    private fun TimerContent(context: Context, state: TimerWidgetState) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(16.dp)
                .padding(12.dp)
                .clickable(deepLinkAction(context, DeepLinks.FOCUS)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (state) {
                is TimerWidgetState.Idle -> Text(
                    "No active session",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp)
                )
                is TimerWidgetState.Active -> {
                    Text(
                        state.label.uppercase(),
                        style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    )
                    Text(
                        state.title,
                        maxLines = 1,
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp)
                    )
                    // Without wrapContentSize the RemoteViews box fills the widget and pins the time top-left
                    AndroidRemoteViews(countdown(context, state), GlanceModifier.wrapContentSize())
                }
            }
        }
    }

    // A system Chronometer counts down on its own, so the widget doesn't need per-minute updates
    private fun countdown(context: Context, state: TimerWidgetState.Active): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_countdown).apply {
            setChronometer(R.id.countdown, SystemClock.elapsedRealtime() + state.remainingMillis, null, state.isRunning)
            setChronometerCountDown(R.id.countdown, true)
        }
}

class BestTaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BestTaskWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        BestTaskWidgetUpdateWorker.schedule(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        BestTaskWidgetUpdateWorker.cancel(context)
    }
}

class FocusTimerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FocusTimerWidget()
}

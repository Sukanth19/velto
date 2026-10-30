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
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentSize
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.decisionexecution.app.MainActivity
import com.decisionexecution.app.R
import com.decisionexecution.app.domain.repository.HabitRepository
import com.decisionexecution.app.domain.repository.InboxRepository
import com.decisionexecution.app.domain.repository.MoodRepository
import com.decisionexecution.app.domain.repository.PreferencesRepository
import com.decisionexecution.app.domain.usecase.GetProgressUseCase
import com.decisionexecution.app.domain.service.FocusSessionManager
import com.decisionexecution.app.domain.usecase.CompleteTaskUseCase
import com.decisionexecution.app.domain.usecase.ExtendFocusSessionUseCase
import com.decisionexecution.app.domain.usecase.GetBestTaskUseCase
import com.decisionexecution.app.domain.usecase.GetInsightsUseCase
import com.decisionexecution.app.domain.usecase.GetProductivityMetricsUseCase
import com.decisionexecution.app.domain.usecase.GetTopThreeTasksUseCase
import com.decisionexecution.app.presentation.navigation.DeepLinks
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** Glance widgets can't be constructor-injected, so they reach the graph through this. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun getBestTaskUseCase(): GetBestTaskUseCase
    fun focusSessionManager(): FocusSessionManager
    fun getTopThreeTasksUseCase(): GetTopThreeTasksUseCase
    fun completeTaskUseCase(): CompleteTaskUseCase
    fun getProductivityMetricsUseCase(): GetProductivityMetricsUseCase
    fun getInsightsUseCase(): GetInsightsUseCase
    fun preferencesRepository(): PreferencesRepository
    fun habitRepository(): HabitRepository
    fun moodRepository(): MoodRepository
    fun inboxRepository(): InboxRepository
    fun getProgressUseCase(): GetProgressUseCase
}

internal fun Context.widgetEntryPoint(): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java)

internal fun deepLinkAction(context: Context, uri: String): Action =
    actionStartActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(uri), context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    )

/**
 * Best task with a Just Start button. Tapping the task opens its details.
 */
class BestTaskWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // A failed or slow load shows the last known task rather than a broken widget
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
                    Spacer(GlanceModifier.height(6.dp))
                    TimerControls(state)
                }
            }
        }
    }

    @Composable
    private fun TimerControls(state: TimerWidgetState.Active) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.isBreak) {
                ControlButton(R.drawable.ic_widget_skip, "Skip break", TimerControlAction.STOP, primary = true)
                return@Row
            }
            ControlButton(R.drawable.ic_widget_stop, "End session", TimerControlAction.STOP, primary = false)
            Spacer(GlanceModifier.width(8.dp))
            if (state.isRunning) {
                ControlButton(R.drawable.ic_widget_pause, "Pause", TimerControlAction.PAUSE, primary = true)
            } else {
                ControlButton(R.drawable.ic_widget_play, "Resume", TimerControlAction.RESUME, primary = true)
            }
            Spacer(GlanceModifier.width(8.dp))
            ControlButton(
                R.drawable.ic_widget_add,
                "Add ${ExtendFocusSessionUseCase.DEFAULT_MINUTES} minutes",
                TimerControlAction.EXTEND,
                primary = false
            )
        }
    }

    @Composable
    private fun ControlButton(icon: Int, description: String, command: String, primary: Boolean) {
        CircleIconButton(
            imageProvider = ImageProvider(icon),
            contentDescription = description,
            onClick = actionRunCallback<TimerControlAction>(actionParametersOf(TimerControlAction.COMMAND to command)),
            backgroundColor = if (primary) GlanceTheme.colors.primary else GlanceTheme.colors.secondaryContainer,
            contentColor = if (primary) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onSecondaryContainer,
            modifier = GlanceModifier.size(if (primary) 48.dp else 40.dp)
        )
    }

    // A system Chronometer counts down on its own, so the widget doesn't need per-minute updates
    private fun countdown(context: Context, state: TimerWidgetState.Active): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_countdown).apply {
            setChronometer(R.id.countdown, SystemClock.elapsedRealtime() + state.remainingMillis, null, state.isRunning)
            setChronometerCountDown(R.id.countdown, true)
        }
}

/**
 * Focus time against the daily goal, tasks done today and the streak.
 */
class TodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = runCatching { loadTodayWidgetState(context.widgetEntryPoint()) }
            .getOrElse { TodayWidgetState(0, 0, 0, 0) }

        provideContent {
            GlanceTheme { TodayContent(context, state) }
        }
    }

    @Composable
    private fun TodayContent(context: Context, state: TodayWidgetState) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(16.dp)
                .padding(12.dp)
                .clickable(deepLinkAction(context, DeepLinks.HOME)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "TODAY",
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                )
                state.streakLabel?.let {
                    Text(it, style = TextStyle(color = GlanceTheme.colors.tertiary, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                }
            }
            Text(
                state.focusLabel,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            )
            Text("focused", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
            Spacer(GlanceModifier.height(8.dp))
            LinearProgressIndicator(
                progress = state.progress,
                modifier = GlanceModifier.fillMaxWidth().height(8.dp).cornerRadius(4.dp),
                color = if (state.goalReached) GlanceTheme.colors.tertiary else GlanceTheme.colors.primary,
                backgroundColor = GlanceTheme.colors.secondaryContainer
            )
            Spacer(GlanceModifier.height(6.dp))
            Text(
                "${state.goalLabel} · ${state.completedTasks} done",
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
            )
        }
    }
}

internal suspend fun loadTodayWidgetState(entryPoint: WidgetEntryPoint): TodayWidgetState {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    val metrics = entryPoint.getProductivityMetricsUseCase()(start, end, CONSISTENCY_DAYS).first()
    val insights = entryPoint.getInsightsUseCase()(today, zone).first()
    val goal = entryPoint.preferencesRepository().getPreferences().first().dailyFocusGoalMinutes
    return TodayWidgetState(metrics.totalFocusTime, goal, metrics.completedTasks, insights.streakDays)
}

private const val CONSISTENCY_DAYS = 7

/**
 * The three best tasks with a checkbox each, so small wins can be ticked off from the home screen.
 */
class TopTasksWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val rows = runCatching { topTaskRows(context.widgetEntryPoint().getTopThreeTasksUseCase()().first()) }
            .getOrElse { emptyList() }

        provideContent {
            GlanceTheme { TopTasksContent(context, rows) }
        }
    }

    @Composable
    private fun TopTasksContent(context: Context, rows: List<TopTaskRow>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(16.dp)
                .padding(12.dp)
        ) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Top tasks",
                    modifier = GlanceModifier.defaultWeight().clickable(deepLinkAction(context, DeepLinks.HOME)),
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                )
                CircleIconButton(
                    imageProvider = ImageProvider(R.drawable.ic_widget_add),
                    contentDescription = "Add task",
                    onClick = deepLinkAction(context, DeepLinks.ADD_TASK),
                    backgroundColor = GlanceTheme.colors.primaryContainer,
                    contentColor = GlanceTheme.colors.onPrimaryContainer,
                    modifier = GlanceModifier.size(36.dp)
                )
            }
            Spacer(GlanceModifier.height(4.dp))
            if (rows.isEmpty()) {
                Text(
                    "All clear. Nothing waiting on you.",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp)
                )
            }
            rows.forEach { row ->
                Row(
                    GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CheckBox(
                        checked = false,
                        onCheckedChange = actionRunCallback<CompleteTopTaskAction>(
                            actionParametersOf(CompleteTopTaskAction.TASK_ID to row.taskId)
                        )
                    )
                    Column(
                        GlanceModifier
                            .defaultWeight()
                            .clickable(deepLinkAction(context, DeepLinks.openTask(row.taskId)))
                    ) {
                        Text(
                            row.title,
                            maxLines = 1,
                            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        )
                        Text(
                            row.quadrantLabel,
                            maxLines = 1,
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Widgets that the periodic refresh keeps current; the timer widget redraws itself with a Chronometer.
 */
abstract class RefreshedWidgetReceiver : GlanceAppWidgetReceiver() {
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        BestTaskWidgetUpdateWorker.schedule(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Only stop the refresh once the last widget of any refreshed kind is gone
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val manager = GlanceAppWidgetManager(context)
                val remaining = refreshedWidgets().sumOf { manager.getGlanceIds(it::class.java).size }
                if (remaining == 0) BestTaskWidgetUpdateWorker.cancel(context)
            } finally {
                pending.finish()
            }
        }
    }
}

/** Every widget the periodic refresh redraws */
fun refreshedWidgets(): List<GlanceAppWidget> = listOf(
    BestTaskWidget(), TodayWidget(), TopTasksWidget(), HabitsWidget(), MoodWidget(), CaptureWidget(), LevelWidget()
)

class BestTaskWidgetReceiver : RefreshedWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BestTaskWidget()
}

class TodayWidgetReceiver : RefreshedWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

class TopTasksWidgetReceiver : RefreshedWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TopTasksWidget()
}

class FocusTimerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FocusTimerWidget()
}

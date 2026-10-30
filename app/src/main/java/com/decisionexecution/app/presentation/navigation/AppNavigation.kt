package com.decisionexecution.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.decisionexecution.app.presentation.habits.HabitsScreen
import com.decisionexecution.app.presentation.inbox.InboxScreen
import com.decisionexecution.app.presentation.journal.JournalScreen
import com.decisionexecution.app.presentation.progress.CelebrationHost
import com.decisionexecution.app.presentation.progress.CelebrationViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import android.net.Uri
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.decisionexecution.app.presentation.home.JustStartRoute
import com.decisionexecution.app.presentation.addedit.AddEditTaskScreen
import com.decisionexecution.app.presentation.addedit.AddEditTaskViewModel
import com.decisionexecution.app.presentation.focus.FocusScreen
import com.decisionexecution.app.presentation.home.HomeScreen
import com.decisionexecution.app.presentation.insights.InsightsScreen
import com.decisionexecution.app.presentation.settings.SettingsScreen
import com.decisionexecution.app.presentation.taskdetail.TaskDetailScreen
import com.decisionexecution.app.presentation.taskdetail.TaskDetailViewModel
import com.decisionexecution.app.presentation.tasks.TasksScreen

/**
 * In-app deep links for notifications and widgets. Requirements: 8.6, 8.7, 13.4, 13.5
 */
object DeepLinks {
    const val SCHEME = "decisionexecution"
    private const val BASE = "$SCHEME://"
    const val HOME = "${BASE}home"
    const val FOCUS = "${BASE}focus"
    const val JUST_START = "${BASE}just_start"
    const val ADD_TASK = "${BASE}add_task"
    const val CAPTURE = "${BASE}capture"
    const val HABITS = "${BASE}habits"
    const val JOURNAL = "${BASE}journal"
    const val PROGRESS = "${BASE}progress"

    fun openTask(taskId: String) = "${BASE}open_task/$taskId"
}

object Routes {
    const val HOME = "home"
    const val TASKS = "tasks"
    const val FOCUS = "focus"
    const val SETTINGS = "settings"
    const val INSIGHTS = "insights"
    const val HABITS = "habits"
    const val JOURNAL = "journal"
    const val CAPTURE_ARG = "capture"
    const val INBOX = "inbox?$CAPTURE_ARG={$CAPTURE_ARG}"
    const val JUST_START = "just_start"
    const val TASK_DETAIL = "task/{${TaskDetailViewModel.TASK_ID_ARG}}"
    const val ADD_EDIT_TASK = "task_edit?${AddEditTaskViewModel.TASK_ID_ARG}={${AddEditTaskViewModel.TASK_ID_ARG}}" +
        "&${AddEditTaskViewModel.TITLE_ARG}={${AddEditTaskViewModel.TITLE_ARG}}" +
        "&${AddEditTaskViewModel.INBOX_ID_ARG}={${AddEditTaskViewModel.INBOX_ID_ARG}}"

    fun taskDetail(taskId: String) = "task/$taskId"
    fun addTask() = "task_edit"
    fun editTask(taskId: String) = "task_edit?${AddEditTaskViewModel.TASK_ID_ARG}=$taskId"
    fun taskFromInbox(title: String, inboxId: String) =
        "task_edit?${AddEditTaskViewModel.TITLE_ARG}=${Uri.encode(title)}&${AddEditTaskViewModel.INBOX_ID_ARG}=$inboxId"
    fun inbox(capture: Boolean = false) = "inbox?$CAPTURE_ARG=$capture"
}

private enum class TopLevelTab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    HOME(Routes.HOME, "Home", Icons.Outlined.Home, Icons.Rounded.Home),
    TASKS(Routes.TASKS, "Tasks", Icons.Outlined.TaskAlt, Icons.Rounded.TaskAlt),
    HABITS(Routes.HABITS, "Habits", Icons.Outlined.LocalFireDepartment, Icons.Rounded.LocalFireDepartment),
    FOCUS(Routes.FOCUS, "Focus", Icons.Outlined.Timer, Icons.Rounded.Timer),
    INSIGHTS(Routes.INSIGHTS, "Progress", Icons.Outlined.Insights, Icons.Rounded.Insights)
}

/**
 * Navigates to a bottom-bar tab without stacking duplicate copies of it.
 */
fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Opens a deep link the way a user would get there. NavHost's own deep-link handling builds a
 * synthetic Home -> target stack inside the Home tab, so tapping Home afterwards brought the
 * target back; switching tabs explicitly keeps each tab's history separate.
 *
 * @return false if the link isn't one of ours
 */
fun NavHostController.openDeepLink(uri: Uri): Boolean {
    if (uri.scheme != DeepLinks.SCHEME) return false
    when (uri.host) {
        "home" -> navigateToTab(Routes.HOME)
        "focus" -> navigateToTab(Routes.FOCUS)
        "just_start" -> navigate(Routes.JUST_START)
        "add_task" -> navigate(Routes.addTask())
        "capture" -> navigate(Routes.inbox(capture = true))
        "habits" -> navigateToTab(Routes.HABITS)
        "journal" -> navigate(Routes.JOURNAL)
        "progress" -> navigateToTab(Routes.INSIGHTS)
        "open_task" -> navigate(Routes.taskDetail(uri.pathSegments.firstOrNull() ?: return false))
        else -> return false
    }
    return true
}

/**
 * Bottom navigation with Home, Tasks, Habits, Focus and Progress, plus the secondary screens.
 * Level-ups and achievements are celebrated over whatever screen is showing.
 * Requirements: 12.3
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = TopLevelTab.entries.any { it.route == currentDestination?.route }
    val celebrations: CelebrationViewModel = hiltViewModel()

    Scaffold(
        // Edge to edge: each screen handles the status bar itself; the bar below handles the nav bar
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                // Expressive short bar: pill indicator, tighter height
                ShortNavigationBar {
                    TopLevelTab.entries.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        ShortNavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTab(tab.route) },
                            icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onNavigateToFocus = { navController.navigateToTab(Routes.FOCUS) },
                    onOpenTask = { navController.navigate(Routes.taskDetail(it)) },
                    onAddTask = { navController.navigate(Routes.addTask()) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenInsights = { navController.navigateToTab(Routes.INSIGHTS) },
                    onOpenHabits = { navController.navigateToTab(Routes.HABITS) },
                    onOpenInbox = { navController.navigate(Routes.inbox()) },
                    onOpenJournal = { navController.navigate(Routes.JOURNAL) }
                )
            }
            composable(Routes.TASKS) {
                TasksScreen(
                    onOpenTask = { navController.navigate(Routes.taskDetail(it)) },
                    onEditTask = { navController.navigate(Routes.editTask(it)) },
                    onAddTask = { navController.navigate(Routes.addTask()) }
                )
            }
            composable(Routes.INSIGHTS) {
                InsightsScreen(onOpenJournal = { navController.navigate(Routes.JOURNAL) })
            }
            composable(Routes.HABITS) {
                HabitsScreen()
            }
            composable(Routes.JOURNAL) {
                JournalScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Routes.INBOX,
                arguments = listOf(navArgument(Routes.CAPTURE_ARG) {
                    type = NavType.BoolType
                    defaultValue = false
                })
            ) { entry ->
                InboxScreen(
                    onBack = { navController.popBackStack() },
                    onMakeTask = { navController.navigate(Routes.taskFromInbox(it.text, it.id)) },
                    focusCapture = entry.arguments?.getBoolean(Routes.CAPTURE_ARG) == true
                )
            }
            composable(Routes.FOCUS) {
                FocusScreen(onGoHome = { navController.navigateToTab(Routes.HOME) })
            }
            composable(Routes.JUST_START) {
                JustStartRoute(
                    onStarted = {
                        navController.popBackStack()
                        navController.navigateToTab(Routes.FOCUS)
                    },
                    onFailed = {
                        navController.popBackStack()
                        navController.navigateToTab(Routes.HOME)
                    }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Routes.TASK_DETAIL,
                arguments = listOf(navArgument(TaskDetailViewModel.TASK_ID_ARG) { type = NavType.StringType })
            ) {
                TaskDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.editTask(it)) }
                )
            }
            composable(
                Routes.ADD_EDIT_TASK,
                arguments = listOf(AddEditTaskViewModel.TASK_ID_ARG, AddEditTaskViewModel.TITLE_ARG, AddEditTaskViewModel.INBOX_ID_ARG).map { name ->
                    navArgument(name) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                }
            ) {
                AddEditTaskScreen(onDone = { navController.popBackStack() })
            }
        }
        CelebrationHost(celebrations)
    }
}

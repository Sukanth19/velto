package com.decisionexecution.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.decisionexecution.app.presentation.addedit.AddEditTaskScreen
import com.decisionexecution.app.presentation.addedit.AddEditTaskViewModel
import com.decisionexecution.app.presentation.focus.FocusScreen
import com.decisionexecution.app.presentation.home.HomeScreen
import com.decisionexecution.app.presentation.settings.SettingsScreen
import com.decisionexecution.app.presentation.taskdetail.TaskDetailScreen
import com.decisionexecution.app.presentation.taskdetail.TaskDetailViewModel
import com.decisionexecution.app.presentation.tasks.TasksScreen

object Routes {
    const val HOME = "home"
    const val TASKS = "tasks"
    const val FOCUS = "focus"
    const val SETTINGS = "settings"
    const val TASK_DETAIL = "task/{${TaskDetailViewModel.TASK_ID_ARG}}"
    const val ADD_EDIT_TASK = "task_edit?${AddEditTaskViewModel.TASK_ID_ARG}={${AddEditTaskViewModel.TASK_ID_ARG}}"

    fun taskDetail(taskId: String) = "task/$taskId"
    fun addTask() = "task_edit"
    fun editTask(taskId: String) = "task_edit?${AddEditTaskViewModel.TASK_ID_ARG}=$taskId"
}

private enum class TopLevelTab(val route: String, val label: String, val icon: ImageVector) {
    HOME(Routes.HOME, "Home", Icons.Default.Home),
    TASKS(Routes.TASKS, "Tasks", Icons.Default.List),
    FOCUS(Routes.FOCUS, "Focus", Icons.Default.PlayArrow)
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
 * Bottom navigation with Home, Tasks and Focus, plus the secondary screens.
 * Requirements: 12.3
 */
@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = TopLevelTab.entries.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                            onClick = { navController.navigateToTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
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
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onNavigateToFocus = { navController.navigateToTab(Routes.FOCUS) },
                    onOpenTask = { navController.navigate(Routes.taskDetail(it)) },
                    onAddTask = { navController.navigate(Routes.addTask()) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.TASKS) {
                TasksScreen(
                    onOpenTask = { navController.navigate(Routes.taskDetail(it)) },
                    onEditTask = { navController.navigate(Routes.editTask(it)) },
                    onAddTask = { navController.navigate(Routes.addTask()) }
                )
            }
            composable(Routes.FOCUS) {
                FocusScreen(onGoHome = { navController.navigateToTab(Routes.HOME) })
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
                arguments = listOf(navArgument(AddEditTaskViewModel.TASK_ID_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) {
                AddEditTaskScreen(onDone = { navController.popBackStack() })
            }
        }
    }
}

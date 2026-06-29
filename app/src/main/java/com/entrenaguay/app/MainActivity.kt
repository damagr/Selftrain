package com.entrenaguay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.entrenaguay.app.ui.EntrenaGuayTheme
import com.entrenaguay.app.ui.exercises.ExerciseLibraryScreen
import com.entrenaguay.app.ui.history.HistoryScreen
import com.entrenaguay.app.ui.routines.RoutineEditScreen
import com.entrenaguay.app.ui.routines.RoutinesScreen
import com.entrenaguay.app.ui.settings.SettingsScreen
import com.entrenaguay.app.ui.train.TrainScreen
import dagger.hilt.android.AndroidEntryPoint

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EntrenaGuayTheme {
                EntrenaGuayMain()
            }
        }
    }
}

@Composable
fun EntrenaGuayMain() {
    val navController = rememberNavController()

    val bottomNavItems = listOf(
        BottomNavItem("routines", "Rutinas", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
        BottomNavItem("history", "Historial", Icons.Filled.History, Icons.Outlined.History),
        BottomNavItem("exercises", "Ejercicios", Icons.Filled.List, Icons.Outlined.List)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "routines",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("routines") {
                RoutinesScreen(
                    onStartWorkout = { routineId -> navController.navigate("train/$routineId") },
                    onEditRoutine = { routineId -> navController.navigate("routine_edit/$routineId") },
                    onSettings = { navController.navigate("settings") }
                )
            }
            composable("routine_edit/{routineId}", arguments = listOf(navArgument("routineId") { type = NavType.LongType })) { backStackEntry ->
                val routineId = backStackEntry.arguments?.getLong("routineId") ?: 0L
                RoutineEditScreen(routineId = routineId, onSaved = { navController.popBackStack() }, onDeleted = { navController.popBackStack() })
            }
            composable("train/{routineId}", arguments = listOf(navArgument("routineId") { type = NavType.LongType })) { backStackEntry ->
                val routineId = backStackEntry.arguments?.getLong("routineId") ?: 0L
                TrainScreen(routineId = routineId, onFinish = { navController.popBackStack() })
            }
            composable("history") {
                HistoryScreen(onSettings = { navController.navigate("settings") })
            }
            composable("exercises") {
                ExerciseLibraryScreen(onSettings = { navController.navigate("settings") })
            }
            composable("settings") {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

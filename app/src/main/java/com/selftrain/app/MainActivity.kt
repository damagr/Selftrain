package com.selftrain.app

import android.os.Bundle
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.selftrain.app.ui.MainViewModel
import com.selftrain.app.ui.SelfTrainTheme
import com.selftrain.app.ui.UpdateState
import com.selftrain.app.ui.exercises.ExerciseLibraryScreen
import com.selftrain.app.ui.history.HistoryScreen
import com.selftrain.app.ui.routines.ProgramDaysScreen
import com.selftrain.app.ui.routines.RoutineEditScreen
import com.selftrain.app.ui.routines.RoutinesScreen
import com.selftrain.app.ui.settings.SettingsScreen
import com.selftrain.app.ui.train.TrainScreen
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
            SelfTrainTheme {
                SelfTrainMain()
            }
        }
    }
}

@Composable
fun SelfTrainMain() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = hiltViewModel()
    val context = LocalContext.current

    // ponytail: one-time POST_NOTIFICATIONS ask on API 33+; minSdk 26 = no-op below
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted/denied handled by app behavior; no-op here */ }

    val bottomNavItems = listOf(
        BottomNavItem("routines", "Rutinas", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
        BottomNavItem("history", "Historial", Icons.Filled.History, Icons.Outlined.History),
        BottomNavItem("exercises", "Ejercicios", Icons.Filled.List, Icons.Outlined.List)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    // Update check on launch
    LaunchedEffect(Unit) {
        viewModel.checkForUpdate()
    }

    // Ask for notification permission on first launch (Android 13+)
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Watch for ready-to-install; handle permission via ActivityResult
    var pendingInstallFile by remember { mutableStateOf<File?>(null) }

    val installPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (!viewModel.needsInstallPermission()) {
            pendingInstallFile?.let { viewModel.installApk(it) }
        }
        pendingInstallFile = null
    }

    LaunchedEffect(viewModel.updateState) {
        if (viewModel.updateState is UpdateState.ReadyToInstall) {
            val file = (viewModel.updateState as UpdateState.ReadyToInstall).file
            if (viewModel.needsInstallPermission()) {
                pendingInstallFile = file
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                installPermissionLauncher.launch(intent)
            } else {
                viewModel.installApk(file)
            }
        }
    }

    // Show user messages (latest-version feedback, backup created, etc.)
    LaunchedEffect(viewModel.userMessage) {
        viewModel.userMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    val themePrefs = com.selftrain.app.util.rememberThemePreferences()
    val themeMode by themePrefs.mode.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                if (themeMode == com.selftrain.app.util.ThemeMode.MODERN) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 3.dp
                    ) {
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
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                } else {
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "routines",
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                when (themeMode) {
                    com.selftrain.app.util.ThemeMode.CLASSIC -> fadeIn(animationSpec = tween(200))
                    com.selftrain.app.util.ThemeMode.MODERN -> fadeIn(animationSpec = spring(dampingRatio = 0.8f)) + slideInHorizontally(
                        animationSpec = spring(dampingRatio = 0.8f),
                        initialOffsetX = { it / 4 }
                    )
                }
            },
            exitTransition = {
                when (themeMode) {
                    com.selftrain.app.util.ThemeMode.CLASSIC -> fadeOut(animationSpec = tween(200))
                    com.selftrain.app.util.ThemeMode.MODERN -> fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                        animationSpec = spring(dampingRatio = 0.8f),
                        targetOffsetX = { -it / 4 }
                    )
                }
            },
            popEnterTransition = {
                when (themeMode) {
                    com.selftrain.app.util.ThemeMode.CLASSIC -> fadeIn(animationSpec = tween(200))
                    com.selftrain.app.util.ThemeMode.MODERN -> fadeIn(animationSpec = spring(dampingRatio = 0.8f)) + slideInHorizontally(
                        animationSpec = spring(dampingRatio = 0.8f),
                        initialOffsetX = { -it / 4 }
                    )
                }
            },
            popExitTransition = {
                when (themeMode) {
                    com.selftrain.app.util.ThemeMode.CLASSIC -> fadeOut(animationSpec = tween(200))
                    com.selftrain.app.util.ThemeMode.MODERN -> fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                        animationSpec = spring(dampingRatio = 0.8f),
                        targetOffsetX = { it / 4 }
                    )
                }
            }
        ) {
            composable("routines") {
                RoutinesScreen(
                    onStartWorkout = { routineId -> navController.navigate("train/$routineId") },
                    onEditRoutine = { routineId -> navController.navigate("routine_edit/$routineId") },
                    onEnterProgram = { routineId -> navController.navigate("program/$routineId") },
                    onSettings = { navController.navigate("settings") },
                    onResumeWorkout = { routineId, workoutId ->
                        navController.navigate("train/$routineId?resumeWorkoutId=$workoutId")
                    }
                )
            }
            composable("program/{routineId}", arguments = listOf(navArgument("routineId") { type = NavType.LongType })) { backStackEntry ->
                val routineId = backStackEntry.arguments?.getLong("routineId") ?: 0L
                ProgramDaysScreen(
                    routineId = routineId,
                    onStartWorkout = { id -> navController.navigate("train/$id") },
                    onEditRoutine = { id -> navController.navigate("routine_edit/$id") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("routine_edit/{routineId}", arguments = listOf(navArgument("routineId") { type = NavType.LongType })) { backStackEntry ->
                val routineId = backStackEntry.arguments?.getLong("routineId") ?: 0L
                RoutineEditScreen(routineId = routineId, onSaved = { navController.popBackStack() }, onDeleted = { navController.popBackStack() })
            }
            composable(
                "train/{routineId}?resumeWorkoutId={resumeWorkoutId}",
                arguments = listOf(
                    navArgument("routineId") { type = NavType.LongType },
                    navArgument("resumeWorkoutId") { type = NavType.LongType; defaultValue = 0L }
                )
            ) { backStackEntry ->
                val routineId = backStackEntry.arguments?.getLong("routineId") ?: 0L
                val resumeWorkoutId = backStackEntry.arguments?.getLong("resumeWorkoutId") ?: 0L
                TrainScreen(
                    routineId = routineId,
                    resumeWorkoutId = if (resumeWorkoutId > 0) resumeWorkoutId else null,
                    onFinish = { navController.popBackStack() }
                )
            }
            composable("history") {
                HistoryScreen(onSettings = { navController.navigate("settings") })
            }
            composable("exercises") {
                ExerciseLibraryScreen(onSettings = { navController.navigate("settings") })
            }
            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onCheckUpdate = { viewModel.checkForUpdate(userInitiated = true) }
                )
            }
        }
    }

    // --- Update dialogs ---

    if (viewModel.showUpdateDialog && viewModel.updateState is UpdateState.Available) {
        val release = (viewModel.updateState as UpdateState.Available).release
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialog() },
            title = { Text("Nueva versión disponible") },
            text = {
                Text("Versión ${release.tagName} disponible.\n" +
                    "Se creará un backup de tus datos antes de actualizar. ¿Actualizar ahora?")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.startUpdate(release) }) {
                    Text("Actualizar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialog() }) {
                    Text("Ahora no")
                }
            }
        )
    }

    when (val state = viewModel.updateState) {
        is UpdateState.Downloading -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Descargando actualización…") },
                text = {
                    Column(Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("${state.progress}%")
                    }
                },
                confirmButton = {}
            )
        }
        is UpdateState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissError() },
                title = { Text("Error") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissError() }) {
                        Text("Vale")
                    }
                }
            )
        }
        else -> {}
    }
}

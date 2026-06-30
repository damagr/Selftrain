package com.selftrain.app.ui.routines

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.selftrain.app.data.model.Routine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramDaysScreen(
    routineId: Long,
    onStartWorkout: (Long) -> Unit,
    onEditRoutine: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: RoutinesViewModel = hiltViewModel()
) {
    val allRoutines by viewModel.routines.collectAsState()
    val parent = allRoutines.find { it.id == routineId }
    val children = allRoutines.filter { it.parentId == routineId }.sortedBy { it.order }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal),
                title = { Text(parent?.name ?: "Programa") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (children.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No hay días en este programa", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(Modifier.padding(padding)) {
                items(children, key = { it.id }) { child ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(child.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            IconButton(onClick = { onStartWorkout(child.id) }) {
                                Icon(Icons.Default.PlayArrow, "Empezar", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onEditRoutine(child.id) }) {
                                Icon(Icons.Default.Edit, "Editar")
                            }
                        }
                    }
                }
            }
        }
    }
}

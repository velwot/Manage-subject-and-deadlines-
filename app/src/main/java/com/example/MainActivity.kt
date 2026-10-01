package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CampusBottomBar
import com.example.ui.screens.*
import com.example.ui.theme.CampusMateTheme
import com.example.ui.viewmodel.CampusMateViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {
    private val viewModel: CampusMateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CampusMateTheme {
                CampusMateApp(viewModel)
            }
        }
    }
}

@Composable
fun CampusMateApp(viewModel: CampusMateViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Listen for messages emitted from viewModel
    LaunchedEffect(Unit) {
        viewModel.message.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Hardware back button navigation handling
    BackHandler(enabled = currentScreen != Screen.Home) {
        viewModel.navigateBack()
    }

    val showBottomBar = currentScreen !is Screen.SubjectDetail

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                CampusBottomBar(
                    currentScreen = currentScreen,
                    onNavigate = { viewModel.navigateTo(it) }
                )
            }
        }
    ) { innerPadding ->
        Modifier.padding(innerPadding).let { _ ->
            when (val screen = currentScreen) {
                is Screen.Home -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigate = { viewModel.navigateTo(it) }
                    )
                }
                is Screen.Subjects -> {
                    SubjectsScreen(
                        viewModel = viewModel,
                        onNavigate = { viewModel.navigateTo(it) }
                    )
                }
                is Screen.SubjectDetail -> {
                    SubjectDetailScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }
                is Screen.TodoList -> {
                    TodoListScreen(viewModel = viewModel)
                }
                is Screen.Alerts -> {
                    AlertsScreen(viewModel = viewModel)
                }
                is Screen.Calendar -> {
                    CalendarScreen(viewModel = viewModel)
                }
                is Screen.Ideas -> {
                    IdeasScreen(viewModel = viewModel)
                }
                is Screen.Settings -> {
                    SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}

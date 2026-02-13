package com.ricca.futacollector.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.ricca.futacollector.ui.screens.*
import com.ricca.futacollector.viewmodel.CollectionViewModel
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Settings

sealed class Screen(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Search : Screen("search", "Cerca", Icons.Default.Search)
    object Collection : Screen("collection", "Collezione", Icons.Default.Collections)
    object Settings : Screen("settings", "Impostazioni", Icons.Default.Settings)
}

@Composable
fun AppNavigation(
    darkTheme: Boolean,
    onDarkThemeToggle: (Boolean) -> Unit,
    collectionViewModel: CollectionViewModel // <- passato da MainActivity
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        collectionViewModel.uiEvents.collect { messaggio ->
            snackbarHostState.currentSnackbarData?.dismiss()
            val job = launch {
                snackbarHostState.showSnackbar(
                    message = messaggio,
                    duration = SnackbarDuration.Indefinite
                )
            }
            kotlinx.coroutines.delay(1300)
            snackbarHostState.currentSnackbarData?.dismiss()
            job.cancel()
        }
    }

    Scaffold(
        snackbarHost = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 180.dp)
            ) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.TopCenter)
                ) { data ->
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(0.9f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = data.visuals.message,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        bottomBar = {
            BottomBar(navController = navController, viewModel = collectionViewModel)
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(navController, collectionViewModel)
            }
            composable(Screen.Search.route) {
                SearchScreen(viewModel = collectionViewModel)
            }
            composable(Screen.Collection.route) {
                CollectionScreen(
                    viewModel = collectionViewModel,
                    onNavigateToSearch = {
                        navController.navigate(Screen.Search.route) {
                            popUpTo(Screen.Home.route)
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    darkThemeEnabled = darkTheme,
                    onDarkThemeToggle = onDarkThemeToggle,
                    viewModel = collectionViewModel
                )
            }
        }
    }
}

@Composable
fun BottomBar(
    navController: NavHostController,
    viewModel: CollectionViewModel
) {
    val items = listOf(
        Screen.Home,
        Screen.Search,
        Screen.Collection,
        Screen.Settings
    )

    NavigationBar {
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

        items.forEach { screen ->
            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = {
                    if (screen == Screen.Search && currentRoute == Screen.Search.route) {
                        viewModel.resetSearchOnTabReselect()
                    } else {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) }
            )
        }
    }
}

package com.ricca.futacollector.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.Modifier
import com.ricca.futacollector.ui.screens.*
import androidx.navigation.NavHostController
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ricca.futacollector.viewmodel.CollectionViewModel
import kotlinx.coroutines.launch


sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Search : Screen("search", "Cerca", Icons.Default.Search)
    object Collection : Screen("collection", "Collezione", Icons.Default.Collections)
    object Settings : Screen("settings", "Impostazioni", Icons.Default.Settings)
}

@Composable
fun AppNavigation(
    darkTheme: Boolean,
    onDarkThemeToggle: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    // 1. Inizializziamo il ViewModel una sola volta qui
    val snackbarHostState = remember { SnackbarHostState() }

    val collectionViewModel: CollectionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    LaunchedEffect(Unit) {
        collectionViewModel.uiEvents.collect { messaggio ->
            // 1. Chiude quello vecchio istantaneamente
            snackbarHostState.currentSnackbarData?.dismiss()

            // 2. Lancia quello nuovo in una coroutine separata per poterla cancellare
            val job = launch {
                snackbarHostState.showSnackbar(
                    message = messaggio,
                    duration = SnackbarDuration.Indefinite // Lo teniamo "infinito" per controllarlo noi
                )
            }

            // 3. Aspetta il tempo che vuoi (es. 1500 millisecondi = 1.5 secondi)
            kotlinx.coroutines.delay(1300)

            // 4. Chiude lo snackbar forzatamente
            snackbarHostState.currentSnackbarData?.dismiss()
            job.cancel()
        }
    }

    Scaffold(
        snackbarHost = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // statusBarsPadding() a volte fa i capricci, aggiungiamo un padding fisso generoso
                    .padding(top = 180.dp)
            ) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.TopCenter)
                ) { data ->
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(0.9f), // Non occupa tutto lo schermo, sembra più una notifica
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
            BottomBar(navController)
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(navController)
            }
            // 3. PASSAGGIO FONDAMENTALE: Passiamo il viewModel alle schermate
            composable(Screen.Search.route) {
                SearchScreen(viewModel = collectionViewModel)
            }
            composable(Screen.Collection.route) {
                CollectionScreen(
                    viewModel = collectionViewModel,
                    onNavigateToSearch = {
                        // Quando premi il tasto +, navighiamo alla rotta della ricerca
                        navController.navigate(Screen.Search.route) {
                            // Opzionale: evita di accumulare copie della ricerca nello stack
                            popUpTo(Screen.Home.route)
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    darkThemeEnabled = darkTheme,
                    onDarkThemeToggle = onDarkThemeToggle
                )
            }
        }
    }
}

@Composable
fun BottomBar(navController: NavHostController) {

    val items = listOf(
        Screen.Home,
        Screen.Search,
        Screen.Collection,
        Screen.Settings
    )

    NavigationBar {

        val currentRoute =
            navController.currentBackStackEntryAsState().value?.destination?.route

        items.forEach { screen ->

            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route)
                },
                icon = {
                    Icon(screen.icon, contentDescription = screen.label)
                },
                label = { Text(screen.label) }
            )
        }
    }
}

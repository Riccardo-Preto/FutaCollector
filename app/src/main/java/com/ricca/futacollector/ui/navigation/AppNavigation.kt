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
import androidx.compose.material.icons.filled.Style
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ricca.futacollector.viewmodel.DeckViewModel
import com.ricca.futacollector.viewmodel.DeckViewModelFactory
import com.ricca.futacollector.data.AppDatabase
import androidx.compose.material.icons.filled.*
import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screen(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Search : Screen("search", "Cerca", Icons.Default.Search)
    object Collection : Screen("collection", "Collezione", Icons.Default.Collections)
    object Settings : Screen("settings", "Menu", Icons.Default.Settings)
    object DeckList : Screen("deck_list", "Mazzi", Icons.Default.Style)
}

@Composable
fun AppNavigation(
    darkTheme: Boolean,
    onDarkThemeToggle: (Boolean) -> Unit,
    collectionViewModel: CollectionViewModel
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- INIZIALIZZAZIONE DECK VIEWMODEL ---
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val deckDao = remember { db.deckDao() }
    val cardDao = remember { db.cardDao() } // Recuperiamo il cardDao

    val deckViewModel: DeckViewModel = viewModel(
        factory = DeckViewModelFactory(deckDao, cardDao) // Li passiamo entrambi qui
    )

    // ... (LaunchedEffect e Scaffold rimangono uguali) ...

    Scaffold(
        // ... (snackbarHost uguale) ...
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
            // --- CORREZIONE QUI ---
            composable(Screen.DeckList.route) {
                DeckListScreen(
                    navController = navController,
                    viewModel = deckViewModel,
                    onNavigateToDetail = { id, name ->
                        navController.navigate("deck_detail/$id/$name")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "select_leader/{deckName}",
                arguments = listOf(navArgument("deckName") { type = NavType.StringType })
            ) { backStackEntry ->
                val deckName = backStackEntry.arguments?.getString("deckName") ?: "Nuovo Mazzo"

                LeaderSelectionScreen(
                    collectionViewModel = collectionViewModel,
                    onBack = { navController.popBackStack() },
                    onLeaderSelected = { leaderId ->
                        // 1. Creiamo il mazzo nel DB
                        deckViewModel.createDeck(deckName, leaderId)

                        // 2. Torniamo alla lista mazzi (rimuovendo la selezione leader dallo stack)
                        navController.popBackStack(Screen.DeckList.route, inclusive = false)
                    }
                )
            }

            composable(
                route = "deck_detail/{deckId}/{deckName}",
                arguments = listOf(
                    navArgument("deckId") { type = NavType.IntType },
                    navArgument("deckName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getInt("deckId") ?: 0
                val deckName = backStackEntry.arguments?.getString("deckName") ?: ""

                DeckDetailScreen(
                    deckId = deckId,
                    deckName = deckName,
                    viewModel = deckViewModel,
                    onBack = { navController.popBackStack() }
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
        Screen.DeckList,
        Screen.Settings
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val currentRoute = currentDestination?.route

        items.forEach { screen ->
            // Verifichiamo se la tab è selezionata (inclusi i sotto-percorsi per i Mazzi)
            val isSelected = currentRoute == screen.route ||
                    (screen == Screen.DeckList && currentRoute?.contains("select_leader") == true)

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentRoute == screen.route) {
                        // Siamo già nella "Home" della Tab:
                        // Se è Search, potresti voler resettare la ricerca
                        if (screen == Screen.Search) {
                            viewModel.clearSearch()
                        }
                    } else if (isSelected) {
                        // Siamo in una sotto-pagina (es: Selezione Leader): torna alla root della Tab
                        navController.popBackStack(screen.route, inclusive = false)
                    } else {
                        // Navigazione standard tra Tab diverse
                        navController.navigate(screen.route) {
                            // Questo evita di accumulare una pila infinita di destinazioni
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
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
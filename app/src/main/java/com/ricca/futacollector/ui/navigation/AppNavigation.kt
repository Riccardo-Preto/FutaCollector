package com.ricca.futacollector.ui.navigation

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
    onDarkThemeToggle: (Boolean) -> Unit) {

    val navController = rememberNavController()

    Scaffold(
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
            composable(Screen.Search.route) { SearchScreen() }
            composable(Screen.Collection.route) { CollectionScreen() }
            composable(Screen.Settings.route) { SettingsScreen(
                darkThemeEnabled = darkTheme,
                onDarkThemeToggle = onDarkThemeToggle
            ) }
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

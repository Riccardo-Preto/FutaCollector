package com.ricca.futacollector

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.ricca.futacollector.ui.theme.FutaCollectorTheme
import com.ricca.futacollector.ui.navigation.AppNavigation


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FutaCollectorTheme {
                AppNavigation()
            }
        }
    }
}

/*
@Composable
fun MainScreen() {

    var showSearch by remember { mutableStateOf(false) }

    if (showSearch) {
        SearchScreen(
            onBack = { showSearch = false }
        )
    } else {
        HomeScreen(
            onAddCardClick = { showSearch = true }
        )
    }
}

@Composable
fun HomeScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Home")
    }
}
@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Search,
        BottomNavItem.Collection,
        BottomNavItem.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = false,
                        onClick = {
                            navController.navigate(item.route)
                        }
                    )
                }
            }
        }
    ) { padding ->

        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(padding)
        ) {

            composable("home") { HomeScreen() }
            composable("search") { SearchScreen() }
            composable("collection") { CollectionScreen() }
            composable("settings") { SettingsScreen() }

        }
    }
}


sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem("home", "Home", Icons.Default.Home)
    object Search : BottomNavItem("search", "Cerca", Icons.Default.Search)
    object Collection : BottomNavItem("collection", "Collezione", Icons.Default.List)
    object Settings : BottomNavItem("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun SearchScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Cerca carta")
    }
}

@Composable
fun CollectionScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("La mia collezione")
    }
}

@Composable
fun SettingsScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Impostazioni")
    }
}


@Composable
fun CardGrid(cards: List<ApiCard>) {

    LazyVerticalGrid(
        columns = GridCells.Adaptive(140.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {

        items(cards) { card ->
            CardItem(card)
        }
    }
}

@Composable
fun CardItem(card: ApiCard) {

    Column(
        modifier = Modifier.padding(4.dp)
    ) {

        AsyncImage(
            model = card.card_image,
            contentDescription = card.card_name,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = card.card_name,
            maxLines = 1
        )
    }
}
*/
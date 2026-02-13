package com.ricca.futacollector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ricca.futacollector.ui.navigation.AppNavigation
import com.ricca.futacollector.ui.theme.FutaCollectorTheme
import com.ricca.futacollector.viewmodel.CollectionViewModel
import com.ricca.futacollector.viewmodel.CollectionViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            // --- STATO GLOBALE TEMA ---
            var isDarkTheme by remember { mutableStateOf(false) }

            // --- APP THEME ---
            FutaCollectorTheme(darkTheme = isDarkTheme) {

                // --- SYSTEM UI CONTROLLER ---
                val systemUiController = rememberSystemUiController()
                val useDarkIcons = !isDarkTheme

                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = Color.Transparent, // oppure MaterialTheme.colorScheme.surface
                        darkIcons = useDarkIcons
                    )
                }

                // --- DAO & VIEWMODEL ---
                val cardDao = com.ricca.futacollector.data.AppDatabase.getDatabase(applicationContext).cardDao()
                val collectionViewModel: CollectionViewModel =
                    androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = CollectionViewModelFactory(cardDao)
                    )

                // --- NAVIGATION ---
                AppNavigation(
                    darkTheme = isDarkTheme,
                    onDarkThemeToggle = { isDarkTheme = it },
                    collectionViewModel = collectionViewModel // passiamo il ViewModel
                )
            }
        }
    }
}

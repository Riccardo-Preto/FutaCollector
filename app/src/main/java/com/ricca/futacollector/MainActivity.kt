package com.ricca.futacollector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ricca.futacollector.data.AppDatabase
import com.ricca.futacollector.ui.navigation.AppNavigation
import com.ricca.futacollector.ui.theme.FutaCollectorTheme
import com.ricca.futacollector.viewmodel.CollectionViewModel
import com.ricca.futacollector.viewmodel.CollectionViewModelFactory
import android.content.Context

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("futa_prefs", Context.MODE_PRIVATE)

        setContent {
            var isDarkTheme by remember {
                mutableStateOf(prefs.getBoolean("dark_theme", false))
            }

            FutaCollectorTheme(darkTheme = isDarkTheme) {
                val systemUiController = rememberSystemUiController()
                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = Color.Transparent,
                        darkIcons = !isDarkTheme
                    )
                }

                val cardDao = AppDatabase.getDatabase(applicationContext).cardDao()
                val collectionViewModel: CollectionViewModel =
                    androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = CollectionViewModelFactory(application, cardDao)
                    )

                AppNavigation(
                    darkTheme = isDarkTheme,
                    onDarkThemeToggle = { newValue ->
                        isDarkTheme = newValue
                        prefs.edit().putBoolean("dark_theme", newValue).apply()
                    },
                    collectionViewModel = collectionViewModel
                )
            }
        }
    }
}
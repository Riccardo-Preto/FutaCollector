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
            // 1️⃣ Stato globale del tema
            var isDarkTheme by remember { mutableStateOf(false) }

            // 2️⃣ AppTheme con tema dinamico
            FutaCollectorTheme(darkTheme = isDarkTheme) {
                // 3️⃣ Passiamo lo stato a AppNavigation
                AppNavigation(
                    darkTheme = isDarkTheme,
                    onDarkThemeToggle = { isDarkTheme = it } // callback per lo switch
                )
            }
        }
    }
}

package com.ricca.futacollector.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController) {

    Button(onClick = {
        navController.navigate("search")
    }) {
        Text("Aggiungi carta")
    }
}

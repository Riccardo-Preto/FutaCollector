package com.ricca.futacollector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ricca.futacollector.CardItemView
import com.ricca.futacollector.ui.navigation.Screen
import com.ricca.futacollector.viewmodel.CollectionViewModel

@Composable
fun HomeScreen(navController: NavHostController, viewModel: CollectionViewModel = viewModel()) {
    val collection by viewModel.collectionCards.collectAsState()

    // Calcolo statistiche corretto (EUR)
    val totalCards = collection.sumOf { it.count }
    val totalPriceEuro = collection.sumOf { it.card.marketPrice * it.count } * 0.92

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // --- 1. HEADER DASHBOARD ---
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "La tua Dashboard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Gestisci la tua collezione e i tuoi mazzi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- 2. STATISTICHE VELOCI ---
        item {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Carte Totali",
                    value = "$totalCards",
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
                StatCard(
                    label = "Valore Stimato",
                    value = "€%.2f".format(totalPriceEuro),
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            }
        }

        // --- 3. BANNER PRINCIPALI ---
        item {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NavigationBanner(
                    title = "La tua Collezione",
                    subtitle = "Sfoglia e gestisci le tue carte",
                    icon = Icons.Default.Collections,
                    color = Color(0xFF42A5F5),
                    onClick = {
                        navController.navigate(Screen.Collection.route) {
                            // Torna alla home prima di navigare per non creare una pila infinita
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            // Evita di riaprire la stessa pagina se ci sei già sopra
                            launchSingleTop = true
                            // Ripristina lo stato (es. se avevi scrollato)
                            restoreState = true
                        }
                    }
                )

                NavigationBanner(
                    title = "Deck Builder",
                    subtitle = "Costruisci e analizza i tuoi mazzi",
                    icon = Icons.Default.Style,
                    color = Color(0xFFEF5350),
                    onClick = {
                        navController.navigate(Screen.DeckList.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }

        // --- 4. ULTIME AGGIUNTE (Versione Top 3 Pulita) ---
        item {
            Text(
                text = "Ultime aggiunte",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        item {
            val lastAdded = collection.takeLast(3).reversed()

            if (lastAdded.isEmpty()) {
                // ... placeholder se vuoto ...
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    lastAdded.forEach { item ->
                        Box(modifier = Modifier.weight(1f)) {
                            CardItemView(
                                card = item.card,
                                count = item.count,
                                onClick = {
                                    // Opzionale: puoi navigare al dettaglio anche da qui
                                }
                            )
                        }
                    }
                    repeat(3 - lastAdded.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationBanner(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = color.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = color
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, containerColor: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
    }
}

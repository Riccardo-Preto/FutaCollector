package com.ricca.futacollector.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ricca.futacollector.CardItemView
import com.ricca.futacollector.data.Card
import com.ricca.futacollector.ui.CardDetailMode
import com.ricca.futacollector.viewmodel.CollectionViewModel

@Composable
fun CollectionScreen(
    viewModel: CollectionViewModel,
    onNavigateToSearch: () -> Unit
) {
    val collection by viewModel.collectionCards.collectAsState()
    var selectedCard by remember { mutableStateOf<Card?>(null) }

    // --- LOGICA DI CALCOLO AGGIORNATA ---
    val totalCards = collection.sumOf { it.count }

    // Convertiamo ogni prezzo in Euro (0.92) prima di sommare
    val totalValueUsd = collection.sumOf { it.card.marketPrice * it.count }
    val totalValueEuro = totalValueUsd * 0.92

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "La tua collezione",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // --- BOX RIASSUNTO ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Carte totali", style = MaterialTheme.typography.labelMedium)
                    Text("$totalCards", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Valore stimato", style = MaterialTheme.typography.labelMedium)
                    Text(
                        // Usiamo il valore convertito
                        "€ ${String.format("%.2f", totalValueEuro)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (collection.isEmpty()) {
            EmptyCollectionPlaceholder()
        } else {
            // --- GRIGLIA DI IMMAGINI ---
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(collection, key = { it.card.id }) { item ->
                    CardItemView(
                        card = item.card,
                        count = item.count,
                        onClick = { selectedCard = item.card }
                    )
                }
            }
        }
    }

    // --- DIALOG DETTAGLIO ---
    selectedCard?.let { card ->
        val currentCount = collection.find { it.card.id == card.id }?.count ?: 0

        Dialog(
            onDismissRequest = { selectedCard = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CardDetailScreen(
                    card = card,
                    mode = CardDetailMode.Collection(ownedCopies = currentCount),
                    onAddToCollection = { viewModel.addCardToCollection(card) },
                    onRemoveFromCollection = {
                        if (currentCount <= 1) {
                            selectedCard = null
                        }
                        viewModel.removeCardFromCollection(card)
                    },
                    onDismiss = { selectedCard = null }
                )
            }
        }
    }
}

@Composable
fun EmptyCollectionPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("¯\\_(ツ)_/¯", style = MaterialTheme.typography.displayMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("La tua collezione è vuota", style = MaterialTheme.typography.headlineSmall)
    }
}
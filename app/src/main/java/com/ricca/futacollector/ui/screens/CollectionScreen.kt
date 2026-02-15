package com.ricca.futacollector.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val gridState = viewModel.collectionGridState

    // --- LOGICA DI CALCOLO ---
    val totalCards = collection.sumOf { it.count }
    val totalValueEuro = collection.sumOf { it.card.marketPrice * it.count } * 0.92

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToSearch,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black // Uniformato a DeckList
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi carte")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = "La tua collezione",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black, // Uniformato a FontWeight.Black
                modifier = Modifier.padding(16.dp)
            )

            if (collection.isEmpty()) {
                // --- STATO VUOTO PERFETTAMENTE ALLINEATO ---
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Collezione vuota",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Clicca sul tasto + per aggiungere una carta!",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                // --- BOX RIASSUNTO ---
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
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
                                "€ ${String.format("%.2f", totalValueEuro)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // --- GRIGLIA DI IMMAGINI ---
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
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
    }

    // --- DIALOG DETTAGLIO (Invariato) ---
    selectedCard?.let { card ->
        val currentCount = collection.find { it.card.id == card.id }?.count ?: 0
        Dialog(
            onDismissRequest = { selectedCard = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CardDetailScreen(
                    card = card,
                    mode = CardDetailMode.Collection(ownedCopies = currentCount),
                    onAddToCollection = { viewModel.addCardToCollection(card) },
                    onRemoveFromCollection = {
                        if (currentCount <= 1) selectedCard = null
                        viewModel.removeCardFromCollection(card)
                    },
                    onDismiss = { selectedCard = null }
                )
            }
        }
    }
}

@Composable
fun EmptyCollectionPlaceholder(onAction: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Collections, // Icona della Tab Collezione
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Collezione vuota",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Inizia a comporre il tuo tesoro cercando le tue carte!",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Un bottone extra al centro non guasta mai
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Vai alla ricerca")
            }
        }
    }
}
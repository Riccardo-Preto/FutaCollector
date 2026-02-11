package com.ricca.futacollector.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ricca.futacollector.CardItemView
import com.ricca.futacollector.data.Card
import com.ricca.futacollector.ui.CardDetailMode
import com.ricca.futacollector.viewmodel.CardWithCount
import com.ricca.futacollector.viewmodel.CollectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    viewModel: CollectionViewModel,
    onNavigateToSearch: () -> Unit
) {
    val collectionByCount by viewModel.collectionCards.collectAsState()
    var selectedItemForDetail by remember { mutableStateOf<CardWithCount?>(null) }
    var sortMode by remember { mutableStateOf(0) }

    // Ordinamento semplificato: marketPrice è già Double!
    val sortedList = remember(collectionByCount, sortMode) {
        when (sortMode) {
            0 -> collectionByCount.sortedBy { it.card.name }
            1 -> collectionByCount.sortedByDescending { it.card.marketPrice }
            else -> collectionByCount
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "La mia collezione",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1).sp
                        )

                        val totalCards = collectionByCount.sumOf { it.count }
                        val totalPrice = collectionByCount.sumOf { it.card.marketPrice * it.count }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$totalCards carte",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(" • ", color = MaterialTheme.colorScheme.secondary)
                            Text(
                                text = "Valore: €${String.format("%.2f", totalPrice)}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                modifier = Modifier.height(110.dp),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToSearch,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Cerca")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (collectionByCount.isEmpty()) {
                EmptyCollectionPlaceholder()
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Chip di ordinamento
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = sortMode == 0,
                            onClick = { sortMode = 0 },
                            label = { Text("Nome A-Z") }
                        )
                        FilterChip(
                            selected = sortMode == 1,
                            onClick = { sortMode = 1 },
                            label = { Text("Valore €€€") }
                        )
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(sortedList) { item ->
                            CardItemView(
                                card = item.card,
                                count = item.count,
                                onClick = { selectedItemForDetail = item }
                            )
                        }
                    }
                }
            }
        }

        // POPUP DETTAGLIO
        selectedItemForDetail?.let { selectedItem ->
            // Usiamo solo l'ID per il conteggio
            val count by viewModel
                .getCardCount(selectedItem.card.id)
                .collectAsState(initial = selectedItem.count)

            if (count > 0) {
                Dialog(
                    onDismissRequest = { selectedItemForDetail = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CardDetailScreen(
                            card = selectedItem.card, // Passiamo direttamente la Card del DB
                            mode = CardDetailMode.Collection(count),
                            onAddToCollection = { viewModel.addCardToCollection(selectedItem.card) },
                            onRemoveFromCollection = { viewModel.removeCardFromCollection(selectedItem.card.id) }
                        )
                    }
                }
            } else {
                selectedItemForDetail = null
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
        Text(text = "¯\\_(ツ)_/¯", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "La tua collezione è vuota", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.outline)
    }
}
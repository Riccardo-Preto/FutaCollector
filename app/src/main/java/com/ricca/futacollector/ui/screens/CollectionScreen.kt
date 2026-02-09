package com.ricca.futacollector.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.ricca.futacollector.ApiCard
import com.ricca.futacollector.RetrofitInstance
import kotlinx.coroutines.launch
import com.ricca.futacollector.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

    val sortedList = remember(collectionByCount, sortMode) {
        when (sortMode) {
            0 -> collectionByCount.sortedBy { it.card.name }
            1 -> collectionByCount.sortedByDescending { it.card.marketPrice.toDoubleOrNull() ?: 0.0 }
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
                        val totalPrice = collectionByCount.sumOf { item ->
                            val price = item.card.marketPrice.toDoubleOrNull() ?: 0.0
                            price * item.count
                        }

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
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (collectionByCount.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "¯\\_(ツ)_/¯", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Ancora niente :(", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // RIGA DEI FILTRI
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

                    // GRIGLIA
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(2.dp),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
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

        // POPUP DETTAGLIO (Fuori dal Box, dentro il padding dello Scaffold)
        selectedItemForDetail?.let { selectedItem ->
            val count by viewModel
                .getCardCount(selectedItem.card.id, selectedItem.card.image)
                .collectAsState(initial = selectedItem.count)

            if (count > 0) {
                val dbCard = selectedItem.card
                val apiCardEquivalent = ApiCard(
                    card_set_id = dbCard.id,
                    card_name = dbCard.name,
                    card_image = dbCard.image,
                    set_name = dbCard.setName,
                    inventory_price = dbCard.inventoryPrice.toDoubleOrNull() ?: 0.0,
                    market_price = dbCard.marketPrice.toDoubleOrNull() ?: 0.0
                )

                Dialog(
                    onDismissRequest = { selectedItemForDetail = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CardDetailScreen(
                            card = apiCardEquivalent,
                            mode = CardDetailMode.Collection(count),
                            onAddToCollection = { viewModel.addCardToCollection(apiCardEquivalent) },
                            onRemoveFromCollection = { viewModel.removeCardFromCollection(dbCard.id, dbCard.image) }
                        )
                    }
                }
            } else {
                selectedItemForDetail = null
            }
        }
    }
}
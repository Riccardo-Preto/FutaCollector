package com.ricca.futacollector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.ricca.futacollector.CardItemView
import com.ricca.futacollector.data.AppConstants
import com.ricca.futacollector.data.Card
import com.ricca.futacollector.ui.CardDetailMode
import com.ricca.futacollector.viewmodel.CollectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
    val totalValueEuro = collection.sumOf { it.card.marketPrice * it.count } * AppConstants.CONVERSION_RATE

    val wishlistGrouped by viewModel.wishlistCards.collectAsState() // <--- Prendi i dati dal ViewModel
    var showWishlist by remember { mutableStateOf(false) } // <--- Stato per il Dialog

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToSearch,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi carta")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "La tua collezione",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )

                // Pulsante cuore più stiloso
                IconButton(
                    onClick = { showWishlist = true },
                    modifier = Modifier
                        .background(
                            color = Color(0xFFEF5350).copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Wishlist",
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

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

    // --- DIALOG WISHLIST ---
    if (showWishlist) {
        WishlistDialog(
            groupedItems = wishlistGrouped,
            onDismiss = { showWishlist = false },
            onRemove = { cardId -> viewModel.removeFromWishlist(cardId) }
        )
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

data class WishlistItem(
    val card: Card,
    val quantity: Int,
    val reason: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistDialog(
    groupedItems: Map<String, List<WishlistItem>>,
    onDismiss: () -> Unit,
    onRemove: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("La mia Wishlist", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                    }
                )
            }
        ) { padding ->
            if (groupedItems.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("La tua lista dei desideri è vuota 🌟", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    groupedItems.forEach { (reason, items) ->
                        item {
                            Text(
                                text = reason.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black
                            )
                            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                        }

                        items(items) { item ->
                            WishlistRow(item, onRemove)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WishlistRow(item: WishlistItem, onRemove: (String) -> Unit) {
    val priceEur = (item.card.marketPrice * AppConstants.CONVERSION_RATE) * item.quantity

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mini immagine
        Card(shape = RoundedCornerShape(4.dp), modifier = Modifier.size(50.dp, 70.dp)) {
            AsyncImage(
                model = "file:///android_asset/immagini_ottimizzate/${item.card.image}",
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(item.card.name ?: "Unknown", fontWeight = FontWeight.Bold, maxLines = 1)
            Text("x${item.quantity} - €${"%.2f".format(priceEur)}", style = MaterialTheme.typography.bodySmall)
        }

        IconButton(onClick = { onRemove(item.card.id) }) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray)
        }
    }
}
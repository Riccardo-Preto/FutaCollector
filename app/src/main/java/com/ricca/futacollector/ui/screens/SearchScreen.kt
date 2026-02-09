package com.ricca.futacollector.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ricca.futacollector.ui.CardDetailMode
import com.ricca.futacollector.viewmodel.CollectionViewModel


@Composable
fun SearchScreen(
    viewModel: CollectionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {

    var searchQuery by remember { mutableStateOf("") }
    var cards by remember { mutableStateOf<List<ApiCard>>(emptyList()) }
    var sets by remember { mutableStateOf<List<CardSet>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()

    // Recupera la lista dei set appena si apre la schermata
    LaunchedEffect(Unit) {
        try {
            sets = RetrofitInstance.api.getAllSets()
        } catch (e: Exception) {
            e.printStackTrace()
            sets = emptyList()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {

        // ---------- Barra di ricerca ----------
        TextField(
            value = searchQuery,
            onValueChange = { query ->
                searchQuery = query

                // Aggiorna risultati
                coroutineScope.launch {
                    if (query.isNotBlank()) {
                        try {
                            cards = RetrofitInstance.api.getFilteredCards(query)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            cards = emptyList()
                        }
                    } else {
                        cards = emptyList()
                    }
                }
            },
            placeholder = { Text("Cerca carta...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = Color.Transparent,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (searchQuery.isBlank()) {
            // ---------- Lista set dall'API ----------
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(sets) { set ->
                    Text(
                        text = "${set.card_id}: ${set.name}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                coroutineScope.launch {
                                    try {
                                        cards = RetrofitInstance.api.getCardsBySet(set.card_id)
                                            .sortedBy { it.card_set_id } // ordina per numero seriale
                                        searchQuery = set.name // opzionale, puoi anche lasciare vuoto
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                    )
                }
            }

        } else {
            // ---------- Griglia risultati ricerca ----------
            var selectedCard by remember { mutableStateOf<ApiCard?>(null) }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(cards) { card -> // 'card' qui è la tua ApiCard

                    // 1. Creiamo l'oggetto Card compatibile con il componente
                    val cardPerGrafica = com.ricca.futacollector.data.Card(
                        id = card.card_set_id,
                        name = card.card_name,
                        image = card.card_image ?: "",
                        setName = card.set_name ?: "",
                        inventoryPrice = card.inventory_price.toString(),
                        marketPrice = card.market_price.toString(),
                        dateAdded = 0 // Valore fittizio, non serve per la visualizzazione
                    )

                    // 2. Usiamo il componente centralizzato
                    CardItemView(
                        card = cardPerGrafica,
                        count = 0, // Nella ricerca non vogliamo il badge
                        onClick = { selectedCard = card } // Apriamo il dettaglio
                    )
                }
            }

// ---------- Mostra CardDetailScreen in overlay se c'è una carta selezionata ----------
            selectedCard?.let { card ->
                Dialog(
                    onDismissRequest = { selectedCard = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    CardDetailScreen(
                        card = card,
                        mode = CardDetailMode.Search,
                        onAddToCollection = {
                            viewModel.addCardToCollection(card)
                            selectedCard = null
                        }
                    )

                }
            }
        }
    }
}


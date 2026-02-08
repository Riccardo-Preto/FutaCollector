package com.ricca.futacollector.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment


@Composable
fun SearchScreen() {

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
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (searchQuery.isBlank()) {
            // ---------- Lista set dall'API ----------
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // permette alla LazyColumn di occupare spazio corretto
            ) {
                items(sets) { set ->
                    Text(
                        text = "${set.card_id}: ${set.name}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { /* per ora niente funzionalità */ }
                    )
                }
            }
        } else {
            // ---------- Griglia risultati ricerca ----------
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), // 2 colonne per immagini più grandi
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(cards) { card ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth()
                            .aspectRatio(0.7f)
                    ) {
                        if (!card.card_image.isNullOrEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(card.card_image),
                                contentDescription = card.card_name,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Placeholder con nome e codice
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${card.card_name}\n${card.card_name}", // o usa card_id se vuoi
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


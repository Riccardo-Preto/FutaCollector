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
                items(cards) { card ->
                    Column(
                        modifier = Modifier
                            .padding(6.dp)
                            .fillMaxWidth()
                            // Cornice esterna principale scura e più spessa
                            .border(
                                width = 2.dp, // Più spessa
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), // Più scura
                                shape = RoundedCornerShape(10.dp)
                            )
                            .shadow(2.dp, shape = RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp)) // Importante: taglia tutto ciò che esce
                            .clickable { selectedCard = card },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. BOX IMMAGINE: Tocca i bordi sopra e ai lati
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.7f)
                        ) {
                            if (!card.card_image.isNullOrEmpty()) {
                                Image(
                                    painter = rememberAsyncImagePainter(card.card_image),
                                    contentDescription = card.card_name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize().background(Color.Gray.copy(alpha = 0.2f)))
                            }
                        }

                        // 2. LINEA DI SEPARAZIONE SCURA (La base della cornice dell'immagine)
                        HorizontalDivider(
                            thickness = 2.dp, // Stesso spessore della cornice esterna
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )

                        // 3. BOX TESTO
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)), // Leggero stacco di colore
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = card.card_name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = card.card_set_id,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
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
                        onAddToCollection = {
                            viewModel.addCardToCollection(card) // CHIAMA IL MAGGIORDOMO!
                            selectedCard = null // Chiude il popup dopo aver aggiunto
                        }
                    )
                }
            }
        }
    }
}


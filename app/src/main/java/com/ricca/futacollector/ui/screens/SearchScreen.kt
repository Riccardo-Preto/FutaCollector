package com.ricca.futacollector.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.ricca.futacollector.data.Card
import com.ricca.futacollector.data.CardSetEntity
import com.ricca.futacollector.ui.CardDetailMode
import com.ricca.futacollector.viewmodel.CollectionViewModel
import com.ricca.futacollector.CardItemView
import com.ricca.futacollector.ui.screens.CardDetailScreen
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    viewModel: CollectionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }

    val cards by viewModel.searchResults.collectAsState()
    val sets by viewModel.allSets.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {

        // Barra di ricerca
        TextField(
            value = searchQuery,
            onValueChange = { query ->
                searchQuery = query
                viewModel.searchCards(query)
            },
            placeholder = { Text("Cerca nel database...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Se la barra è vuota E non ci sono risultati da un set cliccato, mostra i Banner
        if (searchQuery.isBlank() && cards.isEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val reversedSets = sets.reversed()

                items(reversedSets) { set ->
                    // FIX: setId -> id | name -> nome
                    SetBannerItem(set = set) {
                        viewModel.getCardsFromSet(set.id)
                        // Invece di scrivere nella barra, lasciamo che la lista 'cards' si popoli
                        // Se vuoi che la UI passi alla griglia, potresti usare uno stato "isSearching"
                        searchQuery = " " // Trucco veloce: uno spazio per attivare la griglia
                    }
                }
            }
        } else {
            // GRIGLIA CARTE
            var selectedCard by remember { mutableStateOf<Card?>(null) }

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (cards.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nessuna carta trovata", color = Color.Gray)
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(cards) { card ->
                        CardItemView(
                            card = card,
                            count = 0,
                            onClick = { selectedCard = card }
                        )
                    }
                }

                if (gridState.firstVisibleItemIndex > 0) {
                    FloatingActionButton(
                        onClick = { coroutineScope.launch { gridState.animateScrollToItem(0) } },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Torna su")
                    }
                }
            }

            // Overlay Dettaglio
            selectedCard?.let { card ->
                Dialog(
                    onDismissRequest = { selectedCard = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CardDetailScreen(
                            card = card,
                            mode = CardDetailMode.Search,
                            onAddToCollection = {
                                viewModel.addCardToCollection(card)
                                selectedCard = null
                            },
                            onRemoveFromCollection = { selectedCard = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SetBannerItem(set: CardSetEntity, onClick: () -> Unit) {

    // Costruiamo il percorso asset come fai per le carte
    val imageModel = set.coverImage?.let {
        "file:///android_asset/$it"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 12.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.DarkGray)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = set.id,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = set.nome ?: "Set senza nome",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        lineHeight = 26.sp
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

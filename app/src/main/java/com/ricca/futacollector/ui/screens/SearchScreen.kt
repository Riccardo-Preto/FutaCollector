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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.ricca.futacollector.ui.CardDetailMode
import com.ricca.futacollector.viewmodel.CollectionViewModel
import com.ricca.futacollector.R

@Composable
fun SearchScreen(
    viewModel: CollectionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {

    var searchQuery by remember { mutableStateOf("") }
    var cards by remember { mutableStateOf<List<ApiCard>>(emptyList()) }
    var sets by remember { mutableStateOf<List<CardSet>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()

    val gridState = rememberLazyGridState() // Crea lo stato della griglia

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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp) // Spazio tra i banner
            ) {
                val reversedSets = sets.reversed()

                items(reversedSets) { set ->
                    SetBannerItem(set = set) {
                        // Azione al click: carica le carte del set
                        coroutineScope.launch {
                            try {
                                cards = RetrofitInstance.api.getCardsBySet(set.card_id)
                                    .sortedBy { it.card_set_id }
                                searchQuery = set.name
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }

        } else {
            // ---------- Griglia risultati ricerca ----------
            var selectedCard by remember { mutableStateOf<ApiCard?>(null) }

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState, // <--- Colleghiamo lo stato qui!
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
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
                val showScrollbar = gridState.firstVisibleItemIndex > 0
                if (showScrollbar) {
                    // Qui mettiamo la barra o il pulsante "Torna su"
                    FloatingActionButton(
                        onClick = {
                            // Coroutine per tornare su con un'animazione fluida
                            coroutineScope.launch {
                                gridState.animateScrollToItem(0)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd) // In basso a destra
                            .padding(16.dp)
                            .size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Torna su"
                        )
                    }
                }
            }



// ---------- Mostra CardDetailScreen in overlay se c'è una carta selezionata ----------
            selectedCard?.let { card ->
                Dialog(
                    onDismissRequest = { selectedCard = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    // Usiamo un Box per poter mettere i messaggi SOPRA il dettaglio
                    Box(modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CardDetailScreen(
                            card = card,
                            mode = CardDetailMode.Search,
                            onAddToCollection = {
                                viewModel.addCardToCollection(card)
                                // Non chiudere subito il dettaglio se vuoi vedere la notifica!
                                // Oppure chiudilo, ma sappi che la notifica di AppNavigation sarà sotto.
                                selectedCard = null
                            }
                        )

                        // Se vuoi che la notifica si veda MENTRE il dettaglio è aperto,
                        // dovresti spostare il controllo degli eventi anche qui.
                    }
                }
            }
        }
    }
}
@Composable
fun SetBannerItem(set: CardSet, onClick: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val imageRes = remember(set.card_id) {
        val resourceName = set.card_id.lowercase().replace("-", "_").trim()
        val id = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        id
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp) // 1. AUMENTATA L'ALTEZZA per gestire meglio le immagini quadrate
            .padding(horizontal = 12.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // IMMAGINE LOCALE
            if (imageRes != 0) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    // 2. ALLINEAMENTO: Prova Center o TopCenter per inquadrare meglio il pack
                    alignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // OVERLAY SFUMATO
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f), // Un po' di ombra sopra
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)  // Ombra forte sotto per il testo
                            ),
                            startY = 0f
                        )
                    )
            )

            // CONTENUTO TESTUALE
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                // 3. RECUPERO CODICE SET
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = set.card_id,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // NOME SET
                Text(
                    text = set.name,
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


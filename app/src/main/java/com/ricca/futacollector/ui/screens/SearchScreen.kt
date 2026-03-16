package com.ricca.futacollector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
    viewModel: CollectionViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val searchQuery by viewModel.searchQuery.collectAsState()
    val cards by viewModel.searchResults.collectAsState()
    val sets by viewModel.allSets.collectAsState()
    val setsWithCards by viewModel.setsWithCards.collectAsState()
    val showSetList by viewModel.showSetList.collectAsState()

    var selectedCard by remember { mutableStateOf<Card?>(null) }

    // Lazy states salvati nel ViewModel
    val gridState =
        viewModel.gridState ?: rememberLazyGridState().also { viewModel.gridState = it }

    val listState =
        viewModel.listState ?: rememberLazyListState().also { viewModel.listState = it }

    // --- FILTRO SET / STARTER ---
    var selectedFilter by rememberSaveable { mutableStateOf("ALL") }

    val filteredSets = when (selectedFilter) {
        "SETS" -> sets.filter {
            (it.id.startsWith("OP", true) ||
                    it.id.startsWith("EB", true) ||
                    it.id.startsWith("PRB", true)) &&
                    setsWithCards.contains(it.id)
        }
        "STARTERS" -> sets.filter {
            (it.id.startsWith("ST", true) ||
                    it.id.startsWith("LD", true)) &&
                    setsWithCards.contains(it.id)
        }
        "PROMOS" -> sets.filter {
            it.id.equals("P", true) &&
                    setsWithCards.contains(it.id)
        }
        "DON" -> sets.filter {
            it.id == "DON" &&
                    setsWithCards.contains(it.id)
        }
        else -> sets.filter { setsWithCards.contains(it.id) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {

        // --- SEARCH BAR ---
        TextField(
            value = searchQuery,
            onValueChange = { query ->
                viewModel.searchCards(query)

                // Facciamo lo scroll in cima solo se necessario per non appesantire il rendering
                if (query.isNotEmpty()) {
                    coroutineScope.launch {
                        gridState.scrollToItem(0)
                        listState.scrollToItem(0)
                    }
                }
            },
            placeholder = { Text("Cerca nel database...") },
            singleLine = true,
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = {
                        viewModel.clearSearch()
                        focusManager.clearFocus()

                        coroutineScope.launch {
                            gridState.scrollToItem(0)
                            listState.scrollToItem(0)
                        }
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = { focusManager.clearFocus() }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        // --- FILTRI ---
        if (showSetList) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { FilterChip(selected = selectedFilter == "ALL", onClick = { selectedFilter = "ALL" }, label = { Text("Tutti") }) }
                item { FilterChip(selected = selectedFilter == "SETS", onClick = { selectedFilter = "SETS" }, label = { Text("Set") }) }
                item { FilterChip(selected = selectedFilter == "STARTERS", onClick = { selectedFilter = "STARTERS" }, label = { Text("Starter Deck") }) }
                item { FilterChip(selected = selectedFilter == "PROMOS", onClick = { selectedFilter = "PROMOS" }, label = { Text("Promo") }) }
                item { FilterChip(selected = selectedFilter == "DON", onClick = { selectedFilter = "DON" }, label = { Text("DON!!") }) }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // --- CONTENUTO ---
        if (showSetList) {

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {

                val reversedSets = filteredSets.reversed()

                items(reversedSets, key = { it.id }) { set ->

                    SetBannerItem(set) {
                        viewModel.getCardsFromSet(set.id)

                        coroutineScope.launch {
                            gridState.scrollToItem(0)
                        }
                    }
                }
            }

        } else {
            // --- GRIGLIA CARTE ---
            Box(modifier = Modifier.fillMaxSize()) {
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
                    // FIX: card qui è di tipo CardWithCount
                    items(cards, key = { it.card.id }) { cardWithCount ->
                        CardItemView(
                            card = cardWithCount.card, // Passiamo l'oggetto Card interno
                            count = cardWithCount.count, // Ora possiamo passare il count reale!
                            onClick = { selectedCard = cardWithCount.card } // Assegniamo la Card
                        )
                    }
                }

                if (gridState.firstVisibleItemIndex > 0) {
                    FloatingActionButton(
                        onClick = { coroutineScope.launch { gridState.animateScrollToItem(0) } },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(48.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, null)
                    }
                }
            }
        }

        // --- DIALOG DETTAGLIO ---
        selectedCard?.let { card ->

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
                        mode = CardDetailMode.Search,
                        onAddToCollection = {
                            viewModel.addCardToCollection(card)
                            selectedCard = null
                        },
                        onRemoveFromCollection = {
                            selectedCard = null
                        },
                        onDismiss = { selectedCard = null }
                    )
                }
            }
        }
    }
}


@Composable
fun SetBannerItem(set: CardSetEntity, onClick: () -> Unit) {

    val imageModel = set.coverImage?.let { "file:///android_asset/$it" }

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
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
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
                    maxLines = 2
                )
            }
        }
    }
}

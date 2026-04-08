package com.ricca.futacollector.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.ricca.futacollector.CardItemView
import com.ricca.futacollector.data.AppConstants
import com.ricca.futacollector.data.Card
import com.ricca.futacollector.data.DeckWithCount
import com.ricca.futacollector.ui.CardDetailMode
import com.ricca.futacollector.viewmodel.CollectionViewModel
import com.ricca.futacollector.viewmodel.DeckViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.Intent
import androidx.core.content.ContextCompat.startActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    deckId: Int,
    deckName: String,
    viewModel: DeckViewModel,
    collectionViewModel: CollectionViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val deckItems by viewModel.getDeckDetails(deckId).collectAsState(initial = emptyList())
    val allDecks by viewModel.allDecks.collectAsState()
    val currentDeck = allDecks.find { it.deck.id == deckId }

    val leaderColors = remember(currentDeck) {
        currentDeck?.leaderCard?.color?.split(Regex("[/\\s+]"))?.filter { it.isNotBlank() } ?: emptyList()
    }

    val mainDeck = deckItems.filter { !it.isConsidering }
    val consideringDeck = deckItems.filter { it.isConsidering }
    val totalMainCards = mainDeck.sumOf { it.countInDeck }

    var showImportDialog by remember { mutableStateOf(false) }
    var showManualAddSearch by remember { mutableStateOf(false) }
    var showDeleteDeckDialog by remember { mutableStateOf(false) }
    var showClearDeckDialog by remember { mutableStateOf(false) }
    var selectedCard by remember { mutableStateOf<DeckWithCount?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.importResult.collect { message ->
            if (message != null) {
                snackbarHostState.showSnackbar(message)
                viewModel.clearImportResult()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(deckName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Text("$totalMainCards / 50 carte", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }

                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Aggiungi carta") },
                            leadingIcon = { Icon(Icons.Default.Add, null) },
                            onClick = {
                                showManualAddSearch = true
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Aggiungi tutto alla collezione") },
                            leadingIcon = { Icon(Icons.Default.LibraryAdd, null) },
                            onClick = {
                                viewModel.addAllDeckCardsToCollection(deckId, collectionViewModel)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Importa lista") },
                            leadingIcon = { Icon(Icons.Default.ContentPaste, null) },
                            onClick = {
                                showImportDialog = true
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Esporta mazzo") },
                            leadingIcon = { Icon(Icons.Default.Share, null) },
                            onClick = {
                                viewModel.exportDeckList(deckId) { exportText ->
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, exportText)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Esporta mazzo"))
                                }
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Esporta per Cardmarket") },
                            leadingIcon = { Icon(Icons.Default.Share, null) },
                            onClick = {
                                viewModel.exportDeckListCardmarket(deckId) { exportText ->
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, exportText)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Esporta per Cardmarket"))
                                }
                                showMenu = false
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        DropdownMenuItem(
                            text = { Text("Svuota mazzo") },
                            leadingIcon = { Icon(Icons.Default.CleaningServices, null) },
                            onClick = {
                                showClearDeckDialog = true
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Elimina mazzo", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            },
                            onClick = {
                                showDeleteDeckDialog = true
                                showMenu = false
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (deckItems.isEmpty()) {
            EmptyDeckChoice(
                onImportClick = { showImportDialog = true },
                onManualClick = { showManualAddSearch = true },
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                item(span = { GridItemSpan(3) }) {
                    SectionHeader(title = "Main Deck", count = totalMainCards, target = 50)
                }

                items(mainDeck) { item ->
                    DeckCardGridItem(
                        item, deckId, viewModel, collectionViewModel,
                        availableInCollection = item.countInCollection,
                        onCardClick = { selectedCard = it }
                    )
                }

                if (consideringDeck.isNotEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader(title = "Considering", count = consideringDeck.size, target = null)
                    }

                    items(consideringDeck) { item ->
                        val usedInMain = mainDeck.find { it.cardId == item.cardId }?.countInDeck ?: 0
                        val residualCollection = (item.countInCollection - usedInMain).coerceAtLeast(0)
                        DeckCardGridItem(
                            item, deckId, viewModel, collectionViewModel,
                            availableInCollection = residualCollection,
                            onCardClick = { selectedCard = it }  // aggiunto
                        )
                    }
                }

                item(span = { GridItemSpan(3) }) {
                    DeckStatsDashboard(deckItems)
                }

                item(span = { GridItemSpan(3) }) { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        if (showImportDialog) {
            ImportDeckDialog(
                onDismiss = { showImportDialog = false },
                onConfirm = { rawText ->
                    viewModel.importDeckList(deckId, rawText)
                    showImportDialog = false
                }
            )
        }

        if (showManualAddSearch) {
            ManualAddCardDialog(
                deckId = deckId,
                onDismiss = {
                    showManualAddSearch = false
                    collectionViewModel.clearSearch()
                },
                onCardSelected = { card, qty ->
                    viewModel.addMultipleCardsToDeck(deckId, card, qty)
                    Toast.makeText(
                        context,
                        "Aggiunto $qty x ${card.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                viewModel = viewModel,
                collectionViewModel = collectionViewModel,
                leaderColors = leaderColors
            )
        }

        if (showClearDeckDialog) {
            AlertDialog(
                onDismissRequest = { showClearDeckDialog = false },
                title = { Text("Svuota mazzo?") },
                text = { Text("Tutte le carte verranno rimosse dal mazzo. Il mazzo rimarrà nella lista.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.clearDeck(deckId)
                        showClearDeckDialog = false
                    }) {
                        Text("Svuota", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDeckDialog = false }) { Text("Annulla") }
                }
            )
        }

        if (showDeleteDeckDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDeckDialog = false },
                title = { Text("Eliminare il mazzo?") },
                text = { Text("Il mazzo \"$deckName\" verrà eliminato definitivamente.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteDeckById(deckId)
                        showDeleteDeckDialog = false
                        onBack()
                    }) {
                        Text("Elimina", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDeckDialog = false }) { Text("Annulla") }
                }
            )
        }

        selectedCard?.let { deckCard ->
            var fullCard by remember { mutableStateOf<Card?>(null) }

            LaunchedEffect(deckCard.cardId) {
                fullCard = viewModel.getCardById(deckCard.cardId)
            }

            fullCard?.let { card ->
                val currentCount = collectionViewModel.collectionCards.value
                    .find { it.card.id == card.id }?.count ?: 0
                Dialog(
                    onDismissRequest = { selectedCard = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CardDetailScreen(
                            card = card,
                            mode = CardDetailMode.Collection(ownedCopies = currentCount),
                            onAddToCollection = { collectionViewModel.addCardToCollection(card) },
                            onRemoveFromCollection = {
                                if (currentCount <= 1) selectedCard = null
                                collectionViewModel.removeCardFromCollection(card)
                            },
                            onDismiss = { selectedCard = null },
                            onAddToOrders = { quantity, note ->
                                collectionViewModel.addToOrders(card, quantity, note)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeckCardGridItem(
    item: DeckWithCount,
    deckId: Int,
    viewModel: DeckViewModel,
    collectionViewModel: CollectionViewModel,
    availableInCollection : Int,
    onCardClick: (DeckWithCount) -> Unit
) {
    val ordered = item.orderedQuantity
    val needed = item.countInDeck
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val borderColor = when {
        availableInCollection >= needed -> Color(0xFF4CAF50)
        (availableInCollection + ordered) >= needed -> Color(0xFFFFC107)
        else -> Color(0xFFEF5350)
    }

    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .combinedClickable(
                    onClick = { onCardClick(item) },
                    onLongClick = { showMenu = true }
                )
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            ) {
                val cardImage = item.cardImage ?: ""
                val imageModel = if (cardImage.startsWith("http")) cardImage
                else "file:///android_asset/immagini_ottimizzate/$cardImage"

                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Surface(
                    Modifier.align(Alignment.BottomStart), // <--- Allineato a SINISTRA
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(topEnd = 8.dp)
                ) {
                    val eurPrice = (item.marketPrice ?: 0.0) * AppConstants.CONVERSION_RATE
                    val totalPriceForCopies = eurPrice * needed
                    Text(
                        text = "€${"%.2f".format(totalPriceForCopies)}",
                        color = Color(0xFF4CAF50), // Verde per i soldi
                        modifier = Modifier.padding(horizontal = 4.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    Modifier.align(Alignment.BottomEnd),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(topStart = 8.dp)
                ) {
                    Text("x$needed", color = Color.White, modifier = Modifier.padding(horizontal = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.Center) {
                repeat(needed) { index ->
                    val dotColor = when {
                        index < availableInCollection -> Color(0xFF4CAF50)
                        index < (availableInCollection + ordered) -> Color(0xFFFFC107)
                        else -> Color.Gray.copy(alpha = 0.4f)
                    }
                    Box(Modifier.padding(horizontal = 1.dp).size(7.dp).background(dotColor, CircleShape))
                }
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Aggiungi 1 copia") },
                leadingIcon = { Icon(Icons.Default.Add, null) },
                enabled = needed < 4,
                onClick = {
                    viewModel.updateCardQuantity(deckId, item.cardId, item.isConsidering, needed + 1)
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Segna 1 come Ordinata") },
                leadingIcon = { Icon(Icons.Default.LocalShipping, null) },
                enabled = item.orderedQuantity < (needed - availableInCollection),
                onClick = {
                    viewModel.addCardToCollectionFromDeck(item.cardId) { card ->
                        collectionViewModel.addToOrders(card, 1, "")
                    }
                    showMenu = false
                }
            )

            if (item.orderedQuantity > 0) {
                DropdownMenuItem(
                    text = { Text("Rimuovi dalle Ordinate") },
                    leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, null) },
                    onClick = {
                        viewModel.addCardToCollectionFromDeck(item.cardId) { card ->
                            collectionViewModel.removeOneFromOrders(card.id)
                        }
                        showMenu = false
                    }
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            DropdownMenuItem(
                text = { Text(if (item.isConsidering) "Sposta in Main" else "Sposta in Considering") },
                leadingIcon = { Icon(Icons.Default.SwapHoriz, null) },
                onClick = {
                    viewModel.moveOneCard(deckId, item.cardId, item.isConsidering, availableInCollection)
                    showMenu = false
                }
            )
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            DropdownMenuItem(
                text = { Text("Rimuovi 1 copia", color = Color.Red) },
                leadingIcon = { Icon(Icons.Default.Remove, null, tint = Color.Red) },
                onClick = {
                    viewModel.updateCardQuantity(deckId, item.cardId, item.isConsidering, needed - 1)
                    showMenu = false
                }
            )
            HorizontalDivider(Modifier.padding(vertical = 4.dp))

            DropdownMenuItem(
                text = { Text("Aggiungi alla collezione") },
                leadingIcon = { Icon(Icons.Default.LibraryAdd, null, tint = Color(0xFF4CAF50)) },
                onClick = {
                    viewModel.addCardToCollectionFromDeck(item.cardId) { card ->
                        collectionViewModel.addCardToCollection(card)
                        Toast.makeText(
                            context,
                            "${card.name} aggiunta alla collezione! ✅",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    showMenu = false
                }
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, count: Int, target: Int?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        if (target != null) {
            Surface(
                color = if (count == target) Color(0xFF4CAF50).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "$count / $target",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (count == target) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyDeckChoice(
    onImportClick: () -> Unit,
    onManualClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Style, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            Spacer(Modifier.height(16.dp))
            Text("Il mazzo è vuoto", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onManualClick, modifier = Modifier.fillMaxWidth(0.7f), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Search, null)
                Spacer(Modifier.width(8.dp))
                Text("Cerca carte")
            }
            Spacer(Modifier.height(12.dp))
            Text("oppure", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onImportClick, modifier = Modifier.fillMaxWidth(0.7f), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.ContentPaste, null)
                Spacer(Modifier.width(8.dp))
                Text("Importa lista")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ManualAddCardDialog(
    deckId: Int,
    onDismiss: () -> Unit,
    onCardSelected: (Card, Int) -> Unit,
    viewModel: DeckViewModel,
    collectionViewModel: CollectionViewModel,
    leaderColors: List<String>
) {
    val searchQuery by collectionViewModel.searchQuery.collectAsState()
    val allCards by collectionViewModel.searchResults.collectAsState()
    val currentDeckItems by viewModel.getDeckDetails(deckId).collectAsState(initial = emptyList())
    val focusManager = LocalFocusManager.current

    val filteredByColor = remember(allCards, leaderColors, searchQuery) {
        // Se non c'è ricerca e non ci sono colori, mostra tutto
        if (leaderColors.isEmpty() && searchQuery.isBlank()) allCards
        else {
            // Appiattiamo tutto: se leaderColors è ["Red Green"], diventa ["red", "green"]
            val flatLeaderColors = leaderColors
                .flatMap { it.lowercase().split(Regex("[/\\s+]")) }
                .filter { it.isNotBlank() }

            allCards.filter { item ->
                val cardColorRaw = item.card.color?.lowercase() ?: ""

                // Logica: la carta passa se ALMENO UNO dei suoi colori
                // è contenuto nella lista dei colori del leader (OR logico)
                val colorMatch = flatLeaderColors.isEmpty() || flatLeaderColors.any { leaderCol ->
                    cardColorRaw.contains(leaderCol)
                }
                colorMatch
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { collectionViewModel.searchCards(it) },
                            placeholder = { Text("Cerca nel database...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                        )
                    },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) } }
                )
            }
        ) { padding ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredByColor) { item ->
                    val inDeckCount = currentDeckItems.find { it.cardId == item.card.id }?.countInDeck ?: 0
                    var showAddMenu by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .combinedClickable(
                                onClick = { showAddMenu = true },
                                onLongClick = { showAddMenu = true }
                            )
                    ) {
                        CardItemView(
                            card = item.card,
                            count = item.count,
                            onClick = null
                        )

                        if (inDeckCount > 0) {
                            Surface(
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                                shadowElevation = 4.dp
                            ) {
                                Text(
                                    text = "$inDeckCount",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Aggiungi 1 copia") },
                                leadingIcon = { Icon(Icons.Default.Add, null) },
                                onClick = { onCardSelected(item.card, 1); showAddMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Aggiungi 4 copie") },
                                leadingIcon = { Icon(Icons.Default.AddCircle, null) },
                                onClick = { onCardSelected(item.card, 4); showAddMenu = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeckStatsDashboard(items: List<DeckWithCount>) {
    val mainDeck = items.filter { !it.isConsidering }
    if (mainDeck.isEmpty()) return

    // Calcolo del valore totale
    val totalPriceUsd = mainDeck.sumOf { (it.marketPrice ?: 0.0) * it.countInDeck }
    val totalPriceEur = totalPriceUsd * AppConstants.CONVERSION_RATE

    // Raggruppamento costi (0-7+)
    val costGroups = mainDeck
        .groupBy { it.cardCost ?: "0" }
        .mapValues { entry -> entry.value.sumOf { it.countInDeck } }

    // Troviamo qual è il costo più frequente per scalare gli altri di conseguenza
    val maxCardsInSingleCost = costGroups.values.maxOrNull() ?: 1

    val counter2k = mainDeck.filter { it.cardCounter?.contains("2000") == true }.sumOf { it.countInDeck }
    val counter1k = mainDeck.filter { it.cardCounter?.contains("1000") == true }.sumOf { it.countInDeck }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("ANALISI MAZZO", fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = "Valore stimato: € ${"%.2f".format(totalPriceEur)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50), // Un bel verde "soldi"
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CounterBadge(label = "+2000", count = counter2k, color = Color(0xFF4CAF50))
                    CounterBadge(label = "+1000", count = counter1k, color = Color(0xFF2196F3))
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("Distribuzione Costi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

            // AREA GRAFICO CON ALTEZZA FISSA BLOCATA
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp) // Altezza totale fissa (Numeri + Barre + Etichette)
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                (0..7).forEach { cost ->
                    val label = if (cost == 7) "7+" else cost.toString()
                    val count = if (cost == 7) {
                        costGroups.filter { (it.key.toIntOrNull() ?: 0) >= 7 }.values.sum()
                    } else {
                        costGroups[cost.toString()] ?: 0
                    }

                    // Passiamo il valore massimo trovato per la proporzione
                    CostBar(label = label, count = count, maxInDeck = maxCardsInSingleCost)
                }
            }
        }
    }
}

@Composable
fun RowScope.CostBar(label: String, count: Int, maxInDeck: Int) {
    val totalGraphHeight = 100.dp // Spazio totale per numero + barra + etichetta
    val barAreaHeight = 70.dp    // Spazio riservato solo alla barra + numero sopra

    Column(
        modifier = Modifier
            .weight(1f)
            .height(totalGraphHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom // Tutto schiacciato verso il basso
    ) {
        // Area della barra e del numero superiore
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // Prende tutto lo spazio sopra l'etichetta
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (count > 0) {
                    Text(
                        text = count.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Spazio minimo tra numero e barra
                Spacer(Modifier.height(2.dp))

                // La Barra
                val barHeight = if (count > 0) {
                    val ratio = count.toFloat() / maxInDeck
                    // Altezza massima della barra fisica: 50dp
                    50.dp * ratio.coerceIn(0.1f, 1f)
                } else {
                    1.dp
                }

                Box(
                    Modifier
                        .width(18.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            if (count > 0) MaterialTheme.colorScheme.primary
                            else Color.LightGray.copy(alpha = 0.3f)
                        )
                )
            }
        }

        // Etichetta del costo (0, 1, 2...) sempre fissa alla base
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CounterBadge(label: String, count: Int, color: Color) {
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, color.copy(alpha = 0.5f))) {
        Text("$label: $count", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
package com.ricca.futacollector.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var showOrders by remember { mutableStateOf(false) }

    // Ordinamento
    var sortBy by remember { mutableStateOf("id") } // "id", "price", "date"
    var showSortSheet by remember { mutableStateOf(false) }

    var longPressedCard by remember { mutableStateOf<Card?>(null) }

    // Filtri
    var filterSet by remember { mutableStateOf<String?>(null) }
    var filterColor by remember { mutableStateOf<String?>(null) }
    var filterType by remember { mutableStateOf<String?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }

    // Lista set disponibili nella collezione
    val availableSets = remember(collection) {
        collection.mapNotNull { it.card.setId }.distinct().sorted()
    }
    val availableColors = remember(collection) {
        collection.mapNotNull { it.card.color }
            .flatMap { it.split("/") }
            .map { it.trim() }
            .distinct().sorted()
    }
    val availableTypes = remember(collection) {
        collection.mapNotNull { it.card.type }.distinct().sorted()
    }

    // Collezione filtrata e ordinata
    val filteredCollection = remember(collection, sortBy, filterSet, filterColor, filterType) {
        collection
            .filter { item ->
                (filterSet == null || item.card.setId == filterSet) &&
                        (filterColor == null || item.card.color?.contains(filterColor!!, ignoreCase = true) == true) &&
                        (filterType == null || item.card.type?.equals(filterType, ignoreCase = true) == true)
            }
            .let { list ->
                when (sortBy) {
                    "price" -> list.sortedByDescending { it.card.marketPrice }
                    "date" -> list.sortedByDescending { it.addedDate }
                    else -> list.sortedBy { it.card.id }
                }
            }
    }

    val activeFilters = listOfNotNull(filterSet, filterColor, filterType).size

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
                .padding(top = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 20.dp, bottom = 4.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "La tua collezione",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black
                )
                // Le due icone nel buco a destra
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { showWishlist = true },
                        modifier = Modifier
                            .background(Color(0xFFEF5350).copy(alpha = 0.1f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = "Wishlist", tint = Color(0xFFEF5350), modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = { showOrders = true },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = "Ordini", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
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
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Carte totali", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            Text("$totalCards", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Valore stimato", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            Text("€ ${String.format("%.2f", totalValueEuro)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Bottone Ordina
                    OutlinedButton(
                        onClick = { showSortSheet = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(
                            1.dp,
                            if (sortBy != "id") MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Sort, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            when (sortBy) {
                                "price" -> "Prezzo ↓"
                                "date" -> "Data ↓"
                                else -> "Ordina"
                            },
                            color = if (sortBy != "id") MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Bottone Filtra
                    OutlinedButton(
                        onClick = { showFilterSheet = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(
                            1.dp,
                            if (activeFilters > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Icon(Icons.Default.FilterList, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (activeFilters > 0) "Filtra ($activeFilters)" else "Filtra",
                            color = if (activeFilters > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
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
                    items(filteredCollection, key = { it.card.id }) { item ->
                        Box {
                            CardItemView(
                                card = item.card,
                                count = item.count,
                                onClick = { selectedCard = item.card },
                                onLongClick = { longPressedCard = item.card }
                            )
                            DropdownMenu(
                                expanded = longPressedCard?.id == item.card.id,
                                onDismissRequest = { longPressedCard = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Aggiungi una copia") },
                                    leadingIcon = { Icon(Icons.Default.Add, null) },
                                    onClick = {
                                        viewModel.addCardToCollection(item.card)
                                        longPressedCard = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Rimuovi una copia") },
                                    leadingIcon = { Icon(Icons.Default.Remove, null) },
                                    onClick = {
                                        viewModel.removeCardFromCollection(item.card)
                                        longPressedCard = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Aggiungi agli ordini") },
                                    leadingIcon = { Icon(Icons.Default.LocalShipping, null) },
                                    onClick = {
                                        viewModel.addToOrders(item.card, 1, "")
                                        longPressedCard = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Aggiungi alla wishlist") },
                                    leadingIcon = { Icon(Icons.Default.Favorite, null) },
                                    onClick = {
                                        viewModel.addToWishlist(item.card)
                                        longPressedCard = null
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSortSheet) {
        ModalBottomSheet(onDismissRequest = { showSortSheet = false }) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text("Ordina per", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(16.dp))

                listOf(
                    "id" to "ID carta (default)",
                    "price" to "Prezzo (decrescente)",
                    "date" to "Data aggiunta (recente prima)"
                ).forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                sortBy = key
                                showSortSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = sortBy == key, onClick = { sortBy = key; showSortSheet = false })
                        Spacer(Modifier.width(12.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filtra", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    if (activeFilters > 0) {
                        TextButton(onClick = {
                            filterSet = null
                            filterColor = null
                            filterType = null
                        }) { Text("Reset") }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Filtro Set
                Text("Set", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableSets.forEach { set ->
                        FilterChip(
                            selected = filterSet == set,
                            onClick = { filterSet = if (filterSet == set) null else set },
                            label = { Text(set) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Filtro Colore
                Text("Colore", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableColors.forEach { color ->
                        FilterChip(
                            selected = filterColor == color,
                            onClick = { filterColor = if (filterColor == color) null else color },
                            label = { Text(color) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Filtro Tipo
                Text("Tipo", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableTypes.forEach { type ->
                        FilterChip(
                            selected = filterType == type,
                            onClick = { filterType = if (filterType == type) null else type },
                            label = { Text(type) }
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))
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

    if (showOrders) {
        OrdersDialog(
            viewModel = viewModel,
            onDismiss = { showOrders = false }
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
                    onDismiss = { selectedCard = null },
                    onAddToOrders = { quantity, note ->
                        viewModel.addToOrders(card, quantity, note)
                    }
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
    val context = LocalContext.current
    val allItems = groupedItems.values.flatten()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Wishlist", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                    },
                    actions = {
                        if (allItems.isNotEmpty()) {
                            IconButton(onClick = {
                                // Export formato Cardmarket: "1x Perona OP12-034"
                                val exportText = allItems.joinToString("\n") { item ->
                                    val cleanName = item.card.name
                                        ?.substringBefore("(")
                                        ?.replace(Regex("-\\s*[A-Z]{2,5}\\d{2}-\\d{3,4}.*"), "")  // rimuove " - PRB02-002" e simili
                                        ?.trim()
                                        ?: ""
                                    val cleanId = item.card.id.substringBefore("_")
                                    "${item.quantity}x $cleanName $cleanId"
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, exportText)
                                    type = "text/plain"
                                }
                                context.startActivity(
                                    Intent.createChooser(sendIntent, "Esporta Wishlist")
                                )
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Esporta")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            if (allItems.isEmpty()) {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FavoriteBorder,
                            null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "La tua wishlist è vuota",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allItems, key = { it.card.id }) { item ->
                        WishlistGridItem(item = item, onRemove = onRemove)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WishlistGridItem(item: WishlistItem, onRemove: (String) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val priceEur = item.card.marketPrice * AppConstants.CONVERSION_RATE

    Box(
        modifier = Modifier.combinedClickable(
            onClick = { showMenu = true },
            onLongClick = { showMenu = true }
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        2.dp,
                        Color(0xFFEF5350).copy(alpha = 0.6f),
                        RoundedCornerShape(8.dp)
                    )
            ) {
                val cardImage = item.card.image ?: ""
                val imageModel = if (cardImage.startsWith("http")) cardImage
                else "file:///android_asset/immagini_ottimizzate/$cardImage"

                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Prezzo in basso a sinistra
                Surface(
                    Modifier.align(Alignment.BottomStart),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(topEnd = 8.dp)
                ) {
                    Text(
                        text = "€${"%.2f".format(priceEur)}",
                        color = Color(0xFFEF5350),
                        modifier = Modifier.padding(horizontal = 4.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Quantità in basso a destra
                if (item.quantity > 1) {
                    Surface(
                        Modifier.align(Alignment.BottomEnd),
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(topStart = 8.dp)
                    ) {
                        Text(
                            "x${item.quantity}",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Rimuovi dalla wishlist", color = Color.Red) },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                onClick = {
                    onRemove(item.card.id)
                    showMenu = false
                }
            )
        }
    }
}
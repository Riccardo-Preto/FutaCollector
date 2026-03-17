package com.ricca.futacollector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.ricca.futacollector.data.AppConstants
import com.ricca.futacollector.data.Card
import com.ricca.futacollector.data.RecentCardItem
import com.ricca.futacollector.data.DeckDao
import com.ricca.futacollector.data.DeckDao.MissingCard
import com.ricca.futacollector.ui.navigation.Screen
import com.ricca.futacollector.viewmodel.CollectionViewModel
import com.ricca.futacollector.viewmodel.DeckViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ricca.futacollector.data.OrderedCardWithDetails
import com.ricca.futacollector.ui.CardDetailMode

@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: CollectionViewModel,
    deckViewModel: DeckViewModel
) {
    val collection by viewModel.collectionCards.collectAsState()
    var selectedCardId by remember { mutableStateOf<String?>(null) }
    val recentCards by viewModel.recentCards.collectAsState()
    val missingCards by deckViewModel.missingCards.collectAsState()
    val orderedCards by viewModel.orderedCards.collectAsState()

    val totalCards = collection.sumOf { it.count }
    val totalPriceEuro = collection.sumOf { it.card.marketPrice * it.count } * AppConstants.CONVERSION_RATE
    val missingByDeck = missingCards.groupBy { it.deckId to it.deckName }

    var selectedOrderedCard by remember { mutableStateOf<OrderedCardWithDetails?>(null) }
    var selectedMissingCardId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {

        // ── HEADER ────────────────────────────────────────────────
        item {
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp)
            )
        }

        // ── STATISTICHE — unica card larga ────────────────────────
        item {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Carte totali",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$totalCards",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Valore stimato",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "€%.2f".format(totalPriceEuro),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // ── CARTE IN ARRIVO ───────────────────────────────────────
        if (orderedCards.isNotEmpty()) {
            item { SectionTitle(text = "In arrivo") }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    items(orderedCards) { order ->
                        val imageModel = order.image?.let {
                            if (it.startsWith("http")) it
                            else "file:///android_asset/immagini_ottimizzate/$it"
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(80.dp)
                                .clickable { selectedOrderedCard = order }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp, 112.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                if (imageModel != null) {
                                    AsyncImage(
                                        model = imageModel,
                                        contentDescription = order.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Surface(
                                    modifier = Modifier.align(Alignment.BottomEnd),
                                    color = Color(0xFFFFC107),
                                    shape = RoundedCornerShape(topStart = 8.dp)
                                ) {
                                    Text(
                                        text = "x${order.quantity}",
                                        color = Color.Black,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = order.name ?: order.cardId,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Medium
                            )
                            if (order.note.isNotBlank()) {
                                Text(
                                    text = order.note,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── CARTE MANCANTI NEI MAZZI ──────────────────────────────
        if (missingByDeck.isNotEmpty()) {
            item { SectionTitle(text = "Carte da acquistare") }
            missingByDeck.forEach { (deckInfo, cards) ->
                val (deckId, deckName) = deckInfo
                val totalMissing = cards.sumOf { it.missing }
                item {
                    MissingCardsDeckSection(
                        deckName = deckName,
                        totalMissing = totalMissing,
                        cards = cards,
                        onDeckClick = { navController.navigate("deck_detail/$deckId/$deckName") },
                        onCardClick = { cardId -> selectedMissingCardId = cardId }
                    )
                }
            }
        }

        // ── ATTIVITÀ RECENTE ──────────────────────────────────────
        item { SectionTitle(text = "Aggiunte recenti") }

        if (recentCards.isEmpty()) {
            item {
                Text(
                    text = "Nessuna carta aggiunta ancora",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        } else {
            items(recentCards) { item ->
                RecentCardRow(item, onClick = { selectedCardId = item.cardId })
            }
        }
    }

    // ── CARD DETAIL DIALOG ────────────────────────────────────────
    selectedCardId?.let { cardId ->
        var fullCard by remember { mutableStateOf<Card?>(null) }
        LaunchedEffect(cardId) { fullCard = viewModel.getCardById(cardId) }
        fullCard?.let { card ->
            val currentCount = collection.find { it.card.id == card.id }?.count ?: 0
            Dialog(
                onDismissRequest = { selectedCardId = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CardDetailScreen(
                        card = card,
                        mode = CardDetailMode.Collection(ownedCopies = currentCount),
                        onAddToCollection = { viewModel.addCardToCollection(card) },
                        onRemoveFromCollection = {
                            if (currentCount <= 1) selectedCardId = null
                            viewModel.removeCardFromCollection(card)
                        },
                        onDismiss = { selectedCardId = null },
                        onAddToOrders = { quantity, note -> viewModel.addToOrders(card, quantity, note) }
                    )
                }
            }
        }
    }

    selectedMissingCardId?.let { cardId ->
        var fullCard by remember { mutableStateOf<Card?>(null) }
        LaunchedEffect(cardId) { fullCard = viewModel.getCardById(cardId) }
        fullCard?.let { card ->
            Dialog(
                onDismissRequest = { selectedMissingCardId = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CardDetailScreen(
                        card = card,
                        mode = CardDetailMode.Search,
                        onAddToCollection = { viewModel.addCardToCollection(card) },
                        onRemoveFromCollection = { },
                        onDismiss = { selectedMissingCardId = null },
                        onAddToOrders = { quantity, note ->
                            viewModel.addToOrders(card, quantity, note)
                            selectedMissingCardId = null
                        }
                    )
                }
            }
        }
    }

    selectedOrderedCard?.let { order ->
        // Prendi la versione aggiornata dall'orderedCards reattivo
        val currentOrder = orderedCards.find { it.id == order.id }

        // Se non esiste più (tutte arrivate) chiudi il dialog
        if (currentOrder == null) {
            selectedOrderedCard = null
            return@let
        }

        var fullCard by remember { mutableStateOf<Card?>(null) }
        LaunchedEffect(order.cardId) { fullCard = viewModel.getCardById(order.cardId) }
        fullCard?.let { card ->
            val currentCount = collection.find { it.card.id == card.id }?.count ?: 0
            Dialog(
                onDismissRequest = { selectedOrderedCard = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CardDetailScreen(
                        card = card,
                        mode = CardDetailMode.Ordered(
                            ownedCopies = currentCount,
                            orderedQuantity = currentOrder.quantity,
                            orderedItemId = currentOrder.id
                        ),
                        onAddToCollection = { viewModel.addCardToCollection(card) },
                        onRemoveFromCollection = { viewModel.removeCardFromCollection(card) },
                        onDismiss = { selectedOrderedCard = null },
                        onAddToOrders = { quantity, note -> viewModel.addToOrders(card, quantity, note) },
                        onConfirmArrival = {
                            viewModel.markAsArrived(currentOrder)
                        },
                        onRemoveFromOrders = {
                            viewModel.removeOneFromOrders(card.id)
                        }
                    )
                }
            }
        }
    }
}

// ── TITOLO SEZIONE con accent bar ─────────────────────────────────
@Composable
fun SectionTitle(text: String) {
    Row(
        modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black
        )
    }
}

// ── SEZIONE CARTE MANCANTI PER MAZZO ─────────────────────────────
@Composable
fun MissingCardsDeckSection(
    deckName: String,
    totalMissing: Int,
    cards: List<MissingCard>,
    onDeckClick: () -> Unit,
    onCardClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDeckClick() }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = deckName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = "$totalMissing mancanti",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(cards) { card ->
                MissingCardChip(card, onClick = { onCardClick(card.cardId) })
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
fun MissingCardChip(card: MissingCard, onClick: () -> Unit) {
    val imageModel = card.cardImage?.let {
        if (it.startsWith("http")) it
        else "file:///android_asset/immagini_ottimizzate/$it"
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp).clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(80.dp, 112.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = card.cardName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd),
                color = MaterialTheme.colorScheme.error,
                shape = RoundedCornerShape(topStart = 8.dp)
            ) {
                Text(
                    text = "x${card.missing}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = card.cardName ?: card.cardId,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── RIGA CARTA RECENTE ────────────────────────────────────────────
@Composable
fun RecentCardRow(item: RecentCardItem, onClick: () -> Unit) {
    val imageModel = item.image?.let {
        if (it.startsWith("http")) it
        else "file:///android_asset/immagini_ottimizzate/$it"
    }
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.ITALIAN) }
    val dateString = remember(item.addedDate) { dateFormat.format(Date(item.addedDate)) }
    val priceEur = item.marketPrice * AppConstants.CONVERSION_RATE

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp, 68.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name ?: item.cardId,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.cardId,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = dateString,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = "x${item.count}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "€%.2f".format(priceEur),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun NavigationBanner(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(100.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = color.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(56.dp)) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.padding(12.dp), tint = color)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, containerColor: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.Start) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
    }
}
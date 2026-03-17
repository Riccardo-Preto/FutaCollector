package com.ricca.futacollector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.ricca.futacollector.data.AppConstants
import com.ricca.futacollector.data.Card
import com.ricca.futacollector.data.OrderedCardWithDetails
import com.ricca.futacollector.ui.CardDetailMode
import com.ricca.futacollector.viewmodel.CollectionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersDialog(
    viewModel: CollectionViewModel,
    onDismiss: () -> Unit
) {
    val orders by viewModel.orderedCards.collectAsState()
    var selectedOrder by remember { mutableStateOf<OrderedCardWithDetails?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Ordini in corso", fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                )
            }
        ) { padding ->
            if (orders.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.LocalShipping,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Nessun ordine in corso",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Aggiungi carte agli ordini dalla ricerca",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        val totalValue = orders.sumOf {
                            it.marketPrice * AppConstants.CONVERSION_RATE * it.quantity
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${orders.size} carte in arrivo",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "~€${"%.2f".format(totalValue)}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    items(orders, key = { it.id }) { item ->
                        OrderRow(
                            item = item,
                            onMarkArrived = { viewModel.markAsArrived(item) },
                            onDelete = { viewModel.removeFromOrders(item.id) },
                            onEdit = { quantity, note -> viewModel.updateOrder(item.id, quantity, note) },
                            onCardClick = { selectedOrder = item }
                        )
                    }
                }

                selectedOrder?.let { order ->
                    val orders by viewModel.orderedCards.collectAsState()
                    val currentOrder = orders.find { it.id == order.id }

                    if (currentOrder == null) {
                        selectedOrder = null
                        return@let
                    }

                    var fullCard by remember { mutableStateOf<Card?>(null) }
                    LaunchedEffect(order.cardId) { fullCard = viewModel.getCardById(order.cardId) }
                    fullCard?.let { card ->
                        val currentCount = viewModel.collectionCards.value
                            .find { it.card.id == card.id }?.count ?: 0
                        Dialog(
                            onDismissRequest = { selectedOrder = null },
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
                                    onDismiss = { selectedOrder = null },
                                    onAddToOrders = { quantity, note -> viewModel.addToOrders(card, quantity, note) },
                                    onConfirmArrival = {
                                        viewModel.markAsArrived(currentOrder)
                                        // niente selectedOrder = null — si chiude da solo quando quantity = 0
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
        }
    }
}

@Composable
fun OrderRow(
    item: OrderedCardWithDetails,
    onMarkArrived: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (quantity: Int, note: String) -> Unit,
    onCardClick: () -> Unit
) {

    var showEditDialog by remember { mutableStateOf(false) }
    var editQuantity by remember { mutableStateOf(item.quantity) }
    var editNote by remember { mutableStateOf(item.note) }

    val imageModel = item.image?.let {
        if (it.startsWith("http")) it
        else "file:///android_asset/immagini_ottimizzate/$it"
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.ITALIAN) }
    val dateString = remember(item.orderedDate) {
        dateFormat.format(Date(item.orderedDate))
    }

    val priceEur = item.marketPrice * AppConstants.CONVERSION_RATE * item.quantity

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Immagine
            Box(
                modifier = Modifier
                    .size(50.dp, 70.dp)
                    .clip(RoundedCornerShape(6.dp))
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

            Spacer(Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name ?: item.cardId,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.cardId,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (item.note.isNotBlank()) {
                    Text(
                        text = "📦 ${item.note}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "Ordinata il $dateString · x${item.quantity} · ~€${"%.2f".format(priceEur)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(8.dp))

            // Azioni
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Bottone arrivata
                IconButton(
                    onClick = onMarkArrived,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            Color(0xFF4CAF50).copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Arrivata",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Bottone elimina
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Rimuovi",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        editQuantity = item.quantity
                        editNote = item.note
                        showEditDialog = true
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Modifica",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (showEditDialog) {
                    AlertDialog(
                        onDismissRequest = { showEditDialog = false },
                        title = { Text("Modifica ordine") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(item.name ?: item.cardId, fontWeight = FontWeight.Bold)

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Quantità:")
                                    IconButton(onClick = { if (editQuantity > 1) editQuantity-- }) {
                                        Icon(Icons.Default.Remove, null)
                                    }
                                    Text("$editQuantity", fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { if (editQuantity < 4) editQuantity++ }) {
                                        Icon(Icons.Default.Add, null)
                                    }
                                }

                                OutlinedTextField(
                                    value = editNote,
                                    onValueChange = { editNote = it },
                                    label = { Text("Note (venditore, sito...)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                onEdit(editQuantity, editNote)
                                showEditDialog = false
                            }) { Text("Salva") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEditDialog = false }) { Text("Annulla") }
                        }
                    )
                }
            }
        }
    }
}
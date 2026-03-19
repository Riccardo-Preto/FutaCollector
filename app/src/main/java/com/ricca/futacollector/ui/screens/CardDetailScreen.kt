package com.ricca.futacollector.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.ricca.futacollector.data.AppConstants
import com.ricca.futacollector.data.Card
import com.ricca.futacollector.ui.CardDetailMode
import com.ricca.futacollector.getCardColorsList
import android.net.Uri
import androidx.compose.material.icons.filled.OpenInBrowser

@Composable
fun CardDetailScreen(
    card: Card,
    mode: CardDetailMode,
    onAddToCollection: () -> Unit,
    onRemoveFromCollection: () -> Unit,
    onDismiss: () -> Unit,
    onAddToOrders: (quantity: Int, note: String) -> Unit = { _, _ -> },
    onConfirmArrival: (() -> Unit)? = null,
    onRemoveFromOrders: (() -> Unit)? = null
) {
    val cardName = card.name ?: "Nome Sconosciuto"
    val cardImage = card.image ?: ""
    val imagePainter = rememberAsyncImagePainter(
        model = if (cardImage.startsWith("http")) cardImage else "file:///android_asset/immagini_ottimizzate/$cardImage"
    )

    // --- LOGICA COLORI E ID PULITO ---
    val fullColors = getCardColorsList(card.color, isPastel = false)
    val pastelColors = getCardColorsList(card.color, isPastel = true)

    val mainColor = fullColors.first()
    val isBlackCard = card.color?.lowercase()?.trim() == "black"
    val textColor = if (isBlackCard) MaterialTheme.colorScheme.onSurface else mainColor

    // Pulizia ID per il dettaglio (es. OP01-001_v2 -> OP01-001)
    val displayId = card.id.split("_").first()

    // Gradiente Orizzontale Forzato (uguale alla CardItemView per coerenza)
    val backgroundBrush = if (pastelColors.size > 1) {
        Brush.linearGradient(
            colors = pastelColors,
            start = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f),
            end = androidx.compose.ui.geometry.Offset(0f, 0f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(pastelColors.first(), pastelColors.first().copy(alpha = 0.1f)),
            start = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f),
            end = androidx.compose.ui.geometry.Offset(0f, 0f)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.92f),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // --- 1. IMMAGINE PROTAGONISTA ---
                    Box(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(0.95f)
                            .aspectRatio(0.71f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(brush = backgroundBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cardImage.isNotBlank()) {
                            Image(
                                painter = imagePainter,
                                contentDescription = cardName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }

                        if (cardImage.isBlank() || imagePainter.state is AsyncImagePainter.State.Error) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = null,
                                    tint = textColor.copy(alpha = 0.5f),
                                    modifier = Modifier.size(80.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "IMMAGINE NON DISPONIBILE",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = textColor,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- 2. INTESTAZIONE (Con ID pulito) ---
                    Text(
                        text = cardName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = displayId, // USIAMO L'ID PULITO QUI
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                    // ... tutto il resto del file rimane uguale ...

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- 3. GRIGLIA INFORMATIVA ---
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                MiniDetail(label = "COST", value = card.cost)
                                MiniDetail(label = "POWER", value = card.power)
                                MiniDetail(label = "COUNTER", value = card.counter)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                MiniDetail(label = "COLOR", value = card.color)
                                MiniDetail(label = "RARITY", value = card.rarity)
                                MiniDetail(label = "ATTR", value = card.attribute)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                            // --- TYPE & SUB-TYPES ---
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                                Text(text = "TYPE", style = MaterialTheme.typography.labelSmall, color = textColor, fontWeight = FontWeight.Bold)
                                Text(text = card.type ?: "---", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

                                if (!card.subTypes.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = "SUB-TYPES", style = MaterialTheme.typography.labelSmall, color = textColor, fontWeight = FontWeight.Bold)
                                    OptcgSubTypesFlow(card.subTypes, textColor)
                                }
                            }
                        }
                    }

                    // --- 4. EFFETTO CARTA ---
                    if (!card.effect.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "CARD EFFECT",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start).padding(start = 4.dp),
                            color = textColor
                        )
                        Surface(
                            color = textColor.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, textColor.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Text(
                                text = card.effect,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp, fontSize = 15.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    // PriceTag ora include la conversione Euro
                    PriceTag(label = "Market Price", price = card.marketPrice)
                    Spacer(modifier = Modifier.height(24.dp))

                    val context = LocalContext.current
                    OutlinedButton(
                        onClick = {
                            val url = "https://www.cardmarket.com/it/OnePiece/Products/Search?searchString=${card.id.split("_").first()}"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Cerca su Cardmarket")
                    }
                }

                ActionButtonsSection(
                    mode = mode,
                    card = card,
                    onRemove = onRemoveFromCollection,
                    onAdd = onAddToCollection,
                    onAddToOrders = { quantity, note -> onAddToOrders(quantity, note) },
                    onConfirmArrival = onConfirmArrival,
                    onRemoveFromOrders = onRemoveFromOrders
                )
            }

            // Tasto chiudi
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Chiudi")
            }
        }
    }
}

@Composable
fun MiniDetail(label: String, value: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value ?: "---", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PriceTag(label: String, price: Double?) {
    val priceInEuro = (price ?: 0.0) * AppConstants.CONVERSION_RATE
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(
            text = if (priceInEuro > 0.0) "€ ${String.format("%.2f", priceInEuro)}" else "N/A",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OptcgSubTypesFlow(subTypes: String, tintColor: Color) {
    val typesList = subTypes.split(Regex("[/\\s+]")).filter { it.isNotBlank() }
    FlowRow(
        modifier = Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        typesList.forEach { type ->
            Surface(
                color = tintColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, tintColor.copy(alpha = 0.3f))
            ) {
                Text(
                    text = type,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = tintColor
                )
            }
        }
    }
}

@Composable
fun ActionButtonsSection(
    mode: CardDetailMode,
    card: Card,
    onRemove: () -> Unit,
    onAdd: () -> Unit,
    onAddToOrders: (quantity: Int, note: String) -> Unit,
    onConfirmArrival: (() -> Unit)? = null,
    onRemoveFromOrders: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var showOrderDialog by remember { mutableStateOf(false) }
    var orderQuantity by remember { mutableStateOf(1) }
    var orderNote by remember { mutableStateOf("") }

    Surface(tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ordered
            if (mode is CardDetailMode.Ordered) {
                val orderedQty = mode.orderedQuantity
                val ownedQty = mode.ownedCopies
                Text(
                    text = "$orderedQty in ordine · $ownedQty in collezione",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFC107)
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onConfirmArrival?.invoke() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Conferma arrivo", fontSize = 16.sp)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showOrderDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Ordina ancora")
                    }
                    OutlinedButton(
                        onClick = { onRemoveFromOrders?.invoke() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Rimuovi")
                    }
                }
            }

            // Collection
            if (mode is CardDetailMode.Collection) {
                val count = mode.ownedCopies
                AnimatedContent(targetState = count) { targetCount ->
                    Text(
                        text = "$targetCount ${if (targetCount == 1) "copia" else "copie"} in collezione",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onRemove() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = BorderStroke(1.dp, Color.Red),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Rimuovi") }
                    Button(
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onAdd() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Aggiungi") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showOrderDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Aggiungi agli ordini")
                }
            }

            // Search
            if (mode is CardDetailMode.Search) {
                Button(
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onAdd() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Aggiungi alla collezione", fontSize = 16.sp)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showOrderDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Aggiungi agli ordini")
                }
            }
        }
    }

    // DIALOG FUORI DALL'if/else — funziona in entrambe le modalità
    if (showOrderDialog) {
        AlertDialog(
            onDismissRequest = { showOrderDialog = false },
            title = { Text("Aggiungi agli ordini") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(card.name ?: card.id, fontWeight = FontWeight.Bold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Quantità:")
                        IconButton(onClick = { if (orderQuantity > 1) orderQuantity-- }) {
                            Icon(Icons.Default.Remove, null)
                        }
                        Text("$orderQuantity", fontWeight = FontWeight.Bold)
                        IconButton(onClick = { if (orderQuantity < 4) orderQuantity++ }) {
                            Icon(Icons.Default.Add, null)
                        }
                    }
                    OutlinedTextField(
                        value = orderNote,
                        onValueChange = { orderNote = it },
                        label = { Text("Note (venditore, sito...)") },
                        placeholder = { Text("es. Cardmarket - Mario") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAddToOrders(orderQuantity, orderNote)
                    showOrderDialog = false
                    orderQuantity = 1
                    orderNote = ""
                }) { Text("Aggiungi") }
            },
            dismissButton = {
                TextButton(onClick = { showOrderDialog = false }) { Text("Annulla") }
            }
        )
    }
}
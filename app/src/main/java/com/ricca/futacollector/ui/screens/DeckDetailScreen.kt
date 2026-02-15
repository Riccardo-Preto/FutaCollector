package com.ricca.futacollector.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.ricca.futacollector.data.DeckWithCount
import com.ricca.futacollector.viewmodel.DeckViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    deckId: Int,
    deckName: String,
    viewModel: DeckViewModel,
    onBack: () -> Unit
) {
    val deckItems by viewModel.getDeckDetails(deckId).collectAsState(initial = emptyList())

    // Filtriamo Main e Considering
    val mainDeck = deckItems.filter { !it.isConsidering }
    val consideringDeck = deckItems.filter { it.isConsidering }
    val totalMainCards = mainDeck.sumOf { it.countInDeck }

    var showImportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(deckName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("$totalMainCards / 50 carte", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Importa")
                    }
                }
            )
        }
    ) { padding ->
        if (deckItems.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Mazzo vuoto. Importa una lista!", color = Color.Gray)
            }
        } else {
            // Passiamo alla GRIGLIA a 3 colonne
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SEZIONE MAIN DECK
                item(span = { GridItemSpan(3) }) {
                    SectionHeader(title = "Main Deck", count = totalMainCards, target = 50)
                }

                items(mainDeck) { item ->
                    // Nel Main Deck, usiamo il conteggio totale della collezione
                    DeckCardGridItem(item, deckId, viewModel, availableInCollection = item.countInCollection)
                }

                // SEZIONE CONSIDERING
                if (consideringDeck.isNotEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader(title = "Considering", count = consideringDeck.size, target = null)
                    }

                    items(consideringDeck) { item ->
                        // --- LOGICA CRUCIALE ---
                        // Troviamo quante copie di questa carta sono già usate nel Main Deck
                        val usedInMain = mainDeck.find { it.cardId == item.cardId }?.countInDeck ?: 0
                        // Le copie disponibili per il Considering sono: Totali - Usate nel Main
                        val residualCollection = (item.countInCollection - usedInMain).coerceAtLeast(0)

                        DeckCardGridItem(
                            item = item,
                            deckId = deckId,
                            viewModel = viewModel,
                            availableInCollection = residualCollection
                        )
                    }
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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeckCardGridItem(
    item: DeckWithCount,
    deckId: Int,
    viewModel: DeckViewModel,
    availableInCollection : Int
) {

    val ordered = item.orderedQuantity
    val needed = item.countInDeck

    // --- FIX BUG GIALLO -> VERDE ---
    // Il bordo deve essere giallo solo se NON hai abbastanza carte in collezione
    val borderColor = when {
        availableInCollection >= needed -> Color(0xFF4CAF50) // Verde
        (availableInCollection + ordered) >= needed -> Color(0xFFFFC107) // Giallo
        else -> Color(0xFFEF5350) // Rosso
    }

    var showMenu by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(8.dp))
                .border(2.dp, borderColor, RoundedCornerShape(8.dp))
                .combinedClickable(
                    onClick = { /* Dettaglio */ },
                    onLongClick = { showMenu = true }
                )
        ) {
            // --- FIX IMMAGINI ---
            val cardImage = item.cardImage ?: ""
            val imageModel = if (cardImage.startsWith("http")) cardImage
            else "file:///android_asset/immagini_ottimizzate/$cardImage"

            if (cardImage.isNotBlank()) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onState = { state ->
                        if (state is AsyncImagePainter.State.Error) {
                            android.util.Log.e("DECK_IMAGE_ERR", "Non trovo: $imageModel")
                        }
                    }
                )
            } else {
                // Fallback se l'immagine è vuota nel DB
                Box(Modifier.fillMaxSize().background(Color.DarkGray), contentAlignment = Alignment.Center) {
                    Text(item.cardId, color = Color.White, fontSize = 10.sp)
                }
            }

            // Badge quantità
            Surface(
                Modifier.align(Alignment.BottomEnd),
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(topStart = 8.dp)
            ) {
                Text("x$needed", color = Color.White, modifier = Modifier.padding(horizontal = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Pallini (Stessa logica del bordo per coerenza)
        Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.Center) {
            repeat(needed) { index ->
                val dotColor = when {
                    index < availableInCollection -> Color(0xFF4CAF50) // Verde
                    index < (availableInCollection + ordered) -> Color(0xFFFFC107) // Giallo
                    else -> Color.Gray.copy(alpha = 0.4f) // Grigio
                }
                Box(Modifier.padding(horizontal = 1.dp).size(7.dp).background(dotColor, CircleShape))
            }
        }

        // MENU CONTESTUALE
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Aggiungi 1 copia al mazzo") },
                leadingIcon = { Icon(Icons.Default.Add, null) },
                enabled = needed < 4,
                onClick = {
                    viewModel.updateCardQuantity(deckId, item.cardId, item.isConsidering, needed + 1)
                    showMenu = false
                }
            )

            // --- GESTIONE ORDINATE ---
            // Abilitato solo se mancano carte (rispetto a quelle che hai già usato)
            DropdownMenuItem(
                text = { Text("Segna 1 come Ordinata") },
                leadingIcon = { Icon(Icons.Default.LocalShipping, null) },
                enabled = ordered < (needed - availableInCollection),
                onClick = {
                    viewModel.updateOrderedQuantity(deckId, item.cardId, item.isConsidering, ordered + 1)
                    showMenu = false
                }
            )

            // Opzione per diminuire le ordinate (utile per correggere errori)
            if (ordered > 0) {
                DropdownMenuItem(
                    text = { Text("Rimuovi 1 dalle Ordinate") },
                    leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, null) },
                    onClick = {
                        viewModel.updateOrderedQuantity(deckId, item.cardId, item.isConsidering, ordered - 1)
                        showMenu = false
                    }
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 4.dp))

            DropdownMenuItem(
                text = { Text(if (item.isConsidering) "Sposta in Main" else "Sposta in Considering") },
                leadingIcon = { Icon(Icons.Default.SwapHoriz, null) },
                onClick = {
                    // Passiamo availableInCollection qui!
                    viewModel.moveOneCard(deckId, item.cardId, item.isConsidering, availableInCollection)
                    showMenu = false
                }
            )

            HorizontalDivider(Modifier.padding(vertical = 4.dp))

            DropdownMenuItem(
                text = { Text("Rimuovi 1 copia dal mazzo", color = Color.Red) },
                leadingIcon = { Icon(Icons.Default.Remove, null, tint = Color.Red) },
                onClick = {
                    viewModel.updateCardQuantity(deckId, item.cardId, item.isConsidering, needed - 1)
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
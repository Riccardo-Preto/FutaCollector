package com.ricca.futacollector.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.ricca.futacollector.data.Deck
import com.ricca.futacollector.viewmodel.DeckViewModel
import com.ricca.futacollector.viewmodel.DeckWithLeader

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeckListScreen(
    navController: NavHostController,
    viewModel: DeckViewModel,
    onNavigateToDetail: (Int, String) -> Unit, // Cambiato per passare anche il nome
    onBack: () -> Unit
) {
    val decks by viewModel.allDecks.collectAsState()

    var showNewDeckDialog by remember { mutableStateOf(false) }
    var deckToDelete by remember { mutableStateOf<Deck?>(null) }
    var deckToRename by remember { mutableStateOf<Deck?>(null) }
    var deckForImport by remember { mutableStateOf<Deck?>(null) }
    var menuExpandedDeckId by remember { mutableStateOf<Int?>(null) }

    val gridState = viewModel.gridState

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewDeckDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuovo Mazzo")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(top = 0.dp)) {
            Text(
                text = "I tuoi Mazzi",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp)
            )

            if (decks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp), // Spazio per il FAB
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Un'icona grande e stilizzata
                        Icon(
                            imageVector = Icons.Default.Style,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Nessun mazzo creato",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Clicca sul tasto + per creare il tuo primo mazzo!",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(decks) { item ->
                        Box {
                            DeckCardItem(
                                deckWithLeader = item,
                                onClick = { onNavigateToDetail(item.deck.id, item.deck.name) },
                                onLongClick = { menuExpandedDeckId = item.deck.id }
                            )

                            DropdownMenu(
                                expanded = menuExpandedDeckId == item.deck.id,
                                onDismissRequest = { menuExpandedDeckId = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Importa Lista") },
                                    leadingIcon = { Icon(Icons.Default.ContentPaste, null) },
                                    onClick = {
                                        deckForImport = item.deck
                                        menuExpandedDeckId = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Rinomina") },
                                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                                    onClick = {
                                        deckToRename = item.deck
                                        menuExpandedDeckId = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Elimina", color = Color.Red) },
                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                                    onClick = {
                                        deckToDelete = item.deck
                                        menuExpandedDeckId = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Copia mazzo") },
                                    leadingIcon = { Icon(Icons.Default.CopyAll, null) },
                                    onClick = {
                                        viewModel.copyDeck(item.deck.id)
                                        menuExpandedDeckId = null
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- DIALOGS ---

        deckToDelete?.let { deck ->
            AlertDialog(
                onDismissRequest = { deckToDelete = null },
                title = { Text("Elimina Mazzo") },
                text = { Text("Vuoi davvero eliminare '${deck.name}'?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteDeck(deck)
                        deckToDelete = null
                    }) { Text("Elimina", color = Color.Red) }
                },
                dismissButton = { TextButton(onClick = { deckToDelete = null }) { Text("Annulla") } }
            )
        }

        deckToRename?.let { deck ->
            var newName by remember { mutableStateOf(deck.name) }
            AlertDialog(
                onDismissRequest = { deckToRename = null },
                title = { Text("Rinomina Mazzo") },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nuovo nome") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.renameDeck(deck, newName)
                        deckToRename = null
                    }) { Text("Salva") }
                },
                dismissButton = { TextButton(onClick = { deckToRename = null }) { Text("Annulla") } }
            )
        }

        deckForImport?.let { deck ->
            ImportDeckDialog(
                onDismiss = { deckForImport = null },
                onConfirm = { rawText ->
                    viewModel.importDeckList(deck.id, rawText)
                    deckForImport = null
                }
            )
        }

        if (showNewDeckDialog) {
            NewDeckDialog(
                onDismiss = { showNewDeckDialog = false },
                onConfirm = { name ->
                    showNewDeckDialog = false
                    navController.navigate("select_leader/$name")
                }
            )
        }
    }
}

@Composable
fun DeckCardItem(
    deckWithLeader: DeckWithLeader,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val deck = deckWithLeader.deck
    val leader = deckWithLeader.leaderCard
    val totalCards = deckWithLeader.totalCards
    val ownedCards = deckWithLeader.ownedCards

    val cardImage = leader?.image ?: ""
    val imageModel = if (cardImage.startsWith("http")) cardImage
    else "file:///android_asset/immagini_ottimizzate/$cardImage"

    val progressColor = when {
        totalCards == 0 -> Color.Gray
        ownedCards >= totalCards -> Color(0xFF4CAF50)
        ownedCards >= totalCards * 0.75 -> Color(0xFFFFC107)
        else -> Color(0xFFEF5350)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2A2A2A)))

            if (cardImage.isNotBlank()) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onState = { state ->
                        if (state is AsyncImagePainter.State.Error) {
                            android.util.Log.e("IMAGE_DECK_ERROR", "Errore: $imageModel")
                        }
                    }
                )
            }

            // Gradiente più scuro così il testo si legge meglio
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f),
                                Color.Black.copy(alpha = 0.92f)
                            ),
                            startY = 80f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Nome mazzo
                Text(
                    text = deck.name,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Barra progresso + contatore
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Barra progresso
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        if (totalCards > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(
                                        (ownedCards.toFloat() / totalCards).coerceIn(0f, 1f)
                                    )
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(progressColor)
                            )
                        }
                    }

                    // Contatore
                    Text(
                        text = "$ownedCards / $totalCards",
                        style = MaterialTheme.typography.labelSmall,
                        color = progressColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun NewDeckDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var deckName by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuovo Mazzo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Dai un nome al tuo mazzo per iniziare.")
                OutlinedTextField(
                    value = deckName,
                    onValueChange = { deckName = it },
                    label = { Text("Nome del mazzo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done // Mostra la spunta "Fine"
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() } // Chiude la tastiera
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (deckName.isNotBlank()) onConfirm(deckName) },
                enabled = deckName.isNotBlank()
            ) {
                Text("Scegli Leader")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@Composable
fun ImportDeckDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textToImport by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Importa Lista Simulatore") },
        text = {
            Column {
                Text("Incolla qui la lista nel formato '4xOP01-001'", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = textToImport,
                    onValueChange = { textToImport = it },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    placeholder = { Text("1xOP01-001\n4xOP01-013...") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(textToImport) }) { Text("Importa") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}
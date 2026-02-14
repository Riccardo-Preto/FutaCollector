package com.ricca.futacollector.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
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
import coil.compose.AsyncImage
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
    val totalCards = deckItems.sumOf { it.countInDeck }

    var showImportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(deckName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("$totalCards / 50 carte", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    // TASTO IMPORTA NELLA BARRA IN ALTO
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
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(deckItems) { item ->
                    DeckCardRow(item)
                }
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

@Composable
fun DeckCardRow(item: DeckWithCount) {
    // Logica identica a CardItemView
    val cardImage = item.cardImage ?: ""
    val imageModel = if (cardImage.startsWith("http")) cardImage
    else "file:///android_asset/immagini_ottimizzate/$cardImage"

    // Controllo disponibilità
    val countInCollection = item.countInCollection ?: 0
    val isMissing = countInCollection < item.countInDeck

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMissing)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Immagine
            AsyncImage(
                model = imageModel,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop // Crop per farla stare bene nel quadratino
            )

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = item.cardName ?: "Senza nome",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.cardId,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isMissing) {
                    Text(
                        text = "Possedute: $countInCollection/${item.countInDeck}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Badge Quantità
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "x${item.countInDeck}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
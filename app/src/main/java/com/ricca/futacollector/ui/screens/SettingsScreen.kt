package com.ricca.futacollector.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ricca.futacollector.viewmodel.CollectionViewModel
import com.ricca.futacollector.viewmodel.DeckViewModel

@Composable
fun SettingsScreen(
    darkThemeEnabled: Boolean,
    onDarkThemeToggle: (Boolean) -> Unit,
    viewModel: CollectionViewModel,
    deckViewModel: DeckViewModel
) {
    val collection by viewModel.collectionCards.collectAsState()
    val context = LocalContext.current

    var showDeleteCollectionDialog by remember { mutableStateOf(false) }
    var showDeleteDecksDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Impostazioni",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 32.dp)
        )

        // ── SEZIONE 1: PERSONALIZZAZIONE ──────────────────────────
        SettingsSectionLabel("Personalizzazione")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DarkMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(16.dp))
                Text("Tema Scuro", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                Switch(checked = darkThemeEnabled, onCheckedChange = onDarkThemeToggle)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── SEZIONE 2: DATI ───────────────────────────────────────
        SettingsSectionLabel("Dati")

        OutlinedButton(
            onClick = { viewModel.forceRefreshMarketPrices() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Sync, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Aggiorna Prezzi")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                val totalValue = collection.sumOf { it.card.marketPrice * it.count }
                val shareText = "Il valore della mia collezione One Piece è di " +
                        "€${String.format("%.2f", totalValue)}! 🏴‍☠️ " +
                        "Scopri il mio tesoro su FutaCollector."
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                context.startActivity(
                    Intent.createChooser(intent, "Condividi con i tuoi amici pirati")
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Condividi Valore Collezione")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── SEZIONE 3: CANCELLA DATI ──────────────────────────────
        SettingsSectionLabel("Cancella Dati", color = MaterialTheme.colorScheme.error)

        Button(
            onClick = { showDeleteCollectionDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Svuota Collezione", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { showDeleteDecksDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Elimina Tutti i Mazzi", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // ── DIALOGS ───────────────────────────────────────────────────
    if (showDeleteCollectionDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCollectionDialog = false },
            title = { Text("Sei sicuro?") },
            text = { Text("Questa azione cancellerà definitivamente tutte le carte salvate nella tua collezione.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.nukeCollection()
                    showDeleteCollectionDialog = false
                }) {
                    Text("SÌ, CANCELLA TUTTO", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCollectionDialog = false }) { Text("Annulla") }
            }
        )
    }

    if (showDeleteDecksDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDecksDialog = false },
            title = { Text("Eliminare tutti i mazzi?") },
            text = { Text("Questa azione rimuoverà definitivamente tutti i mazzi creati. Le carte in collezione non verranno toccate.") },
            confirmButton = {
                TextButton(onClick = {
                    deckViewModel.nukeDecks()
                    showDeleteDecksDialog = false
                }) {
                    Text("SÌ, ELIMINA MAZZI", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDecksDialog = false }) { Text("Annulla") }
            }
        )
    }
}

@Composable
private fun SettingsSectionLabel(
    text: String,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}
package com.ricca.futacollector.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Share
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Impostazioni",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- SEZIONE TEMA ---
        Text("Personalizzazione", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DarkMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Text("Tema Scuro", modifier = Modifier.weight(1f))
                Switch(checked = darkThemeEnabled, onCheckedChange = onDarkThemeToggle)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SEZIONE DATI E CONDIVISIONE ---
        Text("Dati e Social", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

        // Tasto Condividi
        OutlinedButton(
            onClick = {
                // CALCOLO SEMPLIFICATO: marketPrice è già Double
                val totalValue = collection.sumOf { it.card.marketPrice * it.count }
                val shareText = "Il valore della mia collezione One Piece è di €${String.format("%.2f", totalValue)}! 🏴‍☠️ Scopri il mio tesoro su FutaCollector."

                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(intent, "Condividi con i tuoi amici pirati"))
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Condividi Valore Collezione")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tasto Reset

        var showDeleteCollectionDialog by remember { mutableStateOf(false) }
        var showDeleteDecksDialog by remember { mutableStateOf(false) }

        Text("Gestione Dati", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)

        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // Bottone Svuota Collezione (quello che avevi già)
                Button(
                    onClick = { showDeleteCollectionDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Svuota Collezione")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // NUOVO Bottone Svuota Mazzi
                Button(
                    onClick = { showDeleteDecksDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Elimina Tutti i Mazzi")
                }
            }
        }

        // --- DIALOG CONFERMA MAZZI ---
        if (showDeleteDecksDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDecksDialog = false },
                title = { Text("Eliminare tutti i mazzi?") },
                text = { Text("Questa azione rimuoverà definitivamente tutti i mazzi creati. Le carte in collezione non verranno toccate.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            deckViewModel.nukeDecks()
                            showDeleteDecksDialog = false
                        }
                    ) {
                        Text("SÌ, ELIMINA MAZZI", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDecksDialog = false }) { Text("Annulla") }
                }
            )
        }

        // Dialog di conferma per il Reset
        if (showDeleteCollectionDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteCollectionDialog = false },
                title = { Text("Sei sicuro?") },
                text = { Text("Questa azione cancellerà tutte le carte salvate nel database della tua collezione. Le carte del database generale rimarranno intatte.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.nukeCollection()
                            showDeleteCollectionDialog = false
                        }
                    ) {
                        Text("SÌ, CANCELLA TUTTO", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteCollectionDialog = false }) { Text("Annulla") }
                }
            )
        }
    }
}
package com.ricca.futacollector.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.ricca.futacollector.ApiCard

@Composable
fun CardDetailScreen(
    card: ApiCard,
    onAddToCollection: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ---------- Immagine o placeholder grande ----------
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.7f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!card.card_image.isNullOrEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(card.card_image),
                            contentDescription = card.card_name,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = buildString {
                                    appendLine("Nome: ${card.card_name}")
                                    appendLine("Set: ${card.set_name}")
                                    appendLine("ID: ${card.card_set_id}")
                                    card.inventory_price?.let { appendLine("Inventario: $it€") }
                                    card.market_price?.let { appendLine("Mercato: $it€") }
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ---------- Dati della carta ----------
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp)
                ) {
                    Text("Nome: ${card.card_name}", style = MaterialTheme.typography.titleMedium)
                    Text("Set: ${card.set_name}", style = MaterialTheme.typography.bodyMedium)
                    Text("ID: ${card.card_set_id}", style = MaterialTheme.typography.bodyMedium)
                    card.inventory_price?.let { Text("Inventario: $it€", style = MaterialTheme.typography.bodyMedium) }
                    card.market_price?.let { Text("Mercato: $it€", style = MaterialTheme.typography.bodyMedium) }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ---------- Bottone + per aggiungere alla collezione ----------
                Button(onClick = onAddToCollection) {
                    Text("+ Aggiungi alla collezione")
                }
            }
        }
    }
}

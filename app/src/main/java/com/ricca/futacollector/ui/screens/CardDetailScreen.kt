package com.ricca.futacollector.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.ricca.futacollector.ApiCard
import com.ricca.futacollector.ui.CardDetailMode


@Composable
fun CardDetailScreen(
    card: ApiCard,
    mode: CardDetailMode,
    onAddToCollection: () -> Unit,
    onRemoveFromCollection: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.85f), // Evitiamo che copra tutto tutto lo schermo
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- PARTE SCORREVOLE (Immagine e Info) ---
            Column(
                modifier = Modifier
                    .weight(1f) // Questo prende tutto lo spazio disponibile tranne quello dei bottoni
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Immagine Carta
                Image(
                    painter = rememberAsyncImagePainter(card.card_image),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.7f) // Mantiene le proporzioni delle carte da gioco
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Nome Carta (che può occupare più righe)
                Text(
                    text = card.card_name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = card.set_name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Prezzi o altre info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PriceTag(label = "Market", price = card.market_price ?: 0.0)
                    PriceTag(label = "Inventory", price = card.inventory_price ?: 0.0)
                }
            }

            // --- PARTE FISSA (Bottoni e Copie) ---
            Surface(
                tonalElevation = 4.dp, // Gli dà un leggero distacco visivo
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .navigationBarsPadding(), // Rispetta la barra di sistema se presente
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (mode is CardDetailMode.Collection) {
                        val count = mode.ownedCopies
                        Text(
                            text = if (count == 1) "Hai 1 copia" else "Hai $count copie",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onRemoveFromCollection,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                border = BorderStroke(1.dp, Color.Red)
                            ) {
                                Text("Rimuovi")
                            }
                            Button(
                                onClick = onAddToCollection,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Aggiungi")
                            }
                        }
                    } else {
                        // Bottone standard per quando cerchi
                        Button(
                            onClick = onAddToCollection,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Aggiungi alla collezione")
                        }
                    }
                }
            }
        }
    }
}

// Funzione di comodo per righe di testo pulite
@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
@Composable
fun PriceTag(label: String, price: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = if (price > 0) "€${String.format("%.2f", price)}" else "---",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
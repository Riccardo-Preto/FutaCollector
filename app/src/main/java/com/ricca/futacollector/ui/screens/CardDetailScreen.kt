package com.ricca.futacollector.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.ricca.futacollector.ApiCard
import com.ricca.futacollector.ui.CardDetailMode


@Composable
fun CardDetailScreen(
    card: ApiCard,
    mode: CardDetailMode,
    onAddToCollection: () -> Unit = {},
    onRemoveFromCollection: () -> Unit = {}
) {
    // La Card principale ora occupa il 92% della larghezza e si adatta in altezza
    Card(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .wrapContentHeight()
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Fondamentale: permette di scorrere se il contenuto eccede lo schermo
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ---------- Immagine Grande Arrotondata ----------
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!card.card_image.isNullOrEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(card.card_image),
                            contentDescription = card.card_name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop // Riempie bene il box
                        )
                    } else {
                        // Placeholder se l'immagine manca
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ---------- Box Dati della Carta (Stile Glicine) ----------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = card.card_name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)
                )

                DetailRow("Set", card.set_name)
                DetailRow("ID Seriale", card.card_set_id)

                card.inventory_price?.let {
                    DetailRow("Prezzo Inventario", "${it}€")
                }
                card.market_price?.let {
                    DetailRow("Prezzo Mercato", "${it}€")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (mode) {

                is CardDetailMode.Search -> {

                    Button(
                        onClick = onAddToCollection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("+ Aggiungi alla collezione")
                    }
                }

                is CardDetailMode.Collection -> {

                    Text(
                        text = "Hai ${mode.ownedCopies} copie",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        Button(
                            onClick = onAddToCollection,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Aggiungi copia")
                        }

                        OutlinedButton(
                            onClick = onRemoveFromCollection,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Rimuovi copia")
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
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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.ricca.futacollector.data.Card
import com.ricca.futacollector.ui.CardDetailMode

@Composable
fun CardDetailScreen(
    card: Card,
    mode: CardDetailMode,
    onAddToCollection: () -> Unit,
    onRemoveFromCollection: () -> Unit
) {
    val cardName = card.name ?: "Nome Sconosciuto"
    val cardImage = card.image ?: ""

    val imagePainter = rememberAsyncImagePainter(
        model = if (cardImage.startsWith("http")) {
            cardImage
        } else {
            "file:///android_asset/immagini_ottimizzate/$cardImage"
        }
    )

    Card(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.90f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = imagePainter,
                    contentDescription = cardName,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .aspectRatio(0.71f)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = cardName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = card.id,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        DetailRow(label = "Rarità", value = card.rarity)
                        DetailRow(label = "Colore", value = card.color)
                        DetailRow(label = "Tipo", value = card.type)
                        DetailRow(label = "Costo", value = card.cost)
                        DetailRow(label = "Potere", value = card.power)
                        DetailRow(label = "Counter", value = card.counter)
                        DetailRow(label = "Attributo", value = card.attribute)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                PriceTag(label = "Market Price", price = card.marketPrice)

                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- SEZIONE BOTTONI ---
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (mode is CardDetailMode.Collection) {
                        val count = mode.ownedCopies
                        Text(
                            text = if (count == 1) "1 copia in collezione" else "$count copie in collezione",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onRemoveFromCollection,
                                modifier = Modifier.weight(1.0f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                border = BorderStroke(1.dp, Color.Red),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Rimuovi")
                            }
                            Button(
                                onClick = onAddToCollection,
                                modifier = Modifier.weight(1.0f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Aggiungi")
                            }
                        }
                    } else {
                        Button(
                            onClick = onAddToCollection,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Aggiungi alla collezione", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

// --- QUESTE SONO LE FUNZIONI CHE MANCAVANO ---

@Composable
fun DetailRow(label: String, value: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value ?: "---",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = 16.dp)
        )
    }
}

@Composable
fun PriceTag(label: String, price: Double?) {
    val safePrice = price ?: 0.0
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = if (safePrice > 0.0) "€ ${String.format("%.2f", safePrice)}" else "Prezzo non disponibile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
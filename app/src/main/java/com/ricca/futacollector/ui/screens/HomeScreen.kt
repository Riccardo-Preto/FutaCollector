package com.ricca.futacollector.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.ricca.futacollector.R
import com.ricca.futacollector.viewmodel.CollectionViewModel

@Composable
fun HomeScreen(navController: NavHostController, viewModel: CollectionViewModel = viewModel()) {
    val collection by viewModel.collectionCards.collectAsState()

    // Calcolo statistiche
    val totalCards = collection.sumOf { it.count }
    val totalPrice = collection.sumOf { it.card.marketPrice * it.count }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // 1. BANNER DI BENVENUTO
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.op_01),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                )
                Text(
                    text = "Bentornato, Capitano!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                )
            }
        }

        // 2. SEZIONE STATISTICHE
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(label = "Carte", value = totalCards.toString(), modifier = Modifier.weight(1f))
                // Formattazione prezzo sicura
                val formattedPrice = if (totalPrice <= 0.0) "N/A" else "€%.2f".format(totalPrice)
                StatCard(label = "Valore", value = formattedPrice, modifier = Modifier.weight(1f))
            }
        }

        // 3. ULTIME AGGIUNTE (Titolo)
        item {
            Text(
                text = "Ultime aggiunte",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // 4. LISTA ORIZZONTALE ULTIME CARTE
        item {
            val lastAdded = collection.takeLast(5).reversed()

            if (lastAdded.isEmpty()) {
                Text(
                    "Nessuna carta aggiunta recentemente",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(lastAdded) { item ->
                        // --- FIX QUI: Gestione sicura del nullo ---
                        val safeImage = item.card.image ?: ""

                        val imageModel = if (safeImage.startsWith("http")) {
                            safeImage
                        } else {
                            "file:///android_asset/immagini_ottimizzate/$safeImage"
                        }

                        AsyncImage(
                            model = imageModel,
                            contentDescription = item.card.name ?: "Carta",
                            modifier = Modifier
                                .width(120.dp)
                                .aspectRatio(0.7f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        }
    }
}
package com.ricca.futacollector

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ricca.futacollector.data.Card

@Composable
fun CardItemView(
    card: Card,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Gestione sicura della stringa immagine con valore di fallback
    val cardImage = card.image ?: ""
    val imageModel = if (cardImage.startsWith("http")) {
        cardImage
    } else {
        "file:///android_asset/immagini_ottimizzate/${cardImage}"
    }

    Card(
        modifier = modifier
            .padding(4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            Column {
                // ----- IMMAGINE CARTA -----
                AsyncImage(
                    model = imageModel,
                    // Se il nome è null, mettiamo una stringa vuota o un placeholder
                    contentDescription = card.name ?: "Carta One Piece",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.72f),
                    contentScale = ContentScale.Fit
                )

                // ----- SEZIONE TESTO -----
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        // 2. Se il nome è null, mostra "Sconosciuto"
                        text = card.name ?: "Sconosciuto",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${card.id}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // marketPrice è già Double (non null), quindi qui siamo al sicuro
                        PriceBadge(card.marketPrice)
                    }
                }
            }

            // ----- BADGE COUNT -----
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "x$count",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun PriceBadge(price: Double) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primary
    ) {
        Text(
            text = formatPrice(price),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

fun formatPrice(price: Double): String {
    return if (price > 0) String.format("€ %.2f", price) else "-"
}
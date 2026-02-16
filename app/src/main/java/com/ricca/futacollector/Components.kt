package com.ricca.futacollector

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.ricca.futacollector.data.AppConstants
import com.ricca.futacollector.data.Card

@Composable
fun CardItemView(
    card: Card,
    count: Int,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // 1. Preparazione Colori e ID Estetica
    val fullColors = getCardColorsList(card.color, isPastel = false)
    val pastelColors = getCardColorsList(card.color, isPastel = true)
    val mainColor = fullColors.first()
    val isBlackCard = card.color?.lowercase()?.trim() == "black"
    val textColor = if (isBlackCard) MaterialTheme.colorScheme.onSurface else mainColor

    // Pulizia ID: mostra "OP01-001" anche se nel DB è "OP01-001_v2"
    val displayId = card.id.split("_").first()

    // 2. Gradiente Orizzontale Forzato (Da destra a sinistra)
    val backgroundBrush = if (pastelColors.size > 1) {
        Brush.linearGradient(
            colors = pastelColors,
            start = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f),
            end = androidx.compose.ui.geometry.Offset(0f, 0f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(pastelColors.first(), pastelColors.first().copy(alpha = 0.1f)),
            start = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f),
            end = androidx.compose.ui.geometry.Offset(0f, 0f)
        )
    }

    val cardImage = card.image ?: ""
    val imageModel = if (cardImage.startsWith("http")) cardImage
    else "file:///android_asset/immagini_ottimizzate/$cardImage"

    var isError by remember(card.id) { mutableStateOf(cardImage.isBlank()) }

    Card(
        modifier = modifier
            .padding(4.dp)
            .then(
                // 2. Aggiungi il clickable SOLO se onClick non è null
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column {
                // ----- TOP: IMMAGINE CON GRADIENTE -----
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.72f)
                        .background(brush = backgroundBrush),
                    contentAlignment = Alignment.Center
                ) {
                    if (cardImage.isNotBlank()) {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = card.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            onState = { state -> isError = state is AsyncImagePainter.State.Error }
                        )
                    }

                    if (isError) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.BrokenImage,
                                contentDescription = null,
                                tint = textColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = displayId,
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ----- BOTTOM: INFO CARTA (Layout Anti-Rottura) -----
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = card.name ?: "Unknown",
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
                            text = displayId,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = textColor,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f) // Spinge il prezzo a destra
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        PriceBadge(card.marketPrice)
                    }
                }
            }

            // ----- BADGE COPIE POSSEDUTE -----
            if (count > 0) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp),
                    tonalElevation = 4.dp
                ) {
                    Text(
                        text = "x$count",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun PriceBadge(price: Double) {
    val isAvailable = price > 0.0
    val priceInEuro = price * AppConstants.CONVERSION_RATE

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    ) {
        Text(
            // Forza il testo su una riga sola e impedisce l'espansione verticale
            text = if (isAvailable) "€%.2f".format(priceInEuro) else "N/A",
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold
            ),
            color = if (isAvailable) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
fun getCardColorsList(colorName: String?, isPastel: Boolean = false): List<Color> {
    if (colorName == null) return listOf(MaterialTheme.colorScheme.outline)

    val colorParts = colorName.split(Regex("[/\\s+]")).filter { it.isNotBlank() }

    return colorParts.map { part ->
        val baseColor = when (part.lowercase().trim()) {
            "red" -> Color(0xFFEF5350)
            "blue" -> Color(0xFF42A5F5)
            "green" -> Color(0xFF66BB6A)
            "yellow" -> Color(0xFFFFC107)
            "purple" -> Color(0xFFAB47BC)
            "black" -> Color(0xFF546E7A)
            else -> MaterialTheme.colorScheme.outline
        }

        if (isPastel) baseColor.copy(alpha = 0.35f) else baseColor
    }
}
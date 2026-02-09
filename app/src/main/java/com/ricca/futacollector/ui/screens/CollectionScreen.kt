package com.ricca.futacollector.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.ricca.futacollector.ApiCard
import com.ricca.futacollector.RetrofitInstance
import kotlinx.coroutines.launch
import com.ricca.futacollector.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ricca.futacollector.data.Card
import com.ricca.futacollector.ui.CardDetailMode
import com.ricca.futacollector.viewmodel.CardWithCount
import com.ricca.futacollector.viewmodel.CollectionViewModel




@Composable
fun CollectionScreen(
    viewModel: CollectionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val collectionByCount by viewModel.collectionCards.collectAsState()
    var selectedItemForDetail by remember { mutableStateOf<CardWithCount?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "La mia Collezione",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3), // 3 colonne per la collezione
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(collectionByCount) { item ->
                CardItemView(
                    card = item.card,
                    count = item.count,
                    onClick = { selectedItemForDetail = item }
                )
            }
        }
    }

    // Popup Dettaglio
    selectedItemForDetail?.let { selectedItem ->

        val count by viewModel
            .getCardCount(selectedItem.card.id, selectedItem.card.image)
            .collectAsState(initial = selectedItem.count)

        // Chiude automaticamente il popup
        if (count == 0) {
            selectedItemForDetail = null
            return@let
        }

        val dbCard = selectedItem.card

        val apiCardEquivalent = ApiCard(
            card_set_id = dbCard.id,
            card_name = dbCard.name,
            card_image = dbCard.image,
            set_name = dbCard.setName,
            inventory_price = dbCard.inventoryPrice.toDoubleOrNull() ?: 0.0,
            market_price = dbCard.marketPrice.toDoubleOrNull() ?: 0.0
        )

        Dialog(onDismissRequest = { selectedItemForDetail = null }) {
            CardDetailScreen(
                card = apiCardEquivalent,
                mode = CardDetailMode.Collection(count),
                onAddToCollection = {
                    viewModel.addCardToCollection(apiCardEquivalent)
                },
                onRemoveFromCollection = {
                    viewModel.removeCardFromCollection(dbCard.id, dbCard.image)
                }
            )
        }
    }

}

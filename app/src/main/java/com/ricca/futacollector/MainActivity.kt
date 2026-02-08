package com.ricca.futacollector

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.grid.*
import coil.compose.AsyncImage


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen()
        }
    }
}

@Composable
fun MainScreen() {

    var showSearch by remember { mutableStateOf(false) }

    if (showSearch) {
        SearchScreen(
            onBack = { showSearch = false }
        )
    } else {
        HomeScreen(
            onAddCardClick = { showSearch = true }
        )
    }
}

@Composable
fun HomeScreen(onAddCardClick: () -> Unit) {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = onAddCardClick) {
            Text("Aggiungi carta alla collezione")
        }
    }
}

@Composable
fun SearchScreen(onBack: () -> Unit) {

    var searchText by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<ApiCard>>(emptyList()) }


    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Button(onClick = onBack) {
            Text("← Indietro")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Cerca carta") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            scope.launch {
                try {
                    val results = RetrofitInstance.api.getFilteredCards(
                        cardName = searchText
                    )
                    searchResults = results
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }) {
            Text("Cerca")
        }

        Spacer(modifier = Modifier.height(16.dp))

        CardGrid(cards = searchResults)
    }
}

@Composable
fun CardGrid(cards: List<ApiCard>) {

    LazyVerticalGrid(
        columns = GridCells.Adaptive(140.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {

        items(cards) { card ->
            CardItem(card)
        }
    }
}

@Composable
fun CardItem(card: ApiCard) {

    Column(
        modifier = Modifier.padding(4.dp)
    ) {

        AsyncImage(
            model = card.card_image,
            contentDescription = card.card_name,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = card.card_name,
            maxLines = 1
        )
    }
}

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

    var selectedSetId by remember { mutableStateOf<String?>(null) }
    var allSets by remember { mutableStateOf<List<CardSet>>(emptyList()) }
    val scope = rememberCoroutineScope()

    // Carica i set
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                allSets = RetrofitInstance.api.getAllSets()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Mostra SetListScreen oppure CardSearchScreen se un set è selezionato
    if (selectedSetId == null) {
        SetListScreen(allSets = allSets) { clickedSetId ->
            selectedSetId = clickedSetId
        }
    } else {
        CardSearchScreen(setId = selectedSetId!!) {
            selectedSetId = null // bottone back per tornare alla lista set
        }
    }
}

@Composable
fun SetListScreen(allSets: List<CardSet>, onSetClick: (String) -> Unit) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize() // molto importante, così LazyColumn sa quanto spazio ha
            .padding(16.dp)
    ) {
        items(allSets) { set ->
            Text(
                text = set.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSetClick(set.card_id) }
                    .padding(8.dp)
            )
            Divider()
        }
    }
}

@Composable
fun CardSearchScreen(setId: String, onBack: () -> Unit) {

    var searchText by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Card>>(emptyList()) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier
        .fillMaxSize() // importantissimo
        .padding(16.dp)) {

        Button(onClick = { onBack() }) {
            Text("← Torna ai set")
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
                } catch(e: Exception) {
                    e.printStackTrace()
                    searchResults = emptyList()
                }
            }
        }) {
            Text("Cerca")
        }


        Spacer(modifier = Modifier.height(16.dp))

        // LazyColumn con peso per riempire lo spazio disponibile
        LazyColumn(modifier = Modifier.fillMaxHeight()) {
            items(searchResults) { card ->
                Text(card.name)
                Divider()
            }
        }
    }
}


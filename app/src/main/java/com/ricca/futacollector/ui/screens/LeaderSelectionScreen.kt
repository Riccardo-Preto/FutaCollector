package com.ricca.futacollector.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ricca.futacollector.CardItemView
import com.ricca.futacollector.viewmodel.CollectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderSelectionScreen(
    onLeaderSelected: (String) -> Unit,
    onBack: () -> Unit,
    collectionViewModel: CollectionViewModel
) {
    val focusManager = LocalFocusManager.current

    // 1. Stato locale per la ricerca dei Leader
    var leaderSearchQuery by rememberSaveable { mutableStateOf("") }

    // 2. Osserviamo la lista completa dei leader dal DB
    val allLeaders by remember {
        collectionViewModel.getLeadersOnly()
    }.collectAsState(initial = emptyList())

    // 3. Filtriamo la lista in base a quello che scrive l'utente
    val filteredLeaders = remember(leaderSearchQuery, allLeaders) {
        if (leaderSearchQuery.isBlank()) {
            allLeaders
        } else {
            val terms = leaderSearchQuery.lowercase().split(" ")
            allLeaders.filter { leader ->
                terms.all { term ->
                    leader.name?.lowercase()?.contains(term) == true ||
                            leader.id.lowercase().contains(term) ||
                            leader.setId?.lowercase()?.contains(term) == true
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seleziona Leader", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // --- BARRA DI RICERCA (Stile SearchScreen) ---
            TextField(
                value = leaderSearchQuery,
                onValueChange = { leaderSearchQuery = it },
                placeholder = { Text("Cerca Leader") },
                singleLine = true,
                trailingIcon = {
                    if (leaderSearchQuery.isNotBlank()) {
                        IconButton(onClick = {
                            leaderSearchQuery = ""
                            focusManager.clearFocus()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Svuota")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            // --- GRIGLIA RISULTATI ---
            if (filteredLeaders.isEmpty() && allLeaders.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nessun Leader corrisponde alla ricerca", color = Color.Gray)
                }
            } else if (allLeaders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredLeaders, key = { it.id }) { leaderCard ->
                        CardItemView(
                            card = leaderCard,
                            count = 0,
                            onClick = {
                                focusManager.clearFocus()
                                onLeaderSelected(leaderCard.id)
                            }
                        )
                    }
                }
            }
        }
    }
}
package com.ricca.futacollector.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider // <--- AGGIUNGI QUESTO
import androidx.lifecycle.viewModelScope
import com.ricca.futacollector.data.Card
import com.ricca.futacollector.data.CardDao
import com.ricca.futacollector.data.Deck
import com.ricca.futacollector.data.DeckCard
import com.ricca.futacollector.data.DeckCardDetails
import com.ricca.futacollector.data.DeckDao
import com.ricca.futacollector.data.DeckWithCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DeckWithLeader(
    val deck: Deck,
    val leaderCard: Card?
)

class DeckViewModel(
    private val deckDao: DeckDao,
    private val cardDao: CardDao // <--- Aggiungiamo questo!
) : ViewModel() {

    // Flusso costante dei mazzi dal DB
    // Nel DeckViewModel.kt
    val allDecks: StateFlow<List<DeckWithLeader>> = deckDao.getAllDecks()
        .map { decks ->
            decks.map { deck ->
                // Assicurati che cardDao.getCardById non ritorni null per errore di query
                val leader = cardDao.getCardById(deck.leaderCardId)
                android.util.Log.d("DECK_DEBUG", "Mazzo: ${deck.name}, Leader ID: ${deck.leaderCardId}, Trovato: ${leader?.name}")
                DeckWithLeader(deck, leader)
            }
        }
        .flowOn(Dispatchers.IO) // Forza l'esecuzione in background
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createDeck(name: String, leaderId: String) {
        viewModelScope.launch {
            deckDao.insertDeck(Deck(name = name, leaderCardId = leaderId))
        }
    }

    fun deleteDeck(deck: Deck) {
        viewModelScope.launch {
            deckDao.deleteDeck(deck)
        }
    }

    fun renameDeck(deck: Deck, newName: String) {
        viewModelScope.launch {
            deckDao.renameDeck(deck.id, newName)
        }
    }
    fun importDeckList(deckId: Int, rawList: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val lines = rawList.lines().map { it.trim() }.filter { it.isNotBlank() }

            // Log per vedere quante righe stiamo leggendo
            android.util.Log.d("IMPORT", "Inizio importazione per mazzo $deckId. Righe totali: ${lines.size}")

            lines.forEach { line ->
                // Questa Regex è più permissiva: cerca numero, 'x' opzionale, e l'ID
                val match = Regex("""(\d+)[xX\s-]*([A-Z0-9-]+)""").find(line)

                if (match != null) {
                    val quantity = match.groupValues[1].toInt()
                    val cardId = match.groupValues[2].trim()

                    // Verifichiamo se la carta esiste davvero nel DB
                    val card = cardDao.getCardById(cardId)
                    if (card != null) {
                        deckDao.insertDeckCard(DeckCard(deckId, cardId, quantity))
                        android.util.Log.d("IMPORT", "Aggiunta: $cardId x$quantity")
                    } else {
                        android.util.Log.e("IMPORT", "CARTA NON TROVATA NEL DB: $cardId")
                    }
                } else {
                    android.util.Log.e("IMPORT", "RIGA NON VALIDA: $line")
                }
            }
        }
    }

    fun getDeckDetails(deckId: Int): Flow<List<DeckWithCount>> {
        return deckDao.getDeckDetails(deckId)
    }
}
class DeckViewModelFactory(
    private val deckDao: DeckDao,
    private val cardDao: CardDao // <--- Aggiungiamo questo
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeckViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Ora passiamo entrambi i DAO al ViewModel
            return DeckViewModel(deckDao, cardDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


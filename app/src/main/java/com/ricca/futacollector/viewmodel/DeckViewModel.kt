package com.ricca.futacollector.viewmodel

import android.util.Log
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider // <--- AGGIUNGI QUESTO
import androidx.lifecycle.viewModelScope
import com.ricca.futacollector.data.Card
import com.ricca.futacollector.data.CardDao
import com.ricca.futacollector.data.Deck
import com.ricca.futacollector.data.DeckCard
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
    private val cardDao: CardDao
) : ViewModel() {

    val allDecks: StateFlow<List<DeckWithLeader>> = deckDao.getAllDecks()
        .map { decks ->
            decks.map { deck ->
                val leader = cardDao.getCardById(deck.leaderCardId)
                DeckWithLeader(deck, leader)
            }
        }
        .flowOn(Dispatchers.IO)
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

    fun getDeckDetails(deckId: Int): Flow<List<DeckWithCount>> = deckDao.getDeckDetails(deckId)

    // Aggiorna la quantità totale di una carta nel mazzo/considering
    fun updateCardQuantity(deckId: Int, cardId: String, isConsidering: Boolean, newQuantity: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            if (newQuantity <= 0) {
                deckDao.deleteSpecificDeckCard(deckId, cardId, isConsidering)
            } else {
                val current = deckDao.getSpecificDeckCard(deckId, cardId, isConsidering)
                deckDao.insertDeckCard(
                    DeckCard(
                        deckId = deckId,
                        cardId = cardId,
                        quantity = newQuantity,
                        isConsidering = isConsidering,
                        orderedQuantity = current?.orderedQuantity ?: 0
                    )
                )
            }
        }
    }

    // Aggiorna quante copie di quella riga sono state ordinate
    fun updateOrderedQuantity(deckId: Int, cardId: String, isConsidering: Boolean, newOrderedQty: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = deckDao.getSpecificDeckCard(deckId, cardId, isConsidering)
            val maxAllowed = current?.quantity ?: 0
            // Evitiamo di ordinare più di quante ne servono nel mazzo
            val finalQty = newOrderedQty.coerceIn(0, maxAllowed)
            deckDao.updateOrderedQuantity(deckId, cardId, isConsidering, finalQty)
        }
    }

    fun moveOneCard(deckId: Int, cardId: String, fromConsidering: Boolean, availableHere: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val source = deckDao.getSpecificDeckCard(deckId, cardId, fromConsidering)
            val target = deckDao.getSpecificDeckCard(deckId, cardId, !fromConsidering)

            if (source != null && source.quantity > 0) {
                val totalNeeded = source.quantity
                val countOrdered = source.orderedQuantity
                val countPossessed = availableHere

                // 1. Quante grigie ci sono?
                val countMissingPura = (totalNeeded - countPossessed - countOrdered).coerceAtLeast(0)

                val moveOrder: Boolean
                if (!fromConsidering) {
                    // --- LOGICA DA MAIN A CONSIDERING (Priorità: Grigia > Gialla > Verde) ---
                    moveOrder = when {
                        countMissingPura > 0 -> false // Sposta la grigia (l'ordinata resta nel main)
                        countOrdered > 0 && totalNeeded > countPossessed -> true // Sposta la gialla
                        else -> false // Sposta la verde
                    }
                } else {
                    // --- LOGICA DA CONSIDERING A MAIN (Priorità: Gialla > Verde > Grigia) ---
                    moveOrder = when {
                        countOrdered > 0 -> true // Sposta l'ordinata per prima!
                        countPossessed > 0 -> false // Poi sposta la verde
                        else -> false // Infine la grigia
                    }
                }

                // --- ESECUZIONE SPOSTAMENTO ---
                // 1. Aggiorna Sorgente
                if (source.quantity > 1) {
                    val newOrderedSource = if (moveOrder) source.orderedQuantity - 1 else source.orderedQuantity
                    deckDao.insertDeckCard(source.copy(
                        quantity = source.quantity - 1,
                        orderedQuantity = newOrderedSource.coerceAtLeast(0)
                    ))
                } else {
                    deckDao.deleteSpecificDeckCard(deckId, cardId, fromConsidering)
                }

                // 2. Aggiorna Destinazione
                val newTargetQuantity = (target?.quantity ?: 0) + 1
                val newOrderedTarget = (target?.orderedQuantity ?: 0) + (if (moveOrder) 1 else 0)

                deckDao.insertDeckCard(
                    DeckCard(
                        deckId = deckId,
                        cardId = cardId,
                        quantity = newTargetQuantity,
                        isConsidering = !fromConsidering,
                        orderedQuantity = newOrderedTarget
                    )
                )
            }
        }
    }
    fun importDeckList(deckId: Int, rawList: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val lines = rawList.lines().map { it.trim() }.filter { it.isNotBlank() }
            lines.forEach { line ->
                val match = Regex("""(\d+)[xX\s-]*([A-Z0-9-]+)""").find(line)
                if (match != null) {
                    val quantity = match.groupValues[1].toInt()
                    val cardId = match.groupValues[2].trim()
                    val card = cardDao.getCardById(cardId)
                    if (card != null) {
                        deckDao.insertDeckCard(DeckCard(deckId, cardId, quantity))
                    }
                }
            }
        }
    }

    val gridState = LazyGridState() // Stato dello scroll persistente nel ViewModel

    fun resetScroll() {
        viewModelScope.launch {
            gridState.scrollToItem(0)
        }
    }

    fun nukeDecks() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                deckDao.deleteAllDecks() // Assicurati che questo metodo esista nel tuo DeckDao
                // Se vuoi inviare un feedback all'utente tramite un canale eventi:
                //_uiEvents.send("Tutti i mazzi sono stati eliminati 🗑️")
            } catch (e: Exception) {
                Log.e("DECK_VIEWMODEL", "Errore nuke mazzi: ${e.message}")
            }
        }
    }

    fun addSingleCardToDeck(deckId: Int, card: Card, isConsidering: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = deckDao.getSpecificDeckCard(deckId, card.id, isConsidering)
            if (existing != null) {
                if (existing.quantity < 4) {
                    deckDao.updateCardQuantity(deckId, card.id, isConsidering, existing.quantity + 1)
                }
            } else {
                deckDao.insertDeckCard(
                    DeckCard(
                        deckId = deckId,
                        cardId = card.id,
                        quantity = 1,
                        isConsidering = isConsidering
                    )
                )
            }
        }
    }

    // NEL VIEWMODEL
    fun addMultipleCardsToDeck(deckId: Int, card: Card, qtyToAdd: Int, isConsidering: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            // DEBUG: Aggiungi questo log per vedere se l'ID è giusto
            Log.d("DECK_ADD", "Aggiungo $qtyToAdd copie di ${card.id} al mazzo $deckId")

            val existing = deckDao.getSpecificDeckCard(deckId, card.id, isConsidering)
            val currentQty = existing?.quantity ?: 0
            val newQty = (currentQty + qtyToAdd).coerceAtMost(4)

            deckDao.insertDeckCard(
                DeckCard(
                    deckId = deckId,
                    cardId = card.id,
                    quantity = newQty,
                    isConsidering = isConsidering,
                    orderedQuantity = existing?.orderedQuantity ?: 0
                )
            )
        }
    }

    // DeckViewModel.kt
    suspend fun getCardById(cardId: String): Card? {
        return cardDao.getCardById(cardId)
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


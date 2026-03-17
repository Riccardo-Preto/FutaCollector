package com.ricca.futacollector.viewmodel

import android.util.Log
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ricca.futacollector.data.Card
import com.ricca.futacollector.data.CardDao
import com.ricca.futacollector.data.Deck
import com.ricca.futacollector.data.DeckCard
import com.ricca.futacollector.data.DeckDao
import com.ricca.futacollector.data.DeckWithCount
import com.ricca.futacollector.data.OrderedCardDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine

data class DeckWithLeader(
    val deck: Deck,
    val leaderCard: Card?,
    val totalCards: Int = 0,
    val ownedCards: Int = 0
)

class DeckViewModel(
    private val deckDao: DeckDao,
    private val cardDao: CardDao,
    private val orderedCardDao: OrderedCardDao
) : ViewModel() {

    val allDecks: StateFlow<List<DeckWithLeader>> = combine(
        deckDao.getAllDecks(),
        deckDao.getAllDeckCardsFlow(),
        cardDao.getAllCollectionItems()
    ) { decks, _, _ ->
        // Ogni volta che cambia uno dei tre, ricalcola tutto
        decks.map { deck ->
            val leader = cardDao.getCardById(deck.leaderCardId)
            val deckCards = deckDao.getDeckDetailsOneShot(deck.id)
            val totalCards = deckCards.sumOf { it.countInDeck }
            val ownedCards = deckCards.sumOf { minOf(it.countInDeck, it.countInCollection) }
            DeckWithLeader(deck, leader, totalCards, ownedCards)
        }
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // In cima alla classe DeckViewModel
    private val _importResult = MutableStateFlow<String?>(null)
    val importResult: StateFlow<String?> = _importResult.asStateFlow()

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
                deckDao.insertDeckCard(
                    DeckCard(
                        deckId = deckId,
                        cardId = cardId,
                        quantity = newQuantity,
                        isConsidering = isConsidering
                    )
                )
            }
        }
    }

    fun moveOneCard(deckId: Int, cardId: String, fromConsidering: Boolean, availableHere: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val source = deckDao.getSpecificDeckCard(deckId, cardId, fromConsidering)
            val target = deckDao.getSpecificDeckCard(deckId, cardId, !fromConsidering)

            if (source != null && source.quantity > 0) {
                if (source.quantity > 1) {
                    deckDao.insertDeckCard(source.copy(quantity = source.quantity - 1))
                } else {
                    deckDao.deleteSpecificDeckCard(deckId, cardId, fromConsidering)
                }

                deckDao.insertDeckCard(
                    DeckCard(
                        deckId = deckId,
                        cardId = cardId,
                        quantity = (target?.quantity ?: 0) + 1,
                        isConsidering = !fromConsidering
                    )
                )
            }
        }
    }

    fun importDeckList(deckId: Int, rawList: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val lines = rawList.lines().map { it.trim() }.filter { it.isNotBlank() }

            var imported = 0
            val notFound = mutableListOf<String>()
            val skipped = mutableListOf<String>()

            lines.forEach { line ->
                val match = Regex("""(\d+)[xX\s-]*([A-Z0-9-]+)""").find(line)
                if (match == null) {
                    skipped.add(line)
                    return@forEach
                }

                val quantity = match.groupValues[1].toInt()
                val cardId = match.groupValues[2].trim()
                val card = cardDao.getCardById(cardId)

                if (card != null) {
                    deckDao.insertDeckCard(DeckCard(deckId, cardId, quantity))
                    imported++
                } else {
                    notFound.add("$quantity x $cardId")
                }
            }

            // Costruisci messaggio finale
            val sb = StringBuilder()
            sb.append("✅ $imported carte importate")

            if (notFound.isNotEmpty()) {
                sb.append("\n⚠️ Non trovate (${notFound.size}): ${notFound.joinToString(", ")}")
            }
            if (skipped.isNotEmpty()) {
                sb.append("\n⏭️ Righe ignorate (${skipped.size})")
            }

            // Usa il channel degli eventi per mandare il messaggio
            viewModelScope.launch(Dispatchers.Main) {
                _importResult.value = sb.toString()
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
                    deckDao.insertDeckCard(existing.copy(quantity = existing.quantity + 1))
                }
            } else {
                deckDao.insertDeckCard(DeckCard(deckId, card.id, 1, isConsidering))
            }
        }
    }

    fun addMultipleCardsToDeck(deckId: Int, card: Card, qtyToAdd: Int, isConsidering: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = deckDao.getSpecificDeckCard(deckId, card.id, isConsidering)
            val currentQty = existing?.quantity ?: 0
            val newQty = (currentQty + qtyToAdd).coerceAtMost(4)
            deckDao.insertDeckCard(DeckCard(deckId, card.id, newQty, isConsidering))
        }
    }

    // DeckViewModel.kt
    suspend fun getCardById(cardId: String): Card? {
        return cardDao.getCardById(cardId)
    }

    fun addCardToCollectionFromDeck(
        cardId: String,
        onCardFound: (Card) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val fullCard = cardDao.getCardById(cardId)
            if (fullCard != null) {
                launch(Dispatchers.Main) {
                    onCardFound(fullCard)
                }
            }
        }
    }

    fun addAllDeckCardsToCollection(deckId: Int, collectionViewModel: CollectionViewModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val deckItems = deckDao.getDeckDetailsOneShot(deckId)
            var totalAdded = 0
            deckItems.forEach { item ->
                collectionViewModel.addCardSilently(item.cardId, item.countInDeck)
                totalAdded += item.countInDeck
            }
            collectionViewModel.sendEvent("$totalAdded carte aggiunte alla collezione! ✅")
        }
    }

    val missingCards: StateFlow<List<DeckDao.MissingCard>> = combine(
        deckDao.getMissingCardsForAllDecks(),
        cardDao.getAllCollectionItems()
    ) { missing, _ -> missing }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearImportResult() {
        _importResult.value = null
    }

    fun deleteDeckById(deckId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val deck = allDecks.value.find { it.deck.id == deckId }?.deck
            if (deck != null) deckDao.deleteDeck(deck)
        }
    }

    fun clearDeck(deckId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            deckDao.deleteAllDeckCards(deckId)
        }
    }

    fun copyDeck(deckId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val originalDeck = allDecks.value.find { it.deck.id == deckId }?.deck ?: return@launch

            val newDeckId = deckDao.insertDeck(
                Deck(
                    name = "Copia di ${originalDeck.name}",
                    leaderCardId = originalDeck.leaderCardId
                )
            ).toInt()

            val allCards = deckDao.getAllDeckCardsOneShot(deckId)
            allCards.forEach { card ->
                deckDao.insertDeckCard(
                    DeckCard(
                        deckId = newDeckId,
                        cardId = card.cardId,
                        quantity = card.countInDeck,
                        isConsidering = card.isConsidering
                    )
                )
            }
        }
    }
}
class DeckViewModelFactory(
    private val deckDao: DeckDao,
    private val cardDao: CardDao,
    private val orderedCardDao: OrderedCardDao  // aggiunto
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeckViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DeckViewModel(deckDao, cardDao, orderedCardDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


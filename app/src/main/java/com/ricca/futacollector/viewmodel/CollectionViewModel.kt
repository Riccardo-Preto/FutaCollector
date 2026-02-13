package com.ricca.futacollector.viewmodel

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ricca.futacollector.data.Card
import com.ricca.futacollector.data.CardDao
import com.ricca.futacollector.data.CardSetEntity
import com.ricca.futacollector.data.UserCardEntity
import com.ricca.futacollector.data.api.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Card con count completo: contiene l'intera Card dal DB + quante copie abbiamo
 */
data class CardWithCount(
    val card: Card,
    val count: Int
)

class CollectionViewModel(
    private val cardDao: CardDao
) : ViewModel() {

    // --- SCROLL STATE ---
    var gridState: LazyGridState? = null
    var listState: LazyListState? = null

    init {
        refreshMarketPrices()
    }

    // --- MOSTRA LISTA SET ---
    private val _showSetList = MutableStateFlow(true)
    val showSetList: StateFlow<Boolean> = _showSetList.asStateFlow()

    // --- COLLEZIONE ---
    val collectionCards: StateFlow<List<CardWithCount>> =
        cardDao.getAllCollectionItems()
            .map { userItems ->
                userItems.mapNotNull { userItem ->
                    // Recuperiamo la Card completa dal DB usando cardId
                    val card = cardDao.getCardById(userItem.cardId)
                    if (card != null) {
                        CardWithCount(card = card, count = userItem.count)
                    } else null
                }.sortedBy { it.card.id }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- EVENTI UI ---
    private val _uiEvents = Channel<String>()
    val uiEvents = _uiEvents.receiveAsFlow()

    // --- GESTIONE COPIE ---
    fun getCardCount(cardId: String): Flow<Int> = collectionCards.map { list ->
        list.find { it.card.id == cardId }?.count ?: 0
    }

    fun addCardToCollection(card: Card) {
        viewModelScope.launch {
            try {
                val currentCount = collectionCards.value.find { it.card.id == card.id }?.count ?: 0
                cardDao.insertUserCard(UserCardEntity(cardId = card.id, count = currentCount + 1))
                _uiEvents.send("${card.name ?: "Carta"} aggiunta!")
            } catch (e: Exception) {
                _uiEvents.send("Errore nel salvataggio")
            }
        }
    }

    fun removeCardFromCollection(card: Card) {
        viewModelScope.launch {
            try {
                val currentCount = collectionCards.value.find { it.card.id == card.id }?.count ?: 0
                when {
                    currentCount > 1 -> {
                        cardDao.insertUserCard(UserCardEntity(cardId = card.id, count = currentCount - 1))
                        _uiEvents.send("Una copia rimossa")
                    }
                    currentCount == 1 -> {
                        cardDao.deleteUserCard(card.id)
                        _uiEvents.send("Carta rimossa dalla collezione")
                    }
                    else -> _uiEvents.send("Nessuna copia presente")
                }
            } catch (e: Exception) {
                _uiEvents.send("Errore nella rimozione")
            }
        }
    }

    fun nukeCollection() {
        viewModelScope.launch {
            try {
                cardDao.deleteAllUserCards()
                _uiEvents.send("Collezione svuotata! 🏴‍☠️")
            } catch (e: Exception) {
                _uiEvents.send("Errore durante lo svuotamento")
            }
        }
    }

    // --- SEARCH / FILTRI ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<CardWithCount>>(emptyList())
    val searchResults: StateFlow<List<CardWithCount>> = _searchResults.asStateFlow()

    fun searchCards(query: String) {
        viewModelScope.launch {
            _searchQuery.value = query
            if (query.isBlank()) {
                _searchResults.value = emptyList()
                _showSetList.value = true
            } else {
                // PASSAGGI: Passiamo la stringa 'query', non la lista keywords
                val results = cardDao.searchAdvanced(query)

                val enriched = results.map { card ->
                    val count = collectionCards.value.find { it.card.id == card.id }?.count ?: 0
                    CardWithCount(card, count)
                }
                _searchResults.value = enriched
                _showSetList.value = false
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _showSetList.value = true
    }

    fun resetSearchOnTabReselect() {
        clearSearch()
    }

    // Aggiungi questi dentro la classe CollectionViewModel
    val allSets: StateFlow<List<CardSetEntity>> = cardDao.getAllSets() // Assicurati di avere getAllSets nel DAO
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getCardsFromSet(setId: String) {
        viewModelScope.launch {
            _searchQuery.value = "" // Puliamo la ricerca testuale
            val results = cardDao.getCardsBySet(setId)
            val enriched = results.map { card ->
                val count = collectionCards.value.find { it.card.id == card.id }?.count ?: 0
                CardWithCount(card, count)
            }
            _searchResults.value = enriched
            _showSetList.value = false
        }
    }

    fun refreshMarketPrices() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("SYNC", "Inizio download dati...")

                val setCards = RetrofitInstance.api.getAllSetCards()
                val stCards = RetrofitInstance.api.getAllSTCards()
                val promoCards = RetrofitInstance.api.getAllPromos()

                val allApiCards = setCards + stCards + promoCards
                Log.d("SYNC", "Totale scaricati: ${allApiCards.size}. Inizio transazione DB...")

                // Eseguiamo tutto in una transazione
                val updatedCount = cardDao.updateAllPricesSmart(allApiCards)

                Log.d("SYNC", "Update transazionale completato. Righe modificate: $updatedCount")

                if (updatedCount > 0) {
                    _uiEvents.send("Prezzi aggiornati: $updatedCount varianti sincronizzate")
                }

            } catch (e: Exception) {
                Log.e("SYNC", "ERRORE CRITICO: ${e.message}")
            }
        }
    }
}

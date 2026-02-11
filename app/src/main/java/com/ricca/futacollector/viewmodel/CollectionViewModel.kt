package com.ricca.futacollector.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ricca.futacollector.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel

/**
 * Classe di supporto per la UI: contiene la carta (dati catalogo)
 * e quante copie ne abbiamo in collezione (dati utente)
 */
data class CardWithCount(
    val card: Card,
    val count: Int
)

class CollectionViewModel(application: Application) : AndroidViewModel(application) {

    private val cardDao = AppDatabase.getDatabase(application).cardDao()

    // 1. Tutti i SET (Tabella 'sets')
    val allSets: StateFlow<List<CardSetEntity>> = cardDao.getAllSetsOrdered()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. Risultati della ricerca (Tabella 'cards')
    private val _searchResults = MutableStateFlow<List<Card>>(emptyList())
    val searchResults: StateFlow<List<Card>> = _searchResults.asStateFlow()

    // 3. La tua Collezione Personale
    // Trasformiamo i CollectionItem di Room nei tuoi CardWithCount per la UI
    val collectionCards: StateFlow<List<CardWithCount>> = cardDao.getAllCollectionItems()
        .map { items ->
            items.map { item ->
                CardWithCount(
                    card = item.card,
                    count = item.userCard.count
                )
            }.sortedBy { it.card.id }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Canale per messaggi alla UI
    private val _uiEvents = Channel<String>()
    val uiEvents = _uiEvents.receiveAsFlow()

    // --- LOGICA DI RICERCA (Catalogo) ---

    fun searchCards(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
            } else {
                _searchResults.value = cardDao.searchInDatabase(query)
            }
        }
    }

    fun getCardsFromSet(setId: String) {
        viewModelScope.launch {
            _searchResults.value = cardDao.getCardsBySet(setId)
        }
    }


    // --- GESTIONE COPIE ---

    fun getCardCount(cardId: String): Flow<Int> {
        return collectionCards.map { list ->
            list.find { it.card.id == cardId }?.count ?: 0
        }
    }

    // --- AGGIUNTA / RIMOZIONE (Tabella 'user_collection') ---

    fun addCardToCollection(card: Card) {
        viewModelScope.launch {
            try {
                // Recuperiamo il conteggio attuale dalla nostra lista in memoria
                val currentCount = collectionCards.value.find { it.card.id == card.id }?.count ?: 0

                // Inseriamo o aggiorniamo la riga nella tabella utente
                cardDao.insertUserCard(UserCardEntity(cardId = card.id, count = currentCount + 1))

                _uiEvents.send("${card.name ?: "Carta"} aggiunta!")
            } catch (e: Exception) {
                Log.e("FUTA_LOG", "Errore salvataggio: ${e.message}")
                _uiEvents.send("Errore nel salvataggio")
            }
        }
    }

    fun removeCardFromCollection(cardId: String) {
        viewModelScope.launch {
            try {
                val currentCount = collectionCards.value.find { it.card.id == cardId }?.count ?: 0

                if (currentCount > 1) {
                    // Se ne hai più di una, decrementa
                    cardDao.insertUserCard(UserCardEntity(cardId = cardId, count = currentCount - 1))
                    _uiEvents.send("Una copia rimossa")
                } else if (currentCount == 1) {
                    // Se era l'ultima, elimina proprio la riga
                    cardDao.deleteUserCard(cardId)
                    _uiEvents.send("Carta rimossa dalla collezione")
                } else {
                    _uiEvents.send("Nessuna copia presente")
                }
            } catch (e: Exception) {
                Log.e("FUTA_LOG", "Errore rimozione: ${e.message}")
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
}
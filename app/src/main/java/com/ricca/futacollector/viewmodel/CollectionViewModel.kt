package com.ricca.futacollector.viewmodel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ricca.futacollector.ApiCard
import com.ricca.futacollector.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel

/**
 * Classe di supporto per la UI: contiene la carta e quante copie ne abbiamo
 */
data class CardWithCount(
    val card: Card,
    val count: Int
)

class CollectionViewModel(application: Application) : AndroidViewModel(application) {

    private val cardDao = AppDatabase.getDatabase(application).cardDao()

    // --- AGGIUNTA PER GLI AVVISI ---
    private val _uiEvents = kotlinx.coroutines.channels.Channel<String>()
    val uiEvents = _uiEvents.receiveAsFlow()
    // -------------------------------

    fun getCardCount(cardId: String, image: String): Flow<Int> {
        return collectionCards.map { list ->
            list.find { it.card.id == cardId && it.card.image == image }?.count ?: 0
        }
    }

    val collectionCards: StateFlow<List<CardWithCount>> = cardDao.getAllCards()
        .map { list ->
            list.groupBy { "${it.id}_${it.image}" }
                .map { (_, copies) ->
                    CardWithCount(
                        card = copies.first(),
                        count = copies.size
                    )
                }
                .sortedBy { it.card.id }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addCardToCollection(apiCard: ApiCard) {
        viewModelScope.launch {
            try {
                val cardToSave = Card(
                    id = apiCard.card_set_id,
                    name = apiCard.card_name,
                    image = apiCard.card_image ?: "",
                    setName = apiCard.set_name ?: "",
                    inventoryPrice = apiCard.inventory_price.toString(),
                    marketPrice = apiCard.market_price.toString(),
                    dateAdded = System.currentTimeMillis()
                )

                Log.d("FUTA_LOG", "Salvataggio copia di: ${cardToSave.name}")
                cardDao.insertCard(cardToSave)

                // Invece del Toast:
                _uiEvents.send("${cardToSave.name} aggiunta!")

            } catch (e: Exception) {
                Log.e("FUTA_LOG", "ERRORE salvataggio: ${e.message}", e)
                _uiEvents.send("Errore nel salvataggio")
            }
        }
    }

    fun removeCardFromCollection(cardId: String, image: String) {
        viewModelScope.launch {
            try {
                val copies = cardDao.getCardsByIdAndImage(cardId, image)

                if (copies.isNotEmpty()) {
                    cardDao.deleteCard(copies.first())
                    // Invece del Toast:
                    _uiEvents.send("Una copia rimossa")
                } else {
                    // Invece del Toast:
                    _uiEvents.send("Nessuna copia presente!")
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
                cardDao.deleteAll()
                // Usiamo lo stesso stile delle altre notifiche
                _uiEvents.send("Collezione svuotata con successo! 🏴‍☠️")
            } catch (e: Exception) {
                _uiEvents.send("Errore durante lo svuotamento")
            }
        }
    }
}
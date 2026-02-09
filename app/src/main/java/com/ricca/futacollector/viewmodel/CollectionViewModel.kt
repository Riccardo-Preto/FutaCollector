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

/**
 * Classe di supporto per la UI: contiene la carta e quante copie ne abbiamo
 */
data class CardWithCount(
    val card: Card,
    val count: Int
)

class CollectionViewModel(application: Application) : AndroidViewModel(application) {

    // Inizializziamo il DAO
    private val cardDao = AppDatabase.getDatabase(application).cardDao()
    fun getCardCount(cardId: String, image: String): Flow<Int> {
        return collectionCards.map { list ->
            list.find { it.card.id == cardId && it.card.image == image }?.count ?: 0
        }
    }



    /**
     * Trasforma il flusso di tutte le carte del DB in una lista raggruppata.
     * La chiave di raggruppamento è "ID_URLIMMAGINE" per distinguere le Alternate Art.
     */
    val collectionCards: StateFlow<List<CardWithCount>> = cardDao.getAllCards()
        .map { list ->
            list.groupBy { "${it.id}_${it.image}" }
                .map { (_, copies) ->
                    CardWithCount(
                        card = copies.first(),
                        count = copies.size
                    )
                }
                .sortedBy { it.card.id } // Ordine alfabetico/numerico per ID
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Converte una ApiCard in Card del DB e la salva.
     */
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

                Toast.makeText(
                    getApplication(),
                    "${cardToSave.name} aggiunta!",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Log.e("FUTA_LOG", "ERRORE salvataggio: ${e.message}", e)
                Toast.makeText(getApplication(), "Errore nel salvataggio", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun removeCardFromCollection(cardId: String, image: String) {
        viewModelScope.launch {
            try {
                // Cerchiamo tutte le copie di quella carta specifica
                val copies = cardDao.getCardsByIdAndImage(cardId, image)

                if (copies.isNotEmpty()) {
                    // Eliminiamo solo la prima (la più recente o la prima trovata)
                    cardDao.deleteCard(copies.first())
                    Toast.makeText(getApplication(), "Una copia rimossa", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(getApplication(), "Nessuna copia presente!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("FUTA_LOG", "Errore rimozione: ${e.message}")
            }
        }
    }
}
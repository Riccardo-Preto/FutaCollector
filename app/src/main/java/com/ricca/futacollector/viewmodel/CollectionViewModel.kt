package com.ricca.futacollector.viewmodel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ricca.futacollector.data.*
import com.ricca.futacollector.ApiCard
import kotlinx.coroutines.launch

class CollectionViewModel(application: Application) : AndroidViewModel(application) {

    // Inizializziamo il DAO
    private val cardDao = AppDatabase.getDatabase(application).cardDao()

    /**
     * Converte una ApiCard in Card del DB e la salva.
     * Grazie all'ID auto-incrementale nella classe Card,
     * ora possiamo salvare più versioni della stessa carta.
     */
    fun addCardToCollection(apiCard: ApiCard) {
        viewModelScope.launch {
            try {
                // Creiamo l'oggetto Card.
                // NOTA: Non passiamo l'internalId perché è auto-generato da Room.
                val cardToSave = Card(
                    id = apiCard.card_set_id,
                    name = apiCard.card_name,
                    // Se l'immagine è null, mettiamo una stringa vuota o un link di placeholder
                    image = apiCard.card_image ?: "",
                    setName = apiCard.set_name,
                    // Convertiamo i Double in String per il database
                    inventoryPrice = apiCard.inventory_price.toString(),
                    marketPrice = apiCard.market_price.toString(),
                    dateAdded = System.currentTimeMillis()
                )

                Log.d("FUTA_LOG", "Tentativo di salvataggio: ${cardToSave.name}")

                // Salvataggio nel database
                cardDao.insertCard(cardToSave)

                // Feedback all'utente
                Toast.makeText(
                    getApplication(),
                    "${cardToSave.name} aggiunta alla collezione!",
                    Toast.LENGTH_SHORT
                ).show()

                Log.d("FUTA_LOG", "Salvataggio completato con successo!")

            } catch (e: Exception) {
                Log.e("FUTA_LOG", "ERRORE durante il salvataggio: ${e.message}", e)
                Toast.makeText(getApplication(), "Errore nel salvataggio", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
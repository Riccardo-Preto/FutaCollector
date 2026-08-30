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
import com.ricca.futacollector.data.api.SnapshotRetrofitInstance
import com.ricca.futacollector.ui.screens.WishlistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.ricca.futacollector.data.AppDatabase
import com.ricca.futacollector.data.OrderedCardEntity
import com.ricca.futacollector.data.OrderedCardWithDetails
import com.ricca.futacollector.data.RecentCardItem

/**
 * Card con count completo: contiene l'intera Card dal DB + quante copie abbiamo
 */
data class CardWithCount(
    val card: Card,
    val count: Int,
    val addedDate: Long = 0L
)

class CollectionViewModel(
    application: Application,
    private val cardDao: CardDao
) : AndroidViewModel(application) {

    // --- SCROLL STATE ---
    var gridState: LazyGridState? = null
    var listState: LazyListState? = null

    private val prefs = getApplication<Application>()
        .getSharedPreferences("futa_prefs", Context.MODE_PRIVATE)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val lastUpdate = prefs.getLong("last_price_update", 0L)
            val now = System.currentTimeMillis()
            val twentyFourHours = 24 * 60 * 60 * 1000L

            if (now - lastUpdate > twentyFourHours) {
                refreshMarketPrices()
                prefs.edit().putLong("last_price_update", now).apply()
            }
        }

        // Stessa idea del refresh prezzi qui sopra, ma per il database
        // delle carte (nome/immagine/set). Lo snapshot su GitHub si
        // aggiorna una volta a settimana, quindi controlliamo anche noi
        // una volta a settimana, non più spesso.
        viewModelScope.launch(Dispatchers.IO) {
            val lastSync = prefs.getLong("last_db_sync", 0L)
            val now = System.currentTimeMillis()
            val sevenDays = 7 * 24 * 60 * 60 * 1000L

            if (now - lastSync > sevenDays) {
                syncCardDatabase()
                prefs.edit().putLong("last_db_sync", now).apply()
            }
        }
    }

    // --- MOSTRA LISTA SET ---
    private val _showSetList = MutableStateFlow(true)
    val showSetList: StateFlow<Boolean> = _showSetList.asStateFlow()

    // --- COLLEZIONE ---
    val collectionCards: StateFlow<List<CardWithCount>> =
        cardDao.getAllCollectionItems()
            .map { userItems ->
                userItems.mapNotNull { userItem ->
                    val card = cardDao.getCardById(userItem.cardId)
                    if (card != null) {
                        CardWithCount(card = card, count = userItem.count, addedDate = userItem.addedDate)
                    } else null
                }.sortedBy { it.card.id }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentCards: StateFlow<List<RecentCardItem>> = cardDao.getRecentlyAddedCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- WISHLIST ---
    val wishlistCards: StateFlow<Map<String, List<WishlistItem>>> =
        cardDao.getWishlistItems()
            .map { entities ->
                entities.mapNotNull { entity ->
                    // Recuperiamo la Card completa dal DB
                    val card = cardDao.getCardById(entity.cardId)
                    if (card != null) {
                        WishlistItem(
                            card = card,
                            quantity = entity.quantity,
                            reason = entity.reason
                        )
                    } else null
                }.groupBy { it.reason } // Raggruppa per "General" o "Nome Mazzo"
            }
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // --- EVENTI UI ---
    private val _uiEvents = Channel<String>()
    val uiEvents = _uiEvents.receiveAsFlow()

    // --- GESTIONE COPIE ---
    fun getCardCount(cardId: String): Flow<Int> = collectionCards.map { list ->
        list.find { it.card.id == cardId }?.count ?: 0
    }

    fun addCardToCollection(card: Card) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Ora questa funzione funzionerà perché l'abbiamo aggiunta al DAO
                val existingCard = cardDao.getUserCardById(card.id)

                val newCount = if (existingCard != null) {
                    existingCard.count + 1
                } else {
                    1
                }

                // 2. Inseriamo (o sostituiamo grazie a OnConflictStrategy.REPLACE)
                cardDao.insertUserCard(UserCardEntity(cardId = card.id, count = newCount))

                // 3. Log di verifica e notifica
                Log.d("COLLECTION", "Aggiunta carta: ${card.id}, nuovo totale: $newCount")
                _uiEvents.send("${card.name}: ora ne hai $newCount")

            } catch (e: Exception) {
                Log.e("COLLECTION_ERR", "Errore durante l'aggiunta: ${e.message}")
                _uiEvents.send("Errore nel salvataggio")
            }
        }
    }

    suspend fun addCardSilently(cardId: String, quantity: Int) {
        val existing = cardDao.getUserCardById(cardId)
        val currentCount = existing?.count ?: 0
        cardDao.insertUserCard(
            UserCardEntity(cardId = cardId, count = currentCount + quantity)
        )
    }

    fun sendEvent(message: String) {
        viewModelScope.launch {
            _uiEvents.send(message)
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

        viewModelScope.launch {
            // Resetta lo scroll della lista dei Set
            listState?.scrollToItem(0)
            // Resetta lo scroll della griglia dei risultati (per sicurezza)
            gridState?.scrollToItem(0)
        }
    }

    // Aggiungi questi dentro la classe CollectionViewModel
    val allSets: StateFlow<List<CardSetEntity>> = cardDao.getAllSets() // Assicurati di avere getAllSets nel DAO
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val setsWithCards: StateFlow<Set<String>> = cardDao.getSetIdsWithCards()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun getCardsFromSet(setId: String) {
        viewModelScope.launch {
            _searchQuery.value = ""

            val results = when (setId) {
                "OP14" -> {
                    val op14Cards = cardDao.getCardsBySet("OP14")
                    val eb04Partial = cardDao.getCardsBySet("EB04").filter { card ->
                        val color = card.color?.lowercase() ?: ""
                        color.contains("green") || color.contains("blue") || color.contains("purple")
                    }
                    (op14Cards + eb04Partial).sortedBy { it.id }
                }
                else -> cardDao.getCardsBySet(setId)
            }

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

    fun syncCardDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("SYNC_DB", "Scarico snapshot.json...")

                val snapshot = SnapshotRetrofitInstance.api.getSnapshot()

                Log.d("SYNC_DB", "Snapshot scaricato: ${snapshot.card_count} carte, ${snapshot.set_count} set. Scrivo nel DB...")

                cardDao.syncFromSnapshot(snapshot)

                Log.d("SYNC_DB", "Sync completata.")
                _uiEvents.send("Database carte aggiornato: ${snapshot.card_count} carte")

            } catch (e: Exception) {
                Log.e("SYNC_DB", "ERRORE durante la sync: ${e.message}")
                _uiEvents.send("Sync database fallita, riprovo più tardi")
            }
        }
    }

    fun forceSyncCardDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            syncCardDatabase()
            prefs.edit().putLong("last_db_sync", System.currentTimeMillis()).apply()
        }
    }

    // Da usare UNA VOLTA per ripulire i relitti del vecchio database
    // (set duplicati, alt art doppie). Dopo averla usata una volta,
    // puoi togliere il bottone che la richiama.
    fun resetAndSyncCardDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val snapshot = SnapshotRetrofitInstance.api.getSnapshot()
                cardDao.resetAndSyncFromSnapshot(snapshot)
                prefs.edit().putLong("last_db_sync", System.currentTimeMillis()).apply()
                _uiEvents.send("Catalogo carte ricostruito da zero: ${snapshot.card_count} carte")
            } catch (e: Exception) {
                Log.e("SYNC_DB", "ERRORE durante il reset: ${e.message}")
                _uiEvents.send("Reset fallito, riprova più tardi")
            }
        }
    }

    // Da usare UNA VOLTA sola, poi puoi togliere il bottone che la
    // richiama. Dati presi dal tuo vecchio collezione_one_piece.db.
    // "OP14" è diventato "OP14EB04" perché è lo stesso set, solo con
    // l'id giusto secondo lo schema dell'API.
    fun restoreOriginalSetOrder() {
        viewModelScope.launch(Dispatchers.IO) {
            val entries = listOf(
                Triple("DON", -1, "immagini_ottimizzate/DON_Banner.webp"),
                Triple("P", 0, "immagini_ottimizzate/promos.webp"),
                Triple("OP01", 1, "immagini_ottimizzate/op_01.webp"),
                Triple("ST01", 2, "immagini_ottimizzate/st_01.webp"),
                Triple("ST02", 3, "immagini_ottimizzate/st_02.webp"),
                Triple("ST03", 4, "immagini_ottimizzate/st_03.webp"),
                Triple("ST04", 5, "immagini_ottimizzate/st_04.webp"),
                Triple("ST05", 6, "immagini_ottimizzate/st_05.webp"),
                Triple("OP02", 7, "immagini_ottimizzate/op_02.webp"),
                Triple("ST06", 8, "immagini_ottimizzate/st_06.webp"),
                Triple("ST07", 9, "immagini_ottimizzate/st_07.webp"),
                Triple("OP03", 10, "immagini_ottimizzate/op_03.webp"),
                Triple("ST08", 11, "immagini_ottimizzate/st_08.webp"),
                Triple("ST09", 12, "immagini_ottimizzate/st_09.webp"),
                Triple("OP04", 13, "immagini_ottimizzate/op_04.webp"),
                Triple("ST10", 14, "immagini_ottimizzate/st_10.webp"),
                Triple("OP05", 15, "immagini_ottimizzate/op_05.webp"),
                Triple("ST11", 16, "immagini_ottimizzate/st_11.webp"),
                Triple("ST12", 17, "immagini_ottimizzate/st_12.webp"),
                Triple("OP06", 18, "immagini_ottimizzate/op_06.webp"),
                Triple("ST13", 19, "immagini_ottimizzate/st_13.webp"),
                Triple("EB01", 20, "immagini_ottimizzate/eb_01.webp"),
                Triple("OP07", 21, "immagini_ottimizzate/op_07.webp"),
                Triple("ST14", 22, "immagini_ottimizzate/st_14.webp"),
                Triple("OP08", 23, "immagini_ottimizzate/op_08.webp"),
                Triple("ST15", 24, "immagini_ottimizzate/st_15.webp"),
                Triple("ST16", 25, "immagini_ottimizzate/st_16.webp"),
                Triple("ST17", 26, "immagini_ottimizzate/st_17.webp"),
                Triple("ST18", 27, "immagini_ottimizzate/st_18.webp"),
                Triple("ST19", 28, "immagini_ottimizzate/st_19.webp"),
                Triple("ST20", 29, "immagini_ottimizzate/st_20.webp"),
                Triple("PRB01", 30, "immagini_ottimizzate/prb_01.webp"),
                Triple("OP09", 31, "immagini_ottimizzate/op_09.webp"),
                Triple("ST21", 32, "immagini_ottimizzate/st_21.webp"),
                Triple("OP10", 33, "immagini_ottimizzate/op_10.webp"),
                Triple("EB02", 34, "immagini_ottimizzate/eb_02.webp"),
                Triple("ST23", 35, "immagini_ottimizzate/st_23.webp"),
                Triple("ST24", 36, "immagini_ottimizzate/st_24.webp"),
                Triple("ST25", 37, "immagini_ottimizzate/st_25.webp"),
                Triple("ST26", 38, "immagini_ottimizzate/st_26.webp"),
                Triple("ST27", 39, "immagini_ottimizzate/st_27.webp"),
                Triple("ST28", 40, "immagini_ottimizzate/st_28.webp"),
                Triple("OP11", 41, "immagini_ottimizzate/op_11.webp"),
                Triple("OP12", 42, "immagini_ottimizzate/op_12.webp"),
                Triple("ST22", 43, "immagini_ottimizzate/st_22.webp"),
                Triple("LD01", 44, "immagini_ottimizzate/ld_01.webp"),
                Triple("PRB02", 45, "immagini_ottimizzate/prb_02.webp"),
                Triple("OP13", 46, "immagini_ottimizzate/op_13.webp"),
                Triple("ST29", 47, "immagini_ottimizzate/st_29.webp"),
                Triple("OP14EB04", 48, "immagini_ottimizzate/op14_eb04.webp"),
                Triple("EB03", 49, "immagini_ottimizzate/eb_03.webp"),
                Triple("OP15EB04", 50, "immagini_ottimizzate/op_15.webp"),
                Triple("ST30", 51, "immagini_ottimizzate/st_30.webp"),
                Triple("OP16", 52, "immagini_ottimizzate/op_16.webp"),
                Triple("ST31", 53, "immagini_ottimizzate/st_31.webp"),
                Triple("ST32", 54, "immagini_ottimizzate/st_32.webp"),
                Triple("ST33", 55, "immagini_ottimizzate/st_33.webp"),
                Triple("ST34", 56, "immagini_ottimizzate/st_34.webp"),
                Triple("ST35", 57, "immagini_ottimizzate/st_35.webp"),
                Triple("ST36", 58, "immagini_ottimizzate/st_36.webp"),
                Triple("OP17", 59, "immagini_ottimizzate/op_17.webp")
            )
            cardDao.restoreOriginalSetOrder(entries)
            _uiEvents.send("Ordine e copertine ripristinati ✅")
        }
    }

    fun getLeadersOnly(): Flow<List<Card>> = flow {
        val results = cardDao.getLeadersForSelection()

        // DEBUG: Controlliamo cosa arriva davvero dal DB
        results.take(3).forEach {
            Log.d("DB_CHECK", "ID: ${it.id} | Nome: ${it.name} | Immagine: ${it.image}")
        }

        emit(results)
    }.flowOn(Dispatchers.IO)

    // In CollectionViewModel.kt
    val collectionGridState = LazyGridState()

    fun resetCollectionScroll() {
        viewModelScope.launch {
            collectionGridState.scrollToItem(0)
        }
    }

    fun addToWishlist(card: Card, quantity: Int = 1, reason: String = "General") {
        viewModelScope.launch(Dispatchers.IO) {
            cardDao.addToWishlist(
                com.ricca.futacollector.data.WishlistEntity(
                    cardId = card.id,
                    quantity = quantity,
                    reason = reason
                )
            )
            _uiEvents.send("Aggiunta alla Wishlist! ✨")
        }
    }

    fun removeFromWishlist(cardId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            cardDao.removeFromWishlist(cardId)
        }
    }

    fun forceRefreshMarketPrices() {
        viewModelScope.launch(Dispatchers.IO) {
            refreshMarketPrices()
            prefs.edit().putLong("last_price_update", System.currentTimeMillis()).apply()
            _uiEvents.send("Prezzi aggiornati! ✅")
        }
    }

    // Inizializza il DAO (aggiungilo come parametro o recuperalo dal DB)
    private val orderedCardDao = AppDatabase.getDatabase(application).orderedCardDao()

    val orderedCards: StateFlow<List<OrderedCardWithDetails>> = orderedCardDao.getAllOrderedCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addToOrders(card: Card, quantity: Int = 1, note: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = orderedCardDao.getOrderedCardByCardId(card.id)
            if (existing != null) {
                // Somma alla quantità esistente
                orderedCardDao.insertOrderedCard(
                    existing.copy(quantity = existing.quantity + quantity)
                )
            } else {
                orderedCardDao.insertOrderedCard(
                    OrderedCardEntity(cardId = card.id, quantity = quantity, note = note)
                )
            }
            _uiEvents.send("Aggiunto agli ordini! 📦")
        }
    }

    fun markAsArrived(item: OrderedCardWithDetails) {
        viewModelScope.launch(Dispatchers.IO) {
            // Aggiungi 1 sola copia alla collezione
            val existing = cardDao.getUserCardById(item.cardId)
            val currentCount = existing?.count ?: 0
            cardDao.insertUserCard(
                UserCardEntity(cardId = item.cardId, count = currentCount + 1)
            )

            // Decrementa gli ordini di 1
            if (item.quantity <= 1) {
                // Era l'ultima copia — rimuovi dall'ordine
                orderedCardDao.deleteOrderedCard(item.id)
            } else {
                orderedCardDao.insertOrderedCard(
                    OrderedCardEntity(
                        id = item.id,
                        cardId = item.cardId,
                        quantity = item.quantity - 1,
                        note = item.note,
                        orderedDate = item.orderedDate
                    )
                )
            }

            _uiEvents.send("${item.name} arrivata! ✅")
        }
    }

    fun removeFromOrders(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            orderedCardDao.deleteOrderedCard(id)
        }
    }
    fun removeOneFromOrders(cardId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = orderedCardDao.getOrderedCardByCardId(cardId) ?: return@launch
            if (existing.quantity <= 1) {
                orderedCardDao.deleteOrderedCard(existing.id)
            } else {
                orderedCardDao.insertOrderedCard(existing.copy(quantity = existing.quantity - 1))
            }
        }
    }

    fun updateOrder(id: Int, quantity: Int, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = orderedCardDao.getOrderedCardById(id) ?: return@launch
            orderedCardDao.insertOrderedCard(existing.copy(quantity = quantity, note = note))
        }
    }

    suspend fun getCardById(cardId: String): Card? {
        return cardDao.getCardById(cardId)
    }

    fun nukeOrders() {
        viewModelScope.launch(Dispatchers.IO) {
            orderedCardDao.deleteAllOrders()
            _uiEvents.send("Ordini svuotati! 🗑️")
        }
    }

    fun cleanCardNameForExport(name: String?): String {
        if (name == null) return ""
        // Prende tutto prima della prima parentesi e rimuove spazi extra
        return name.substringBefore("(").trim()
    }

    fun cleanCardIdForExport(id: String): String {
        // Rimuove suffissi tipo _v2, _v3, _r1, ecc.
        return id.substringBefore("_")
    }
}


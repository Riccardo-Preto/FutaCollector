package com.ricca.futacollector.data

import androidx.room.*
import com.ricca.futacollector.data.api.ApiCard // Assicurati che l'import sia corretto per il tuo modello API
import com.ricca.futacollector.data.api.SnapshotCard
import com.ricca.futacollector.data.api.SnapshotResponse
import com.ricca.futacollector.data.api.SnapshotSet
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CardDao {

    // 1. La tua funzione esistente (Resta uguale, è la nostra "base")
    @Query("""
    SELECT * FROM cards 
    WHERE nome LIKE '%' || :query || '%' 
    OR id LIKE '%' || :query || '%' 
    OR card_type LIKE '%' || :query || '%' -- <--- AGGIUNGI QUESTA RIGA
    ORDER BY id ASC
""")
    abstract suspend fun searchInDatabase(query: String): List<Card>

    open suspend fun searchAdvanced(query: String): List<Card> {
        // Dividiamo la stringa in parole qui dentro
        val terms = query.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (terms.isEmpty()) return emptyList()

        // Prima passata SQL usando la prima parola
        val firstResults = searchInDatabase(terms[0])

        if (terms.size == 1) return firstResults

        // Raffinamento Kotlin usando .name (come in Card.kt)
        return firstResults.filter { card ->
            terms.all { term ->
                val nameMatch = card.name?.lowercase()?.contains(term) == true
                val idMatch = card.id.lowercase().contains(term)
                val subTypesMatch = card.subTypes?.lowercase()?.contains(term) == true
                val typeMatch = card.type?.lowercase()?.contains(term) == true

                nameMatch || idMatch || subTypesMatch || typeMatch
            }
        }
    }
    @Query("SELECT * FROM cards WHERE card_type = 'Leader' OR card_type = 'LEADER' ORDER BY id ASC")
    abstract suspend fun getLeadersForSelection(): List<Card>

    @Query("SELECT * FROM cards WHERE set_id = :setId ORDER BY id ASC")
    abstract suspend fun getCardsBySet(setId: String): List<Card>

    @Query("SELECT * FROM cards WHERE id LIKE :prefix || '%' ORDER BY id ASC")
    abstract suspend fun getCardsByPrefix(prefix: String): List<Card>

    @Query("SELECT * FROM cards WHERE id = :cardId LIMIT 1")
    abstract suspend fun getCardById(cardId: String): Card?

    @Query("SELECT * FROM user_collection WHERE card_id = :cardId LIMIT 1")
    abstract suspend fun getUserCardById(cardId: String): UserCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUserCard(userCard: UserCardEntity)

    @Transaction
    @Query("SELECT * FROM user_collection")
    abstract fun getAllCollectionItems(): Flow<List<UserCardEntity>>

    @Query("DELETE FROM user_collection WHERE card_id = :cardId")
    abstract suspend fun deleteUserCard(cardId: String)

    @Query("DELETE FROM user_collection")
    abstract suspend fun deleteAllUserCards()

    @Query("SELECT * FROM sets ORDER BY ordine_utente ASC")
    abstract fun getAllSets(): Flow<List<CardSetEntity>>

    @Query("SELECT DISTINCT set_id FROM cards WHERE set_id IS NOT NULL")
    abstract fun getSetIdsWithCards(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSets(sets: List<CardSetEntity>)

    // --- LOGICA AGGIORNAMENTO PREZZI ---

    @Query("""
        UPDATE cards 
        SET market_price = :price,
            card_text = CASE WHEN (card_text IS NULL OR card_text = '') THEN :text ELSE card_text END
        WHERE id LIKE :apiId || '%' 
        AND LOWER(nome) = LOWER(:fullName)
    """)
    abstract suspend fun updatePriceByExactNameAndPrefix(price: Double, text: String?, apiId: String, fullName: String): Int

    @Query("""
        UPDATE cards 
        SET market_price = :price,
            card_text = CASE WHEN (card_text IS NULL OR card_text = '') THEN :text ELSE card_text END
        WHERE card_image = :imageUrl
    """)
    abstract suspend fun updatePriceByImageUrl(price: Double, text: String?, imageUrl: String): Int

    /**
     * Esegue l'intero aggiornamento in una singola transazione atomica.
     * Molto più veloce e previene i lag della UI.
     */
    @Transaction
    open suspend fun updateAllPricesSmart(apiCards: List<com.ricca.futacollector.data.api.ApiCard>): Int {
        var totalUpdated = 0
        apiCards.forEach { apiCard ->
            val price = apiCard.market_price ?: 0.0
            if (price > 0) {
                // Tentativo 1: URL Immagine
                var rows = 0
                if (!apiCard.card_image.isNullOrBlank()) {
                    rows = updatePriceByImageUrl(price, apiCard.card_text, apiCard.card_image)
                }

                // Tentativo 2: Nome + Prefisso ID
                if (rows == 0) {
                    rows = updatePriceByExactNameAndPrefix(
                        price,
                        apiCard.card_text,
                        apiCard.card_set_id,
                        apiCard.card_name
                    )
                }
                totalUpdated += rows
            }
        }
        return totalUpdated
    }

    @Query("""
    SELECT uc.card_id, uc.count, uc.added_date, 
           c.nome, c.card_image, c.market_price, c.rarity, c.set_id
    FROM user_collection uc
    JOIN cards c ON uc.card_id = c.id
    ORDER BY uc.added_date DESC
    LIMIT 10
""")
    abstract fun getRecentlyAddedCards(): Flow<List<RecentCardItem>>

    @Query("SELECT * FROM wishlist ORDER BY addedDate DESC")
    abstract fun getWishlistItems(): Flow<List<WishlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun addToWishlist(item: WishlistEntity)

    @Query("DELETE FROM wishlist WHERE cardId = :cardId")
    abstract suspend fun removeFromWishlist(cardId: String)


    // ======================================================================
    // --- SYNC DA snapshot.json (sostituisce le migration scritte a mano) ---
    // ======================================================================

    // Le carte le sovrascriviamo del tutto: vengono interamente da
    // optcgapi.com, non hai mai modificato questi campi a mano.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCards(cards: List<Card>)

    @Query("DELETE FROM cards")
    abstract suspend fun deleteAllCards()

    @Query("DELETE FROM sets")
    abstract suspend fun deleteAllSets()

    // Aggiorna SOLO ordine e cover di un set che esiste già. Se il set
    // non c'è (per esempio è stato rinominato), non fa nulla — non crea
    // righe nuove, quindi è sicura da chiamare anche con id sbagliati.
    @Query("UPDATE sets SET ordine_utente = :order, set_cover_image = :cover WHERE id = :id")
    abstract suspend fun restoreSetOrderAndCover(id: String, order: Int, cover: String?)

    // Per i set invece NON possiamo sovrascrivere tutto: "ordine_utente"
    // e "set_cover_image" sono cose che magari hai sistemato tu a mano
    // nell'app, e lo snapshot non le conosce. Con questa query, se il set
    // esiste già aggiorniamo solo nome/api_id e lasciamo intatto il resto;
    // se è un set nuovo, gli diamo l'ordine "fallbackOrder" passato da
    // fuori (che rispecchia l'ordine in cui l'API restituisce i set,
    // quindi tendenzialmente l'ordine di uscita) invece di un numero
    // fisso uguale per tutti.
    @Query("""
        INSERT INTO sets (id, api_id, nome, ordine_utente, set_cover_image)
        VALUES (
            :id,
            :apiId,
            :nome,
            COALESCE((SELECT ordine_utente FROM sets WHERE id = :id), :fallbackOrder),
            (SELECT set_cover_image FROM sets WHERE id = :id)
        )
        ON CONFLICT(id) DO UPDATE SET
            api_id = excluded.api_id,
            nome = excluded.nome
    """)
    abstract suspend fun upsertSet(id: String, apiId: String?, nome: String?, fallbackOrder: Int)

    // Cancella i set che non compaiono più nello snapshot E che, dopo la
    // sync, nessuna carta usa più come set_id. La doppia condizione (non
    // nello snapshot + zero carte collegate) evita di cancellare per
    // sbaglio un set ancora in uso.
    @Query("""
        DELETE FROM sets
        WHERE id NOT IN (:validSetIds)
        AND id NOT IN (SELECT DISTINCT set_id FROM cards WHERE set_id IS NOT NULL)
    """)
    abstract suspend fun deleteOrphanSets(validSetIds: List<String>)

    /**
     * Punto d'ingresso unico per la sync: prende la risposta già scaricata
     * da SnapshotApiService e la scrive nel database, nell'ordine giusto
     * (prima i set, poi le carte, perché le carte hanno una foreign key
     * verso i set). Tutto dentro una singola transazione: o va tutto a
     * buon fine, o non cambia nulla — niente stati a metà.
     */
    @Transaction
    open suspend fun syncFromSnapshot(snapshot: SnapshotResponse) {
        // 1. Set, uno per uno con l'upsert che preserva ordine/cover.
        // fallbackOrder parte da 10000 e cresce nell'ordine restituito
        // dall'API: così i set nuovi restano ordinati tra loro (di solito
        // l'API li restituisce in ordine di uscita) e vengono comunque
        // dopo tutti quelli che avevi già ordinato tu a mano.
        snapshot.sets.forEachIndexed { index, s: SnapshotSet ->
            upsertSet(id = s.id, apiId = s.api_id, nome = s.nome, fallbackOrder = 10000 + index)
        }

        // 2. Carte, tutte insieme: mappiamo SnapshotCard -> Card (stessa
        // forma, cambia solo il tipo) e inseriamo in blocco.
        val cardEntities = snapshot.cards.map { sc: SnapshotCard ->
            Card(
                id = sc.id,
                name = sc.nome,
                marketPrice = sc.market_price,
                image = sc.card_image,
                rarity = sc.rarity,
                color = sc.card_color,
                type = sc.card_type,
                cost = sc.card_cost,
                power = sc.card_power,
                counter = sc.counter_amount,
                attribute = sc.attribute,
                apiImageId = sc.api_image_id,
                effect = sc.card_text,
                setId = sc.set_id,
                subTypes = sc.sub_types
            )
        }
        insertCards(cardEntities)

        // 3. Pulizia: rimuoviamo i set che non esistono più nello
        // snapshot e che ora non hanno più nessuna carta collegata
        // (i "relitti" tipo OP14/OP15/EB04 da soli).
        deleteOrphanSets(snapshot.sets.map { it.id })
    }

    /**
     * Da usare UNA VOLTA per ripartire puliti: cancella tutto il catalogo
     * carte/set esistente (quello del vecchio schema) e lo ricostruisce
     * da zero seguendo solo la convenzione di id/ordine dell'API. Non
     * tocca collezione, mazzi, ordini o wishlist — solo cards e sets.
     */
    @Transaction
    open suspend fun resetAndSyncFromSnapshot(snapshot: SnapshotResponse) {
        deleteAllCards()
        deleteAllSets()
        syncFromSnapshot(snapshot)
    }

    /**
     * Da usare UNA VOLTA per ripristinare ordine e copertine perse col
     * reset del catalogo. "entries" è la tripletta (id, ordine, cover)
     * presa dal tuo vecchio database. Aggiorna solo i set che esistono
     * ancora con quell'id: non ricrea set cancellati né ne inventa.
     */
    @Transaction
    open suspend fun restoreOriginalSetOrder(entries: List<Triple<String, Int, String?>>) {
        entries.forEach { (id, order, cover) ->
            restoreSetOrderAndCover(id, order, cover)
        }
    }
}

data class RecentCardItem(
    @ColumnInfo(name = "card_id") val cardId: String,
    @ColumnInfo(name = "count") val count: Int,
    @ColumnInfo(name = "added_date") val addedDate: Long,
    @ColumnInfo(name = "nome") val name: String?,
    @ColumnInfo(name = "card_image") val image: String?,
    @ColumnInfo(name = "market_price") val marketPrice: Double,
    @ColumnInfo(name = "rarity") val rarity: String?,
    @ColumnInfo(name = "set_id") val setId: String?
)

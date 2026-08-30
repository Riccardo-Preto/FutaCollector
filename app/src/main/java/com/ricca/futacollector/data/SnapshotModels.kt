package com.ricca.futacollector.data.api

// Questi campi hanno ESATTAMENTE gli stessi nomi delle colonne della
// tua tabella "cards" (vedi Card.kt), quindi non servono @SerializedName:
// Gson mappa il JSON direttamente su queste proprietà.
data class SnapshotCard(
    val id: String,
    val set_id: String?,
    val nome: String?,
    val card_color: String?,
    val card_type: String?,
    val card_cost: String?,
    val card_power: String?,
    val counter_amount: String?,
    val attribute: String?,
    val rarity: String?,
    val market_price: Double = 0.0,
    val card_image: String?,
    val api_image_id: String?,
    val card_text: String?,
    val sub_types: String? = ""
)

data class SnapshotSet(
    val id: String,
    val api_id: String?,
    val nome: String?
)

data class SnapshotResponse(
    val generated_at: String,
    val card_count: Int,
    val set_count: Int,
    val cards: List<SnapshotCard>,
    val sets: List<SnapshotSet>
)

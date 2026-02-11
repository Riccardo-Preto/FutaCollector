package com.ricca.futacollector.data

import androidx.room.Embedded
import androidx.room.Relation

data class CollectionItem(
    @Embedded val userCard: UserCardEntity, // Contiene l'ID e la quantità (count)
    @Relation(
        parentColumn = "card_id", // La colonna in UserCardEntity
        entityColumn = "id"       // La colonna corrispondente in Card (tabella cards)
    )
    val card: Card // Qui Room caricherà automaticamente tutti i dettagli (nome, forza, immagine...)
)
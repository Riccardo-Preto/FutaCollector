package com.ricca.futacollector.ui

sealed class CardDetailMode {
    object Search : CardDetailMode()
    data class Collection(val ownedCopies: Int) : CardDetailMode()
    data class Ordered(
        val ownedCopies: Int,
        val orderedQuantity: Int,
        val orderedItemId: Int  // id in ordered_cards per markAsArrived/removeOne
    ) : CardDetailMode()
}

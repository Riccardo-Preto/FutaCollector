package com.ricca.futacollector.ui

sealed class CardDetailMode {

    object Search : CardDetailMode()

    data class Collection(
        val ownedCopies: Int
    ) : CardDetailMode()
}

package com.ricca.futacollector.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ricca.futacollector.data.CardDao

class CollectionViewModelFactory(
    private val cardDao: CardDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CollectionViewModel::class.java)) {
            return CollectionViewModel(cardDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

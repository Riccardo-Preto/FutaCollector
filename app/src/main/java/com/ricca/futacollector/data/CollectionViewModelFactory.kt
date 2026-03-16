package com.ricca.futacollector.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ricca.futacollector.data.CardDao
import android.app.Application

class CollectionViewModelFactory(
    private val application: Application,
    private val cardDao: CardDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CollectionViewModel::class.java)) {
            return CollectionViewModel(application, cardDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
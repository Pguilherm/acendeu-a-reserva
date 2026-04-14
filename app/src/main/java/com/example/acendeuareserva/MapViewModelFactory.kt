package com.example.acendeuareserva

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MapViewModelFactory(
    private val repository: MapRepository
) : ViewModelProvider.Factory {

    /* Cria instâncias do ViewModel injetando as dependências necessárias, como o repositório de dados */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MapViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
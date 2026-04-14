package com.example.acendeuareserva

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

class MapViewModel(private val repository: MapRepository) : ViewModel() {

    /* Objetos LiveData que encapsulam o estado da interface, incluindo listas de postos, rotas e mensagens de erro */
    val postos = MutableLiveData<List<Posto>>()
    val rotas = MutableLiveData<List<RotaInfo>>()
    val erro = MutableLiveData<String>()

    /* Executa uma corrotina para carregar a lista de postos de combustível com base na localização e raio informados */
    fun carregarPostos(lat: Double, lon: Double, raio: Int) {
        viewModelScope.launch {
            val resultado = repository.buscarPostos(lat, lon, raio)
            if (resultado.isNotEmpty()) postos.postValue(resultado)
            else erro.postValue("Nenhum posto encontrado.")
        }
    }

    /* Solicita ao repositório o traçado de rotas entre dois pontos e atualiza o estado para exibição no mapa */
    fun tracarRota(origem: GeoPoint, destino: GeoPoint) {
        viewModelScope.launch {
            val lista = repository.tracarRota(origem, destino)
            if (lista.isNotEmpty()) rotas.postValue(lista)
            else erro.postValue("Não foi possível traçar uma rota.")
        }
    }

    /* Obtém os dados da rota mais rápida de forma assíncrona para popular informações prévias no painel de detalhes (BottomSheet) */
    fun buscarInfoRotaRapida(origem: GeoPoint, destino: GeoPoint, callback: (RotaInfo?) -> Unit) {
        viewModelScope.launch {
            val rotas = repository.tracarRota(origem, destino)
            callback(rotas.firstOrNull())
        }
    }
}
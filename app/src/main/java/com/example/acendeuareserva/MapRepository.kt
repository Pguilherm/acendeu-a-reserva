package com.example.acendeuareserva

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.URLEncoder
import java.util.Locale

/* Representa a entidade de um posto de combustível com sua localização e nome */
data class Posto(val ponto: GeoPoint, val nome: String)

/* Encapsula os dados detalhados de uma rota, incluindo coordenadas, distância e tempo estimado */
data class RotaInfo(
    val pontos: List<GeoPoint>,
    val distanciaKm: Double,
    val tempoMinutos: Int
)

class MapRepository(private val api: ApiService) {

    private val overpassServers = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter"
    )

    /* Consulta a API Overpass para encontrar postos de combustível em um raio específico ao redor da localização do usuário */
    suspend fun buscarPostos(lat: Double, lon: Double, raio: Int): List<Posto> =
        withContext(Dispatchers.IO) {
            val query = "[out:json];node(around:$raio,$lat,$lon)[amenity=fuel];out;"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")

            for (server in overpassServers) {
                try {
                    val url = "$server?data=$encodedQuery"
                    val response = api.getData(url)
                    val obj = JSONObject(response)
                    val elements = obj.getJSONArray("elements")

                    val listaPostos = mutableListOf<Posto>()
                    for (i in 0 until elements.length()) {
                        val node = elements.getJSONObject(i)
                        val ponto = GeoPoint(node.getDouble("lat"), node.getDouble("lon"))
                        val tags = node.optJSONObject("tags")
                        val nome = tags?.optString("name") ?: "Posto de Combustível"
                        val bandeira = tags?.optString("brand") ?: ""

                        listaPostos.add(Posto(ponto, if (bandeira.isNotEmpty()) "$nome ($bandeira)" else nome))
                    }
                    if (listaPostos.isNotEmpty()) return@withContext listaPostos
                } catch (_: Exception) { }
            }
            emptyList()
        }

    /* Solicita ao servidor OSRM o traçado de rotas (principal e alternativas) entre a origem e o destino selecionado */
    suspend fun tracarRota(origem: GeoPoint, destino: GeoPoint): List<RotaInfo> =
        withContext(Dispatchers.IO) {
            try {
                // 1. Força a formatação americana para as coordenadas (ponto em vez de vírgula)
                val oriLon = String.format(Locale.US, "%.6f", origem.longitude)
                val oriLat = String.format(Locale.US, "%.6f", origem.latitude)
                val destLon = String.format(Locale.US, "%.6f", destino.longitude)
                val destLat = String.format(Locale.US, "%.6f", destino.latitude)

                // 2. Monta a URL limpa e segura
                val url = "https://router.project-osrm.org/route/v1/driving/$oriLon,$oriLat;$destLon,$destLat?overview=full&geometries=geojson&alternatives=true"

                android.util.Log.d("URL_ROTA", "Testando URL: $url")

                val response = api.getData(url)
                val obj = JSONObject(response)
                val routes = obj.getJSONArray("routes")

                val listaRotas = mutableListOf<RotaInfo>()

                for (r in 0 until routes.length()) {
                    val routeObj = routes.getJSONObject(r)

                    val distanceMeters = routeObj.getDouble("distance")
                    val durationSeconds = routeObj.getDouble("duration")

                    val coords = routeObj.getJSONObject("geometry").getJSONArray("coordinates")
                    val pontos = mutableListOf<GeoPoint>()
                    for (i in 0 until coords.length()) {
                        val coord = coords.getJSONArray(i)
                        pontos.add(GeoPoint(coord.getDouble(1), coord.getDouble(0)))
                    }

                    listaRotas.add(
                        RotaInfo(
                            pontos = pontos,
                            distanciaKm = distanceMeters / 1000.0,
                            tempoMinutos = (durationSeconds / 60.0).toInt()
                        )
                    )
                }
                listaRotas
            } catch (e: Exception) {
                android.util.Log.e("ERRO_ROTA", "Falha ao buscar a rota no OSRM: ${e.message}")
                emptyList()
            }
        }
}
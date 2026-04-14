package com.example.acendeuareserva

import android.content.Context
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object MapCalculos {

    val raios = listOf(1000, 2000, 5000, 15000, 25000, 50000, 100000)
    val labels = listOf("1 km", "2 km", "5 km", "15 km", "25 km", "50 km", "100 km")

    /* Calcula a distância total em quilômetros percorrendo uma lista de pontos geográficos */
    fun calcularDistancia(pontos: List<GeoPoint>): Double {
        var distancia = 0.0
        for (i in 0 until pontos.size - 1) {
            distancia += pontos[i].distanceToAsDouble(pontos[i + 1])
        }
        return distancia / 1000.0
    }

    /* Calcula o ângulo de orientação (bearing) em graus entre dois pontos para alinhar o mapa */
    fun calcularOrientacao(origem: GeoPoint, destino: GeoPoint): Float {
        val lat1 = Math.toRadians(origem.latitude)
        val lon1 = Math.toRadians(origem.longitude)
        val lat2 = Math.toRadians(destino.latitude)
        val lon2 = Math.toRadians(destino.longitude)

        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val bearing = Math.toDegrees(atan2(y, x))

        return ((bearing + 360) % 360).toFloat()
    }

    /* Determina a menor distância entre um ponto e um segmento de reta para verificar desvios de rota */
    fun distanciaPontoSegmento(p: GeoPoint, a: GeoPoint, b: GeoPoint): Double {
        val ax = a.latitude
        val ay = a.longitude
        val bx = b.latitude
        val by = b.longitude
        val px = p.latitude
        val py = p.longitude

        val abx = bx - ax
        val aby = by - ay
        val apx = px - ax
        val apy = py - ay

        val ab2 = abx * abx + aby * aby
        val ap_ab = apx * abx + apy * aby
        val t = if (ab2 != 0.0) ap_ab / ab2 else -1.0

        val closest = when {
            t < 0.0 -> GeoPoint(ax, ay)
            t > 1.0 -> GeoPoint(bx, by)
            else -> GeoPoint(ax + abx * t, ay + aby * t)
        }

        return p.distanceToAsDouble(closest)
    }

    /* Gerencia os eventos de mudança do SeekBar para ajustar o raio de busca e atualizar os postos no mapa */
    fun configurarSeekBar(
        context: Context,
        seekBar: SeekBar,
        raioLabel: TextView,
        map: MapView,
        ultimaLocalizacaoProvider: () -> GeoPoint?,
        carregarPostos: (Double, Double, Int) -> Unit
    ) {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val raioSelecionado = raios[progress]
                raioLabel.text = "Raio: ${labels[progress]}"

                val ultimaLocalizacao = ultimaLocalizacaoProvider()
                if (ultimaLocalizacao != null) {
                    MapStyle.desenharRaio(map, ultimaLocalizacao, raioSelecionado, temporario = true)

                    try {
                        carregarPostos(
                            ultimaLocalizacao.latitude,
                            ultimaLocalizacao.longitude,
                            raioSelecionado
                        )
                    } catch (e: Exception) {
                        Toast.makeText(context, "Erro ao atualizar postos", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                map.overlays.removeAll { it is Polygon }
                map.invalidate()
            }
        })
    }
}
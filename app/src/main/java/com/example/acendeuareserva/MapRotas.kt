package com.example.acendeuareserva

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

object MapRotas {

    private var rotaAtual: Polyline? = null
    private var pontosRota: List<GeoPoint>? = null

    /* Gerencia a exibição dos marcadores de postos no mapa e configura o BottomSheet com informações e avaliações ao clicar */
    fun observarPostos(
        context: Context,
        map: MapView,
        lista: List<Posto>,
        onCalcularInfo: (GeoPoint, (RotaInfo?) -> Unit) -> Unit,
        onTraçarRota: (GeoPoint) -> Unit
    ) {
        map.overlays.removeAll { it is Marker && it != MapIcons.userMarker }

        if (lista.isEmpty()) return

        lista.forEach { posto ->
            val marker = Marker(map)
            marker.position = posto.ponto
            marker.title = posto.nome
            marker.icon = MapStyle.resizeDrawable(context, R.drawable.ic_fuel, 48, 48)

            marker.setOnMarkerClickListener { m, _ ->
                val bottomSheet = BottomSheetDialog(context)
                val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_posto, null)
                bottomSheet.setContentView(view)

                val txtNome = view.findViewById<TextView>(R.id.txtNomePosto)
                val txtDistanciaTempo = view.findViewById<TextView>(R.id.txtDistanciaTempo)
                val txtMedia = view.findViewById<TextView>(R.id.txtMediaAvaliacao)
                val txtMinhaAvaliacao = view.findViewById<TextView>(R.id.txtMinhaAvaliacao)
                val imgPosto = view.findViewById<ImageView>(R.id.imgPosto)
                val ratingBar = view.findViewById<RatingBar>(R.id.ratingBarPosto)
                val btnRota = view.findViewById<Button>(R.id.btnIrAtePosto)

                txtNome.text = m.title

                when {
                    m.title?.contains("Ipiranga", true) == true -> imgPosto.setImageResource(R.drawable.img_ipiranga)
                    m.title?.contains("Shell", true) == true -> imgPosto.setImageResource(R.drawable.img_shell)
                    m.title?.contains("Petrobras", true) == true || m.title?.contains("BR", true) == true -> imgPosto.setImageResource(R.drawable.img_petrobras)
                    else -> imgPosto.setImageResource(R.drawable.posto_placeholder)
                }

                txtDistanciaTempo.text = "⏳ Calculando distância..."
                onCalcularInfo(m.position) { info ->
                    if (info != null) {
                        txtDistanciaTempo.text = "🚗 %.1f km  •  ⏳ ~%d min".format(info.distanciaKm, info.tempoMinutos)
                        txtDistanciaTempo.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    } else {
                        txtDistanciaTempo.text = "Distância indisponível no momento"
                    }
                }

                RatingManager.buscarMedia(m.position.latitude, m.position.longitude) { media ->
                    txtMedia.text = if (media > 0) "Média geral: %.1f ⭐".format(media) else "Média geral: Nenhuma"
                }

                RatingManager.buscarMinhaAvaliacao(m.position.latitude, m.position.longitude) { minhaNota ->
                    if (minhaNota != null) {
                        txtMinhaAvaliacao.text = "Sua avaliação:"
                        txtMinhaAvaliacao.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_light))
                        ratingBar.rating = minhaNota
                    } else {
                        txtMinhaAvaliacao.text = "Você ainda não avaliou"
                        txtMinhaAvaliacao.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                        ratingBar.rating = 0f
                    }
                }

                ratingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
                    if (fromUser) {
                        RatingManager.salvarAvaliacao(m.position.latitude, m.position.longitude, rating)
                        txtMinhaAvaliacao.text = "Sua avaliação (Salva!)"
                        txtMinhaAvaliacao.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_light))
                        Toast.makeText(context, "Avaliação salva!", Toast.LENGTH_SHORT).show()
                    }
                }

                btnRota.setOnClickListener {
                    onTraçarRota(m.position)
                    bottomSheet.dismiss()
                }

                bottomSheet.show()
                true
            }
            map.overlays.add(marker)
        }
        map.invalidate()
    }

    /* Desenha a linha da rota (Polyline) no mapa utilizando as coordenadas e informações fornecidas pelo provedor de rota */
    fun observarRota(context: Context, map: MapView, rotaInfo: RotaInfo, onRotaDesenhada: (Double, Int) -> Unit) {
        if (rotaInfo.pontos.isEmpty()) return

        rotaAtual?.let { map.overlays.remove(it) }

        val polyline = Polyline().apply {
            setPoints(rotaInfo.pontos)
            color = ContextCompat.getColor(context, android.R.color.holo_blue_light)
            width = 10f
        }
        rotaAtual = polyline
        pontosRota = rotaInfo.pontos

        map.overlays.add(polyline)
        map.invalidate()

        onRotaDesenhada(rotaInfo.distanciaKm, rotaInfo.tempoMinutos)
    }

    /* Remove a representação visual da rota atual e limpa os dados de pontos armazenados */
    fun cancelarRota(map: MapView) {
        rotaAtual?.let { map.overlays.remove(it) }
        rotaAtual = null
        pontosRota = null
        map.invalidate()
    }

    /* Calcula a menor distância entre a posição do usuário e o traçado da rota para detectar possíveis desvios */
    fun distanciaAteRota(userPoint: GeoPoint): Double {
        var menorDistancia = Double.MAX_VALUE
        pontosRota?.let { pontos ->
            for (i in 0 until pontos.size - 1) {
                val d = MapCalculos.distanciaPontoSegmento(userPoint, pontos[i], pontos[i+1])
                if (d < menorDistancia) menorDistancia = d
            }
        }
        return menorDistancia
    }
}
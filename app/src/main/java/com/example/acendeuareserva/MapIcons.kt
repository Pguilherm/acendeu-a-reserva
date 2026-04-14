package com.example.acendeuareserva

import android.app.Activity
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

object MapIcons {

    lateinit var map: MapView
    lateinit var seekBarRaio: SeekBar
    lateinit var raioLabel: TextView
    lateinit var btnNavegacao: Button
    lateinit var btnCancelarRota: Button
    lateinit var distanceText: TextView
    lateinit var userMarker: Marker

    /* Vincula as variáveis do objeto aos componentes de interface (views) definidos no layout XML */
    fun init(activity: Activity) {
        map = activity.findViewById(R.id.map)
        seekBarRaio = activity.findViewById(R.id.seekBarRaio)
        raioLabel = activity.findViewById(R.id.raioLabel)
        btnNavegacao = activity.findViewById(R.id.btnNavegacao)
        btnCancelarRota = activity.findViewById(R.id.btnCancelarRota)
        distanceText = activity.findViewById(R.id.distanceText)
    }

    /* Atualiza o componente de texto com a distância em quilômetros e o tempo estimado de chegada */
    fun atualizarInfoRota(km: Double, minutos: Int) {
        distanceText.text = "%.1f km • ~%d min".format(km, minutos)
    }

    /* Restaura o texto informativo de distância para o estado padrão após o encerramento de uma rota */
    fun limparInfoRota() {
        distanceText.text = "Distância: -- km"
    }

    /* Atualiza o rótulo de texto que indica o raio de busca selecionado pelo usuário */
    fun atualizarRaio(label: String) {
        raioLabel.text = "Raio: $label"
    }

    /* Altera o estado visual e o texto do botão de navegação conforme a ativação do recurso */
    fun setNavegacaoAtiva(ativa: Boolean) {
        btnNavegacao.text = if (ativa) "Navegação ON" else "Navegação OFF"
    }

    /* Remove as sobreposições do mapa e libera referências para evitar vazamentos de memória */
    fun limparReferencias() {
        if (this::map.isInitialized) map.overlays.clear()
    }
}
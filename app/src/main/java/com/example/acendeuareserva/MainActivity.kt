package com.example.acendeuareserva

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import okhttp3.OkHttpClient
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private var modoNavegacaoAtivo = false
    private var destinoAtual: GeoPoint? = null
    private var orientacaoRota: Float? = null
    private var ultimaLocalizacao: GeoPoint? = null

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private val apiService: ApiService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://overpass-api.de/")
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private val mapViewModel: MapViewModel by viewModels {
        MapViewModelFactory(MapRepository(apiService))
    }

    private lateinit var locationCallback: LocationCallback

    /* Gerencia a resposta do usuário à solicitação de permissão de localização */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) iniciarLocalizacao()
        else Toast.makeText(this, "Permissão de localização negada.", Toast.LENGTH_LONG).show()
    }

    /* Configura o estado inicial da Activity, o mapa e verifica as permissões de GPS */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName
        setContentView(R.layout.activity_main)

        MapIcons.init(this)
        MapIcons.map.setMultiTouchControls(true)
        MapStyle.aplicarTilesEscuros(MapIcons.map, this)

        configurarObservadores()
        configurarBotoes()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            iniciarLocalizacao()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    /* Configura o monitoramento da localização em tempo real e atualiza a posição do usuário no mapa */
    private fun iniciarLocalizacao() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val userPoint = GeoPoint(it.latitude, it.longitude)
                    ultimaLocalizacao = userPoint
                    MapIcons.map.controller.setZoom(15.0)
                    MapIcons.map.controller.setCenter(userPoint)

                    MapIcons.userMarker = Marker(MapIcons.map).apply {
                        position = userPoint
                        title = "Sua Posição"
                        icon = MapStyle.resizeDrawable(this@MainActivity, R.drawable.ic_user, 64, 64)
                    }
                    MapIcons.map.overlays.add(MapIcons.userMarker)
                    mapViewModel.carregarPostos(it.latitude, it.longitude, 1000)
                }
            }

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation ?: return
                    val userPoint = GeoPoint(location.latitude, location.longitude)
                    ultimaLocalizacao = userPoint
                    MapIcons.userMarker.position = userPoint

                    if (modoNavegacaoAtivo) {
                        MapIcons.map.controller.setCenter(userPoint)
                        destinoAtual?.let { destino ->

                            // Lógica de monitoramento de chegada ao destino
                            if (userPoint.distanceToAsDouble(destino) < 30.0) {
                                Toast.makeText(this@MainActivity, "Você chegou ao destino!", Toast.LENGTH_LONG).show()
                                MapIcons.btnCancelarRota.performClick()
                            } else {
                                // Lógica original de verificação de desvio
                                val desvio = MapRotas.distanciaAteRota(userPoint)
                                if (desvio > 50) mapViewModel.tracarRota(userPoint, destino)
                            }
                        }
                    }
                    MapIcons.map.invalidate()
                }
            }

            fusedLocationClient.requestLocationUpdates(
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build(),
                locationCallback,
                mainLooper
            )
        } catch (e: SecurityException) { e.printStackTrace() }
    }

    /* Define os observadores do ViewModel para atualizar postos, rotas e mensagens de erro na interface */
    private fun configurarObservadores() {
        mapViewModel.postos.observe(this) { lista ->
            MapRotas.observarPostos(
                this,
                MapIcons.map,
                lista,
                onCalcularInfo = { destino, callback ->
                    ultimaLocalizacao?.let { origem -> mapViewModel.buscarInfoRotaRapida(origem, destino, callback) } ?: callback(null)
                },
                onTraçarRota = { destino ->
                    ultimaLocalizacao?.let { origem ->
                        destinoAtual = destino
                        mapViewModel.tracarRota(origem, destino)
                    }
                }
            )
        }

        mapViewModel.rotas.observe(this) { listaRotas ->
            if (listaRotas.isEmpty()) {
                MapIcons.btnCancelarRota.visibility = View.GONE
                return@observe
            }
            if (listaRotas.size == 1) {
                desenharRotaEscolhida(listaRotas[0])
            } else {
                val opcoes = listaRotas.mapIndexed { index, rota ->
                    val tipo = if (index == 0) "Mais rápida" else "Alternativa ${index}"
                    "$tipo: %.1f km (~%d min)".format(rota.distanciaKm, rota.tempoMinutos)
                }.toTypedArray()

                AlertDialog.Builder(this)
                    .setTitle("Escolha uma rota")
                    .setItems(opcoes) { _, which -> desenharRotaEscolhida(listaRotas[which]) }
                    .setCancelable(false)
                    .show()
            }
        }

        mapViewModel.erro.observe(this) { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    /* Desenha a rota selecionada no mapa e ajusta a câmera e orientação para o modo de navegação */
    private fun desenharRotaEscolhida(rota: RotaInfo) {
        MapRotas.observarRota(this, MapIcons.map, rota) { km, tempo ->
            MapIcons.atualizarInfoRota(km, tempo)
        }
        modoNavegacaoAtivo = true
        MapIcons.setNavegacaoAtiva(true)
        MapIcons.btnCancelarRota.visibility = View.VISIBLE

        ultimaLocalizacao?.let { origem ->
            destinoAtual?.let { destino ->
                val orientacao = MapCalculos.calcularOrientacao(origem, destino)
                MapIcons.map.mapOrientation = orientacao
            }
        }
        MapIcons.map.controller.setZoom(18.0)
    }

    /* Configura os ouvintes de clique para os botões de navegação, cancelamento de rota e controle de raio */
    private fun configurarBotoes() {
        MapIcons.btnCancelarRota.visibility = View.GONE

        MapIcons.btnNavegacao.setOnClickListener {
            modoNavegacaoAtivo = !modoNavegacaoAtivo
            MapIcons.setNavegacaoAtiva(modoNavegacaoAtivo)
            if (modoNavegacaoAtivo) MapIcons.map.controller.setZoom(18.0)
            else {
                MapIcons.map.mapOrientation = 0f
                MapIcons.map.controller.setZoom(15.0)
            }
        }

        MapCalculos.configurarSeekBar(
            this, MapIcons.seekBarRaio, MapIcons.raioLabel, MapIcons.map,
            { ultimaLocalizacao }
        ) { lat, lon, raio -> mapViewModel.carregarPostos(lat, lon, raio) }

        MapIcons.btnCancelarRota.setOnClickListener {
            MapRotas.cancelarRota(MapIcons.map)
            destinoAtual = null
            modoNavegacaoAtivo = false
            MapIcons.setNavegacaoAtiva(false)
            MapIcons.map.mapOrientation = 0f
            MapIcons.map.controller.setZoom(15.0)
            ultimaLocalizacao?.let { MapIcons.map.controller.animateTo(it) }
            MapIcons.limparInfoRota()
            MapIcons.btnCancelarRota.visibility = View.GONE
        }
    }

    /* Finaliza o monitoramento de localização e limpa as referências de interface ao destruir a atividade */
    override fun onDestroy() {
        super.onDestroy()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        MapIcons.limparReferencias()
    }
}
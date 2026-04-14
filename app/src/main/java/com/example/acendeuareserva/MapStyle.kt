package com.example.acendeuareserva

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon

object MapStyle {

    private val iconCache = mutableMapOf<String, BitmapDrawable>()

    /* Redimensiona recursos gráficos (drawables) para dimensões específicas e armazena em cache para otimizar o desempenho */
    fun resizeDrawable(context: Context, drawableId: Int, width: Int, height: Int): BitmapDrawable? {
        val key = "$drawableId-$width-$height"
        iconCache[key]?.let { return it }

        val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        val resized = BitmapDrawable(context.resources, bitmap)

        iconCache[key] = resized
        return resized
    }

    /* Desenha uma representação geométrica circular (polígono) no mapa para indicar visualmente o raio de busca */
    fun desenharRaio(
        map: MapView,
        userPoint: GeoPoint,
        raioMetros: Int,
        temporario: Boolean = false,
        duracaoMs: Long = 2000
    ) {
        val circle = Polygon(map)
        circle.points = Polygon.pointsAsCircle(userPoint, raioMetros.toDouble())
        circle.fillColor = 0x550099BB
        circle.strokeColor = 0xFF0099BB.toInt()
        circle.strokeWidth = 2f
        map.overlays.add(circle)
        map.invalidate()

        if (temporario) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                map.overlays.remove(circle)
                map.invalidate()
            }, duracaoMs)
        }
    }

    /* Configura e aplica uma fonte de mapa estilizada (Dark Mode) utilizando servidores de tiles externos */
    fun aplicarTilesEscuros(map: MapView, context: Context) {
        Configuration.getInstance().userAgentValue = context.packageName

        try {
            val darkTiles = XYTileSource(
                "CartoDark",
                0, 20, 256, ".png",
                arrayOf(
                    "https://a.basemaps.cartocdn.com/dark_all/",
                    "https://b.basemaps.cartocdn.com/dark_all/",
                    "https://c.basemaps.cartocdn.com/dark_all/",
                    "https://d.basemaps.cartocdn.com/dark_all/"
                )
            )
            map.setTileSource(darkTiles)
        } catch (e: Exception) {
            map.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
        }
    }
}
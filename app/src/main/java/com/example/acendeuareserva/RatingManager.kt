package com.example.acendeuareserva

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale

object RatingManager {

    private val database = FirebaseDatabase.getInstance().reference.child("avaliacoes")
    private val auth = FirebaseAuth.getInstance()

    /* Gera um identificador único para o posto baseado em suas coordenadas geográficas com precisão controlada */
    private fun gerarIdPosto(lat: Double, lon: Double): String {
        val latFormatada = String.format(Locale.US, "%.4f", lat)
        val lonFormatada = String.format(Locale.US, "%.4f", lon)
        return "${latFormatada}_${lonFormatada}".replace(".", "_")
    }

    /* Registra ou atualiza a nota atribuída pelo usuário autenticado a um posto específico no banco de dados em tempo real */
    fun salvarAvaliacao(lat: Double, lon: Double, nota: Float) {
        val userId = auth.currentUser?.uid ?: return
        val idPosto = gerarIdPosto(lat, lon)

        database.child(idPosto).child(userId).setValue(nota)
    }

    /* Calcula a média aritmética de todas as avaliações registradas para um posto e retorna o resultado via callback */
    fun buscarMedia(lat: Double, lon: Double, callback: (Float) -> Unit) {
        val idPosto = gerarIdPosto(lat, lon)

        database.child(idPosto).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var soma = 0f
                var count = 0

                for (notaSnap in snapshot.children) {
                    val nota = notaSnap.getValue(Float::class.java) ?: 0f
                    soma += nota
                    count++
                }

                if (count > 0) callback(soma / count) else callback(0f)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(0f)
            }
        })
    }

    /* Recupera exclusivamente a avaliação técnica atribuída pelo usuário logado a um determinado estabelecimento */
    fun buscarMinhaAvaliacao(lat: Double, lon: Double, callback: (Float?) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(null)
            return
        }

        val idPosto = gerarIdPosto(lat, lon)

        database.child(idPosto).child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val minhaNota = snapshot.getValue(Float::class.java)
                callback(minhaNota)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
    }
}
package com.example.acendeuareserva

import retrofit2.http.GET
import retrofit2.http.Url

interface ApiService {

    /* Realiza requisições HTTP do tipo GET para uma URL específica e retorna a resposta como String */
    @GET
    suspend fun getData(@Url url: String): String
}
package com.example.termproject.data.location

import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleGeocodingApi {

    @GET("maps/api/geocode/json")
    suspend fun reverseGeocode(
        @Query("latlng") latLng: String,
        @Query("key") apiKey: String,
        @Query("language") language: String = "ko"
    ): GeocodingResponse
}

data class GeocodingResponse(
    val results: List<GeocodingResult>,
    val status: String
)

data class GeocodingResult(
    val formatted_address: String
)
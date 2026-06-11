package com.example.termproject.data.location

import com.example.termproject.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LocationRepository {

    private val api: GoogleGeocodingApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleGeocodingApi::class.java)
    }

    suspend fun getAddressFromLatLng(
        latitude: Double,
        longitude: Double
    ): String {
        return try {
            val response = api.reverseGeocode(
                latLng = "$latitude,$longitude",
                apiKey = BuildConfig.MAPS_API_KEY
            )

            if (response.status == "OK" && response.results.isNotEmpty()) {
                response.results[0].formatted_address
            } else {
                "주소 알 수 없음"
            }
        } catch (e: Exception) {
            "주소 알 수 없음"
        }
    }
}
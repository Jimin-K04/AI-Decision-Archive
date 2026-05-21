package com.example.termproject.data.weather

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {

    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String =
            "temperature_2m,relative_humidity_2m,precipitation,rain,cloud_cover,weather_code",
        @Query("timezone") timezone: String = "Asia/Seoul"
    ): WeatherResponse

    @GET("v1/air-quality")
    suspend fun getAirQuality(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String =
            "pm10,pm2_5,uv_index",
        @Query("timezone") timezone: String = "Asia/Seoul"
    ): AirQualityResponse
}
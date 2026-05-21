package com.example.termproject.data.weather

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherRepository {

    private val weatherApi: WeatherApi =
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)

    private val airQualityApi: WeatherApi =
        Retrofit.Builder()
            .baseUrl("https://air-quality-api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)

    suspend fun getEnvironmentData(
        latitude: Double,
        longitude: Double
    ): EnvironmentData {
        val weather = weatherApi.getWeather(latitude, longitude)
        val air = airQualityApi.getAirQuality(latitude, longitude)

        val currentWeather = weather.current
        val currentAir = air.current

        val temperature = currentWeather.temperature_2m ?: 0.0
        val humidity = currentWeather.relative_humidity_2m ?: 0

        val discomfortIndex =
            calculateDiscomfortIndex(temperature, humidity)

        return EnvironmentData(
            temperature = temperature,
            humidity = humidity,
            precipitation = currentWeather.precipitation ?: 0.0,
            rain = currentWeather.rain ?: 0.0,
            cloudCover = currentWeather.cloud_cover ?: 0,
            weatherCode = currentWeather.weather_code ?: -1,
            weatherText = weatherCodeToText(currentWeather.weather_code),
            pm10 = currentAir.pm10 ?: 0.0,
            pm25 = currentAir.pm2_5 ?: 0.0,
            uvIndex = currentAir.uv_index ?: 0.0,
            discomfortIndex = discomfortIndex,
            discomfortText = discomfortIndexToText(discomfortIndex)
        )
    }

    private fun calculateDiscomfortIndex(
        temperature: Double,
        humidity: Int
    ): Double {
        return 0.81 * temperature +
                0.01 * humidity * (0.99 * temperature - 14.3) +
                46.3
    }

    private fun discomfortIndexToText(value: Double): String {
        return when {
            value < 68 -> "쾌적"
            value < 75 -> "보통"
            value < 80 -> "약간 불쾌"
            value < 83 -> "불쾌"
            else -> "매우 불쾌"
        }
    }

    private fun weatherCodeToText(code: Int?): String {
        return when (code) {
            0 -> "맑음"
            1, 2, 3 -> "대체로 맑음/흐림"
            45, 48 -> "안개"
            51, 53, 55 -> "이슬비"
            61, 63, 65 -> "비"
            71, 73, 75 -> "눈"
            80, 81, 82 -> "소나기"
            95, 96, 99 -> "뇌우"
            else -> "알 수 없음"
        }
    }
}

data class EnvironmentData(
    val temperature: Double,
    val humidity: Int,
    val precipitation: Double,
    val rain: Double,
    val cloudCover: Int,
    val weatherCode: Int,
    val weatherText: String,
    val pm10: Double,
    val pm25: Double,
    val uvIndex: Double,
    val discomfortIndex: Double,
    val discomfortText: String
)
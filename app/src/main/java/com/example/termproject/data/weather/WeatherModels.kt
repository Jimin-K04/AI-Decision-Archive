package com.example.termproject.data.weather

data class WeatherResponse(
    val current: CurrentWeather
)

data class CurrentWeather(
    val temperature_2m: Double?,
    val relative_humidity_2m: Int?,
    val precipitation: Double?,
    val rain: Double?,
    val cloud_cover: Int?,
    val weather_code: Int?
)

data class AirQualityResponse(
    val current: CurrentAirQuality
)

data class CurrentAirQuality(
    val pm10: Double?,
    val pm2_5: Double?,
    val uv_index: Double?
)
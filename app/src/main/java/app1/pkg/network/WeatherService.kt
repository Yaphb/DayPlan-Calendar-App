package app1.pkg.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query
import app1.pkg.R

interface WeatherService {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m",
        @Query("hourly") hourly: String = "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,wind_speed_10m_max",
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoResponse
}

data class OpenMeteoResponse(
    val current: CurrentWeather?,
    val hourly: HourlyForecast?,
    val daily: DailyForecast?
)

data class CurrentWeather(
    @SerializedName("temperature_2m")
    val temperature: Double,
    @SerializedName("relative_humidity_2m")
    val humidity: Int,
    @SerializedName("weather_code")
    val weatherCode: Int,
    @SerializedName("wind_speed_10m")
    val windSpeed: Double,
    val time: String
)

data class HourlyForecast(
    val time: List<String>,
    @SerializedName("temperature_2m")
    val temperature: List<Double>,
    @SerializedName("relative_humidity_2m")
    val humidity: List<Int>,
    @SerializedName("weather_code")
    val weatherCode: List<Int>,
    @SerializedName("wind_speed_10m")
    val windSpeed: List<Double>
)

data class DailyForecast(
    val time: List<String>,
    @SerializedName("weather_code")
    val weatherCode: List<Int>,
    @SerializedName("temperature_2m_max")
    val tempMax: List<Double>,
    @SerializedName("temperature_2m_min")
    val tempMin: List<Double>,
    @SerializedName("wind_speed_10m_max")
    val windSpeedMax: List<Double>
)

fun getWeatherDescriptionRes(code: Int): Int {
    return when (code) {
        0 -> R.string.weather_clear_sky
        1 -> R.string.weather_mainly_clear
        2 -> R.string.weather_partly_cloudy
        3 -> R.string.weather_overcast
        45, 48 -> R.string.weather_fog
        51, 53, 55 -> R.string.weather_drizzle
        56, 57 -> R.string.weather_freezing_drizzle
        61, 63, 65 -> R.string.weather_rain
        66, 67 -> R.string.weather_freezing_rain
        71, 73, 75 -> R.string.weather_snow_fall
        77 -> R.string.weather_snow_grains
        80, 81, 82 -> R.string.weather_rain_showers
        85, 86 -> R.string.weather_snow_showers
        95 -> R.string.weather_thunderstorm
        96, 99 -> R.string.weather_thunderstorm_hail
        else -> R.string.weather_unknown
    }
}

fun getWeatherIcon(code: Int): String {
    return when (code) {
        0 -> "01d"
        1 -> "02d"
        2 -> "03d"
        3 -> "04d"
        45, 48 -> "50d"
        51, 53, 55 -> "09d"
        56, 57 -> "09d"
        61, 63, 65 -> "10d"
        66, 67 -> "13d"
        71, 73, 75 -> "13d"
        77 -> "13d"
        80, 81, 82 -> "09d"
        85, 86 -> "13d"
        95 -> "11d"
        96, 99 -> "11d"
        else -> "01d"
    }
}

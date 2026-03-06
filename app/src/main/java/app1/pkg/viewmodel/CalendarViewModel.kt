package app1.pkg.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app1.pkg.model.CalendarEvent
import app1.pkg.model.CalendarSettings
import app1.pkg.model.WeatherInfo
import app1.pkg.network.WeatherService
import app1.pkg.network.getWeatherIcon
import app1.pkg.notifications.AlarmScheduler
import app1.pkg.widget.CalendarWidgetProvider
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPrefs = application.getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private val alarmScheduler = AlarmScheduler(application)

    private val _selectedDate = mutableStateOf(LocalDate.now())
    val selectedDate: State<LocalDate> = _selectedDate

    private val _events = mutableStateOf<List<CalendarEvent>>(loadEvents())
    val events: State<List<CalendarEvent>> = _events

    private val _settings = mutableStateOf(loadSettings())
    val settings: State<CalendarSettings> = _settings

    private val _weatherData = mutableStateOf<Map<LocalDate, WeatherInfo>>(emptyMap())
    val weatherData: State<Map<LocalDate, WeatherInfo>> = _weatherData
    
    private val _hourlyWeatherData = mutableStateOf<Map<String, WeatherInfo>>(emptyMap())
    val hourlyWeatherData: State<Map<String, WeatherInfo>> = _hourlyWeatherData

    private val _isLoadingWeather = mutableStateOf(false)
    val isLoadingWeather: State<Boolean> = _isLoadingWeather

    private val weatherService: WeatherService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherService::class.java)
    }

    val selectedDayEvents = derivedStateOf {
        val currentSelectedDate = _selectedDate.value
        _events.value.filter { it.date == currentSelectedDate }
    }

    val datesWithEvents = derivedStateOf {
        _events.value.map { it.date }.toSet()
    }

    init {
        fetchWeatherForDate(LocalDate.now())
        _events.value.map { it.date }.distinct().forEach {
            fetchWeatherForDate(it)
        }
    }

    fun onDateSelected(date: LocalDate) {
        if (_selectedDate.value != date) {
            _selectedDate.value = date
        }
    }

    fun addEvent(title: String, date: LocalDate, color: Color, time: String, description: String) {
        val newEvent = CalendarEvent(
            id = UUID.randomUUID().toString(),
            title = title,
            date = date,
            memberId = null,
            color = color,
            time = time,
            description = description
        )
        _events.value = _events.value + newEvent
        saveEvents()
        alarmScheduler.schedule(newEvent)
        fetchWeatherForDate(date)
        updateWidget()
    }

    fun updateEvent(updatedEvent: CalendarEvent) {
        _events.value = _events.value.map {
            if (it.id == updatedEvent.id) {
                alarmScheduler.cancel(it)
                alarmScheduler.schedule(updatedEvent)
                updatedEvent
            } else it
        }
        saveEvents()
        fetchWeatherForDate(updatedEvent.date)
        updateWidget()
    }

    fun deleteEvent(eventId: String) {
        val eventToRemove = _events.value.find { it.id == eventId }
        eventToRemove?.let { alarmScheduler.cancel(it) }
        _events.value = _events.value.filter { it.id != eventId }
        saveEvents()
        updateWidget()
    }

    fun updateSettings(newSettings: CalendarSettings) {
        _settings.value = newSettings
        saveSettings()
        updateWidget()
    }

    fun resetSettings() {
        _settings.value = CalendarSettings()
        saveSettings()
        updateWidget()
    }

    @SuppressLint("MissingPermission")
    fun fetchWeatherForDate(date: LocalDate) {
        val today = LocalDate.now()
        if (date.isBefore(today.minusDays(1)) || date.isAfter(today.plusDays(10))) return

        viewModelScope.launch {
            _isLoadingWeather.value = true
            try {
                val cts = CancellationTokenSource()
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cts.token
                ).await()

                location?.let { loc ->
                    val response = weatherService.getForecast(loc.latitude, loc.longitude)
                    
                    if (date == today && response.current != null) {
                        val info = WeatherInfo(
                            temperature = response.current.temperature,
                            description = "", 
                            weatherCode = response.current.weatherCode,
                            icon = getWeatherIcon(response.current.weatherCode),
                            date = date,
                            humidity = response.current.humidity,
                            windSpeed = response.current.windSpeed,
                            tempMax = response.daily?.tempMax?.firstOrNull(),
                            tempMin = response.daily?.tempMin?.firstOrNull()
                        )
                        _weatherData.value = _weatherData.value + (date to info)
                        saveCurrentWeather(info)
                    } else if (response.daily != null) {
                        val dateString = date.toString()
                        val index = response.daily.time.indexOf(dateString)
                        if (index != -1) {
                            val info = WeatherInfo(
                                temperature = response.daily.tempMax[index],
                                description = "",
                                weatherCode = response.daily.weatherCode[index],
                                icon = getWeatherIcon(response.daily.weatherCode[index]),
                                date = date,
                                tempMax = response.daily.tempMax[index],
                                tempMin = response.daily.tempMin[index],
                                windSpeed = response.daily.windSpeedMax[index]
                            )
                            _weatherData.value = _weatherData.value + (date to info)
                        }
                    }
                    
                    response.hourly?.let { hourly ->
                        val newHourlyData = mutableMapOf<String, WeatherInfo>()
                        hourly.time.forEachIndexed { index, timeStr ->
                            val info = WeatherInfo(
                                temperature = hourly.temperature[index],
                                description = "",
                                weatherCode = hourly.weatherCode[index],
                                icon = getWeatherIcon(hourly.weatherCode[index]),
                                date = LocalDateTime.parse(timeStr).toLocalDate(),
                                humidity = hourly.humidity[index],
                                windSpeed = hourly.windSpeed[index]
                            )
                            newHourlyData[timeStr] = info
                        }
                        _hourlyWeatherData.value = _hourlyWeatherData.value + newHourlyData
                    }
                    updateWidget()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingWeather.value = false
            }
        }
    }

    private fun saveEvents() {
        val eventDataList = _events.value.map { event ->
            EventData(
                event.id,
                event.title,
                event.date.toString(),
                event.color.toArgb(),
                event.description,
                event.time
            )
        }
        val json = gson.toJson(eventDataList)
        sharedPrefs.edit().putString("events_json", json).apply()
    }

    private fun loadEvents(): List<CalendarEvent> {
        val json = sharedPrefs.getString("events_json", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<EventData>>() {}.type
            val eventDataList: List<EventData> = gson.fromJson(json, type)
            eventDataList.map { data ->
                CalendarEvent(
                    data.id,
                    data.title,
                    LocalDate.parse(data.date),
                    null,
                    Color(data.colorArgb),
                    data.description,
                    data.time
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveSettings() {
        val json = gson.toJson(_settings.value)
        sharedPrefs.edit().putString("settings_json", json).apply()
    }

    private fun loadSettings(): CalendarSettings {
        val json = sharedPrefs.getString("settings_json", null) ?: return CalendarSettings()
        return try {
            gson.fromJson(json, CalendarSettings::class.java)
        } catch (e: Exception) {
            CalendarSettings()
        }
    }

    private fun saveCurrentWeather(info: WeatherInfo) {
        val json = gson.toJson(info)
        sharedPrefs.edit().putString("current_weather_json", json).apply()
    }

    private fun updateWidget() {
        val intent = Intent(getApplication<Application>(), CalendarWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val ids = AppWidgetManager.getInstance(getApplication())
            .getAppWidgetIds(ComponentName(getApplication(), CalendarWidgetProvider::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        getApplication<Application>().sendBroadcast(intent)
    }

    private data class EventData(
        val id: String,
        val title: String,
        val date: String,
        val colorArgb: Int,
        val description: String,
        val time: String
    )
}
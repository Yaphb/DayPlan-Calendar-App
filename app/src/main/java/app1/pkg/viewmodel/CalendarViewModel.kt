package app1.pkg.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import app1.pkg.model.CalendarEvent
import app1.pkg.model.CalendarSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.util.UUID

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPrefs = application.getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _selectedDate = mutableStateOf(LocalDate.now())
    val selectedDate: State<LocalDate> = _selectedDate

    private val _events = mutableStateOf<List<CalendarEvent>>(loadEvents())
    val events: State<List<CalendarEvent>> = _events

    private val _settings = mutableStateOf(loadSettings())
    val settings: State<CalendarSettings> = _settings

    val selectedDayEvents = derivedStateOf {
        val currentSelectedDate = _selectedDate.value
        _events.value.filter { it.date == currentSelectedDate }
    }

    val datesWithEvents = derivedStateOf {
        _events.value.map { it.date }.toSet()
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
    }

    fun updateEvent(updatedEvent: CalendarEvent) {
        _events.value = _events.value.map {
            if (it.id == updatedEvent.id) updatedEvent else it
        }
        saveEvents()
    }

    fun deleteEvent(eventId: String) {
        _events.value = _events.value.filter { it.id != eventId }
        saveEvents()
    }

    fun updateSettings(newSettings: CalendarSettings) {
        _settings.value = newSettings
        saveSettings()
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

    private data class EventData(
        val id: String,
        val title: String,
        val date: String,
        val colorArgb: Int,
        val description: String,
        val time: String
    )
}

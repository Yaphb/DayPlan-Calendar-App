package app1.pkg.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import app1.pkg.MainActivity
import app1.pkg.R
import app1.pkg.model.CalendarSettings
import app1.pkg.model.EventData
import app1.pkg.model.WeatherInfo
import app1.pkg.notifications.AlarmScheduler
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CalendarWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE_NOTIFY = "app1.pkg.action.TOGGLE_NOTIFY"
        const val EXTRA_EVENT_ID = "extra_event_id"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val sharedPrefs = context.getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE)
        val gson = Gson()
        
        // 1. Prepare shared data
        val today = LocalDate.now()
        // Use Locale.getDefault() to respect system language settings
        val dateStr = today.format(DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault())).uppercase()
        val dayStr = today.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))

        val eventsJson = sharedPrefs.getString("events_json", null)
        val events: List<EventData> = try {
            val type = object : TypeToken<List<EventData>>() {}.type
            gson.fromJson(eventsJson, type) ?: emptyList()
        } catch (e: Exception) { 
            Log.e("CalendarWidget", "Error parsing events", e)
            emptyList() 
        }

        val todayEvent = events.find { it.date == today.toString() }

        val weatherJson = sharedPrefs.getString("current_weather_json", null)
        val weather = try { gson.fromJson(weatherJson, WeatherInfo::class.java) } catch (e: Exception) { null }

        val settingsJson = sharedPrefs.getString("settings_json", null)
        val settings = try { gson.fromJson(settingsJson, CalendarSettings::class.java) } catch (e: Exception) { null }

        // 2. Update each widget instance synchronously with text
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.calendar_widget)
            
            views.setTextViewText(R.id.widget_date, dateStr)
            views.setTextViewText(R.id.widget_day, dayStr)

            if (todayEvent != null) {
                views.setTextViewText(R.id.widget_event_title, todayEvent.title)
                views.setTextViewText(R.id.widget_event_time, todayEvent.time)
                views.setTextColor(R.id.widget_event_time, todayEvent.colorArgb)
                
                views.setViewVisibility(R.id.widget_notify_toggle, View.VISIBLE)
                val iconRes = if (todayEvent.isNotificationEnabled) R.drawable.ic_notifications_active else R.drawable.ic_notifications_off
                views.setImageViewResource(R.id.widget_notify_toggle, iconRes)

                val toggleIntent = Intent(context, CalendarWidgetProvider::class.java).apply {
                    action = ACTION_TOGGLE_NOTIFY
                    putExtra(EXTRA_EVENT_ID, todayEvent.id)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, 
                    todayEvent.id.hashCode(), 
                    toggleIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_notify_toggle, pendingIntent)
            } else {
                // Use string resource for localization
                views.setTextViewText(R.id.widget_event_title, context.getString(R.string.no_events))
                views.setTextViewText(R.id.widget_event_time, "")
                views.setViewVisibility(R.id.widget_notify_toggle, View.GONE)
            }

            views.setTextViewText(R.id.widget_temp, weather?.let { "${it.temperature.toInt()}°C" } ?: "--°")

            val mainIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        // 3. Update images asynchronously
        if (settings?.wallpaperUri != null || weather?.icon?.isNotEmpty() == true) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    for (appWidgetId in appWidgetIds) {
                        val views = RemoteViews(context.packageName, R.layout.calendar_widget)
                        var changed = false

                        if (settings?.wallpaperUri != null) {
                            try {
                                val request = ImageRequest.Builder(context)
                                    .data(settings.wallpaperUri)
                                    .size(600, 400)
                                    .precision(Precision.EXACT)
                                    .build()
                                val result = context.imageLoader.execute(request)
                                result.drawable?.let {
                                    views.setImageViewBitmap(R.id.widget_background, drawableToBitmap(it))
                                    views.setViewVisibility(R.id.widget_overlay, View.VISIBLE)
                                    changed = true
                                }
                            } catch (e: Exception) { Log.e("CalendarWidget", "Wallpaper load failed", e) }
                        }

                        if (weather?.icon?.isNotEmpty() == true) {
                            try {
                                val iconRequest = ImageRequest.Builder(context)
                                    .data("https://openweathermap.org/img/wn/${weather.icon}@2x.png")
                                    .size(100, 100)
                                    .build()
                                val iconResult = context.imageLoader.execute(iconRequest)
                                iconResult.drawable?.let {
                                    views.setImageViewBitmap(R.id.widget_weather_icon, drawableToBitmap(it))
                                    changed = true
                                }
                            } catch (e: Exception) { Log.e("CalendarWidget", "Weather icon load failed", e) }
                        }

                        if (changed) {
                            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TOGGLE_NOTIFY) {
            val eventId = intent.getStringExtra(EXTRA_EVENT_ID)
            if (eventId != null) {
                toggleEventNotification(context, eventId)
            }
        } else {
            super.onReceive(context, intent)
        }
    }

    private fun toggleEventNotification(context: Context, eventId: String) {
        val sharedPrefs = context.getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val eventsJson = sharedPrefs.getString("events_json", null)
        val type = object : TypeToken<List<EventData>>() {}.type
        val events: List<EventData> = try {
            gson.fromJson(eventsJson, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }

        val updatedEvents = events.map { event ->
            if (event.id == eventId) {
                val updated = event.copy(isNotificationEnabled = !event.isNotificationEnabled)
                
                // Sync with Alarm system
                val alarmScheduler = AlarmScheduler(context)
                val calendarEvent = app1.pkg.model.CalendarEvent(
                    id = updated.id,
                    title = updated.title,
                    date = LocalDate.parse(updated.date),
                    color = androidx.compose.ui.graphics.Color(updated.colorArgb),
                    description = updated.description,
                    time = updated.time,
                    isNotificationEnabled = updated.isNotificationEnabled
                )

                if (updated.isNotificationEnabled) {
                    alarmScheduler.schedule(calendarEvent)
                } else {
                    alarmScheduler.cancel(calendarEvent)
                }
                updated
            } else event
        }
        
        sharedPrefs.edit().putString("events_json", gson.toJson(updatedEvents)).apply()

        // Trigger a refresh of all widgets
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, CalendarWidgetProvider::class.java))
        onUpdate(context, appWidgetManager, ids)
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) return drawable.bitmap
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1), 
            drawable.intrinsicHeight.coerceAtLeast(1), 
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
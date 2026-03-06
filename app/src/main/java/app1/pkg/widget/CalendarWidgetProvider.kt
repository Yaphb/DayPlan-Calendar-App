package app1.pkg.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.RemoteViews
import app1.pkg.MainActivity
import app1.pkg.R
import app1.pkg.model.CalendarSettings
import app1.pkg.model.WeatherInfo
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

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.calendar_widget)
        val sharedPrefs = context.getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE)
        val gson = Gson()

        // 1. Basic Content
        val today = LocalDate.now()
        views.setTextViewText(R.id.widget_date, today.format(DateTimeFormatter.ofPattern("MMM dd", Locale.ENGLISH)).uppercase())
        views.setTextViewText(R.id.widget_day, today.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)))

        // 2. Load Events
        val eventsJson = sharedPrefs.getString("events_json", null)
        val events = try {
            val type = object : TypeToken<List<EventData>>() {}.type
            gson.fromJson<List<EventData>>(eventsJson, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }

        val todayEvents = events.filter { it.date == today.toString() }
        if (todayEvents.isNotEmpty()) {
            val event = todayEvents.first()
            views.setTextViewText(R.id.widget_event_title, event.title)
            views.setTextViewText(R.id.widget_event_time, event.time)
            views.setTextColor(R.id.widget_event_time, event.colorArgb)
        } else {
            views.setTextViewText(R.id.widget_event_title, "No events today")
            views.setTextViewText(R.id.widget_event_time, "")
        }

        // 3. Weather
        val weatherJson = sharedPrefs.getString("current_weather_json", null)
        if (weatherJson != null) {
            try {
                val weather = gson.fromJson(weatherJson, WeatherInfo::class.java)
                views.setTextViewText(R.id.widget_temp, "${weather.temperature.toInt()}°C")
            } catch (e: Exception) { views.setTextViewText(R.id.widget_temp, "--°") }
        }

        // 4. Click Action
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        // 5. Update immediately with current data
        appWidgetManager.updateAppWidget(appWidgetId, views)

        // 6. Async Background Loading (Wallpaper & Icons)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settingsJson = sharedPrefs.getString("settings_json", null)
                if (settingsJson != null) {
                    val settings = gson.fromJson(settingsJson, CalendarSettings::class.java)
                    val wallpaperUri = settings.wallpaperUri ?: settings.bannerUri
                    if (wallpaperUri != null) {
                        val request = ImageRequest.Builder(context)
                            .data(wallpaperUri)
                            .size(600, 400)
                            .precision(Precision.EXACT)
                            .build()
                        val result = context.imageLoader.execute(request)
                        result.drawable?.let {
                            views.setImageViewBitmap(R.id.widget_background, drawableToBitmap(it))
                        }
                    }
                }
                
                // Load Weather Icon
                if (weatherJson != null) {
                    val weather = gson.fromJson(weatherJson, WeatherInfo::class.java)
                    if (weather.icon.isNotEmpty()) {
                        val iconRequest = ImageRequest.Builder(context)
                            .data("https://openweathermap.org/img/wn/${weather.icon}@2x.png")
                            .size(100, 100)
                            .build()
                        context.imageLoader.execute(iconRequest).drawable?.let {
                            views.setImageViewBitmap(R.id.widget_weather_icon, drawableToBitmap(it))
                        }
                    }
                }
                
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e("Widget", "Async update failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) return drawable.bitmap
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth.coerceAtLeast(1), drawable.intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private data class EventData(val id: String, val title: String, val date: String, val colorArgb: Int, val description: String, val time: String)
}
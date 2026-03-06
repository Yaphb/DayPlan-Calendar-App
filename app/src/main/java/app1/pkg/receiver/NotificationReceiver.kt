package app1.pkg.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app1.pkg.model.CalendarEvent
import app1.pkg.notifications.NotificationHelper
import com.google.gson.Gson
import java.time.LocalDate
import androidx.compose.ui.graphics.Color

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventJson = intent.getStringExtra("event_json")
        if (eventJson != null) {
            val gson = Gson()
            try {
                // We need a simple way to deserialize, or just pass individual fields
                // For simplicity in this receiver, we'll reconstruct a basic event if needed
                // but usually we'd pass the full object.
                val event = gson.fromJson(eventJson, CalendarEventData::class.java).toCalendarEvent()
                val notificationHelper = NotificationHelper(context)
                notificationHelper.showNotification(event)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private data class CalendarEventData(
        val id: String,
        val title: String,
        val date: String,
        val color: Int,
        val description: String,
        val time: String
    ) {
        fun toCalendarEvent() = CalendarEvent(
            id = id,
            title = title,
            date = LocalDate.parse(date),
            color = Color(color),
            description = description,
            time = time
        )
    }
}
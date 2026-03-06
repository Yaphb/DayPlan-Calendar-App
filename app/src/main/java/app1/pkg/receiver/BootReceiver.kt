package app1.pkg.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app1.pkg.model.CalendarEvent
import app1.pkg.notifications.AlarmScheduler
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import androidx.compose.ui.graphics.Color

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val sharedPrefs = context.getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE)
            val json = sharedPrefs.getString("events_json", null) ?: return
            
            try {
                val gson = Gson()
                val type = object : TypeToken<List<EventData>>() {}.type
                val eventDataList: List<EventData> = gson.fromJson(json, type)
                val alarmScheduler = AlarmScheduler(context)
                
                eventDataList.forEach { data ->
                    val event = CalendarEvent(
                        data.id,
                        data.title,
                        LocalDate.parse(data.date),
                        null,
                        Color(data.colorArgb),
                        data.description,
                        data.time
                    )
                    // Only reschedule future events
                    alarmScheduler.schedule(event)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
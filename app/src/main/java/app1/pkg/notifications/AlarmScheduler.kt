package app1.pkg.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import app1.pkg.model.CalendarEvent
import app1.pkg.receiver.NotificationReceiver
import com.google.gson.Gson
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val gson = Gson()

    fun schedule(event: CalendarEvent) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("event_json", gson.toJson(EventData.from(event)))
        }

        val time = parseTime(event.time)
        val scheduleTime = LocalDateTime.of(event.date, time)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (scheduleTime < System.currentTimeMillis()) return

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    scheduleTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    scheduleTime,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                scheduleTime,
                pendingIntent
            )
        }
    }

    fun cancel(event: CalendarEvent) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun parseTime(timeStr: String): LocalTime {
        return try {
            if (timeStr.equals("All Day", ignoreCase = true)) {
                LocalTime.of(8, 0)
            } else {
                val parts = timeStr.trim().split(" ")
                val timeParts = parts[0].split(":")
                var hour = timeParts[0].toInt()
                val minute = timeParts[1].toInt()
                
                if (parts.size > 1 && parts[1].equals("PM", ignoreCase = true) && hour < 12) {
                    hour += 12
                } else if (parts.size > 1 && parts[1].equals("AM", ignoreCase = true) && hour == 12) {
                    hour = 0
                }
                LocalTime.of(hour, minute)
            }
        } catch (e: Exception) {
            LocalTime.of(9, 0)
        }
    }

    private data class EventData(
        val id: String,
        val title: String,
        val date: String,
        val color: Int,
        val description: String,
        val time: String
    ) {
        companion object {
            fun from(event: CalendarEvent) = EventData(
                event.id,
                event.title,
                event.date.toString(),
                event.color.toArgb(),
                event.description,
                event.time
            )
        }
    }
}
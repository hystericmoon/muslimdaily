package id.haho.muslimdaily.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

/** Whether a prayer-times widget is on the home or lock screen (either receiver, any host). */
fun isWidgetPlaced(context: Context): Boolean {
    val manager = AppWidgetManager.getInstance(context) ?: return false
    return listOf(NextPrayerWidgetReceiver::class.java, DayScheduleWidgetReceiver::class.java).any {
        manager.getAppWidgetIds(ComponentName(context, it)).isNotEmpty()
    }
}

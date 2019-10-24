/*
 * Copyright 2019 Peter Respondek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.lanyard.helpers

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lanyard.pirateerlite.MapActivity
import java.util.*


open class NotificationReceiver : BroadcastReceiver() {

    companion object {
        private val TAG = "notifications"
        private val PREFS_NAME = "NotificationsPrefs"
        private var notificationCount = 0
    }

    data class NotificationData(
        val icon: Int,
        val id: Int,
        val channel: String,
        val title: String,
        val description: String,
        val time: Long
    )

    override fun onReceive(context: Context, intent: Intent) {
        Log.v(TAG, "Scheduled Alarm Recived at: " + System.currentTimeMillis())
        val notification = NotificationData(
            intent.getIntExtra("icon", 0),
            intent.getIntExtra("id", 1),
            intent.getStringExtra("channel"),
            intent.getStringExtra("title"),
            intent.getStringExtra("description"),
            intent.getLongExtra("time", 0)
        )

        val resultIntent = Intent(context, MapActivity::class.java)
        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(resultIntent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val builder = NotificationCompat.Builder(context, notification.channel)
            .setSmallIcon(notification.icon)
            .setContentTitle(notification.title)
            .setContentText(notification.description)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(resultPendingIntent)
            .setAutoCancel(true)

        builder.setContentIntent(resultPendingIntent)
        NotificationManagerCompat.from(context).notify(notification.id, builder.build())
    }

    fun scheduleNotification(context: Context, notification: NotificationData) {
        val settings = context.getSharedPreferences(PREFS_NAME, 0)
        val editor = settings.edit()

        notificationCount++
        editor.putInt("NotificationCount", notificationCount)
        editor.commit()

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = notification.time

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        intent.putExtra("icon", notification.icon)
        intent.putExtra("channel", notification.channel)
        intent.putExtra("title", notification.title)
        intent.putExtra("description", notification.description)
        intent.putExtra("time", notification.time)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationCount,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }

    fun clearNotifications(context: Context) {
        val settings = context.getSharedPreferences(PREFS_NAME, 0)
        notificationCount = settings.getInt("NotificationCount", 0)
        if (notificationCount == 0) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        for (i in 1..notificationCount) {
            val pendingIntent = PendingIntent.getBroadcast(context, i, intent, 0)
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
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
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import java.util.*

open class NotificationReceiver : BroadcastReceiver() {

    companion object {
        private val TAG = "notifications"
        private val PREFS_NAME = "NotificationsPrefs"
        private var notificationCount = 0
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.v(TAG, "Scheduled Alarm Recived at: " + System.currentTimeMillis())
        val notification = intent.getParcelableExtra<Notification>("notification")
        notification
        val id = intent.getIntExtra("id", 0)
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    fun scheduleNotification(context: Context, id: Int, notification: Notification, time: Date) {
        val settings = context.getSharedPreferences(PREFS_NAME, 0)
        val editor = settings.edit()
        notificationCount++
        editor.putInt("NotificationCount", notificationCount)
        editor.commit()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        intent.putExtra("notification", notification)
        intent.putExtra("id", id)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationCount,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.set(AlarmManager.RTC_WAKEUP, time.time, pendingIntent)
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
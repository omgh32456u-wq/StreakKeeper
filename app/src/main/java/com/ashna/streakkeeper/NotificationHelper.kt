package com.ashna.streakkeeper

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ashna.streakkeeper.data.ContactEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotificationHelper {

    const val CHANNEL_ID = "streak_tasks"
    const val ACTION_OPEN = "com.ashna.streakkeeper.action.OPEN_TIKTOK"
    const val ACTION_CONFIRM = "com.ashna.streakkeeper.action.CONFIRM_SENT"
    const val EXTRA_CONTACT_ID = "contactId"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Streak reminders", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Daily reminders to send your TikTok streak stickers" }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun showTaskNotification(context: Context, c: ContactEntity) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return // permission not granted — silently skipped, app still works

        fun broadcast(action: String): PendingIntent = PendingIntent.getBroadcast(
            context, (1000 + c.id).toInt(),
            Intent(context, NotificationActionReceiver::class.java)
                .setAction(action).putExtra(EXTRA_CONTACT_ID, c.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openApp = PendingIntent.getActivity(
            context, c.id.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Streak with ${c.name}")
            .setContentText("Send your ${c.stickerEmoji} sticker — open TikTok, then confirm.")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .addAction(0, "Open TikTok", broadcast(ACTION_OPEN))
            .addAction(0, "Sent ✓", broadcast(ACTION_CONFIRM))
            .build()

        NotificationManagerCompat.from(context).notify(c.id.toInt(), notification)
    }
}

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(NotificationHelper.EXTRA_CONTACT_ID, -1L)
        if (id < 0) return
        val app = context.applicationContext as StreakApp
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    NotificationHelper.ACTION_OPEN -> {
                        app.repository.markOpened(id)          // distinct "opened" state
                        TikTokHelper.openTikTok(context)
                        NotificationManagerCompat.from(context).cancel(id.toInt())
                    }
                    NotificationHelper.ACTION_CONFIRM -> {
                        app.repository.confirmSent(id)        // explicit user confirmation
                        NotificationManagerCompat.from(context).cancel(id.toInt())
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}

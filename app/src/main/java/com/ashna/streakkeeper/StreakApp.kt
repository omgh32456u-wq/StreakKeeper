package com.ashna.streakkeeper

import android.app.Application
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ashna.streakkeeper.data.StreakDatabase
import com.ashna.streakkeeper.data.StreakRepository
import java.util.concurrent.TimeUnit

class StreakApp : Application() {

    lateinit var repository: StreakRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = StreakRepository(
            Room.databaseBuilder(this, StreakDatabase::class.java, "streak_keeper.db")
                .fallbackToDestructiveMigration()
                .build()
                .contactDao()
        )
        NotificationHelper.createChannel(this)
        // Reliable background scheduling while the app is closed / screen locked.
        // WorkManager minimum period is 15 minutes, so reminders fire within ~15 min of the set time.
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "streak_check",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<StreakWorker>(15, TimeUnit.MINUTES).build()
        )
    }
}

class StreakWorker(context: android.content.Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val app = applicationContext as StreakApp
        app.repository.maintainStreaks()
        app.repository.dueForNotification().forEach { contact ->
            NotificationHelper.showTaskNotification(applicationContext, contact)
            app.repository.markNotified(contact.id)
        }
        Result.success()
    } catch (e: Exception) {
        if (runAttemptCount < 3) Result.retry() else Result.failure()
    }
}

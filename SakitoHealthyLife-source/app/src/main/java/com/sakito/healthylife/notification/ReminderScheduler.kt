package com.sakito.healthylife.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sakito.healthylife.HealthyLifeApp
import com.sakito.healthylife.MainActivity
import com.sakito.healthylife.R
import com.sakito.healthylife.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun scheduleDailyReminder() {
        scope.launch {
            val settings = (context.applicationContext as HealthyLifeApp)
                .settingsRepository.settings.first()
            updateSchedule(settings)
        }
    }

    fun reschedule(settings: AppSettings) {
        scope.launch { updateSchedule(settings) }
    }

    private suspend fun updateSchedule(settings: AppSettings) {
        val workManager = WorkManager.getInstance(context)
        if (!settings.reminderEnabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val now = LocalDateTime.now()
        var next = LocalDateTime.of(now.toLocalDate(), LocalTime.of(settings.reminderHour, settings.reminderMinute))
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        val delayMillis = Duration.between(now, next).toMillis().coerceAtLeast(1)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.NONE)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    companion object {
        private const val WORK_NAME = "daily_record_reminder"
        const val CHANNEL_ID = "daily_record_reminder"
        const val NOTIFICATION_ID = 1001
    }
}

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as HealthyLifeApp
        val settings = app.settingsRepository.settings.first()
        if (!settings.reminderEnabled) return Result.success()

        val today = LocalDate.now().toString()
        val hasDiet = app.dietRepository.getByDate(today).isNotEmpty()
        val hasWeight = app.bodyRepository.getWeightDates().contains(today)
        val hasMeasurement = app.bodyRepository.getMeasurementDates().contains(today)

        if (!hasDiet && !hasWeight && !hasMeasurement) {
            showNotification(applicationContext)
        }
        return Result.success()
    }

    private fun showNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val channel = NotificationChannel(
            ReminderScheduler.CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.reminder_channel_desc)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("今天还没有记录哦～")
            .setContentText("今天还没有记录饮食或体重，记得花一分钟记录一下～")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(ReminderScheduler.NOTIFICATION_ID, notification)
    }
}

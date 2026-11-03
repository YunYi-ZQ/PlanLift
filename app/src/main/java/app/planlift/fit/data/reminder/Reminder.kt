package app.planlift.fit.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.planlift.fit.MainActivity
import app.planlift.fit.PlanLiftApp
import app.planlift.fit.R
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? PlanLiftApp ?: return Result.success()
        val repo = app.container.repo
        val force = inputData.getBoolean(KEY_FORCE, false)

        val enrollment = repo.activeEnrollmentOnce()
        val plan = enrollment?.let { repo.plan(it.planId) }
        val day = if (enrollment != null && plan != null) {
            plan.days.getOrNull(repo.nextDayIndex(plan, enrollment))
        } else null

        if (day == null && !force) return Result.success()

        notify(
            applicationContext,
            applicationContext.getString(R.string.reminder_title),
            if (day != null && plan != null) {
                applicationContext.getString(R.string.reminder_body, day.zh, plan.zh)
            } else {
                applicationContext.getString(R.string.reminder_sample_body)
            }
        )
        return Result.success()
    }

    private fun notify(context: Context, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "PlanLift", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val intent = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        nm.notify(NOTIFY_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "planlift_reminder"
        const val NOTIFY_ID = 1001
        const val WORK_NAME = "planlift_daily_reminder"
        const val KEY_FORCE = "force"
    }
}

object ReminderScheduler {
    private const val PREFS = "planlift_prefs"
    private const val KEY = "reminder_enabled"
    private const val KEY_HOUR = "reminder_hour"
    private const val KEY_MINUTE = "reminder_minute"

    fun hour(context: Context): Int =
        context.getSharedPreferences(PREFS, 0).getInt(KEY_HOUR, 18)

    fun minute(context: Context): Int =
        context.getSharedPreferences(PREFS, 0).getInt(KEY_MINUTE, 0)

    /** 修改提醒时间：已在运行的话重新排期 */
    fun setTime(context: Context, hour: Int, minute: Int) {
        context.getSharedPreferences(PREFS, 0).edit()
            .putInt(KEY_HOUR, hour)
            .putInt(KEY_MINUTE, minute)
            .apply()
        if (isEnabled(context)) schedule(context)
    }

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, 0).getBoolean(KEY, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, 0).edit().putBoolean(KEY, enabled).apply()
        if (enabled) schedule(context) else cancel(context)
    }

    fun schedule(context: Context) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour(context))
            set(Calendar.MINUTE, minute(context))
            set(Calendar.SECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val delay = target.timeInMillis - now.timeInMillis

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /** 立即执行一次（调试自检 / 预览通知长什么样）。没有计划时也发，方便验证通知本身。 */
    fun runNow(context: Context) {
        val request = androidx.work.OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(androidx.work.workDataOf(ReminderWorker.KEY_FORCE to true))
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(ReminderWorker.WORK_NAME)
    }
}

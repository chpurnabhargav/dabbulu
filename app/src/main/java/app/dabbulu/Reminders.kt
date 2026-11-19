package app.dabbulu

import android.Manifest
import android.app.AlarmManager
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
import app.dabbulu.domain.Money
import app.dabbulu.quickadd.QuickAddActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZonedDateTime

/** A gentle 9 PM nudge: "Logged everything today?" Tapping it opens Quick Add. */
object Reminders {
    private const val CHANNEL = "daily_reminder"
    private val AT: LocalTime = LocalTime.of(21, 0)

    fun schedule(context: Context, enabled: Boolean) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(
            context, 1, Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        am.cancel(pi)
        if (!enabled) return
        val now = ZonedDateTime.now()
        var next = now.with(AT)
        if (!next.isAfter(now)) next = next.plusDays(1)
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), AlarmManager.INTERVAL_DAY, pi)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(context: Context, text: String) {
        if (!canNotify(context)) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Daily reminder", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(
            context, 2, Intent(context, QuickAddActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_tile_rupee)
            .setContentTitle("Logged everything today?")
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(1, n)
        } catch (_: SecurityException) {
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = context.repository
                if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
                    Reminders.schedule(context, repo.settings.first().dailyReminder)
                } else {
                    val s = repo.todayStats.first()
                    val left = s.budget?.leftToday
                    val text = when {
                        left == null -> "${Money.format(s.spentToday)} spent today. Tap to add anything you missed."
                        left >= 0 -> "${Money.format(s.spentToday)} spent · ${Money.format(left)} left today. Tap to add anything you missed."
                        else -> "${Money.format(s.spentToday)} spent · ${Money.format(-left)} over today's limit."
                    }
                    Reminders.show(context, text)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

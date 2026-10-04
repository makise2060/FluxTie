package com.huanchengfly.tieba.post.update

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.huanchengfly.tieba.post.MainActivityV2
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.pendingIntentFlagImmutable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 下载更新通知（固定 ID 22）：
 * 进度走「更新下载」渠道（LOW，静默）；完成后换「更新提醒」渠道（HIGH，可点击安装）。
 * POST_NOTIFICATIONS 未授权时静默跳过（弹窗进度不受影响）。
 */
@Singleton
class UpdateNotification @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val notificationManager by lazy { NotificationManagerCompat.from(context) }

    fun showProgress(percent: Int, indeterminate: Boolean) {
        if (!canNotify()) return
        ensureChannels()
        notificationManager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_DOWNLOAD)
                .setSmallIcon(R.drawable.ic_update_download)
                .setContentTitle(context.getString(R.string.tip_update_downloading))
                .setContentText(
                    if (indeterminate) {
                        null
                    } else {
                        context.getString(R.string.tip_update_notification_progress, percent)
                    }
                )
                .setProgress(100, percent.coerceIn(0, 100), indeterminate)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build()
        )
    }

    fun showComplete() {
        if (!canNotify()) return
        ensureChannels()
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivityV2::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(UpdateManager.EXTRA_INSTALL_UPDATE, true)
            },
            pendingIntentFlagImmutable() or PendingIntent.FLAG_UPDATE_CURRENT
        )
        notificationManager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_READY)
                .setSmallIcon(R.drawable.ic_update_download)
                .setContentTitle(context.getString(R.string.title_update_notification_done))
                .setContentText(context.getString(R.string.tip_update_notification_done))
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        )
    }

    fun cancel() {
        runCatching { notificationManager.cancel(NOTIFICATION_ID) }
    }

    private fun ensureChannels() {
        notificationManager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_DOWNLOAD, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(context.getString(R.string.title_update_channel_download))
                .setShowBadge(false)
                .build()
        )
        notificationManager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_READY, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.title_update_channel_ready))
                .setShowBadge(true)
                .build()
        )
    }

    private fun canNotify(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val NOTIFICATION_ID = 22
        private const val CHANNEL_DOWNLOAD = "4"
        private const val CHANNEL_READY = "5"
    }
}

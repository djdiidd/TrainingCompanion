package com.companion.android.workoutcompanion.timeutils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.companion.android.workoutcompanion.R
import com.companion.android.workoutcompanion.activities.MainActivity
import com.companion.android.workoutcompanion.timeutils.ActionManager.Companion.getTimeInFormatHMMSS
import java.util.*

/**
 * Класс для работы основного секундомера в фоновом режиме
 */
class StopwatchService : Service() {
    override fun onBind(p0: Intent?): IBinder? = null

    // Экземпляр класса для планирования выполнения задачи
    private val timer = Timer()

    private var isFirstRun = true

    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder

    override fun onCreate() {
        super.onCreate()
        notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
    }

    /**
     * Данный метод начнет свою работу только тогда,
     * когда будет вызвана startService(Intent);
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (isFirstRun) {
            startForegroundService()
            isFirstRun = false
        }
        val time = intent.getIntExtra(TIME_EXTRA, 0)
        // Отправка обновленного времени на 1 секунду каждую секунду с задержкой 0
        timer.scheduleAtFixedRate(TimeTask(time), 0, 1000)
        return START_NOT_STICKY
    }

    /**
     * После уничтожения приложения, отменим нашу
     * длительную операцию обновления времени;
     */
    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
    }

    /**
     * Внутренний класс, который выполняет обновление времени;
     */
    private inner class TimeTask(private var time: Int) : TimerTask() {
        // Переопределяем действия, которые будут происходит каждую секунду
        override fun run() {
            val intent = Intent(TIMER_UPDATED)
            Log.d("Tic", "sw - $time")
            notificationBuilder.setContentText(getString(R.string.notification_content_text, getTimeInFormatHMMSS(time)))
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
            intent.putExtra(TIME_EXTRA, time++)
            // Отправляем интент с временем, который будет получен
            sendBroadcast(intent) //  в TimeViewModel.updateTime (Receiver)
        }
    }

    /**
     * Создание канала с уведомлениями;
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Запуск текущего сервиса в качестве сервиса,
     * работающего даже при выключении приложения;
     */
    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_notifying)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText("0:00:00")
            .setContentIntent(getMainActivityPendingIntent())
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun getMainActivityPendingIntent() = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java)
            .also { it.action = ACTION_SHOW_MAIN_FRAGMENT },
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) FLAG_IMMUTABLE
        else FLAG_UPDATE_CURRENT
    )


    /**
     * Константы, доступные извне
     */
    companion object {
        const val TIMER_UPDATED = "timer-updated"
        const val TIME_EXTRA = "timer-extra"
        const val NOTIFICATION_CHANNEL_ID = "notification-channel"
        const val NOTIFICATION_CHANNEL_NAME = "Workout in progress"
        const val NOTIFICATION_ID = 69
        const val ACTION_SHOW_MAIN_FRAGMENT = "action-show-start-fragment"
    }
}
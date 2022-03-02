package com.companion.android.workoutcompanion.timeutils

import android.app.Service
import android.content.Intent
import android.os.IBinder
import java.util.Timer
import java.util.TimerTask

/**
 * Сервис, который обновляет общее время тренировки.
 */
class CountDownService : Service() {

    override fun onBind(p: Intent?): IBinder? = null

    // Объект, который производит счет времени
    private var timer = Timer()

    /**
     * Данный метод сработает после запуска startService(Intent)
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val time = intent.getIntExtra(TIME_EXTRA, 0)
        if (time <= 0) {
            stopSelf(startId)
        }
        // Отправка обновленного времени на 1 секунду каждую секунду с задержкой 0
        timer.scheduleAtFixedRate(TimeTask(time), 0, 1000)
        return START_NOT_STICKY
    }

    // При остановке сервиса (по нажатию кнопки пауза)
    // остановим счет таймера
    override fun onDestroy() {
        timer.cancel()
        super.onDestroy()
    }

    /**
     * Внутренний класс, который выполняет обновление времени с заданной задержкой и периодом
     */
    private inner class TimeTask(private var time: Int) : TimerTask() {
        // Переопределяем действия, которые будут происходит каждую секунду
        override fun run() {
            val intent = Intent(TIMER_UPDATED)
            --time // Обновляем полученное время на секунду
            if (time <= 0) { timer.cancel(); timer = Timer() }
            intent.putExtra(TIME_EXTRA, time)
            // Отправляем интент с временем, который будет получен
            sendBroadcast(intent) //  в TimeViewModel.updateTime (Receiver)
        }
    }

    /**
     * Константы, доступные извне
     */
    companion object {
        const val TIMER_UPDATED = "cd-timer-updated"
        const val TIME_EXTRA = "cd-time-extra"
    }

}
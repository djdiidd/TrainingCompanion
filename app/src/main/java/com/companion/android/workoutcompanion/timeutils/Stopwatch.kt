package com.companion.android.workoutcompanion.timeutils

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.companion.android.workoutcompanion.R

/**
 * Сервис секундомер, использующий broadCast
 */
class Stopwatch(private val context: Context) {

    // Инициализация интента для класса сервиса
    private var serviceIntent: Intent =
        Intent(context.applicationContext, StopwatchService::class.java)

    init {
        addLifeCycleObserver()
    }

    private var generalTime: Int = 0 // Общее время (в секундах)
    private var timeIsGoing = false   // Идет ли счет времени

    private val clockView: TextView? = (context as Activity).findViewById(R.id.general_clock)
    private val pauseResumeButton: AppCompatImageButton =
        (context as Activity).findViewById(R.id.pause_resume_button)

    private var isEnabled: Boolean = true

    /** Идет ли счет времени */
    fun isGoing() = timeIsGoing

    /** Установить, что счет времени идет/не идет */
    fun setGoing(isGoing: Boolean) {
        timeIsGoing = isGoing
    }

    /** Получение оставшегося времени */
    fun getRemaining() = generalTime

    /** Установка нового времени */
    fun setTime(time: Int) {
        generalTime = time
    }

    /**
     * Получение времени в String из Int
     */
    fun getTimeInFormatHMMSS(time: Int): String {
        time.also {
            return String.format(
                "%d:%02d:%02d",
                it % 86400 / 3600,
                it % 86400 % 3600 / 60,
                it % 86400 % 3600 % 60
            )
        }
    }

    /**
     * Полная остановка секундомера;
     */
    fun stopAndUnregister() {
        stop()
        isEnabled = false
        context.unregisterReceiver(newTimeReceiver)
    }

    /**
     * Запуск секундомера если он не запущен и,
     * соответственно, остановка, если он запущен
     */
    fun startOrStop() {
        if (timeIsGoing) stop()
        else start()
    }

    /**
     * Объект, который будет сохранять полученное
     * значение и отображать его на экране
     */
    private val newTimeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            generalTime = intent.getIntExtra(StopwatchService.TIME_EXTRA, 0)
            updateText()
        }
    }

    /**
     * Запуск секундомера посредством запуска сервиса
     * с переданным интентом в виде значения текущего времени
     */
    private fun start() {
        if (!isEnabled) {
            context.registerReceiver(newTimeReceiver,
                IntentFilter(StopwatchService.TIMER_UPDATED))
            generalTime = 0
            isEnabled = true
        }
        pauseResumeButton.setImageResource(R.drawable.ic_pause)
        serviceIntent.putExtra(StopwatchService.TIME_EXTRA, generalTime)
        context.startService(serviceIntent)
        timeIsGoing = true
    }

    /**
     * Остановка секундомера путем вызова stopService(serviceIntent), после которого будет прекращен
     */
    private fun stop() {
        pauseResumeButton.setImageResource(R.drawable.ic_play)
        context.stopService(serviceIntent)
        timeIsGoing = false
    }

    private fun updateText() {
        clockView?.text = getTimeInFormatHMMSS(generalTime)
    }

    /**
     * Определение поведения наблюдателя жизненного цикла
     */
    private fun addLifeCycleObserver() {
        val defaultLifecycleObserver = object : DefaultLifecycleObserver {
            // При возобновлении:
            // Связывание объекта отправляющего обновленное
            // время и получающего по интенту TIMER_UPDATED
            override fun onCreate(owner: LifecycleOwner) {
                super.onCreate(owner)
                context.registerReceiver(
                    newTimeReceiver,
                    IntentFilter(StopwatchService.TIMER_UPDATED)
                )
            }

            // При уничтожении, удаляем связь
            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                context.unregisterReceiver(newTimeReceiver)
            }
        }
        // Добавляем наблюдателя
        (context as LifecycleOwner).lifecycle.addObserver(defaultLifecycleObserver)
    }
}
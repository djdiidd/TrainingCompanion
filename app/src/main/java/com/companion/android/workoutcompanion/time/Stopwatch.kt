package com.companion.android.workoutcompanion.time

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.companion.android.workoutcompanion.R
import com.companion.android.workoutcompanion.interfaces.TimeCounting
import com.companion.android.workoutcompanion.time.ActionManager.Companion.getTimeInFormatHMMSS

/**
 * Сервис секундомер, использующий broadcast
 */
class Stopwatch(private val context: Context) : TimeCounting {

    // Инициализация интента для класса сервиса
    private var serviceIntent: Intent =
        Intent(context.applicationContext, StopwatchService::class.java)

    var sendTicksTo: TimeCallback? = null

    init {
        addLifeCycleObserver()
    }

    var time: Int = 0 // Общее время (в секундах)
        set(value) {
            field = value
            toolbar?.title = context.resources.getString(R.string.total_time, getTimeInFormatHMMSS(time))
        }
    override var isGoing = false   // Идет ли счет времени
    override var isPaused = false

    private val toolbar: Toolbar? = (context as Activity).findViewById(R.id.toolbar)

    private var isServiceEnabled: Boolean = true

    /**
     * Полная остановка секундомера;
     */
    override fun stop() {
        if (!isServiceEnabled) return
        pause()
        isServiceEnabled = false
        context.unregisterReceiver(newTimeReceiver)
    }

    override fun reset() {
        TODO("Not yet implemented")
    }

    /**
     * Запуск секундомера если он не запущен и,
     * соответственно, остановка, если он запущен
     */
    fun startOrPause() {
        if (isGoing) pause()
        else start()
    }

    /**
     * Объект, который будет сохранять полученное
     * значение и отображать его на экране
     */
    private val newTimeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            time = intent.getIntExtra(StopwatchService.TIME_EXTRA, 0)
            sendTicksTo?.tick()
        }
    }

    override fun enable(vararg views: View) {
        assert(toolbar != null)
    }

    /**
     * Запуск секундомера посредством запуска сервиса
     * с переданным интентом в виде значения текущего времени
     */
    override fun start() {
        if (!isServiceEnabled) {
            context.registerReceiver(
                newTimeReceiver,
                IntentFilter(StopwatchService.TIMER_UPDATED)
            )
            time = 0
            isServiceEnabled = true
        }
        serviceIntent.putExtra(StopwatchService.TIME_EXTRA, time)
        context.startService(serviceIntent)
        isGoing = true
        isPaused = false
    }

    /**
     * Остановка секундомера путем вызова stopService(serviceIntent), после которого будет прекращен
     */
    override fun pause() {
        context.stopService(serviceIntent)
        isGoing = false
        isPaused = true
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
                isServiceEnabled = true
            }

            // При уничтожении, удаляем связь
            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                if (isServiceEnabled) {
                    isServiceEnabled = false
                    context.unregisterReceiver(newTimeReceiver)
                }
            }
        }
        // Добавляем наблюдателя
        (context as LifecycleOwner).lifecycle.addObserver(defaultLifecycleObserver)
    }

    interface TimeCallback {
        fun tick()
    }
}
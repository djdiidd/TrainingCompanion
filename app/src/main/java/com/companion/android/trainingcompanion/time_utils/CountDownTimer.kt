package com.companion.android.trainingcompanion.time_utils

import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import com.companion.android.trainingcompanion.objects.Params

/**
 * Таймер, работающий в качестве сервиса, который использует широкое вещание.
 */
class CountDownTimer(private val context: Context, private val serviceIntent: Intent) {


//---------------------------------------[ Данные ]-------------------------------------------------

//                                                                             Второстепенные данные
    // Элементы экрана, которые можно добавить с помощью attachUI
    private var clockTextView: TextView? = null
    private var clockProgressBar: ProgressBar? = null

    // Экземпляр интерфейса, функция которого вызовется при окончании таймера
    private var callback: Callback = context as Callback

    // Аниматор для ProgressBar
    private lateinit var animator: ObjectAnimator

    // Коэффициент анимации
    private var animCoefficient: Int = 0
        get() {
            if (startTime in Params.restTimeDefaultRange) {
                return (40 - startTime * 0.13).toInt()
            }
            else {
                return (40 - startTime * 0.05).toInt()
            }
        }
        set(value) {
            if (value > 0) field = value
            else throw Error("Coefficient of animation cannot be <= 0")
        }

//                                                                             Первостепенные данные
    private var time: Int = 60    // Оставшееся время;

    var isGoing: Boolean = false  // Идет ли счет времени;

    var startTime: Int = 0        // Изначальное время;

    private var isFinished = false        // Закончился ли таймер;

    // Получатель данных, обновляющий значение времени
    val timeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("MyTag", "time is $time")
            time = intent.getIntExtra(CountDownService.TIME_EXTRA, 0)
            updateTextUI()
            if (time == 0) { finish() }
        }
    }


//--------------------------------[ Открытый интерфейс ]--------------------------------------------


    /**
     * Получение текущего времени;
     */
    fun getTime() = time

    /**
     * Установка время на заданное;
     */
    fun setTime(time: Int) {
        this.time = time
        if (startTime == 0 || !isGoing) {
            Log.d("MyTag", "start time successfully set to $time")
            startTime = time
        }
    }

    /**
     * Запуск секундомера если он не запущен и,
     * соответственно, остановка, если он запущен
     */
    fun startOrStop() {
        if (isGoing) stop() else start()
    }

    /**
     *  Добавление элементов экрана к обработке времени;
     * @param textView Текстовое представление,
     *  которое будет использоваться для отображения времени;
     * @param progressBar Индикатор, который будет уменьшаться по мере уменьшения времени;
     */
    fun attachUI(textView: TextView, progressBar: ProgressBar) {
        clockTextView = textView
        textView.text = getTimeInFormatMMSS()
        clockProgressBar = progressBar
        restoreProgressBar()
    }

    /**
     * Удаления представлений на экране, которые
     * использовались для отображения соответствующих данных;
     */
    fun detachUI() {
        clockProgressBar = null
        clockTextView = null
    }

    /**
     * Установка стартовых значений объекту;
     */
    fun setDefaults(start_time: Int): CountDownTimer {
        time = start_time
        startTime = start_time
        isGoing = false
        isFinished = false
        return this
    }

    /**
     * Интерфейс, функция которого будет вызвана при завершении таймера;
     */
    interface Callback {
        fun timerFinished()
    }


//--------------------------------[ Закрытый интерфейс ]--------------------------------------------

    /**
     * Завершение таймера;
     */
    private fun finish() {
        isFinished = true
        startTime = 0
        callback.timerFinished()
    }

    /**
     * Получение времени в формате "ММ:СС"
     */
    private fun getTimeInFormatMMSS(time: Int = this.time): String {
        time.also {
            return String.format(
                "%02d:%02d",
                it % 86400 % 3600 / 60,
                it % 86400 % 3600 % 60
            )
        }
    }

    /**
     * Восстановление или переопределение прогресса индикатора заполненности (при наличии)
     */
    private fun restoreProgressBar(maxValue: Int = startTime, currentValue: Int = time) {
        if (clockProgressBar == null || currentValue == 0) { return }

        val coefficient = animCoefficient

        clockProgressBar!!.max = (maxValue * coefficient)

        val currentProgress = currentValue * coefficient

        animator = ObjectAnimator.ofInt(
            clockProgressBar!!, "progress", currentProgress, 0
        ).apply {
            duration = (currentProgress * 2000.0 / coefficient).toLong()
            interpolator = null
            if (isGoing) { start() } else { clockProgressBar!!.progress = currentProgress }
        }
    }

    /**
     * Запуск секундомера посредством запуска сервиса
     * с переданным интентом в виде значения текущего времени
     */
    private fun start() {
        if (time <= 0) {
            return
        } else {
            isFinished = false
        }
        if (startTime == 0)
            throw Exception("startTime is 0. \nPossible problem: " +
                    "Time was not selected automatically")
        serviceIntent.putExtra(CountDownService.TIME_EXTRA, time)
        context.startService(serviceIntent)
        isGoing = true
        restoreProgressBar()
    }

    /**
     * Остановка секундомера путем вызова stopService(serviceIntent),
     * после которого будет прекращен
     */
    private fun stop() {
        animator.pause()
        context.stopService(serviceIntent)
        isGoing = false
    }

    /**
     * Обновление TextView, содержащего время секундомера;
     */
    private fun updateTextUI() {
        clockTextView?.text = getTimeInFormatMMSS(time)
    }

}//=================================================================================================
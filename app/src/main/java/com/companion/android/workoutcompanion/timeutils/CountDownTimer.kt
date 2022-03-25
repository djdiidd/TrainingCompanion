package com.companion.android.workoutcompanion.timeutils

import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.companion.android.workoutcompanion.R
import com.companion.android.workoutcompanion.objects.BreakNotifyingMode
import com.companion.android.workoutcompanion.objects.WorkoutParams
import com.companion.android.workoutcompanion.timeutils.ActionManager.Companion.getTimeInFormatMMSS
import com.companion.android.workoutcompanion.timeutils.CountDownService.Companion.TIMER_UPDATED


/**
 * Таймер, работающий в качестве сервиса, который использует широкое вещание.
 */
class CountDownTimer(private val context: Context, private val callbackNew: Callback) {
//==================================================================================================

    init {
        addLifeCycleObserver()
    }

//---------------------------------------[ Данные ]-------------------------------------------------

    //                                                                         Второстепенные данные
    // Элементы экрана, которые можно добавить с помощью attachUI
    private var clockTextView: TextView? = null
    private var clockProgressBar: ProgressBar? = null

    private var notifying: Notifying? = null
    private var notifyingSignalCount: Int = 0

    // Аниматор для ProgressBar
    private var animator: ObjectAnimator? = null

    //                                                                         Первостепенные данные
    private var time: Int = 60     // Оставшееся время;

    var isGoing: Boolean = false   // Идет ли счет времени;

    var startTime: Int = 0         // Изначальное время;
        set(value) { field = value; Log.d("MyTag", "Start time set to $field") }

    var isFinished = false         // Закончился ли таймер;
        private set

    private var isEnabled = true   // Зарегистрирован ли сервис?

    // Получатель данных, обновляющий значение времени
    private val timeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            time = intent.getIntExtra(CountDownService.TIME_EXTRA, 0)
            handleReceivedTime()
        }
    }

    private val serviceIntent = Intent(context.applicationContext, CountDownService::class.java)


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
        if (startTime == 0) startTime = time
        notifyingSignalCount = getSignalCountOfTime()
    }

    /**
     * Запуск секундомера если он не запущен и,
     * соответственно, остановка, если он запущен
     */
    fun startOrStop() {
        if (isGoing) stop() else start()
    }

    /**
     * Добавление режима/типа/способа уведомления
     * пользователя перед началом подхода;
     */
    fun setNotifyingType(type: Int) {
        if (type in WorkoutParams.breakNotificationRange) {
            notifying = Notifying(context, type)
        } else throw Error("Incorrect notifying type")
    }

    /**
     *  Добавление элементов экрана к обработке времени;
     * @param textView Текстовое представление,
     *  которое будет использоваться для отображения времени;
     * @param progressBar Индикатор, который будет уменьшаться по мере уменьшения времени;
     */
    fun attachUI(textView: TextView, progressBar: ProgressBar) {
        clockTextView = textView
        textView.text = getTimeInFormatMMSS(time)
        clockProgressBar = progressBar
        restoreProgressBar()
    }

    /**
     * Удаления представлений на экране, которые
     * использовались для отображения соответствующих данных;
     */
    fun detachUI(waitEnd: Boolean = false) {
        clockTextView = null
        if (!waitEnd) {
            animator?.cancel()
            clockProgressBar = null
        } else {
            animator?.doOnEnd {
                clockProgressBar = null
                animator?.cancel()
            }
        }
    }

    /**
     * Установка стартовых значений объекту;
     */
    fun setStartState(start_time: Int): CountDownTimer {
        time = start_time
        startTime = start_time
        isGoing = false
        isFinished = false
        return this
    }

    /**
     * Полная остановка таймера;
     */
    fun stopAndUnregister() {
        if (!isEnabled) return
        stop(); isEnabled = false
        context.unregisterReceiver(timeReceiver)
        detachUI()
    }


    /**
     * Интерфейс, функция которого будет вызвана при завершении таймера;
     */
    interface Callback {
        fun timerFinished()
    }


//--------------------------------[ Закрытый интерфейс ]--------------------------------------------

    /**
     * Получение количества сигналов/оповещений в зависимости от изначального времени;
     */
    private fun getSignalCountOfTime(): Int {
        var count = 0
        WorkoutParams.notifyingSignalAt.forEach {
            if (time <= it) ++count
            else return count
        }
        return count
    }

    /**
     * Завершение таймера;
     */
    private fun finish() {
        isFinished = true
        startTime = 0
        notifyingSignalCount = 0
        callbackNew.timerFinished()
    }

    /**
     * Восстановление или переопределение прогресса индикатора заполненности (при наличии)
     */
    private fun restoreProgressBar(maxValue: Int = startTime, currentValue: Int = time) {
        if (clockProgressBar == null || currentValue == 0) return

        if (maxValue == 0) throw Error("maxValue is 0")

        val coefficient = 100
        val currentProgress = currentValue * coefficient

        Log.d("MyTag", "currentProgress = $currentProgress; maxProgress = ${maxValue * coefficient}")

        clockProgressBar!!.max = (maxValue * coefficient)

        animator = ObjectAnimator.ofInt(
            clockProgressBar!!, "progress", currentProgress, 0
        ).apply {
            interpolator = null
            duration = (currentProgress * 1000.0 / coefficient).toLong()
            if (isGoing) {
                start()
            } else {
                clockProgressBar!!.progress = currentProgress
            }
        }
    }

    /**
     * Запуск секундомера посредством запуска сервиса
     * с переданным интентом в виде значения текущего времени
     */
    private fun start() {
        if (time == 0) return
        else isFinished = false

        if (startTime == 0)
            throw Exception(
                "startTime is 0. \nPossible problem: " +
                        "Time was not selected automatically"
            )
        if (!isEnabled) {
            context.registerReceiver(
                timeReceiver,
                IntentFilter(TIMER_UPDATED)
            )
            isEnabled = true
        }
        serviceIntent.putExtra(CountDownService.TIME_EXTRA, time)
        context.startService(serviceIntent)
        isGoing = true
        restoreProgressBar()
        notifyingSignalCount = getSignalCountOfTime()
    }

    /**
     * Остановка секундомера путем вызова stopService(serviceIntent),
     * после которого будет прекращен
     */
    private fun stop() {
        animator?.pause()
        context.stopService(serviceIntent)
        isGoing = false
    }

    /**
     * Обновление TextView, содержащего время секундомера;
     */
    private fun updateTextUI() {
        clockTextView?.text = getTimeInFormatMMSS(time)
    }

    /**
     * Определение поведения наблюдателя жизненного цикла
     */
    private fun addLifeCycleObserver() {
        val defaultLifecycleObserver = object : DefaultLifecycleObserver {
            // При возобновлении:
            // Связывание объекта отправляющего обновленное
            // время и получающего по интенту TIMER_UPDATED
            override fun onResume(owner: LifecycleOwner) {
                super.onResume(owner)
                context.registerReceiver(
                    timeReceiver,
                    IntentFilter(TIMER_UPDATED)
                )
                isEnabled = true
            }

            // При уничтожении, удаляем связь
            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                if (isEnabled) {
                    isEnabled = false
                    context.unregisterReceiver(timeReceiver)
                }
            }
        }
        // Добавляем наблюдателя
        (context as LifecycleOwner).lifecycle
            .addObserver(defaultLifecycleObserver)
    }

    /**
     * Определяем обработчик текущего времени;
     */
    private fun handleReceivedTime() {
        updateTextUI()
        if (time == WorkoutParams.notifyingSignalAt[notifyingSignalCount]) {
            if (notifying == null) throw Error("notifying == null")
            notifying?.play()
            ++notifyingSignalCount
        } else if (time == 0) {
            notifying?.playLast()
            finish()
        }
    }

    private inner class Notifying(context: Context, notifyingType: Int) {

        private var notifying: Any? = null
        private var lastNotifying: Any? = null

        init {
            when (notifyingType) {
                BreakNotifyingMode.SOUND -> {
                    notifying = MediaPlayer.create(context, R.raw.countdown)
                    lastNotifying = MediaPlayer.create(context, R.raw.countdown_last)
                }
                BreakNotifyingMode.VIBRATION -> {
                    notifying =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val vibratorManager = context
                                .getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                            vibratorManager.defaultVibrator
                        } else {
                            @Suppress("Deprecation")
                            context.getSystemService(VIBRATOR_SERVICE) as Vibrator
                        }
                }
                BreakNotifyingMode.ANIMATION -> Unit
                else -> throw Error("Notifying type is not defined")
            }
        }

        fun play() {
            when (notifying) {
                null -> Unit
                is MediaPlayer -> (notifying as MediaPlayer).start()
                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        (notifying as Vibrator).vibrate(
                            VibrationEffect
                                .createOneShot(700, VibrationEffect.EFFECT_TICK)
                        )
                    } else {
                        @Suppress("Deprecation")
                        (notifying as Vibrator).vibrate(700)
                    }
                }
            }
        }

        fun playLast() {
            when (lastNotifying) {
                null -> {
                    if (notifying == null) return
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        (notifying as Vibrator).vibrate(
                            VibrationEffect
                                .createOneShot(1000, VibrationEffect.EFFECT_TICK)
                        )
                    } else {
                        @Suppress("Deprecation")
                        (notifying as Vibrator).vibrate(1000)
                    }
                }
                else -> (lastNotifying as MediaPlayer).start()
            }
        }
    }
}


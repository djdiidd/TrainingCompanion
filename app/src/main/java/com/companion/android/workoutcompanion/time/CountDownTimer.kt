package com.companion.android.workoutcompanion.time

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.*
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.companion.android.workoutcompanion.R
import com.companion.android.workoutcompanion.interfaces.TimeCounting
import com.companion.android.workoutcompanion.objects.Anim
import com.companion.android.workoutcompanion.objects.BreakNotifyingMode
import com.companion.android.workoutcompanion.objects.WorkoutParams
import com.companion.android.workoutcompanion.time.ActionManager.Companion.getTimeInFormatMMSS
import com.companion.android.workoutcompanion.time.CountDownService.Companion.TIMER_UPDATED


/**
 * Таймер, работающий в качестве сервиса, который использует широкое вещание.
 */
class CountDownTimer(private val context: Context, private val callback: Callback) : TimeCounting {
//==================================================================================================

    init {
        addLifeCycleObserver()
    }

//---------------------------------------[ Данные ]-------------------------------------------------

                                                                             //Второстепенные данные

    // Элементы экрана, которые можно добавить с помощью attachUI
    private var clockTextView: TextView? = null
    private var clockProgressBar: ProgressBar? = null

    private var notifying: Notifying? = null
    private var notifyingSignalCount: Int = 0

    // Аниматор для ProgressBar
    private var progressBarAnimator: ObjectAnimator? = null

    private val progressPulseEffectSet = AnimatorSet()

    private var attemptsToRunPulseAnimation = 0

    private val pulseAnimRunnable = PulseAnimRunnable()


                                                                            // Первостепенные данные

    private var time: Int = 60     // Оставшееся время;

    override var isGoing: Boolean = false   // Идет ли счет времени;

    var startTime: Int = 0         // Изначальное время;
        set(value) {
            field = value; Log.d("MyTag", "Start time set to $field")
        }

    var isFinished = false         // Закончился ли таймер;
        private set

    override var isPaused = false

    private var isServiceEnabled = true   // Зарегистрирован ли сервис?

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
    fun startOrPause() {
        if (isGoing) pause() else start()
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
     * Добавление элементов экрана к обработке времени
     */
    override fun enable(vararg views: View) {
        clockTextView = views[0] as TextView
        clockTextView!!.text = getTimeInFormatMMSS(time)
        clockProgressBar = views[1] as ProgressBar
        pulseAnimRunnable.animatedCircle = views[2]
        if (attemptsToRunPulseAnimation != 0) {
            pausePulseAnimation()
            startPulseAnimation(attemptsToRunPulseAnimation)
        }
        restoreProgressBar()
    }

    /**
     * Удаления представлений на экране, которые
     * использовались для отображения соответствующих данных;
     */
    override fun cancel(animate: Boolean) {
        clockTextView = null
        if (!animate) {
            progressBarAnimator?.cancel()
            progressBarAnimator = null
            clockProgressBar?.animation = null
            clockProgressBar?.visibility = View.GONE
            Log.d("MyTag", "${clockProgressBar?.visibility}")
            clockProgressBar = null
        } else {
            progressBarAnimator?.doOnEnd {
                clockProgressBar?.progress = 0
                clockProgressBar = null
                progressBarAnimator?.cancel()
                progressBarAnimator = null
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
    override fun stop() {
        if (!isServiceEnabled) return
        pause()
        isServiceEnabled = false
        context.unregisterReceiver(timeReceiver)
        startTime = 0
        cancel(animate = false)
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
        stopPulseAnimation()
        setProgressBarInvisible()
        callback.timerFinished()
        isGoing = false
    }

    private fun setProgressBarInvisible() {
        val disappearAnim = AnimationUtils.loadAnimation(context, R.anim.progress_bar_disappear)
        Anim.doOnEndOf(disappearAnim) {
            clockProgressBar?.visibility = View.INVISIBLE
        }
        clockProgressBar?.startAnimation(disappearAnim)
    }

    /**
     * Восстановление или переопределение прогресса индикатора заполненности (при наличии)
     */
    private fun restoreProgressBar(maxValue: Int = startTime, currentValue: Int = time) {
        if (clockProgressBar == null || currentValue == 0) return

        if (maxValue == 0) throw Error("maxValue is 0")

        val coefficient = 100
        val currentProgress = currentValue * coefficient

        Log.d("MyTag",
            "currentProgress = $currentProgress; maxProgress = ${maxValue * coefficient}")

        clockProgressBar!!.max = (maxValue * coefficient)

        progressBarAnimator = ObjectAnimator.ofInt(
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
    override fun start() {
        if (time == 0) return
        else isFinished = false

        if (startTime == 0)
            throw Exception(
                "startTime is 0. \nPossible problem: " +
                        "Time was not selected automatically"
            )
        if (!isServiceEnabled) {
            context.registerReceiver(
                timeReceiver,
                IntentFilter(TIMER_UPDATED)
            )
            isServiceEnabled = true
        }
        if (time == startTime || !isPaused) {
            val appearAnim = AnimationUtils
                .loadAnimation(context, R.anim.progress_bar_appear)
            Anim.doOnEndOf(appearAnim) {
                clockProgressBar?.visibility = View.VISIBLE
            }
            clockProgressBar?.startAnimation(appearAnim)
        } else clockProgressBar?.visibility = View.VISIBLE
        serviceIntent.putExtra(CountDownService.TIME_EXTRA, time)
        context.startService(serviceIntent)
        isGoing = true
        restoreProgressBar()
        if (attemptsToRunPulseAnimation != 0)
            startPulseAnimation(attemptsToRunPulseAnimation)
        notifyingSignalCount = getSignalCountOfTime()
        isPaused = false
    }

    /**
     * Остановка таймера путем вызова stopService(serviceIntent),
     * после которого будет прекращен
     */
    override fun pause() {
        progressBarAnimator?.pause()
        stopPulseAnimation()
        context.stopService(serviceIntent)
        isGoing = false
        isPaused = true
    }

    /**
     * Обновление TextView, содержащего время секундомера;
     */
    private fun updateText() {
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
                isServiceEnabled = true
            }

            // При уничтожении, удаляем связь
            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                if (isServiceEnabled) {
                    isServiceEnabled = false
                    context.unregisterReceiver(timeReceiver)
                }
            }
        }
        // Добавляем наблюдателя
        (context as LifecycleOwner).lifecycle.addObserver(defaultLifecycleObserver)
    }

    /**
     * Определяем обработчик текущего времени;
     */
    private fun handleReceivedTime() {
        updateText()
        if (time == WorkoutParams.notifyingSignalAt[notifyingSignalCount]) {
            startPulseAnimation(++attemptsToRunPulseAnimation)
            notifying!!.play()
            ++notifyingSignalCount
        } else if (time == 0) {
            notifying!!.playLast()
            finish()
        }
    }

    private fun startPulseAnimation(attemptNo: Int) {
        if (clockProgressBar == null || progressPulseEffectSet.isRunning) {
            if (attemptNo == 2) pulseAnimRunnable.interval = 1000
            return
        }
        progressPulseEffectSet.also {
            it.playTogether(
                ObjectAnimator.ofPropertyValuesHolder(
                    clockProgressBar!!,
                    PropertyValuesHolder.ofFloat("scaleX", 1.02f),
                    PropertyValuesHolder.ofFloat("scaleY", 1.02f)
                ).also {
                    it.repeatCount = ObjectAnimator.INFINITE
                    it.repeatMode = ObjectAnimator.REVERSE
                },
                ObjectAnimator.ofPropertyValuesHolder(
                    clockTextView!!,
                    PropertyValuesHolder.ofFloat("scaleX", 1.01f),
                    PropertyValuesHolder.ofFloat("scaleY", 1.01f)
                ).also {
                    it.repeatCount = ObjectAnimator.INFINITE
                    it.repeatMode = ObjectAnimator.REVERSE
                },
            )
            it.duration = 250
        }
        pulseAnimRunnable.run()
        progressPulseEffectSet.start()
    }

    private fun stopPulseAnimation() {
        pulseAnimRunnable.stop()
        progressPulseEffectSet.cancel()
        attemptsToRunPulseAnimation = 0
        clockTextView?.scaleX = 1.0f
        clockTextView?.scaleY = 1.0f
        clockProgressBar?.scaleX = 1.0f
        clockProgressBar?.scaleY = 1.0f
    }

    private fun pausePulseAnimation() {
        pulseAnimRunnable.pause()
        progressPulseEffectSet.cancel()
    }

    private inner class PulseAnimRunnable : Runnable {
        private var handler: Handler = Handler(Looper.myLooper()!!)
        var animatedCircle: View? = null
        var interval: Long = 2000

        private val pulseAnim = AnimationUtils
            .loadAnimation(context, R.anim.fullscreen_pulse_effect)

        override fun run() {
            animatedCircle?.let { circle ->
                if (!circle.isVisible)
                    circle.isVisible = true
                circle.startAnimation(pulseAnim)
            }
            handler.postDelayed(this, interval)
        }

        fun stop() {
            animatedCircle?.isGone = true
            handler.removeCallbacks(this)
            interval = 2000
        }

        fun pause() {
            handler.removeCallbacks(this)
        }
    }

    private inner class Notifying(context: Context, notifyingType: Int) {

        private val shortVibrationMs = 800L
        private val longVibrationMs = 1200L

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
                                .createOneShot(shortVibrationMs, VibrationEffect.EFFECT_TICK)
                        )
                    } else {
                        @Suppress("Deprecation")
                        (notifying as Vibrator).vibrate(shortVibrationMs)
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
                                .createOneShot(longVibrationMs, VibrationEffect.EFFECT_TICK)
                        )
                    } else {
                        @Suppress("Deprecation")
                        (notifying as Vibrator).vibrate(longVibrationMs)
                    }
                }
                else -> (lastNotifying as MediaPlayer).start()
            }
        }
    }
}


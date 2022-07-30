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
class CountDownTimer(
    private val context: Context,
    private val callback: Callback
) : TimeCounting {
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

    var pulseAnimationIsRunning = false


    // Первостепенные данные

    private var time: Int = 60     // Оставшееся время;

    override var isGoing: Boolean = false   // Идет ли счет времени;

    override var isPaused = false
    var isEnabled = false

    var startTime: Int = 0         // Изначальное время;
        set(value) {
            field = value; Log.d("MyTag", "Start time set to $field")
        }

    var isFinished = false         // Закончился ли таймер;
        private set

    private var pulseAnimatorSetInitialized = false

    var lifecycleOwnerIsAlive = true

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
     * Установка времени на заданное;
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
        notifying = Notifying(context, type)
    }

    /**
     * Добавление элементов экрана к обработке времени
     */
    override fun enable(vararg views: View) {
        clockTextView = views[0] as TextView
        clockProgressBar = views[1] as ProgressBar
        pulseAnimRunnable.animatedCircle = views[2]
    }

    /**
     * Удаления представлений на экране, которые
     * использовались для отображения соответствующих данных;
     */
    override fun reset() {
        clockTextView = null
        progressBarAnimator!!.cancel()
        progressBarAnimator = null
        clockProgressBar!!.animation = null
        clockProgressBar!!.visibility = View.INVISIBLE
        Log.d("MyTag", "${clockProgressBar?.visibility}")
        clockProgressBar = null
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
        isPaused = false
        isServiceEnabled = false
        context.unregisterReceiver(timeReceiver)
        startTime = 0
        reset()
    }


    /**
     * Интерфейс, метод которого будет вызван при завершении таймера;
     */
    interface Callback {
        fun timerFinished()
    }

    /**
     * Восстановление или переопределение прогресса индикатора заполненности (при наличии)
     */
    fun initProgressAnimation(maxValue: Int = startTime, currentValue: Int = time) {

        val coefficient = 100
        val currentProgress = currentValue * coefficient

        Log.d(
            "MyTag",
            "currentProgress = $currentProgress; maxProgress = ${maxValue * coefficient}"
        )

        clockProgressBar!!.max = (maxValue * coefficient)

        progressBarAnimator = ObjectAnimator.ofInt(
            clockProgressBar!!, "progress", currentProgress, 0
        ).apply {
            interpolator = null
            duration = (currentProgress * 1000f / coefficient).toLong()
            clockProgressBar!!.progress = currentProgress
        }
    }

    fun startProgressAnimation() {
        if (progressBarAnimator == null) {
            initProgressAnimation()
            progressBarAnimator!!.start()
        } else {
            if (progressBarAnimator!!.isPaused)
                progressBarAnimator!!.resume()
            else
                progressBarAnimator!!.start()
        }

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
        isGoing = false
        startTime = 0
        notifyingSignalCount = 0
        stopPulseAnimation()
        progressBarAnimator!!.doOnEnd {
            hideProgressBar(wDisappearAnim = lifecycleOwnerIsAlive) {
                stop()
                callback.timerFinished()
            }
        }

    }

    private fun hideProgressBar(wDisappearAnim: Boolean, actionAfter: () -> Unit) {
        if (wDisappearAnim) {
            val disappearAnim = AnimationUtils.loadAnimation(context, R.anim.progress_bar_disappear)
            Anim.doOnEndOf(disappearAnim) {
                clockProgressBar!!.visibility = View.INVISIBLE
                actionAfter.invoke()
            }
            clockProgressBar!!.startAnimation(disappearAnim)
        } else {
            clockProgressBar!!.visibility = View.INVISIBLE
            actionAfter.invoke()
        }
    }

    /**
     * Запуск секундомера посредством запуска сервиса
     * с переданным интентом в виде значения текущего времени
     */
    override fun start() {
        isFinished = false
        if (!isServiceEnabled) {
            context.registerReceiver(
                timeReceiver,
                IntentFilter(TIMER_UPDATED)
            )
            isServiceEnabled = true
        }
        if (!isGoing) {
            if (time == startTime) {
                showProgressBar(
                    wAppearAnim = lifecycleOwnerIsAlive,
                    actionAfter = {
                        startProgressAnimation()
                        serviceIntent.putExtra(CountDownService.TIME_EXTRA, time)
                        context.startService(serviceIntent)
                    }
                )
            } else {
                showProgressBar(
                    wAppearAnim = false,
                    actionAfter = {
                        startProgressAnimation()
                        serviceIntent.putExtra(CountDownService.TIME_EXTRA, time)
                        context.startService(serviceIntent)
                    }
                )
            }
            isGoing = true
        } else {
            serviceIntent.putExtra(CountDownService.TIME_EXTRA, time)
            context.startService(serviceIntent)
        }
        isPaused = false
        isEnabled = true
        isGoing = true
        if (attemptsToRunPulseAnimation != 0)
            startPulseAnimation(attemptsToRunPulseAnimation)
        notifyingSignalCount = getSignalCountOfTime()
    }

    /**
     * Остановка таймера путем вызова stopService(serviceIntent),
     * после которого будет прекращен
     */
    override fun pause() {
        progressBarAnimator?.pause()
        pausePulseAnimation()
        context.stopService(serviceIntent)
        isGoing = false
        isPaused = true
    }

    fun showProgressBar(wAppearAnim: Boolean, actionAfter: () -> Unit) {
        if (wAppearAnim) {
            val appearAnim = AnimationUtils
                .loadAnimation(context, R.anim.progress_bar_appear)
            Anim.doOnEndOf(appearAnim) {
                clockProgressBar!!.visibility = View.VISIBLE
                actionAfter.invoke()
            }
            clockProgressBar!!.startAnimation(appearAnim)
        } else {
            clockProgressBar!!.visibility = View.VISIBLE
            actionAfter.invoke()
        }
    }

    /**
     * Обновление TextView, содержащего время секундомера;
     */
    fun updateText() {
        clockTextView!!.text = getTimeInFormatMMSS(time)
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
            if (!pulseAnimationIsRunning)
                startPulseAnimation(++attemptsToRunPulseAnimation)
            notifying!!.play()
            ++notifyingSignalCount
        } else if (time == 0) {
            notifying!!.playLast()
            finish()
        }
    }

    fun startPulseAnimation(attemptNo: Int = attemptsToRunPulseAnimation) {
        if (clockProgressBar == null) {
            if (attemptNo == 2) pulseAnimRunnable.interval = 1000
            return
        }
        if (!pulseAnimatorSetInitialized)
            initPulseAnimationSet()
        pulseAnimRunnable.run()
        if (progressPulseEffectSet.isPaused)
            progressPulseEffectSet.resume()
        else
            progressPulseEffectSet.start()
        pulseAnimationIsRunning = true
    }

    fun initPulseAnimationSet() {
        progressPulseEffectSet.also { set ->
            set.playTogether(
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
            set.duration = 250
        }
        pulseAnimatorSetInitialized = true
    }

    private fun stopPulseAnimation() {
        pulseAnimRunnable.stop()
        progressPulseEffectSet.cancel()
        pulseAnimationIsRunning = false
        attemptsToRunPulseAnimation = 0
        clockTextView?.scaleX = 1.0f
        clockTextView?.scaleY = 1.0f
        clockProgressBar?.scaleX = 1.0f
        clockProgressBar?.scaleY = 1.0f
    }

    private fun pausePulseAnimation() {
        pulseAnimationIsRunning = false
        pulseAnimRunnable.pause()
        progressPulseEffectSet.pause()
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


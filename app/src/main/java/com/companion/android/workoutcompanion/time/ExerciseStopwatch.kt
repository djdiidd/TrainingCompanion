package com.companion.android.workoutcompanion.time

import android.animation.ObjectAnimator
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.TextView
import com.companion.android.workoutcompanion.R
import com.companion.android.workoutcompanion.interfaces.TimeCounting
import com.companion.android.workoutcompanion.objects.Anim
import com.companion.android.workoutcompanion.time.ActionManager.Companion.getTimeInFormatMMSS


class ExerciseStopwatch(
    private var textView: TextView? = null,
    private var progressView: View? = null
) : TimeCounting, Stopwatch.TimeCallback {


    private val exerciseTime: ArrayList<Int> = arrayListOf()

    private var circleAnimator: ObjectAnimator? = null

    var millisToNextSecond = 0L

    override var isGoing = false
    override var isPaused = false
    var isEnabled = false

    var time: Int = 0

    var lifecycleOwnerIsAlive = true

    override fun start() {
        startFrom(0)
    }

    fun startFrom(start: Int) {
        time = start
        if (!isGoing) {
            isGoing = true
            if (time == 0) {
                showProgressBar(
                    wAppearAnim = lifecycleOwnerIsAlive,
                    actionAfter = { startProgressAnimation() }
                )
            } else showProgressBar(
                wAppearAnim = false,
                actionAfter = { startProgressAnimation() }
            )
        }
        isPaused = false
        isEnabled = true
    }

    override fun pause() {
        circleAnimator?.pause()
        isGoing = false
        isPaused = true
        millisToNextSecond = circleAnimator?.currentPlayTime?.rem(1000L) ?: 0
    }

    override fun stop() {
        pause()
        isPaused = false
        reset()
        millisToNextSecond = 0
        isEnabled = false
    }

    fun restoreProgressAnimation() {
        if (circleAnimator == null) {
            initCircleRotationAnimator()
        } else {
            circleAnimator!!.target = progressView
            circleAnimator!!.currentPlayTime = time * 1000L + millisToNextSecond
        }
    }

    override fun enable(vararg views: View) {
        textView = views[0] as TextView
        progressView = views[1]
    }

    fun updateTextByTime(upTime: Int = time) {
        textView?.text = getTimeInFormatMMSS(upTime)
    }

    fun startProgressAnimation() {
        if (circleAnimator == null) {
            // Аниматор может быть равен нулю,
            // так как при переключении на секундомер
            // приложение или MainFragment могут быть вне видимости
            initCircleRotationAnimator()
            circleAnimator!!.start()
        } else {
            if (circleAnimator!!.isPaused)
                circleAnimator!!.resume()
            else
                circleAnimator!!.start()
        }
    }

    fun showProgressBar(wAppearAnim: Boolean, actionAfter: () -> Unit) {
        if (wAppearAnim) {
            val appearAnim = AnimationUtils
                .loadAnimation(textView!!.context, R.anim.progress_bar_appear)
            Anim.doOnEndOf(appearAnim) {
                progressView!!.visibility = View.VISIBLE
                actionAfter.invoke()
            }
            progressView!!.startAnimation(appearAnim)
        } else {
            // Если запускается без анимации,
            // значит есть вероятность вызова при null Views
            progressView?.visibility = View.VISIBLE
            actionAfter.invoke()
        }
    }

    fun hideProgressBar(wDisappearAnim: Boolean, actionAfter: () -> Unit) {
        if (wDisappearAnim) {
            val disappearAnim = AnimationUtils
                .loadAnimation(textView!!.context, R.anim.progress_bar_disappear)
            Anim.doOnEndOf(disappearAnim) {
                progressView!!.visibility = View.INVISIBLE
                actionAfter.invoke()
            }
            progressView!!.startAnimation(disappearAnim)
        } else {
            progressView!!.visibility = View.INVISIBLE
            actionAfter.invoke()
        }
    }

    override fun reset() {
        circleAnimator?.cancel()
        circleAnimator = null
        progressView?.animation = null
        progressView?.visibility = View.INVISIBLE
        progressView?.rotation = 0f
        progressView = null
        textView = null
    }

    private fun initCircleRotationAnimator() {
        circleAnimator = ObjectAnimator.ofFloat(
            progressView,
            "rotation",
            0F, 360F
        ).also {
            it.duration = 10000
            it.interpolator = AccelerateDecelerateInterpolator()
            it.repeatMode = ObjectAnimator.RESTART
            it.repeatCount = ObjectAnimator.INFINITE
            it.startDelay = 200
        }
    }

    fun getExerciseTimeOf(index: Int): Int {
        if (index in exerciseTime.indices) {
            return exerciseTime[index]
        } else {
            throw Exception(
                "Trying to get exercise time of " +
                        "$index exercise which is not executed"
            )
        }
    }

    fun getExerciseTimeOfLast(): Int {
        if (exerciseTime.isEmpty())
            throw NullPointerException(
                "Trying to get exercise time of last" +
                        "exercise when exerciseTime is empty"
            )
        return exerciseTime.last()
    }

    fun setExerciseTimeOf(index: Int, value: Int) {
        if (index in exerciseTime.indices) {
            exerciseTime[index] = value
        } else {
            throw Exception(
                "Trying to get exercise time of " +
                        "$index exercise which is not executed"
            )
        }
    }

    fun setExerciseTimeOfLast(value: Int) {
        if (exerciseTime.isEmpty())
            throw NullPointerException(
                "Trying to get exercise time of last" +
                        "exercise when exerciseTime is empty"
            )
        exerciseTime[exerciseTime.lastIndex] = value
    }

    fun addExerciseWithTime(value: Int) {
        exerciseTime.add(value)
    }


    override fun tick() {
        Log.d("Tic", "esw - $time")
        updateTextByTime(time++)
    }

}
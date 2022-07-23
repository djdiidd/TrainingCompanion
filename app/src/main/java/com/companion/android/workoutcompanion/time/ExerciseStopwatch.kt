package com.companion.android.workoutcompanion.time

import android.animation.ObjectAnimator
import android.os.Handler
import android.os.Looper
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
    private var circleBg: View? = null
) : TimeCounting {

    private val exerciseTime: ArrayList<Int> = arrayListOf()

    private val timeRunnable = TimeUpdating()

    private var circleAnimator: ObjectAnimator? = null

    var millisToNextSecond = 0L

    override var isGoing = false
    override var isPaused = false

    var time: Int = 0

    override fun start() {
        startFrom(0)
    }

    fun startFrom(start: Int) {
        val restoreProgress = {
            circleBg?.visibility = View.VISIBLE
            if (circleAnimator?.isPaused == true) {
                circleAnimator?.resume()
            } else circleAnimator?.start()
        }
        time = start

        if (!isGoing) {
            if (!timeRunnable.isRunning) {
                if (time != 0) updateTextByTime(time - 1)
                Handler(Looper.getMainLooper())
                    .postDelayed({ timeRunnable.run() }, millisToNextSecond)
                timeRunnable.isRunning = true
            }
            isGoing = true

            if (time == 0) {
                val appearAnim = AnimationUtils
                    .loadAnimation(textView?.context, R.anim.progress_bar_appear)
                Anim.doOnEndOf(appearAnim) { restoreProgress() }
                circleBg?.startAnimation(appearAnim)
            } else restoreProgress()
        } else {
            val appearAnim = AnimationUtils
                .loadAnimation(textView?.context, R.anim.progress_bar_appear)
            Anim.doOnEndOf(appearAnim) { restoreProgress() }
            circleBg?.startAnimation(appearAnim)
        }
        isPaused = false
    }

    override fun pause() {
        timeRunnable.stop()
        circleAnimator?.pause()
        isGoing = false
        isPaused = true
        millisToNextSecond = circleAnimator?.currentPlayTime?.rem(1000L) ?: 0
    }

    override fun stop() {
        pause()
        cancel(false)
        millisToNextSecond = 0
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

    override fun enable(vararg views: View) {
        textView = views[0] as TextView
        circleBg = views[1]
        if (circleAnimator == null) {
            initCircleRotationAnimator()
        } else {
            circleAnimator!!.target = circleBg
            circleAnimator!!.currentPlayTime = time * 1000L + millisToNextSecond
        }
        if (isGoing) {
            if (!timeRunnable.isRunning) {
                timeRunnable.run()
                timeRunnable.isRunning = true
            }
            circleAnimator!!.currentPlayTime = time * 1000L
            circleAnimator!!.start()
        }
    }

    fun updateTextByTime(upTime: Int = time) {
        textView?.text = getTimeInFormatMMSS(upTime)
    }

    override fun cancel(animate: Boolean) {
        if (!animate) {
            circleAnimator?.cancel()
            circleAnimator = null
            circleBg?.animation = null
            circleBg?.visibility = View.INVISIBLE
            circleBg?.rotation = 0f
            circleBg = null
            textView = null
        } else {
            val disappearAnim = AnimationUtils
                .loadAnimation(textView?.context, R.anim.progress_bar_disappear)
            Anim.doOnEndOf(disappearAnim) {
                circleBg!!.visibility = View.INVISIBLE
                circleBg?.rotation = 0f
                circleBg = null
                textView = null
            }
            circleAnimator?.cancel()
            circleAnimator = null
            circleBg?.startAnimation(disappearAnim)
        }
        isPaused = false
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

    private fun initCircleRotationAnimator() {
        circleAnimator = ObjectAnimator.ofFloat(
            circleBg!!,
            "rotation",
            0F, 360F
        )
            .also {
                it.duration = 10000
                it.interpolator = AccelerateDecelerateInterpolator()
                it.repeatMode = ObjectAnimator.RESTART
                it.repeatCount = ObjectAnimator.INFINITE
                it.startDelay = 200
            }
    }

    private inner class TimeUpdating : Runnable {
        private var handler: Handler = Handler(Looper.getMainLooper())
        private val interval = 1000L
        var isRunning = false

        override fun run() {
            Log.d("Tic", "esw - $time")
            textView?.text = getTimeInFormatMMSS(time++)
            handler.postDelayed(this, interval)
        }

        fun stop() {
            isRunning = false
            handler.removeCallbacks(this)
        }

    }

}
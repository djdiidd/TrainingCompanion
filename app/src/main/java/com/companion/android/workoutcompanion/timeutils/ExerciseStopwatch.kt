package com.companion.android.workoutcompanion.timeutils

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import com.companion.android.workoutcompanion.timeutils.ActionManager.Companion.getTimeInFormatMMSS

class ExerciseStopwatch(
    private var observedView: TextView,
    private var textView: TextView? = null,
    private var progressBar: ProgressBar? = null
) {

    private val exerciseTime: ArrayList<Int> = arrayListOf()

    private val updateTimeWatcher = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        override fun afterTextChanged(p0: Editable?) {}
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            Log.d("Tic", "esw - $time")
            textView?.text = getTimeInFormatMMSS(time++)
        }
    }

    var isGoing = false

    var time: Int = 0

    fun start() {
        startFrom(0)
    }

    fun startFrom(start: Int) {
        time = start
        if (!isGoing)
            observedView.addTextChangedListener(updateTimeWatcher)
        isGoing = true
    }

    fun `continue`() {
        startFrom(time)
    }

    fun stop() {
        observedView.removeTextChangedListener(updateTimeWatcher)
        isGoing = false
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

    fun attachUI(listening_tv: TextView, tv: TextView?, pb: ProgressBar?) {
        observedView = listening_tv; textView = tv; progressBar = pb
    }

    fun updateTextByTime(uTime: Int = time) {
        textView?.text = getTimeInFormatMMSS(uTime)
    }

    fun detachUI() {
        progressBar = null
        textView = null
        stop()
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

}
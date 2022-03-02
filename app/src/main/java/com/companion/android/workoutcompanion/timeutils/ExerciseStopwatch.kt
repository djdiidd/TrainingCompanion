package com.companion.android.workoutcompanion.timeutils

import android.text.Editable
import android.text.TextWatcher
import android.widget.ProgressBar
import android.widget.TextView

class ExerciseStopwatch(
    private var listeningView: TextView,
    private var textView: TextView? = null,
    private var progressBar: ProgressBar? = null
) {

    private val exerciseTime: ArrayList<Int> = arrayListOf()

    private val updateTimeWatcher = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            textView?.text = getTimeInFormatMMSS(time)
            time++
        }
        override fun afterTextChanged(p0: Editable?) {}

    }

    var time: Int = 0

    fun start() { startFrom(0) }

    fun startFrom(start: Int) {
        if (start < 0) throw Exception("start time cannot be negative")
        time = start
        listeningView.addTextChangedListener(updateTimeWatcher)
    }

    fun `continue`() {
        startFrom(time)
    }

    fun stop() {
        listeningView.removeTextChangedListener(updateTimeWatcher)
    }

    /**
     * Получение времени в формате "ММ:СС"
     */
    private fun getTimeInFormatMMSS(time: Int): String {
        time.also {
            return String.format(
                "%02d:%02d",
                it % 86400 % 3600 / 60,
                it % 86400 % 3600 % 60
            )
        }
    }

    fun getExerciseTimeOf(index: Int) : Int {
        if (index in exerciseTime.indices) {
            return exerciseTime[index]
        } else {
            throw Exception("Trying to get exercise time of " +
                    "$index exercise which is not executed")
        }
    }

    fun getExerciseTimeOfLast() : Int {
        if (exerciseTime.isEmpty())
            throw NullPointerException("Trying to get exercise time of last" +
                    "exercise when exerciseTime is empty")
        return exerciseTime.last()
    }

    fun setExerciseTimeOf(index: Int, value: Int) {
        if (index in exerciseTime.indices) {
            exerciseTime[index] = value
        } else {
            throw Exception("Trying to get exercise time of " +
                    "$index exercise which is not executed")
        }
    }

    fun attachUI(listening_tv: TextView, tv: TextView?, pb: ProgressBar?) {
        listeningView= listening_tv; textView = tv; progressBar = pb
        textView?.text = getTimeInFormatMMSS(time)
    }

    fun setExerciseTimeOfLast(value: Int) {
        if (exerciseTime.isEmpty())
            throw NullPointerException("Trying to get exercise time of last" +
                    "exercise when exerciseTime is empty")
        exerciseTime[exerciseTime.lastIndex] = value
    }

    fun addExerciseWithTime(value: Int) {
        exerciseTime.add(value)
    }

}
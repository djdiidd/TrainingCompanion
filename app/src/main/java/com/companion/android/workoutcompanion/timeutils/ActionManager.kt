package com.companion.android.workoutcompanion.timeutils

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.os.bundleOf
import com.companion.android.workoutcompanion.R
import com.companion.android.workoutcompanion.objects.BreakNotifyingMode
import com.companion.android.workoutcompanion.objects.WorkoutProcess


private const val GENERAL_CLOCK_ID = R.id.general_clock
private const val SET_TIMER_ID = R.id.set_timer
private const val SET_TIMER_PROGRESS_ID = R.id.set_timer_progress

private const val CURRENT_PROCESS = "current-progress"
private const val CURRENT_ACTIONS = "current-actions"

private const val GENERAL_STOPWATCH_IS_GOING = "g-stopwatch-is-going"
private const val GENERAL_STOPWATCH_TIME = "g-stopwatch-time"

private const val BREAK_TIMER_TIME = "b-timer-time"
private const val BREAK_TIMER_IS_GOING = "b-timer-is-going"
private const val BREAK_TIMER_START_TIME = "b-timer-start-time"

private const val EXERCISE_STOPWATCH_TIME = "e-stopwatch-time"
private const val EXERCISE_STOPWATCH_IS_GOING = "e-stopwatch-is-going"

private const val NOTIFYING_TYPE = "notifying-type"


/**
 * Данный класс отвечает за переход к действиям (запустить общий секундомер,
 * остановить его, запустить основной таймер, поставить на паузу,
 * добавить временный таймер, обработать внешние действия, и так далее)
 */
class ActionManager(private val activity: Activity) : CountDownTimer.Callback {

    private var currentProcess = WorkoutProcess.NOT_STARTED
    private var currentActions = mutableListOf<Action>()

    private val breakTimer = CountDownTimer(activity, this)
    private val generalStopwatch = Stopwatch(activity)
    private val exerciseStopwatch = ExerciseStopwatch(
        activity.findViewById(GENERAL_CLOCK_ID)
    )

    private val callback = activity as ActionCallback

    var notifyingType: Int = BreakNotifyingMode.ANIMATION
        set(value) {
            breakTimer.setNotifyingType(value)
            field = value
        }

    fun perform(action: Action, initTime: Int? = null, notifyingType: Int = this.notifyingType) {
        when (action) {
            Action.GENERAL_STOPWATCH -> performGeneralStopwatch(initTime ?: 0)

            Action.BREAK_TIMER -> performBreakTimer(initTime ?: breakTimer.getTime())

            Action.EXERCISE_STOPWATCH -> performExerciseStopwatch(initTime ?: 0)

            Action.TEMP_STOPWATCH -> {}

            Action.TEMP_TIMER -> {}
        }
        this.notifyingType = notifyingType
    }

    fun performOrPause(action: Action, initTime: Int? = null) {
        when (action) {
            Action.GENERAL_STOPWATCH -> {
                if (!generalStopwatch.isGoing)
                    performGeneralStopwatch(initTime ?: generalStopwatch.time)
                else pause(Action.GENERAL_STOPWATCH)
            }

            Action.BREAK_TIMER -> {
                Log.d("MyTag", "breakTimer.getTime() = ${breakTimer.getTime()}")
                if (!breakTimer.isGoing)
                    performBreakTimer(initTime ?: breakTimer.getTime())
                else pause(Action.BREAK_TIMER)
            }

            Action.EXERCISE_STOPWATCH -> {
                if (!exerciseStopwatch.isGoing)
                    performExerciseStopwatch(initTime ?: exerciseStopwatch.time)
                else pause(Action.EXERCISE_STOPWATCH)
            }

            Action.TEMP_STOPWATCH -> {}

            Action.TEMP_TIMER -> {}
        }
    }

    private fun performBreakTimer(time: Int, notifyingType: Int = this.notifyingType) {
        if (breakTimer.isFinished) breakTimer.setStartState(time)
        else breakTimer.setTime(time)

        breakTimer.setNotifyingType(notifyingType)

        breakTimer.attachUI(
            activity.findViewById(SET_TIMER_ID),
            activity.findViewById(SET_TIMER_PROGRESS_ID)
        )
        if (breakTimer.isGoing) return

        breakTimer.startOrStop()

        if (currentActions.contains(Action.BREAK_TIMER)) return

        currentProcess = WorkoutProcess.TIMER
        currentActions.remove(Action.EXERCISE_STOPWATCH)
            .also { if (it) exerciseStopwatch.detachUI() }
        currentActions.add(Action.BREAK_TIMER)
    }

    private fun performExerciseStopwatch(fromTime: Int = exerciseStopwatch.time) {
        exerciseStopwatch.attachUI(
            activity.findViewById(GENERAL_CLOCK_ID),
            activity.findViewById(SET_TIMER_ID),
            activity.findViewById(SET_TIMER_PROGRESS_ID)
        )

        if (exerciseStopwatch.isGoing) {
            exerciseStopwatch.updateTextByTime()
            return
        }
        exerciseStopwatch.updateTextByTime(0)
        exerciseStopwatch.startFrom(fromTime)
        if (currentActions.contains(Action.EXERCISE_STOPWATCH)) return

        currentProcess = WorkoutProcess.EXERCISE_STOPWATCH
        currentActions.remove(Action.BREAK_TIMER).also { removed ->
            if (removed) breakTimer.detachUI(true) //todo: also stop timer
        }
        currentActions.add(Action.EXERCISE_STOPWATCH)
    }

    private fun performGeneralStopwatch(fromTime: Int) {
        if (generalStopwatch.isGoing) return
        generalStopwatch.startOrStop()
        if (currentActions.contains(Action.GENERAL_STOPWATCH)) return
        currentActions.add(Action.GENERAL_STOPWATCH)
        generalStopwatch.time = fromTime
    }

    fun pause(action: Action) {
        when (action) {
            Action.BREAK_TIMER ->
                if (breakTimer.isGoing) breakTimer.startOrStop()

            Action.GENERAL_STOPWATCH ->
                if (generalStopwatch.isGoing) generalStopwatch.startOrStop()

            Action.EXERCISE_STOPWATCH ->
                if (exerciseStopwatch.isGoing) exerciseStopwatch.stop()

            Action.TEMP_STOPWATCH -> {}
            Action.TEMP_TIMER -> {}
        }
    }

    fun updateTimeOf(action: Action, toSeconds: Int) {
        when (action) {
            Action.GENERAL_STOPWATCH ->
                generalStopwatch.time = toSeconds

            Action.EXERCISE_STOPWATCH ->
                if (currentProcess == WorkoutProcess.EXERCISE_STOPWATCH)
                    exerciseStopwatch.time = toSeconds

            Action.BREAK_TIMER ->
                if (currentProcess == WorkoutProcess.TIMER)
                    breakTimer.setTime(toSeconds)

            Action.TEMP_STOPWATCH -> {}
            Action.TEMP_TIMER -> {}
        }
    }

    fun updateUI(timerTextView: TextView, circleView: ProgressBar) {
        when (currentProcess) {
            WorkoutProcess.EXERCISE_STOPWATCH -> {
                exerciseStopwatch.attachUI(
                    activity.findViewById(GENERAL_CLOCK_ID), timerTextView, circleView
                )
                exerciseStopwatch.updateTextByTime()
            }
            WorkoutProcess.TIMER -> breakTimer.attachUI(
                timerTextView, circleView
            )
            // TODO: add new actions
        }
    }


    fun clear() {
        breakTimer.stopAndUnregister()
        exerciseStopwatch.detachUI()
        generalStopwatch.stopAndUnregister()
    }

    fun getInstanceState(): Bundle? {
        if (currentProcess == WorkoutProcess.NOT_STARTED) return null
        return bundleOf(
            Pair(CURRENT_PROCESS, currentProcess),
            Pair(CURRENT_ACTIONS, getPositionsOfActions(currentActions)),
            Pair(GENERAL_STOPWATCH_IS_GOING, generalStopwatch.isGoing),
            Pair(GENERAL_STOPWATCH_TIME, generalStopwatch.time),
            Pair(NOTIFYING_TYPE, notifyingType),
            Pair(BREAK_TIMER_IS_GOING, breakTimer.isGoing),
            Pair(BREAK_TIMER_TIME, breakTimer.getTime()),
            Pair(BREAK_TIMER_START_TIME, breakTimer.startTime),
            Pair(EXERCISE_STOPWATCH_IS_GOING, exerciseStopwatch.isGoing),
            Pair(EXERCISE_STOPWATCH_TIME, exerciseStopwatch.time)

            //TODO: ADD ACTIONS (TEMP SW AND TIMER)
        )
    }

    fun restoreInstanceState(savedState: Bundle?) {
        if (savedState == null) return
        savedState.apply {
            currentProcess = getInt(CURRENT_PROCESS)
            currentActions = getActionsFromPositions(getIntArray(CURRENT_ACTIONS)!!.toTypedArray())
            generalStopwatch.isGoing = getBoolean(GENERAL_STOPWATCH_IS_GOING)
            generalStopwatch.time = getInt(GENERAL_STOPWATCH_TIME)
            notifyingType = getInt(NOTIFYING_TYPE)
            breakTimer.isGoing = getBoolean(BREAK_TIMER_IS_GOING)
            breakTimer.setTime(getInt(BREAK_TIMER_TIME))
            breakTimer.startTime = getInt(BREAK_TIMER_START_TIME)
            exerciseStopwatch.time = getInt(EXERCISE_STOPWATCH_TIME)
            if (getBoolean(EXERCISE_STOPWATCH_IS_GOING)) exerciseStopwatch.`continue`()

            //TODO: ADD ACTIONS (TEMP SW AND TIMER)
        }
    }

    private fun getActionsFromPositions(actionPos: Array<Int>): MutableList<Action> {
        val posToAction = mapOf(
            0 to Action.BREAK_TIMER,
            1 to Action.GENERAL_STOPWATCH,
            2 to Action.EXERCISE_STOPWATCH,
            3 to Action.TEMP_STOPWATCH,
            4 to Action.TEMP_TIMER,
        )
        val actions = mutableListOf<Action>()
        actionPos.forEach { pos ->
            actions.add(posToAction[pos]!!)
        }
        return actions
    }

    private fun getPositionsOfActions(actions: MutableList<Action>): IntArray {
        val actionToPos = mapOf(
            Action.BREAK_TIMER to 0,
            Action.GENERAL_STOPWATCH to 1,
            Action.EXERCISE_STOPWATCH to 2,
            Action.TEMP_STOPWATCH to 3,
            Action.TEMP_TIMER to 4,
        )
        if (actions.size > actionToPos.size) throw Exception("Error")

        val actionPos = IntArray(actions.size) { 0 }
        for (i in actions.indices) {
            actionPos[i] = actionToPos[actions[i]]!!
        }
        return actionPos
    }

    override fun timerFinished() {
        callback.onTimerFinished()
    }

    interface ActionCallback {
        fun onTimerFinished()
    }

    companion object {
        enum class Action {
            BREAK_TIMER,
            GENERAL_STOPWATCH,
            EXERCISE_STOPWATCH,
            TEMP_STOPWATCH,
            TEMP_TIMER
        }

        /**
         * Получение времени в формате "ММ:СС"
         */
        fun getTimeInFormatMMSS(time: Int): String {
            time.also {
                return String.format(
                    "%02d:%02d",
                    it / 60,
                    it % 60
                )
            }
        }

        /**
         * Получение времени в формате "Ч:ММ:СС"
         */
        fun getTimeInFormatHMMSS(time: Int): String {
            time.also {
                return String.format(
                    "%d:%02d:%02d",
                    it / 3600,
                    it % 3600 / 60,
                    it % 3600 % 60
                )
            }
        }
    }
}

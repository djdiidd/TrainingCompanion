package com.companion.android.workoutcompanion.time

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.os.bundleOf
import com.companion.android.workoutcompanion.R
import com.companion.android.workoutcompanion.objects.BreakNotifyingMode
import com.companion.android.workoutcompanion.objects.WorkoutProcess
import com.companion.android.workoutcompanion.viewmodels.WorkoutViewModel


private const val SET_TIMER_ID = R.id.set_timer
private const val SET_TIMER_PROGRESS_ID = R.id.set_timer_progress
private const val CIRCLE_ID = R.id.circle
private const val SET_TIMER_PULSE_VIEW_ID = R.id.pulse_anim_view

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
class ActionManager(
    private val activity: Activity,
    private val viewModel: WorkoutViewModel
) : CountDownTimer.Callback {

    private val breakTimer = CountDownTimer(activity, this)
    private val generalStopwatch = Stopwatch(activity)
    private val exerciseStopwatch = ExerciseStopwatch()

    var notifyingType: Int = BreakNotifyingMode.ANIMATION
        set(value) {
            breakTimer.setNotifyingType(value)
            field = value
        }

    private val actionToPos = mapOf(
        Action.BREAK_TIMER to 0,
        Action.GENERAL_STOPWATCH to 1,
        Action.EXERCISE_STOPWATCH to 2,
        Action.TEMP_STOPWATCH to 3,
        Action.TEMP_TIMER to 4,
    )

    fun isActionEnabled(action: Action) = viewModel.actionStates[actionToPos[action]!!].isEnabled
    fun isActionPaused(action: Action) = viewModel.actionStates[actionToPos[action]!!].isPaused

    fun perform(
        action: Action,
        initTime: Int,
        notifyingType: Int = this.notifyingType
    ) {
        when (action) {
            Action.GENERAL_STOPWATCH -> performGeneralStopwatch(initTime)

            Action.BREAK_TIMER -> performBreakTimer(initTime)

            Action.EXERCISE_STOPWATCH -> performExerciseStopwatch(initTime)

            Action.TEMP_STOPWATCH -> {}

            Action.TEMP_TIMER -> {}
        }
        this.notifyingType = notifyingType
    }

    /**
     * @param action Что будет выполняться;
     * @param fromStart true - Начать сначала, false - продолжить;
     * @param notifyingType Тип уведомления;
     */
    fun perform(
        action: Action,
        fromStart: Boolean,
        notifyingType: Int = this.notifyingType
    ) {
        val startTime: Int
        when (action) {
            Action.GENERAL_STOPWATCH -> {
                startTime = if (fromStart) 0 else generalStopwatch.time
                performGeneralStopwatch(startTime)
            }

            Action.BREAK_TIMER -> {
                startTime = if (fromStart) 0 else breakTimer.getTime()
                performBreakTimer(startTime)
            }

            Action.EXERCISE_STOPWATCH -> {
                startTime = if (fromStart) 0 else exerciseStopwatch.time
                performExerciseStopwatch(startTime)
            }

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

        breakTimer.enable(
            activity.findViewById(SET_TIMER_ID),
            activity.findViewById(SET_TIMER_PROGRESS_ID),
            activity.findViewById(SET_TIMER_PULSE_VIEW_ID)
        )
        if (breakTimer.isGoing) return
        else exerciseStopwatch.pause()

        breakTimer.start()

        val breakTimerAction = viewModel.actionStates[actionToPos[Action.BREAK_TIMER]!!]
        if (breakTimerAction.isEnabled && !breakTimerAction.isPaused) return
        viewModel.activeProcess.value = WorkoutProcess.TIMER
        val exerciseStopwatchAction = viewModel.actionStates[actionToPos[Action.EXERCISE_STOPWATCH]!!]
        if (exerciseStopwatchAction.isEnabled) {
            exerciseStopwatch.cancel(true)
            exerciseStopwatchAction.isEnabled = false
            exerciseStopwatchAction.isPaused = false
        }
        breakTimerAction.isEnabled = true
        breakTimerAction.isPaused = false
    }

    private fun performExerciseStopwatch(fromTime: Int = exerciseStopwatch.time) {
        Log.d("MyTag", "fromTime = $fromTime")
        exerciseStopwatch.enable(
            activity.findViewById(SET_TIMER_ID),
            activity.findViewById(CIRCLE_ID)
        )

        if (exerciseStopwatch.isGoing) {
            exerciseStopwatch.updateTextByTime()
            return
        }

        exerciseStopwatch.updateTextByTime(0)
        exerciseStopwatch.startFrom(fromTime)
        val thisAction = viewModel.actionStates[actionToPos[Action.EXERCISE_STOPWATCH]!!]
        if (thisAction.isEnabled && !thisAction.isPaused) return
        viewModel.activeProcess.value = WorkoutProcess.EXERCISE_STOPWATCH
        val prevAction = viewModel.actionStates[actionToPos[Action.BREAK_TIMER]!!]
        if (prevAction.isEnabled) {
            breakTimer.cancel(true)
            prevAction.isEnabled = false
            prevAction.isPaused = false
        }
        thisAction.isEnabled = true
        thisAction.isPaused = false
    }

    private fun performGeneralStopwatch(fromTime: Int) {
        if (generalStopwatch.isGoing) return
        generalStopwatch.start()
        val stopwatchAction = viewModel.actionStates[actionToPos[Action.GENERAL_STOPWATCH]!!]
        if (stopwatchAction.isEnabled && !stopwatchAction.isPaused) return
        stopwatchAction.isEnabled = true
        stopwatchAction.isPaused = false
        generalStopwatch.time = fromTime
    }

    fun pauseAllActions() {
        viewModel.actionStates.forEach { state ->
            if (state.isEnabled && !state.isPaused) {
                pause(state.action)
            }
        }
    }

    fun resumeAllActions() {
        viewModel.actionStates.forEach { state ->
            if (state.isEnabled && state.isPaused) {
                perform(state.action, fromStart = false)
            }
        }
    }

    fun pause(action: Action) {
        when (action) {
            Action.BREAK_TIMER -> {
                if (breakTimer.isGoing) {
                    breakTimer.pause()
                    viewModel.actionStates[actionToPos[Action.BREAK_TIMER]!!].isPaused = true
                }
            }

            Action.GENERAL_STOPWATCH -> {
                if (generalStopwatch.isGoing) {
                    generalStopwatch.pause()
                    viewModel.actionStates[actionToPos[Action.GENERAL_STOPWATCH]!!].isPaused = true
                    viewModel.activeProcess.value = WorkoutProcess.PAUSED
                }
            }

            Action.EXERCISE_STOPWATCH -> {
                if (exerciseStopwatch.isGoing) {
                    exerciseStopwatch.pause()
                    viewModel.actionStates[actionToPos[Action.EXERCISE_STOPWATCH]!!].isPaused = true
                }
            }

            Action.TEMP_STOPWATCH -> {}
            Action.TEMP_TIMER -> {}
        }
        viewModel.actionStates[actionToPos[action]!!].isPaused = true
    }

    fun updateTimeOf(action: Action, toSeconds: Int) {
        when (action) {
            Action.GENERAL_STOPWATCH ->
                generalStopwatch.time = toSeconds

            Action.EXERCISE_STOPWATCH ->
                if (viewModel.activeProcess.value == WorkoutProcess.EXERCISE_STOPWATCH)
                    exerciseStopwatch.time = toSeconds

            Action.BREAK_TIMER ->
                if (viewModel.activeProcess.value == WorkoutProcess.TIMER)
                    breakTimer.setTime(toSeconds)

            Action.TEMP_STOPWATCH -> {}
            Action.TEMP_TIMER -> {}
        }
    }

    fun saveObjectsStates() {
//        exerciseStopwatch.saveAnimationState()
    }

    fun updateUI(
        timerTextView: TextView,
        progressBar: ProgressBar,
        circleView: View,
        pulseView: View
    ) {
        when (viewModel.activeProcess.value) {
            WorkoutProcess.EXERCISE_STOPWATCH -> {
                exerciseStopwatch.enable(timerTextView, circleView)
                exerciseStopwatch.updateTextByTime()
            }
            WorkoutProcess.TIMER -> breakTimer.enable(
                timerTextView, progressBar, pulseView
            )
            // TODO: add new actions
        }
    }


    fun clear() {
        breakTimer.stop()
        exerciseStopwatch.stop()
        generalStopwatch.stop()
    }

    fun getInstanceState(): Bundle? {
        if (viewModel.activeProcess.value == WorkoutProcess.NOT_STARTED) return null
        return bundleOf(
            Pair(GENERAL_STOPWATCH_IS_GOING, generalStopwatch.isGoing),
            Pair(GENERAL_STOPWATCH_TIME, generalStopwatch.time),
            Pair(NOTIFYING_TYPE, notifyingType),
            Pair(BREAK_TIMER_IS_GOING, breakTimer.isGoing),
            Pair(BREAK_TIMER_TIME, breakTimer.getTime()),
            Pair(BREAK_TIMER_START_TIME, breakTimer.startTime),
            Pair(EXERCISE_STOPWATCH_IS_GOING, exerciseStopwatch.isGoing),
            Pair(EXERCISE_STOPWATCH_TIME, exerciseStopwatch.time),

            //TODO: ADD ACTIONS (TEMP SW AND TIMER)
        )
    }

    fun restoreInstanceState(savedState: Bundle?) {
        if (savedState == null) return
        savedState.apply {
            generalStopwatch.isGoing = getBoolean(GENERAL_STOPWATCH_IS_GOING)
            generalStopwatch.time = getInt(GENERAL_STOPWATCH_TIME)
            notifyingType = getInt(NOTIFYING_TYPE)
            breakTimer.isGoing = getBoolean(BREAK_TIMER_IS_GOING)
            breakTimer.setTime(getInt(BREAK_TIMER_TIME))
            breakTimer.startTime = getInt(BREAK_TIMER_START_TIME)
            exerciseStopwatch.time = getInt(EXERCISE_STOPWATCH_TIME)
            if (getBoolean(EXERCISE_STOPWATCH_IS_GOING))
                exerciseStopwatch.startFrom(exerciseStopwatch.time)


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

    override fun timerFinished() {
        perform(Action.EXERCISE_STOPWATCH, 0)
    }

    companion object {
        enum class Action {
            BREAK_TIMER,
            GENERAL_STOPWATCH,
            EXERCISE_STOPWATCH,
            TEMP_STOPWATCH,
            TEMP_TIMER,
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

    data class ActionState(
        val action: Action,
        var isEnabled: Boolean = false,
        var isPaused: Boolean = false,
    )
}

package com.companion.android.workoutcompanion.time

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.companion.android.workoutcompanion.R
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
private const val BREAK_TIMER_IS_PULSING = "b-timer-is-pulsing"
private const val BREAK_TIMER_IS_ENABLED = "b-timer-is-enabled"

private const val EXERCISE_STOPWATCH_TIME = "e-stopwatch-time"
private const val EXERCISE_STOPWATCH_IS_GOING = "e-stopwatch-is-going"
private const val EXERCISE_STOPWATCH_IS_ENABLED = "e-stopwatch-is-enabled"


private const val TAG = "bug1"


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

    private var lifecycleOwnerIsAlive = true

    private val actionToPos = mapOf(
        Action.BREAK_TIMER to 0,
        Action.GENERAL_STOPWATCH to 1,
        Action.EXERCISE_STOPWATCH to 2,
        Action.TEMP_STOPWATCH to 3,
        Action.TEMP_TIMER to 4,
    )

    fun isActionEnabled(action: Action) = viewModel.actionStates[actionToPos[action]!!].isEnabled
    fun isActionPaused(action: Action) = viewModel.actionStates[actionToPos[action]!!].isPaused

    fun launch(action: Action, initTime: Int) {
        when (action) {
            Action.GENERAL_STOPWATCH -> launchGeneralStopwatch(initTime)

            Action.BREAK_TIMER -> launchBreakTimer(initTime)

            Action.EXERCISE_STOPWATCH -> launchExerciseStopwatch(initTime)

            Action.TEMP_STOPWATCH -> {}

            Action.TEMP_TIMER -> {}
        }
    }

    /**
     * @param action Что будет выполняться;
     * @param fromStart true - Начать сначала, false - продолжить;
     */
    fun launch(action: Action, fromStart: Boolean) {
        val startTime: Int
        when (action) {
            Action.GENERAL_STOPWATCH -> {
                startTime = if (fromStart) 0 else generalStopwatch.time
                launchGeneralStopwatch(startTime)
            }

            Action.BREAK_TIMER -> {
                startTime = if (fromStart) 0 else breakTimer.getTime()
                launchBreakTimer(startTime)
            }

            Action.EXERCISE_STOPWATCH -> {
                startTime = if (fromStart) 0 else exerciseStopwatch.time
                launchExerciseStopwatch(startTime)
            }

            Action.TEMP_STOPWATCH -> {}

            Action.TEMP_TIMER -> {}
        }
    }

    fun launchOrPause(action: Action, initTime: Int? = null) {
        when (action) {
            Action.GENERAL_STOPWATCH -> {
                if (!generalStopwatch.isGoing)
                    launchGeneralStopwatch(initTime ?: generalStopwatch.time)
                else pause(Action.GENERAL_STOPWATCH)
            }

            Action.BREAK_TIMER -> {
                if (!breakTimer.isGoing)
                    launchBreakTimer(initTime ?: breakTimer.getTime())
                else pause(Action.BREAK_TIMER)
            }

            Action.EXERCISE_STOPWATCH -> {
                if (!exerciseStopwatch.isGoing)
                    launchExerciseStopwatch(initTime ?: exerciseStopwatch.time)
                else pause(Action.EXERCISE_STOPWATCH)
            }

            Action.TEMP_STOPWATCH -> {}

            Action.TEMP_TIMER -> {}
        }
    }

    private fun launchBreakTimer(time: Int) {
        Log.d(TAG, "launchBreakTimer: Passed time = $time")
        // Устанавливаем необходимое время;
        breakTimer.setTime(time)
        // Устанавливаем тип уведомления перед началом выполнения;
        breakTimer.setNotifyingType(viewModel.breakNotifyingMode!!)

        // Обновляем UI-элементы;
        breakTimer.enable(
            activity.findViewById(SET_TIMER_ID),
            activity.findViewById(SET_TIMER_PROGRESS_ID),
            activity.findViewById(SET_TIMER_PULSE_VIEW_ID)
        )
        // Инициализируем ProgressBar;
        breakTimer.initProgressAnimation()
        // Обновляем время на экране (необходимо для
        // мгновенного отображения соответствующего время);
        breakTimer.updateText()

        // Если таймер идет, то возобновляем его;
        if (breakTimer.isGoing) {
            Log.d(TAG, "launchBreakTimer: breakTimer is already going")
            breakTimer.start()
            return
        }
        // В случае если секундомер активен, останавливаем его;
        if (exerciseStopwatch.isEnabled) {
            generalStopwatch.sendTicksTo = null
            exerciseStopwatch.hideProgressBar(
                wDisappearAnim = true,
                actionAfter = {
                    exerciseStopwatch.stop()
                    breakTimer.start()
                }
            )
            Log.d(TAG, "launchBreakTimer: exerciseStopwatch is enabled")
        } else {
            breakTimer.start()
        }

        val breakTimerAction = viewModel.actionStates[actionToPos[Action.BREAK_TIMER]!!]
        viewModel.activeProcess.value = WorkoutProcess.TIMER
        if (breakTimerAction.isEnabled && !breakTimerAction.isPaused) return
        val exerciseStopwatchAction =
            viewModel.actionStates[actionToPos[Action.EXERCISE_STOPWATCH]!!]
        if (exerciseStopwatchAction.isEnabled) {
            exerciseStopwatchAction.isEnabled = false
            exerciseStopwatchAction.isPaused = false
        }
        breakTimerAction.isEnabled = true
        breakTimerAction.isPaused = false
    }

    private fun launchExerciseStopwatch(fromTime: Int = exerciseStopwatch.time) {
        // Передаем элементы на экране секундомеру;
        if (lifecycleOwnerIsAlive) {
            exerciseStopwatch.enable(
                activity.findViewById(SET_TIMER_ID),
                activity.findViewById(CIRCLE_ID),
            )
        }
        // Если секундомер уже запущен, не выполняем более действий;
        if (exerciseStopwatch.isGoing) return
        // Добавляем возможность обновления времени
        //  с помощью callbacks от глобального секундомера
        generalStopwatch.sendTicksTo = exerciseStopwatch
        // Устанавливаем секундомеру состояние готовности к обновлению времени;
        exerciseStopwatch.startFrom(fromTime)
        // Устанавливаем активным процесс работы секундомера;
        viewModel.activeProcess.value = WorkoutProcess.EXERCISE_STOPWATCH

        val thisAction = viewModel.actionStates[actionToPos[Action.EXERCISE_STOPWATCH]!!]
        viewModel.activeProcess.value = WorkoutProcess.EXERCISE_STOPWATCH
        if (thisAction.isEnabled && !thisAction.isPaused) return // STOPSHIP: bullshit
        val prevAction = viewModel.actionStates[actionToPos[Action.BREAK_TIMER]!!]
        if (prevAction.isEnabled) {
            prevAction.isEnabled = false
            prevAction.isPaused = false
        }
        thisAction.isEnabled = true
        thisAction.isPaused = false
    }

    private fun launchGeneralStopwatch(fromTime: Int) {
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
                launch(state.action, fromStart = false)
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
                exerciseStopwatch.restoreProgressAnimation()
                if (exerciseStopwatch.isGoing)
                    exerciseStopwatch.startProgressAnimation()
            }
            WorkoutProcess.TIMER -> {
                breakTimer.enable(timerTextView, progressBar, pulseView)
                breakTimer.updateText()
                breakTimer.initProgressAnimation()
                if (breakTimer.isGoing)
                    breakTimer.startProgressAnimation()
                breakTimer.initPulseAnimationSet()
                if (breakTimer.pulseAnimationIsRunning)
                    breakTimer.startPulseAnimation()
            }
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
        val capacity = 7
        val resultBundle = Bundle(capacity).also {
            it.putBoolean(GENERAL_STOPWATCH_IS_GOING, generalStopwatch.isGoing)
            it.putInt(GENERAL_STOPWATCH_TIME, generalStopwatch.time)
        }
        resultBundle.apply {
            if (viewModel.activeProcess.value!! == WorkoutProcess.TIMER) {
                putBoolean(BREAK_TIMER_IS_ENABLED, breakTimer.isEnabled)
                putBoolean(BREAK_TIMER_IS_GOING, breakTimer.isGoing)
                putBoolean(BREAK_TIMER_IS_PULSING, breakTimer.pulseAnimationIsRunning)
                putInt(BREAK_TIMER_START_TIME, breakTimer.startTime)
                putInt(BREAK_TIMER_TIME, breakTimer.getTime())
            } else if (viewModel.activeProcess.value!! == WorkoutProcess.EXERCISE_STOPWATCH) {
                putBoolean(EXERCISE_STOPWATCH_IS_ENABLED, exerciseStopwatch.isEnabled)
                putBoolean(EXERCISE_STOPWATCH_IS_GOING, exerciseStopwatch.isGoing)
                putInt(EXERCISE_STOPWATCH_TIME, exerciseStopwatch.time)
            }
            return this
        }


        //TODO: ADD ACTIONS (TEMP SW AND TIMER)

    }

    fun restoreInstanceState(savedState: Bundle?) {
        if (savedState == null) return
        savedState.apply {
            generalStopwatch.isGoing = getBoolean(GENERAL_STOPWATCH_IS_GOING)
            generalStopwatch.time = getInt(GENERAL_STOPWATCH_TIME)
            when (viewModel.activeProcess.value!!) {
                WorkoutProcess.TIMER -> {
                    breakTimer.isEnabled = getBoolean(BREAK_TIMER_IS_ENABLED)
                    breakTimer.setTime(getInt(BREAK_TIMER_TIME))
                    breakTimer.isGoing = getBoolean(BREAK_TIMER_IS_GOING)
                    breakTimer.startTime = getInt(BREAK_TIMER_START_TIME)
                    breakTimer.pulseAnimationIsRunning = getBoolean(BREAK_TIMER_IS_PULSING).also {
                        if (it) breakTimer.startPulseAnimation()
                    }
                    breakTimer.setNotifyingType(viewModel.breakNotifyingMode!!)
                }
                WorkoutProcess.EXERCISE_STOPWATCH -> {
                    exerciseStopwatch.time = getInt(EXERCISE_STOPWATCH_TIME)
                    exerciseStopwatch.isGoing = getBoolean(EXERCISE_STOPWATCH_IS_GOING)
                    exerciseStopwatch.isEnabled = getBoolean(EXERCISE_STOPWATCH_IS_ENABLED)
                    if (exerciseStopwatch.isGoing) {
                        generalStopwatch.sendTicksTo = exerciseStopwatch
                    }
                }
            }

            //TODO: ADD ACTIONS (TEMP SW AND TIMER)
        }
    }

    fun notifyLifecycleOwnerStopped() {
        lifecycleOwnerIsAlive = false
        breakTimer.lifecycleOwnerIsAlive = false
        exerciseStopwatch.lifecycleOwnerIsAlive = false
    }

    fun notifyLifecycleOwnerCreated() {
        lifecycleOwnerIsAlive = true
        breakTimer.lifecycleOwnerIsAlive = true
        exerciseStopwatch.lifecycleOwnerIsAlive = true
    }

//    private fun getActionsFromPositions(actionPos: Array<Int>): MutableList<Action> {
//        val posToAction = mapOf(
//            0 to Action.BREAK_TIMER,
//            1 to Action.GENERAL_STOPWATCH,
//            2 to Action.EXERCISE_STOPWATCH,
//            3 to Action.TEMP_STOPWATCH,
//            4 to Action.TEMP_TIMER,
//        )
//        val actions = mutableListOf<Action>()
//        actionPos.forEach { pos ->
//            actions.add(posToAction[pos]!!)
//        }
//        return actions
//    }

    override fun timerFinished() {
        launch(Action.EXERCISE_STOPWATCH, 0)
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

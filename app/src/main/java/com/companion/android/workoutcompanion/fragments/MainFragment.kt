package com.companion.android.workoutcompanion.fragments

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.companion.android.workoutcompanion.R
import com.companion.android.workoutcompanion.databinding.FragmentMainBinding
import com.companion.android.workoutcompanion.objects.Anim
import com.companion.android.workoutcompanion.objects.ChangeAppearance.setCorners
import com.companion.android.workoutcompanion.objects.DrawableUtils
import com.companion.android.workoutcompanion.objects.Vibration
import com.companion.android.workoutcompanion.objects.WorkoutProcess
import com.companion.android.workoutcompanion.viewmodels.WorkoutViewModel
import com.google.android.material.button.MaterialButton


private const val DIALOG_START = "dialog-start" // Метка диалога


/**
 * Данный фрагмент сопровождает пользователя во время тренировки
 */
class MainFragment : Fragment() {

    private val args: MainFragmentArgs by navArgs()

    private var _binding: FragmentMainBinding? = null  // Объект класса привязки данных
    private val binding get() = _binding!!

    private val viewModel: WorkoutViewModel by activityViewModels() // Общая ViewModel
    private var callback: FragmentCallback? = null

    private var mainButtonAnimation: ObjectAnimator? = null

    private var isLargeLayout = false

    private val actionsCoords = Array(5) { IntArray(2) { 0 } }

    private val actionViews = Array<MaterialButton?>(5) { null }

    private var actionsAreVisible = false

    private var actionButtonCorner = 0f


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("LF", "F onCreateView")
        _binding = FragmentMainBinding // определяем привязку данных
            .inflate(layoutInflater, container, false)

        isLargeLayout = resources.getBoolean(R.bool.large_layout)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("LF", "F onViewCreated")
        // Слушатель нажатий для кнопки начала тренировки
        binding.startButton.setOnClickListener {
            // Загружаем анимацию для кнопки старта тренировки
            val playButtonAnimation = AnimationUtils
                .loadAnimation(requireContext(), R.anim.bounce_enlarge)
            val backgroundButtonAnimation = AnimationUtils
                .loadAnimation(requireContext(), R.anim.fade_out)
            binding.startButton.apply {
                startAnimation(playButtonAnimation)
                isClickable = false // Деактивируем ее на время анимации
            }
            binding.startButtonStatic.startAnimation(backgroundButtonAnimation)
            // Диалоговое окно настроек тренировки еще не запущено
            if (parentFragmentManager.findFragmentByTag(DIALOG_START) == null) {
                // Открываем диалоговое окно с задержкой, на время выполнения анимации
                Handler(Looper.getMainLooper()).postDelayed({
                    showStartDialog()
                    binding.startButton.apply {
                        clearAnimation()
                        isClickable = true // Активируем кнопку после анимации
                    }
                    binding.startButtonGroup.visibility = View.GONE
                }, 200)
            }
        }
        // Установка бесконечной анимации поворота для основной кнопки
        mainButtonAnimation = ObjectAnimator.ofPropertyValuesHolder(
            binding.mainButton,
            PropertyValuesHolder.ofFloat("rotation", 0F, 360F)
        ).also {
            it.repeatCount = ValueAnimator.INFINITE
            it.repeatMode = ValueAnimator.REVERSE
            it.duration = 5000
            it.start()
        }

        // Инициализируем массив с действиями (кнопки)
        actionViews[0] = binding.action1
        actionViews[1] = binding.action2
        actionViews[2] = binding.action3
        actionViews[3] = binding.action4
        actionViews[4] = binding.actionFlip
    }


    override fun onStart() {
        super.onStart()

        callback?.onFragmentUICreated(
            binding.setTimer,
            binding.setTimerProgress,
            binding.circle,
            binding.pulseAnimView
        )
        Log.d("LF", "F onStart")

        // Если тренировка уже началась, то убираем лишнее с экрана;
        if (viewModel.activeProcess.value == WorkoutProcess.NOT_STARTED) {
            if (args.workoutStartedSuccessfully) {
                callback?.onWorkoutStarted()
                setProcessViews()
            } else {
                setStartViews()
            }
        } else {
            setProcessViews()
        }

        // Фиксируем разметку, что позволит вводить текст с клавиатуры без изменений разметки;
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        if (viewModel.areActionsOnLeftSide) {
            updateActionButtonsCorners(viewModel.areActionsOnLeftSide)
        } else {
            flipActionsRight()
        }

        val vibrator = Vibration.getVibrator(requireContext())

        val actionHoverAnimations = getValueAnimators()

        /// Обработка касаний кнопки действий

        var actionsAreActive = false // В случае видимости меню, оно будет скрыто; так же наоборот
        var timeAtTouchStart = 0L   // Время с момента касания кнопки;
        var timeAtTouchEnd = 0L    // Время с момента поднятия пальца с кнопки;

        var lastHoveredAction: Int? = null

        binding.mainButton.setOnTouchListener { view, motionEvent ->

            when (motionEvent.action) {

                MotionEvent.ACTION_DOWN -> {
                    timeAtTouchStart = System.currentTimeMillis()
                    if (timeAtTouchStart - timeAtTouchEnd < 600) {
                        timeAtTouchStart += 400 // Анимация появления действий появится с задержкой
                    }
                    view.startAnimation(
                        AnimationUtils.loadAnimation(context, R.anim.button_pressing)
                    )
                }

                MotionEvent.ACTION_UP -> {
                    view.startAnimation(
                        AnimationUtils.loadAnimation(context, R.anim.button_unpressing)
                    )
                    if (view.isClickable) {
                        if (!actionsAreActive)
                            view.performClick()
                        else {
                            val waitBeforeHideMs: Long = 200
                            Handler(Looper.getMainLooper())
                                .postDelayed({
                                    hideActions(lastHoveredAction) {
                                        lastHoveredAction?.let { selectAction(it) }
                                    }
                                }, waitBeforeHideMs)
                            actionsAreActive = false
                        }
                    }
                    return@setOnTouchListener true
                }

            }
            val x = motionEvent.rawX.toInt()
            val y = motionEvent.rawY.toInt()

            initActionCoordsIfNeeded()


            fun undoHoverEffect() {
                lastHoveredAction?.let {
                    val backgroundColor = ContextCompat
                        .getColor(requireContext(), R.color.secondary_container)
                    actionHoverAnimations[it].cancel()
                    actionViews[it]!!.setBackgroundColor(backgroundColor)
                }

                lastHoveredAction = null
            }

            fun doHoverEffect(action: Int) {
                if (lastHoveredAction == action) return
                undoHoverEffect()
                actionHoverAnimations[action].start()
                lastHoveredAction = action
            }


            with(binding) {

                if (actionsAreVisible && y in actionsCoords[0][1]..actionsCoords[3][1] + action4.height) {

                    if (x in actionsCoords[1][0]..actionsCoords[0][0] + action1.width) {

                        when (y) {
                            in actionsCoords[0][1]..actionsCoords[0][1] + action1.height ->
                                doHoverEffect(0)

                            in actionsCoords[1][1]..actionsCoords[1][1] + action2.height ->
                                doHoverEffect(1)

                            in actionsCoords[2][1]..actionsCoords[2][1] + action3.height ->
                                doHoverEffect(2)

                            in actionsCoords[3][1]..actionsCoords[3][1] + action4.height ->
                                doHoverEffect(3)
                        }

                    } else if (x in actionsCoords[4][0]..actionsCoords[4][0] + actionFlip.width) {
                        if (y in actionsCoords[4][1]..actionsCoords[4][1] + actionFlip.height) {
                            doHoverEffect(4)
                        } else undoHoverEffect()

                    } else undoHoverEffect()

                } else undoHoverEffect()
            }

            timeAtTouchEnd = System.currentTimeMillis()
            // Если палец задерживается на 300 миллисекунд на кнопке, инициируем длительный клик
            if (!actionsAreActive && timeAtTouchEnd - timeAtTouchStart >= 300) {
                view.performLongClick()
                actionsAreActive = true
            }
            true
        }

        // Обработка длительного нажатия кнопки действий
        binding.mainButton.setOnLongClickListener {
            Vibration.make(vibrator, amplitude = VibrationEffect.EFFECT_HEAVY_CLICK)
            showActions()
            true
        }

        // Обработка обычного нажатия кнопки действий
        binding.mainButton.setOnClickListener {
            it.isClickable = false
            Log.d("MyTag", "is clicked set to false")
            callback?.onMainButtonClicked()
            Handler(requireContext().mainLooper)
                .postDelayed({
                    it.isClickable = true
                    Log.d("MyTag", "is clicked set to true")
                }, 1000)
        }


        requireActivity()
            .onBackPressedDispatcher
            .addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    Toast.makeText(requireContext(), "Attempt to exit...", Toast.LENGTH_SHORT)
                        .show()
                }
            })

        viewModel.activeProcess.observe(this) {
            if (it == WorkoutProcess.NOT_STARTED && binding.mainButton.isVisible) {
                setStartViews()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        callback!!.onFragmentStopped()
        Log.d("LF", "F onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("LF", "F onDestroy")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        actionButtonCorner = resources.getDimension(R.dimen.main_button_action_button_corner_size)
        Log.d("LF", "F onAttach")
        callback = context as FragmentCallback
    }

    override fun onDetach() {
        super.onDetach()
        Log.d("LF", "F onDetach")
        callback = null
    }

    private fun showStartDialog() {
        if (isLargeLayout) {
            throw Exception("There is no dialog for large screen yet")
        } else {
            findNavController().navigate(R.id.navigate_to_workoutStartFSDialog)
        }
    }

    private fun setStartViews() {
        binding.startButtonGroup.visibility = View.VISIBLE
        binding.setTimerProgress.visibility = View.GONE
        binding.circle.visibility = View.GONE
        binding.setTimer.visibility = View.GONE
        binding.mainButton.apply {
            animation = null
            visibility = View.GONE
        }
    }

    private fun setProcessViews() {
        Log.d("MyTag", "setProcessViews()")
        binding.startButtonGroup.visibility = View.GONE
        if (viewModel.activeProcess.value == WorkoutProcess.EXERCISE_STOPWATCH)
            binding.circle.visibility = View.VISIBLE
        else
            binding.setTimerProgress.visibility = View.VISIBLE
        binding.setTimer.visibility = View.VISIBLE
        binding.mainButton.visibility = View.VISIBLE
    }

    private fun initActionCoordsIfNeeded(forced: Boolean = false) {
        if (actionsCoords[0][1] != 0 && !forced) return
        actionViews.forEachIndexed { index, button ->
            button!!.getLocationOnScreen(actionsCoords[index])
        }
    }

    private fun showActions() {
        val actionsAppearResource: Int
        val flipActionAppearResource: Int
        if (viewModel.areActionsOnLeftSide) {
            actionsAppearResource = R.anim.main_button_action_appear_from_left
            flipActionAppearResource = R.anim.main_button_flip_action_appear_from_right
        } else {
            actionsAppearResource = R.anim.main_button_action_appear_from_right
            flipActionAppearResource = R.anim.main_button_flip_action_appear_from_left
        }
        DrawableUtils.setStroke(
            binding.mainButton.background,
            ContextCompat.getColor(requireContext(), R.color.main_button_action_outline),
            4
        )
        val actionsAppear = AnimationUtils.loadAnimation(context, actionsAppearResource)
        val flipButtonAppear = AnimationUtils.loadAnimation(context, flipActionAppearResource)
        val circleAppear = AnimationUtils.loadAnimation(context, R.anim.circle_expand)

        with(binding) {
            mainButtonBackground.isVisible = true
            Anim.doOnEndOf(circleAppear) {
                actionButtonsGroup.visibility = View.VISIBLE
                for (i in 0..actionViews.size - 2)
                    actionViews[i]!!.startAnimation(actionsAppear)
                actionFlip.startAnimation(flipButtonAppear)
                actionsAreVisible = true
            }
            mainButtonBackground.startAnimation(circleAppear)
        }
    }

    private fun hideActions(selectedAction: Int?, doOnEnd: () -> Unit = {}) {
        val actionsDisappearResource: Int
        val flipActionDisappearResource: Int
        if (viewModel.areActionsOnLeftSide) {
            actionsDisappearResource = R.anim.main_button_action_disappear_to_right
            flipActionDisappearResource = R.anim.main_button_flip_action_disappear_to_left
        } else {
            actionsDisappearResource = R.anim.main_button_action_disappear_to_left
            flipActionDisappearResource = R.anim.main_button_flip_action_disappear_to_right
        }
        val actionsDisappear = AnimationUtils.loadAnimation(context, actionsDisappearResource)
        val flipButtonDisappear = AnimationUtils.loadAnimation(context, flipActionDisappearResource)
        val circleDisappear = AnimationUtils.loadAnimation(context, R.anim.circle_shrink)

        with(binding) {
            Anim.doOnEndOf(circleDisappear) {
                mainButtonBackground.visibility = View.INVISIBLE
                selectedAction?.let { actionViews[it]?.visibility = View.INVISIBLE }
                DrawableUtils.setStroke(
                    binding.mainButton.background,
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.secondary_container
                    ),
                    1
                )
            }
            Anim.doOnEndOf(actionsDisappear) {
                actionViews.forEachIndexed { index, view ->
                    if (selectedAction != index)
                        view!!.visibility = View.INVISIBLE
                }
                mainButtonBackground.startAnimation(circleDisappear)
                doOnEnd.invoke()
            }

            val fadeAnim = AnimationUtils.loadAnimation(context, R.anim.fade_out_delayed_100)

            Anim.doOnEndOf(fadeAnim) {
                selectedAction?.let { actionViews[it]?.visibility = View.INVISIBLE }
            }

            for (i in actionViews.indices) {
                if (i != selectedAction)
                    if (i != actionViews.lastIndex)
                        actionViews[i]!!.startAnimation(actionsDisappear)
                    else
                        actionViews[i]!!.startAnimation(flipButtonDisappear)
                else
                    actionViews[i]!!.startAnimation(fadeAnim)
            }
            actionsAreVisible = false
        }
    }

    private fun selectAction(action: Int) {
        when (action) {
            0 -> {

            }
            1 -> {

            }
            2 -> {

            }
            3 -> {

            }
            4 -> {
                flipActions()
            }
        }
    }

    private fun flipActionsRight() {
        binding.actionFlip.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            initActionCoordsIfNeeded(true)
        }

        for (i in 0 until actionViews.lastIndex) {
            actionViews[i]?.updateLayoutParams<ConstraintLayout.LayoutParams> {
                endToStart = -1
                startToStart = binding.verticalCenterGuideline.id
            }
        }
        binding.actionFlip.updateLayoutParams<ConstraintLayout.LayoutParams> {
            startToStart = -1
            endToStart = binding.verticalCenterGuideline.id
        }
        binding.actionFlip.setText(R.string.action_flip_left)
        viewModel.areActionsOnLeftSide = false
        updateActionButtonsCorners(false)
    }

    private fun flipActionsLeft() {
        binding.actionFlip.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            initActionCoordsIfNeeded(true)
        }

        for (i in 0 until actionViews.lastIndex) {
            actionViews[i]?.updateLayoutParams<ConstraintLayout.LayoutParams> {
                startToStart = -1
                endToStart = binding.verticalCenterGuideline.id
            }
        }
        binding.actionFlip.updateLayoutParams<ConstraintLayout.LayoutParams> {
            endToStart = -1
            startToStart = binding.verticalCenterGuideline.id
        }
        binding.actionFlip.setText(R.string.action_flip_right)
        viewModel.areActionsOnLeftSide = true
        updateActionButtonsCorners(true)
    }

    private fun flipActions() {
        if (viewModel.areActionsOnLeftSide) flipActionsRight()
        else flipActionsLeft()
    }

    private fun getValueAnimators(): Array<ValueAnimator> {
        return Array(actionViews.size) { i ->
            Anim.getColorChangingAnimation(
                actionViews[i] as View,
                requireContext(),
                R.color.secondary_container,
                R.color.surface
            )
        }
    }

    private fun updateActionButtonsCorners(areActionsOnLeftSide: Boolean) {
        if (areActionsOnLeftSide) {
            actionViews[0]?.setCorners(
                actionButtonCorner,
                actionButtonCorner,
                actionButtonCorner,
                0f
            )
            actionViews[1]?.setCorners(actionButtonCorner, 0f, actionButtonCorner, 0f)
            actionViews[2]?.setCorners(actionButtonCorner, 0f, actionButtonCorner, 0f)
            actionViews[3]?.setCorners(
                actionButtonCorner,
                0f,
                actionButtonCorner,
                actionButtonCorner
            )
            actionViews[4]?.setCorners(0f, actionButtonCorner, 0f, actionButtonCorner)
        } else {
            actionViews[0]?.setCorners(
                actionButtonCorner,
                actionButtonCorner,
                0f,
                actionButtonCorner
            )
            actionViews[1]?.setCorners(0f, actionButtonCorner, 0f, actionButtonCorner)
            actionViews[2]?.setCorners(0f, actionButtonCorner, 0f, actionButtonCorner)
            actionViews[3]?.setCorners(
                0f,
                actionButtonCorner,
                actionButtonCorner,
                actionButtonCorner
            )
            actionViews[4]?.setCorners(actionButtonCorner, 0f, actionButtonCorner, 0f)
        }
    }


    interface FragmentCallback {
        fun onMainButtonClicked()
        fun onFragmentStopped()
        fun onWorkoutStarted()
        fun onFragmentUICreated(
            textView: TextView,
            progressBar: ProgressBar,
            circleView: View,
            pulseView: View
        )
    }

}

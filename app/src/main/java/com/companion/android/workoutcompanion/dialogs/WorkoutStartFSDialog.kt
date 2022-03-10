package com.companion.android.workoutcompanion.dialogs


import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.Transformation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.companion.android.workoutcompanion.R
import com.companion.android.workoutcompanion.activities.TAG_MAIN_FRAGMENT
import com.companion.android.workoutcompanion.databinding.DialogStartWorkoutBinding
import com.companion.android.workoutcompanion.objects.BreakNotifyingMode
import com.companion.android.workoutcompanion.objects.Place
import com.companion.android.workoutcompanion.objects.WorkoutParams
import com.companion.android.workoutcompanion.viewmodels.WorkoutViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.slider.Slider


// Ключ для слушателя получения результата
const val SELECT_BODY_PART_DIALOG = "select-body-part-dialog"

// Ключ для слушателя получения результата
const val SELECT_MUSCLE_DIALOG = "select-muscle-dialog"

// Тег для передачи списка выбранных объектов из диалога
const val LIST_BUNDLE_TAG = "list-bundle-tag"


/**
 * Диалоговое окно, занимающее весь экран,
 * для выбора параметров перед тренировкой;
 * Будет запускать необходимые диалоговые окна
 */
class WorkoutStartFSDialog : DialogFragment() {
    // ViewModel для сохранения необходимых данных, выбранных пользователем
    private val viewModel: WorkoutViewModel by activityViewModels()

    // Инициализация объекта класса привязки данных
    private var _binding: DialogStartWorkoutBinding? = null
    private val binding get() = _binding!!

    private var callback: Callback? = null

    private val fadeAnim = Fade()


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // The only reason you might override this method when using onCreateView() is
        // to modify any dialog characteristics. For example, the dialog includes a
        // title by default, but your custom layout might not need it. So here you can
        // remove the dialog title, but you must call the superclass to get the Dialog.
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setWindowAnimations(R.style.BounceAnimation)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogStartWorkoutBinding // определяем привязку данных
            .inflate(inflater, container, false)

        if (savedInstanceState != null) {
            recoverData()
        }
        return binding.root
    }


    override fun onStart() {
        super.onStart()
        callback = parentFragmentManager.findFragmentByTag(TAG_MAIN_FRAGMENT) as Callback

        remakeCorners()


        /** Обработка нажатий на элементы выбора параметров тренировки */

        fun handlePlaceItemClick(item: View, place: Int) = with(binding) {
            selectWorkoutPlace.animateAsSuccess()
            viewModel.trainingPlace = place
            selectWorkoutPlace.collapse(
                placeItemsLayout,
                titleSelectWorkoutPlace,
                {
                    item.visibility = View.GONE
                    when (item) {
                        itemHome -> {
                            if (!itemGym.isVisible)
                                itemGym.visibility = View.VISIBLE
                            if (!itemOutdoors.isVisible)
                                itemOutdoors.visibility = View.VISIBLE
                        }
                        itemGym -> {
                            if (!itemHome.isVisible)
                                itemHome.visibility = View.VISIBLE
                            if (!itemOutdoors.isVisible)
                                itemOutdoors.visibility = View.VISIBLE
                        }
                        itemOutdoors -> {
                            if (!itemHome.isVisible)
                                itemHome.visibility = View.VISIBLE
                            if (!itemGym.isVisible)
                                itemGym.visibility = View.VISIBLE
                        }
                    }
                }
            )
        }

        fun handleBreakModeItemClick(item: View, mode: Int) = with(binding) {
            selectNotifyingMode.animateAsSuccess()
            viewModel.breakNotificationMode = mode
            selectNotifyingMode.collapse(
                notifyingItemsLayout,
                titleSelectNotifyingMode,
                {
                    item.visibility = View.GONE
                    when (item) {
                        itemSound -> {
                            if (!itemVibration.isVisible)
                                itemVibration.visibility = View.VISIBLE
                            if (!itemAnimations.isVisible)
                                itemAnimations.visibility = View.VISIBLE
                        }
                        itemVibration -> {
                            if (!itemSound.isVisible)
                                itemSound.visibility = View.VISIBLE
                            if (!itemAnimations.isVisible)
                                itemAnimations.visibility = View.VISIBLE
                        }
                        itemAnimations -> {
                            if (!itemSound.isVisible)
                                itemSound.visibility = View.VISIBLE
                            if (!itemVibration.isVisible)
                                itemVibration.visibility = View.VISIBLE
                        }
                    }
                }
            )
        }

        fadeAnim.apply {
            duration = 400
            addTarget(binding.bpsAndMusclesButtons)
            addTarget(binding.selectRestTimeLayout)
            addTarget(binding.placeItemsLayout)
            addTarget(binding.notifyingItemsLayout)
            addTarget(binding.titleSelectBpsAndMuscles)
            addTarget(binding.titleSelectRestTime)
            addTarget(binding.titleSelectWorkoutPlace)
            addTarget(binding.titleSelectNotifyingMode)
        }

        // Инициализация поведения кнопки выбора места тренировки:
        binding.selectWorkoutPlace.setOnClickListener {
            binding.apply {
                if (placeItemsLayout.visibility == View.VISIBLE) {
                    selectWorkoutPlace.collapse(placeItemsLayout, titleSelectWorkoutPlace)
                } else {
                    remakeCorners(place = true, mode = false)
                    selectWorkoutPlace.expand(placeItemsLayout, titleSelectWorkoutPlace)
                }

                itemHome.setOnClickListener { handlePlaceItemClick(it, Place.TRAINING_AT_HOME) }
                itemGym.setOnClickListener { handlePlaceItemClick(it, Place.TRAINING_IN_GYM) }
                itemOutdoors.setOnClickListener {
                    handlePlaceItemClick(
                        it,
                        Place.TRAINING_OUTDOORS
                    )
                }

                it.hideKeyboard()
            }
        }

        // Инициализация поведения кнопки выбора режима уведомления:
        binding.selectNotifyingMode.setOnClickListener {
            binding.apply {
                if (notifyingItemsLayout.visibility == View.VISIBLE) {
                    selectNotifyingMode.collapse(notifyingItemsLayout, titleSelectNotifyingMode)
                } else {
                    remakeCorners(place = false, mode = true)
                    //TransitionManager.beginDelayedTransition(it as ViewGroup, fadeAnim)
                    selectNotifyingMode.expand(notifyingItemsLayout, titleSelectNotifyingMode)
                }
                itemSound.setOnClickListener {
                    handleBreakModeItemClick(it, BreakNotifyingMode.SOUND)
                }
                itemVibration.setOnClickListener {
                    handleBreakModeItemClick(it, BreakNotifyingMode.VIBRATION)
                }
                itemAnimations.setOnClickListener {
                    handleBreakModeItemClick(it, BreakNotifyingMode.ANIMATION)
                }
            }
            it.hideKeyboard()
        }

        // Инициализация поведения кнопки выбора частей тела и мышц:
        binding.selectBpsAndMuscles.setOnClickListener {
            if (binding.bpsAndMusclesButtons.visibility == View.VISIBLE) {
                binding.selectBpsAndMuscles.collapse(
                    binding.bpsAndMusclesButtons,
                    binding.titleSelectBpsAndMuscles
                )
            } else {
                //TransitionManager.beginDelayedTransition(it as ViewGroup, fadeAnim)
                binding.selectBpsAndMuscles.expand(
                    binding.bpsAndMusclesButtons,
                    binding.titleSelectBpsAndMuscles
                )
            }
        }

        // Инициализация поведения вложенных элементов:
        binding.bodyPartsButton.setOnClickListener {

            // Устанавливаем слушатель для получения списка выбранных частей тела
            // По ключу SELECT_BODY_PART_DIALOG_TAG
            setFragmentResultListener(SELECT_BODY_PART_DIALOG) { _, bundle ->
                // Уберем предупреждение, если пользователь пытался начать
                // тренировку без выбора тренируемой части тела
                // В переменную numbersOfSelectedBodyParts записываем arrayList
                // полученный из объекта Bundle по ключу BODY_PART_LIST_KEY
                val whichBPIsSelected = bundle.getBooleanArray(LIST_BUNDLE_TAG)

                // Если полученный список не изменился, то перезаписывать данные не будем
                if (!viewModel.getWhichBPsAreSelected().toBooleanArray()
                        .contentEquals(whichBPIsSelected)
//                    || binding.bodyPartSelector.text.isEmpty() //TODO: include
                ) {
                    viewModel.updateData(requireContext(), whichBPIsSelected!!.toTypedArray())

                }
                // Отображаем вторую кнопку и корректируем
                // местоположение кнопки частей тела
                if (!binding.musclesButton.isVisible) {
                    binding.selectBpsAndMuscles.expand(
                        binding.bpsAndMusclesButtons,
                        binding.titleSelectBpsAndMuscles,
                        getAnimation = true,
                        binding.selectBpsAndMuscles.height
                    )!!.apply {
                        setAnimationListener(object : Animation.AnimationListener {
                            override fun onAnimationStart(a: Animation?) {
                                binding.titleSelectBpsAndMuscles.visibility = View.GONE
                                binding.layoutSelectBpsAndMuscles.visibility = View.GONE
                                removeBottomCorners(binding.bodyPartsButton)
                                removeTopCorners(binding.musclesButton)
                            }

                            override fun onAnimationEnd(a: Animation?) {
                                binding.titleSelectBpsAndMuscles.visibility = View.VISIBLE
                                binding.layoutSelectBpsAndMuscles.visibility = View.VISIBLE
                                binding.musclesButton.visibility = View.VISIBLE
                            }

                            override fun onAnimationRepeat(a: Animation?) {}
                        })
                        binding.selectBpsAndMuscles.startAnimation(this)
                    }
                }
                binding.selectBpsAndMuscles.animateAsSuccess()
                // TODO: animation
            }
            // Запуск диалогового окна с выбором частей тела
            if (parentFragmentManager.findFragmentByTag(SELECT_BODY_PART_DIALOG) == null) {
                MultiChoiceDialog(
                    viewModel.getAllBP(requireContext()),
                    viewModel.getWhichBPsAreSelected().toBooleanArray()
                ).show(parentFragmentManager, SELECT_BODY_PART_DIALOG)
            }
        }
        binding.musclesButton.setOnClickListener {
            // Устанавливаем слушатель для получения списка мышц по ключу SELECT_MUSCLE_DIALOG.
            // Список мышц определяется в зависимости от выбранных частей тела
            setFragmentResultListener(SELECT_MUSCLE_DIALOG) { _, bundle ->
                val whichMuscleIsSelected = bundle.getBooleanArray(LIST_BUNDLE_TAG)
                viewModel.saveSelectedMuscles(whichMuscleIsSelected!!.toTypedArray())
                binding.selectBpsAndMuscles.collapse(
                    binding.bpsAndMusclesButtons,
                    binding.titleSelectBpsAndMuscles
                )
                binding.selectBpsAndMuscles.animateAsSuccess()
                var count: Short = 0
                whichMuscleIsSelected.forEach { if (it) count++ }
            }
            // Запуск диалогового окна с выбором мышц
            if (parentFragmentManager.findFragmentByTag(SELECT_MUSCLE_DIALOG) == null) {
                MultiChoiceDialog(
                    viewModel.getAvailableMuscles(requireContext()),
                    viewModel.getWhichMusclesAreSelected().toBooleanArray()
                ).show(parentFragmentManager, SELECT_MUSCLE_DIALOG)
            }
        }
        var startedInput = false
        binding.selectRestTime.setOnClickListener {
            if (binding.selectRestTimeLayout.visibility == View.VISIBLE) {
                startedInput = false
                binding.selectRestTime.collapse(
                    binding.selectRestTimeLayout,
                    binding.titleSelectRestTime,
                    { setRestTimeInitState() }
                )
            } else {
                //TransitionManager.beginDelayedTransition(it as ViewGroup, fadeAnim)
                binding.selectRestTime.expand(
                    binding.selectRestTimeLayout,
                    binding.titleSelectRestTime
                )
            }
        }

        binding.selectTimeViaSliderButton.setOnClickListener {
            if (binding.timeSlider.visibility == View.VISIBLE) {
                binding.selectRestTime.animateAsSuccess()
                viewModel.restTime.value = binding.timeSlider.value.toInt()
                binding.selectRestTime.collapse(
                    binding.selectRestTimeLayout,
                    binding.titleSelectRestTime,
                    { setRestTimeInitState() }
                )
            } else {
                binding.selectRestTime.expandRestTimeItemTo(
                    binding.timeSlider,
                    binding.inputTimeField
                )
                if (!binding.inputTimeField.text.isNullOrEmpty()) {
                    binding.selectTimeManuallyButton.animateTextChange(
                        R.string.select_rest_time_manually, true
                    )
                }
            }
        }

        binding.selectTimeManuallyButton.setOnClickListener {
            if (binding.inputTimeField.visibility == View.VISIBLE) {
                if (!binding.inputTimeField.text.isNullOrEmpty()) {
                    validateRestTime(binding.inputTimeField.text.toString().toInt())
                    startedInput = false
                }
            } else {
                binding.selectRestTime.expandRestTimeItemTo(
                    binding.inputTimeField,
                    binding.timeSlider
                )
                if (!binding.inputTimeField.text.isNullOrEmpty()) {
                    binding.selectTimeManuallyButton.setText(R.string.button_dialog_accept)
                }
                if (binding.selectTimeViaSliderButton.text.toString() == getString(R.string.button_dialog_accept)
                ) {
                    binding.selectTimeViaSliderButton.animateTextChange(
                        R.string.select_rest_time_via_slider, true
                    )
                }
            }
        }

        binding.inputTimeField.onDone {
            if (!binding.inputTimeField.text.isNullOrEmpty()) {
                startedInput = false
                validateRestTime(binding.inputTimeField.text.toString().toInt())
            }
        }

        binding.timeSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}

            override fun onStopTrackingTouch(slider: Slider) {
                // Сохранить выбранное значение
                binding.selectTimeViaSliderButton.animateTextChange(R.string.button_dialog_accept)
            }
        })

        binding.inputTimeField.onFocusChangeListener =
            View.OnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    v.hideKeyboard()
                }
            }

        binding.inputTimeField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {}
            override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (!startedInput && !text.isNullOrEmpty()) {
                    startedInput = true
                    binding.selectTimeManuallyButton
                        .animateTextChange(R.string.button_dialog_accept, false)
                }
            }
        })

        binding.timeSlider.setLabelFormatter { value ->
            if (value % 60f == 0f) "${value.toInt() / 60} min"
            else "${value.toInt()} sec"
        }

        binding.startButton.setOnClickListener {
            // Если обязательные поля заполнены:
            if (requiredFieldsCompleted()) {
                callback?.workoutStarted(true)
                if (viewModel.getNumberOfSelectedMuscles() != 0.toShort()) {
                    val unused = viewModel.getWhichBPAreUnused()
                    if (unused.contains(true)) {
                        WarningUnusedBPDialog(unused).show(parentFragmentManager, "")
                    }
                }

                // TODO:Сохранить все значения в бд
                dismiss()
            }
            // Если не все обязательные поля заполнены
            else {
                requireAllFieldsCompleted()
            }
        }

        binding.cancelButton.setOnClickListener {
            viewModel.clearAllData()
            callback?.workoutStarted(false)
            dismiss()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("1", true)
    }

    override fun onDetach() {
        super.onDetach()
        callback?.workoutStarted(false)
        callback = null
        viewModel.clearAllData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Произойдет очистка
    }

    /**
     * Метод предназначен для проверки на заполненность обязательных полей
     * В случае удачи (все нужные поля заполнены) вернется true
     * В случае если не все обязательные поля заполнены, вернется false
     */
    private fun requiredFieldsCompleted(): Boolean {
        return viewModel.getSelectedBP(requireContext()).isNotEmpty()
                && viewModel.breakNotificationMode != null
                && viewModel.restTime.value != null
                && viewModel.trainingPlace != null
    }

    /**
     * Удаление углов у содержимого CardView,
     * а именно с выбором места и режима уведомления;
     */
    private fun remakeCorners(place: Boolean = true, mode: Boolean = true) = with(binding) {
        if (place) {
            when (viewModel.trainingPlace) {
                null -> {
                    removeTopCorners(itemOutdoors)
                    removeBottomCorners(itemHome)
                }
                Place.TRAINING_AT_HOME -> {
                    removeBottomCorners(itemGym)
                }
                Place.TRAINING_OUTDOORS -> {
                    removeTopCorners(itemGym)
                }
            }
        }
        if (mode) {
            when (viewModel.breakNotificationMode) {
                null -> {
                    removeTopCorners(itemAnimations)
                    removeBottomCorners(itemSound)
                }
                BreakNotifyingMode.SOUND -> {
                    removeBottomCorners(itemVibration)
                }
                BreakNotifyingMode.ANIMATION -> {
                    removeTopCorners(itemVibration)
                }
            }
        }
    }

    /**
     * Установка необходимых атрибутов View,
     * таких, которые были установлены изначально;
     */
    private fun setRestTimeInitState() {
        if (binding.timeSlider.visibility == View.VISIBLE)
            binding.timeSlider.visibility = View.GONE
        if (binding.inputTimeField.visibility == View.VISIBLE) {
            binding.inputTimeField.text = null
            binding.inputTimeField.visibility = View.GONE
            binding.inputTimeField.clearFocus()
        }
        binding.selectRestTimeLayout.visibility = View.GONE
        binding.selectTimeViaSliderButton.setText(R.string.select_rest_time_via_slider)
        binding.selectTimeManuallyButton.setText(R.string.select_rest_time_manually)
    }

    /**
     * Удаление верхних углов кнопки;
     */
    private fun removeTopCorners(button: MaterialButton) {
        button.shapeAppearanceModel = ShapeAppearanceModel.builder()
            .setTopLeftCorner(CornerFamily.ROUNDED, 10f)
            .setTopRightCorner(CornerFamily.ROUNDED, 10f)
            .setBottomLeftCorner(
                CornerFamily.ROUNDED,
                resources.getDimension(R.dimen.start_dialog_inner_item_corner_size)
            )
            .setBottomRightCorner(
                CornerFamily.ROUNDED,
                resources.getDimension(R.dimen.start_dialog_inner_item_corner_size)
            )
            .build()
    }

    /**
     * Удаление нижних углов кнопки;
     */
    private fun removeBottomCorners(button: MaterialButton) {
        button.shapeAppearanceModel = ShapeAppearanceModel.builder()
            .setBottomLeftCorner(CornerFamily.ROUNDED, 10f)
            .setBottomRightCorner(CornerFamily.ROUNDED, 10f)
            .setTopLeftCorner(
                CornerFamily.ROUNDED,
                resources.getDimension(R.dimen.start_dialog_inner_item_corner_size)
            )
            .setTopRightCorner(
                CornerFamily.ROUNDED,
                resources.getDimension(R.dimen.start_dialog_inner_item_corner_size)
            )
            .build()
    }

    /**
     * Метод вызовет анимацию предупреждения
     * у не заполненных объектов TextView;
     */
    private fun requireAllFieldsCompleted() {
        if (viewModel.trainingPlace == null) {
            binding.selectWorkoutPlace.animateAsError()
        }
        if (viewModel.getSelectedBP(requireContext()).isEmpty()) {
            binding.selectBpsAndMuscles.animateAsError()
        }
        if (viewModel.breakNotificationMode == null) {
            binding.selectNotifyingMode.animateAsError()
        }
        if (viewModel.restTime.value == null) {
            binding.selectRestTime.animateAsError()
        }
    }

    /**
     * Анимация изменения цвета;
     */
    private fun CardView.animateAsError() {
        animateToColor(R.color.md_theme_errorContainer)
    }

    /**
     * Анимация изменения цвета у CardView;
     */
    private fun CardView.animateToColor(colorId: Int) {
        ObjectAnimator.ofInt(
            this,
            "cardBackgroundColor",
            ContextCompat.getColor(requireContext(), R.color.md_theme_primaryContainer),
            ContextCompat.getColor(requireContext(), colorId)
        ).apply {
            interpolator = LinearInterpolator()
            duration = 700
            repeatCount = ValueAnimator.RESTART
            repeatMode = ValueAnimator.REVERSE
            setEvaluator(ArgbEvaluator())
            val aSet = AnimatorSet()
            aSet.play(this)
            aSet.start()
        }
    }

    /**
     * Анимация изменения цвета для успеха;
     */
    private fun CardView.animateAsSuccess() {
        animateToColor(R.color.success_color)
    }

    /**
     * Функция для скрытия клавиатуры;
     */
    private fun View.hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager
        imm.hideSoftInputFromWindow(this.windowToken, 0)
    }

    /**
     * Функция подтверждения ввода с клавиатуры;
     */
    private fun EditText.onDone(callback: () -> Unit) {
        setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                callback.invoke()
                hideKeyboard()
            }
            false
        }
    }

    /**
     * После изменения конфигурации данный метод позволит восстановить информацию на всех View
     */
    private fun recoverData() = with(binding) {
        // Восстановление состояния внутренних элементов
        when (viewModel.trainingPlace) {
            Place.TRAINING_AT_HOME -> itemHome.visibility = View.GONE
            Place.TRAINING_IN_GYM -> itemGym.visibility = View.GONE
            Place.TRAINING_OUTDOORS -> itemOutdoors.visibility = View.GONE
        }
        when (viewModel.breakNotificationMode) {
            BreakNotifyingMode.SOUND -> itemSound.visibility = View.GONE
            BreakNotifyingMode.VIBRATION -> itemVibration.visibility = View.GONE
            BreakNotifyingMode.ANIMATION -> itemAnimations.visibility = View.GONE
        }
        if (viewModel.isSomeBPSelected()) {
            musclesButton.visibility = View.VISIBLE
        }

    }

    /**
     * Проверка данного времени на корректность;
     * В случае правильности/неправильности,
     * соответствующее CardView будет анимировано;
     */
    private fun validateRestTime(checkableTime: Int) {
        if (checkableTime in WorkoutParams.restTimeAdvRange) {
            viewModel.restTime.value = checkableTime
            binding.selectRestTime.animateAsSuccess()
            binding.selectRestTime.collapse(
                binding.selectRestTimeLayout,
                binding.titleSelectRestTime,
                { setRestTimeInitState() }
            )
        } else {
            binding.selectRestTime.animateAsError()
            Toast.makeText(requireContext(), R.string.error_incorrect_time, Toast.LENGTH_SHORT)
                .show()
        }
    }

    /**
     * Анимация изменения текста;
     */
    private fun TextView.animateTextChange(resId: Int, fast: Boolean = false) {
        val duration = if (fast) 50L else 200L
        val anim = AlphaAnimation(1.0f, 0.0f)
        anim.duration = duration
        anim.repeatCount = 1
        anim.repeatMode = Animation.REVERSE

        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationEnd(animation: Animation?) {}
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {
                this@animateTextChange.setText(resId)
            }
        })

        this.startAnimation(anim)
    }


    /**
     * Раскрытие содержимого CardView;
     * @param innerLayout Layout, который будет
     * преобразован из состояния GONE в состояние VISIBLE;
     *
     * @param title Заголовок, который есть в каждом
     * CardView элемента тренировки;
     *
     * @param getAnimation Вернуть или нет анимацию
     * для самостоятельной обработки;
     *
     * @param startHeight Начальная высота, при значении null
     * определяется автоматически от высоты данного CardView;
     *
     * @param endHeight Конечная высота,
     * вычисляется аналогично startHeight;
     */
    private fun CardView.expand(
        innerLayout: ViewGroup,
        title: TextView,
        getAnimation: Boolean = false,
        startHeight: Int? = null,
        endHeight: Int? = null
    ): Animation? {
        val initialHeight: Int = startHeight
            ?: resources.getDimension(R.dimen.start_dialog_cardView_height).toInt()

        innerLayout.measure(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val targetHeight: Int = endHeight
            ?: innerLayout.measuredHeight + initialHeight
        val distanceToExpand = targetHeight - initialHeight
        object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                layoutParams.height = (initialHeight + distanceToExpand * interpolatedTime).toInt()
                requestLayout()
            }

            override fun willChangeBounds() = true

        }.apply {
            duration = 128L
            if (getAnimation)
                return this
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(a: Animation?) {
                    title.visibility = View.GONE
                    innerLayout.visibility = View.GONE
                }

                override fun onAnimationEnd(a: Animation?) {
                    TransitionManager.beginDelayedTransition(this@expand, fadeAnim)
                    innerLayout.visibility = View.VISIBLE
                    title.visibility = View.VISIBLE
                }

                override fun onAnimationRepeat(a: Animation?) {}

            })
            startAnimation(this)
        }
        return null
    }

    /**
     * Сокрытие содержимого CardView
     * с применением анимации;
     *
     * @param innerLayout Layout, который будет
     * преобразован из состояния VISIBLE в состояние GONE;
     *
     * @param title Заголовок, который есть в каждом
     * CardView элемента тренировки;
     *
     * @param doOnEnd Команды, которые
     * выполнятся в момент конца анимации;
     *
     * @param getAnimation Вернуть или нет анимацию
     * для самостоятельной обработки;
     */
    private fun CardView.collapse(
        innerLayout: ViewGroup,
        title: TextView,
        doOnEnd: (() -> Unit)? = null,
        getAnimation: Boolean = false
    ): Animation? {
        val initialHeight: Int = measuredHeight
        val distanceToCollapse =
            initialHeight - resources.getDimension(R.dimen.start_dialog_cardView_height)
        object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                layoutParams.height =
                    (initialHeight - distanceToCollapse * interpolatedTime).toInt()
                requestLayout()
            }
            override fun willChangeBounds() = true
        }.apply {
            duration = 256L
            if (getAnimation) return this
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(a: Animation?) {
                    TransitionManager.beginDelayedTransition(this@collapse, fadeAnim)
                    innerLayout.visibility = View.GONE
                    title.visibility = View.GONE
                }

                override fun onAnimationEnd(a: Animation?) {
                    TransitionManager.beginDelayedTransition(this@collapse, fadeAnim)
                    title.visibility = View.VISIBLE
                    doOnEnd?.invoke()
                }

                override fun onAnimationRepeat(p0: Animation?) {}

            })
            startAnimation(this)
        }
        return null
    }

    /**
     * Особенное раскрытие содержимого
     * для элемента выбора времени;
     *
     * @param show View, которое будет
     * показано перед началом анимации;
     *
     * @param hide View, которое будет
     * сокрыто перед началом анимации;
     */
    private fun CardView.expandRestTimeItemTo(show: View, hide: View) {
        fun expand() {
            expand(
                binding.selectRestTimeLayout,
                binding.titleSelectRestTime,
                getAnimation = true,
                binding.selectRestTime.height
            )!!.apply {
                setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(a: Animation?) {
                        hide.visibility = View.GONE
                        binding.selectRestTimeLayout.visibility = View.GONE
                        binding.titleSelectRestTime.visibility = View.GONE
                    }

                    override fun onAnimationEnd(a: Animation?) {
                        show.visibility = View.VISIBLE
                        binding.selectRestTimeLayout.visibility = View.VISIBLE
                        binding.titleSelectRestTime.visibility = View.VISIBLE
                    }

                    override fun onAnimationRepeat(a: Animation?) {}
                })
                startAnimation(this)
            }
        }
        if (hide.isVisible) {
            collapse(
                binding.selectRestTimeLayout,
                binding.titleSelectRestTime,
                getAnimation = true,
            )!!.apply {
                setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(p0: Animation?) {
                        binding.selectRestTimeLayout.visibility = View.GONE
                        binding.titleSelectRestTime.visibility = View.GONE
                    }

                    override fun onAnimationEnd(p0: Animation?) {
                        expand()
                    }

                    override fun onAnimationRepeat(p0: Animation?) {}
                })
                startAnimation(this)
            }
        } else expand()
    }

    /*
    Интерфейс, необходимый для отправки данных в хост Activity;
     */
    interface Callback {
        fun workoutStarted(success: Boolean)
    }
}

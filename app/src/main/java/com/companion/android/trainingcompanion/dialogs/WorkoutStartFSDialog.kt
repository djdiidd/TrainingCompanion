package com.companion.android.trainingcompanion.dialogs


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
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import com.companion.android.trainingcompanion.R
import com.companion.android.trainingcompanion.activities.TAG_MAIN_FRAGMENT
import com.companion.android.trainingcompanion.databinding.DialogStartWorkoutBinding
import com.companion.android.trainingcompanion.objects.BreakNotifyingMode
import com.companion.android.trainingcompanion.objects.Params
import com.companion.android.trainingcompanion.objects.Place
import com.companion.android.trainingcompanion.viewmodels.WorkoutViewModel
import com.google.android.material.slider.Slider


// Ключ для слушателя получения результата
const val SELECT_BODY_PART_DIALOG = "select-body-part-dialog"

// Ключ для слушателя получения результата
const val SELECT_MUSCLE_DIALOG = "select-muscle-dialog"

// Тег для передачи списка выбранных объектов из диалога
const val LIST_BUNDLE_TAG = "list-bundle-tag"

/**
 * Диалоговое окно, занимающее весь экран, для выбора параметров перед тренировкой
 * Будет запускать необходимые диалоговые окна
 */
class WorkoutStartFSDialog : DialogFragment() {
    // ViewModel для сохранения необходимых данных, выбранных пользователем
    private val viewModel: WorkoutViewModel by activityViewModels()

    // Инициализация объекта класса привязки данных
    private lateinit var binding: DialogStartWorkoutBinding

    var callback: Callback? = null

    // TODO: add to notion
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
        binding = DataBindingUtil  // определяем привязку данных
            .inflate(layoutInflater, R.layout.dialog_start_workout, container, false)
        if (savedInstanceState != null) {
            recoverData()
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        callback =
            parentFragmentManager.findFragmentByTag(TAG_MAIN_FRAGMENT) as Callback
        // Проверяем на нажатие кнопки назад, после которой закроем окно
        dialog?.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                //TODO: start alert dialog
                viewModel.clearAllData()
                dialog?.dismiss()
            }
            false
        }

        /** Обработка нажатий на элементы выбора параметров тренировки */


        fun handlePlaceItemClick(item: Button, place: Int) {
            binding.selectWorkoutPlace.animateAsSuccess()
            viewModel.trainingPlace = place
            item.visibility = View.GONE
            binding.placeItemsLayout.visibility = View.GONE
            when (item) {
                binding.itemHome -> {
                    if (!binding.itemGym.isVisible)
                        binding.itemGym.visibility = View.VISIBLE
                    if (!binding.itemOutdoors.isVisible)
                        binding.itemOutdoors.visibility = View.VISIBLE
                }
                binding.itemGym -> {
                    if (!binding.itemHome.isVisible)
                        binding.itemHome.visibility = View.VISIBLE
                    if (!binding.itemOutdoors.isVisible)
                        binding.itemOutdoors.visibility = View.VISIBLE
                }
                binding.itemOutdoors -> {
                    if (!binding.itemHome.isVisible)
                        binding.itemHome.visibility = View.VISIBLE
                    if (!binding.itemGym.isVisible)
                        binding.itemGym.visibility = View.VISIBLE
                }
            }
        }

        // Инициализация поведения кнопки выбора места тренировки:
        binding.selectWorkoutPlace.setOnClickListener {
            binding.apply {
                if (placeItemsLayout.visibility == View.VISIBLE)
                    placeItemsLayout.visibility = View.GONE
                else
                    placeItemsLayout.visibility = View.VISIBLE

                itemHome.setOnClickListener {
                    handlePlaceItemClick(it as Button, Place.TRAINING_AT_HOME)
                }
                itemGym.setOnClickListener {
                    handlePlaceItemClick(it as Button, Place.TRAINING_IN_GYM)
                }
                itemOutdoors.setOnClickListener {
                    handlePlaceItemClick(it as Button, Place.TRAINING_OUTDOORS)
                }
                it.hideKeyboard()
            }
        }

        // Инициализация поведения кнопки выбора режима уведомления:
        binding.selectNotifyingMode.setOnClickListener {
            binding.apply {
                notifyingItemsLayout.visibility =
                    if (notifyingItemsLayout.visibility == View.VISIBLE) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }
                itemSound.setOnClickListener {
                    binding.selectNotifyingMode.animateAsSuccess()
                    viewModel.breakNotificationMode = BreakNotifyingMode.SOUND
                    notifyingItemsLayout.visibility = View.GONE
                    it.visibility = View.GONE
                    if (itemVibration.visibility == View.GONE)
                        itemVibration.visibility = View.VISIBLE
                    if (itemAnimations.visibility == View.GONE)
                        itemAnimations.visibility = View.VISIBLE
                }
                itemVibration.setOnClickListener {
                    binding.selectNotifyingMode.animateAsSuccess()
                    viewModel.breakNotificationMode = BreakNotifyingMode.VIBRATION
                    notifyingItemsLayout.visibility = View.GONE
                    it.visibility = View.GONE
                    if (itemSound.visibility == View.GONE)
                        itemSound.visibility = View.VISIBLE
                    if (itemAnimations.visibility == View.GONE)
                        itemAnimations.visibility = View.VISIBLE
                }
                itemAnimations.setOnClickListener {
                    binding.selectNotifyingMode.animateAsSuccess()
                    viewModel.breakNotificationMode = BreakNotifyingMode.ANIMATION
                    notifyingItemsLayout.visibility = View.GONE
                    it.visibility = View.GONE
                    if (itemVibration.visibility == View.GONE)
                        itemVibration.visibility = View.VISIBLE
                    if (itemSound.visibility == View.GONE)
                        itemSound.visibility = View.VISIBLE
                }
            }
            it.hideKeyboard()
        }


        // Инициализация поведения кнопки выбора частей тела и мышц:
        binding.selectBpsAndMuscles.setOnClickListener {
            binding.bpsOrMusclesButtons.apply {
                visibility = if (visibility == View.VISIBLE) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
            }
        }

        binding.selectRestTime.setOnClickListener {
            if (binding.openableLayoutSelectRestTime.visibility == View.VISIBLE) {
                setRestTimeInitState()
            } else {
                binding.openableLayoutSelectRestTime.visibility = View.VISIBLE
            }
        }

        // Инициализация поведения вложенных элементов:
        binding.bodyPartsButton.setOnClickListener {

            // Устанавливаем слушатель для получения списка выбранных частей тела
            // По ключу SELECT_BODY_PART_DIALOG_TAG
            setFragmentResultListener(SELECT_BODY_PART_DIALOG) { _, bundle ->
                // Уберем предупреждение, если пользователь пытался начать
                // тренировку без выбора тренируемой части тела
                // TODO:remove error;
                // binding.bodyPartSelector.removeError()
                // В переменную numbersOfSelectedBodyParts записываем arrayList
                // полученный из объекта Bundle по ключу BODY_PART_LIST_KEY
                val whichBPIsSelected = bundle.getBooleanArray(LIST_BUNDLE_TAG)

                // Если полученный список не изменился, то перезаписывать данные не будем
                if (!viewModel.getWhichBPsAreSelected().toBooleanArray()
                        .contentEquals(whichBPIsSelected)
//                    || binding.bodyPartSelector.text.isEmpty() //TODO: include
                ) {
                    viewModel.updateData(requireContext(), whichBPIsSelected!!.toTypedArray())
//                    viewModel.getSelectedBP(requireContext())
//                        .contentToString().dropLast(1).drop(1).also {
//                            if (it.split(" ").size != 5) {
//                                binding.bodyPartSelector.text =
//                                    getString(
//                                        R.string.train_on, it
//                                    )
//                            } else {
//                                binding.bodyPartSelector.text =
//                                    getString(
//                                        R.string.train_on, getString(R.string.full_body)
//                                    )
//                            }
//                        }
//                    var count: Short = 0
//                    viewModel.getWhichMusclesAreSelected().forEach { if (it) count++ }
//
//                    if (viewModel.isSomeMuscleSelected())
//                        binding.musclesSelector.text = getString(
//                            R.string.number_of_selected_el,
//                            count
//                        )
//                    else binding.musclesSelector.text = null
//
                }
                // Отображаем вторую кнопку и корректируем
                // местоположение кнопки частей тела
                binding.musclesButton.visibility = View.VISIBLE

                binding.bodyPartsButton.layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT
                ).also {
                    it.setMargins(0, 0, 8, 0)
                }

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
                binding.bpsOrMusclesButtons.visibility = View.GONE
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

        binding.selectTimeViaSliderButton.setOnClickListener {
            if (binding.timeSlider.visibility == View.VISIBLE) {
                binding.selectRestTime.animateAsSuccess()
                viewModel.restTime.value = binding.timeSlider.value.toInt()
                setRestTimeInitState()
            } else {
                binding.timeSlider.visibility = View.VISIBLE
                binding.inputTimeField.visibility = View.GONE
                if (!binding.inputTimeField.text.isNullOrEmpty()) {
                    binding.selectTimeManuallyButton.animateTextChange(
                        R.string.select_rest_time_manually, true
                    )
                }
            }
        }
        var startedInput = false
        binding.selectTimeManuallyButton.setOnClickListener {
            if (binding.inputTimeField.visibility == View.VISIBLE) {
                validateRestTime(binding.inputTimeField.text.toString().toInt())
                startedInput = false
            } else {
                binding.inputTimeField.visibility = View.VISIBLE
                binding.timeSlider.visibility = View.GONE
                if (!binding.inputTimeField.text.isNullOrEmpty()) {
                    binding.selectTimeManuallyButton.setText(R.string.button_dialog_accept)
                }
                if (binding.selectTimeViaSliderButton.text.toString()
                    == getString(R.string.button_dialog_accept)
                ) {
                    binding.selectTimeViaSliderButton.animateTextChange(
                        R.string.select_rest_time_via_slider, true
                    )
                }
            }
        }

        binding.inputTimeField.onDone {
            startedInput = false
            validateRestTime(binding.inputTimeField.text.toString().toInt())
        }

        binding.timeSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}

            override fun onStopTrackingTouch(slider: Slider) {
                // Сохранить выбранное значение
                binding.selectTimeViaSliderButton.animateTextChange(
                    R.string.button_dialog_accept
                )
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
        callback = null
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

    private fun setRestTimeInitState() {
        binding.openableLayoutSelectRestTime.visibility = View.GONE
        binding.selectTimeViaSliderButton.setText(R.string.select_rest_time_via_slider)
        binding.selectTimeManuallyButton.setText(R.string.select_rest_time_manually)
        if (binding.timeSlider.visibility == View.VISIBLE)
            binding.timeSlider.visibility = View.GONE
        if (binding.inputTimeField.visibility == View.VISIBLE) {
            binding.inputTimeField.text = null
            binding.inputTimeField.visibility = View.GONE
            binding.inputTimeField.clearFocus()
        }
    }
//    private fun closeItems() = with(binding) {
//        placeItemsLayout.visibility = View.GONE
//        notifyingItemsLayout.visibility = View.GONE
//        bpsOrMusclesButtons.visibility = View.GONE
//        openableLayoutSelectRestTime.visibility = View.GONE
//    }

    /**
     * Метод вызовет error у не заполненных объектов TextView
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
     * Анимация изменения цвета
     */
    private fun CardView.animateAsError() {
        animateToColor(R.color.md_theme_errorContainer)
    }

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

    private fun CardView.animateAsSuccess() {
        animateToColor(R.color.success_color)
    }


    private fun View.hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager
        imm.hideSoftInputFromWindow(this.windowToken, 0)
    }

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
            binding.bodyPartsButton.layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.setMargins(0, 0, 8, 0)
            }
        }

    }

    private fun validateRestTime(checkableTime: Int) {
        if (checkableTime in Params.restTimeAdvRange) {
            viewModel.restTime.value = checkableTime
            binding.selectRestTime.animateAsSuccess()
            setRestTimeInitState()
        } else {
            binding.selectRestTime.animateAsError()
            Toast.makeText(requireContext(), R.string.error_incorrect_time, Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun TextView.animateTextChange(resId: Int, fast: Boolean = false) {
        val duration = if (fast) 100L else 200L
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

    interface Callback {
        fun workoutStarted(success: Boolean)
    }
}
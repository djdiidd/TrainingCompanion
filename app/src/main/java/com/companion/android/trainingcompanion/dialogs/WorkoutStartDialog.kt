package com.companion.android.trainingcompanion.dialogs

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import com.companion.android.trainingcompanion.R
import com.companion.android.trainingcompanion.adapters.PlaceSpinnerAdapter
import com.companion.android.trainingcompanion.databinding.DialogWorkoutStartBinding
import com.companion.android.trainingcompanion.objects.BreakNotificationMode
import com.companion.android.trainingcompanion.objects.Params
import com.companion.android.trainingcompanion.objects.Place
import com.companion.android.trainingcompanion.viewmodels.WorkoutViewModel
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider


// Ключ для слушателя получения результата
const val SELECT_BODY_PART_DIALOG = "select-body-part-dialog"

// Ключ для слушателя получения результата
const val SELECT_MUSCLE_DIALOG = "select-muscle-dialog"

// Тег для передачи списка выбранных объектов из диалога
const val LIST_BUNDLE_TAG = "list-bundle-tag"

/**
 * Окно для выбора параметров перед тренировкой
 * Будет запускать необходимые диалоговые окна
 */
class WorkoutStartDialog
    : DialogFragment(), View.OnClickListener, AdapterView.OnItemSelectedListener {

    // ViewModel для сохранения необходимых данных, выбранных пользователем
    private val viewModel: WorkoutViewModel by activityViewModels()

    // Инициализация объекта класса привязки данных
    private lateinit var binding: DialogWorkoutStartBinding

    // Инициализация Слушателя Нажатий Для Слайдера (времени)
    private lateinit var sliderTouchListener: Slider.OnSliderTouchListener

    // Инициализация слушателей
    private lateinit var muscleLongTouchListener: View.OnLongClickListener
    private lateinit var timeLabelFormatter: LabelFormatter


    /**
     * Этап создания диалогового окна
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ОПРЕДЕЛЕНИЕ ПОВЕДЕНИЯ СЛУШАТЕЛЯ ДЛЯ СЛАЙДЕРА С ВРЕМЕНЕМ ОТДЫХА
        sliderTouchListener = object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                // Уберем предупреждение, если пользователь пытался начать
                // тренировку без выбора времени тренировки
                binding.timeSliderDescription.removeError()
                hideTextInput()
            }

            override fun onStopTrackingTouch(slider: Slider) {
                // Сохранить выбранное значение
                viewModel.restTime.value = slider.value.toInt()
                binding.timeSliderValue.text =
                    getString(R.string.selected_seconds, viewModel.restTime.value!!)
            }
        }
        // ОПРЕДЕЛЕНИЕ ВИДА ОТОБРАЖЕНИЯ ТЕКСТА НА СЛАЙДЕРЕ ВЫБОРА ВРЕМЕНИ
        timeLabelFormatter = LabelFormatter { value ->
            if (value % 60f == 0f) "${value.toInt() / 60} min"
            else "${value.toInt()} sec"
        }
        // ОПРЕДЕЛЕНИЕ ПОВЕДЕНИЯ СЛУШАТЕЛЯ ДЛЯ View С ВЫБОРОМ МЫШЦ
        muscleLongTouchListener = View.OnLongClickListener {
            if (viewModel.getWhichMusclesAreSelected().contains(true)) {
                Toast.makeText(
                    requireContext(),
                    getString(
                        R.string.toast_amount_of_selected_mscls,
                        viewModel.getSelectedMuscles(requireContext())
                            .contentToString()
                            .drop(1)
                            .dropLast(1)
                    ),
                    Toast.LENGTH_LONG
                ).show()
                true
            } else false
        }
    }

    /**
     * Этап создания View во фрагменте
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil  // определяем привязку данных
            .inflate(layoutInflater, R.layout.dialog_workout_start, container, false)
        binding.viewModel = viewModel // Определение viewModel 'и

        // Скругление углов (отображение прозрачного фона за диалоговым окном)
        if (dialog != null && dialog?.window != null) {
            dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        }
        binding.timeSliderDescription.text = getString(R.string.description_time_slider)

        // Переменная из файла разметки для работы с видимостью
        binding.musclesIsVisible = binding.bodyPartSelector.text.isNotEmpty()

        // Устанавливаем спиннер с выбором места тренировок
        setUpSpinners()

        binding.apply {
            buttonAccept.setOnClickListener(this@WorkoutStartDialog)     // Слушатель нажатий для кнопки начала тр-ки
            buttonCancel.setOnClickListener(this@WorkoutStartDialog)     // Слушатель нажатий для кнопки отмены тр-ки
            bodyPartSelector.setOnClickListener(this@WorkoutStartDialog) // Слушатель диалога с выбором части тела
            musclesSelector.setOnClickListener(this@WorkoutStartDialog)  // Слушатель диалога с выбором мышц
            musclesSelector.setOnLongClickListener(muscleLongTouchListener)
            timeSlider.addOnSliderTouchListener(sliderTouchListener)
            timeSlider.setLabelFormatter(timeLabelFormatter)
        }
        // Если конфигурация менялась, то восстанавливаем данные
        if (savedInstanceState != null) {
            recoverData()
        }
        return binding.root
    }

    /**
     * Метод onStart жизненного цикла фрагмента
     */
    override fun onStart() {
        super.onStart()

        setDialogSize()

        // Анимация
        setWindowAnimation(R.style.BounceAnimation)

        binding.inputTimeManuallyLl.setOnClickListener {

            expandTextInput()

            it.focusAndShowKeyboard()

            binding.timeSlider.toStartPosition()

            binding.inputTimeField.onDone {
                val seconds = binding.inputTimeField.text.toString().toInt()
                if (seconds in Params.restTimeAdvRange) {
                    hideTextInputAndSaveData(seconds)
                    binding.inputTimeField.removeError()
                } else
                    binding.inputTimeField.addError(
                        R.string.error_incorrect_time,
                        R.drawable.ic_error
                    )
            }
        }

        binding.placeSpinner.setOnTouchListener { view, _ ->
            view.performClick()
            hideTextInput()
            return@setOnTouchListener true
        }


        binding.notificationSpinner.setOnTouchListener { view, _ ->
            view.performClick()
            hideTextInput()
            return@setOnTouchListener true
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("1", true)
    }

    /* Интерфейс */
    /**
     * Определение действий по нажатию на View, находящихся на
     * диалоговом окне с выбором предтренировочных характеристик.
     */
    override fun onClick(v: View?) {
        when (v?.id) {
            /** Кнопка подтверждения -- проверка и сохранение данных*/
            binding.buttonAccept.id -> {
                // Если обязательные поля заполнены:
                if (requiredFieldsCompleted()) {
                    if (viewModel.getNumberOfSelectedMuscles() != 0.toShort()) {
                        val unused = viewModel.getWhichBPAreUnused()
                        if (unused.contains(true)) {
                            WarningUnusedBPDialog(unused).show(parentFragmentManager, "")
                        }
                    }
                    viewModel.workoutSuccessfullyStarted.value = true
                    // Сохранить все значения в бд
//------------------НЕОБХОДИМО ПРОВЕРИТЬ НА НАЧАЛО ТРЕНИРОВКИ (после кнопки accept) ИНАЧЕ ПОЯВИТСЯ ВЫБОР МЫШЦ
                    dismiss()
                }
                // Если не все обязательные поля заполнены
                else {
                    requireAllFieldsCompleted()
                }
            }
            /** Кнопка отмены -- закрытие окна и удаление данных*/
            binding.buttonCancel.id -> {
                viewModel.clearAllData()
                dismiss()
            }
            /** Выбор тренеруемых частей тела -- вызов соответствующего окна и обработка данных */
            binding.bodyPartSelector.id -> {
                // Если открыт выбор времени вручную, то закроем
                if (textInputIsExpanded()) {
                    hideTextInput()
                }
                // Устанавливаем слушатель для получения списка выбранных частей тела
                // По ключу SELECT_BODY_PART_DIALOG_TAG
                setFragmentResultListener(SELECT_BODY_PART_DIALOG) { _, bundle ->
                    // Уберем предупреждение, если пользователь пытался начать
                    // тренировку без выбора тренируемой части тела
                    binding.bodyPartSelector.removeError()
                    // В переменную numbersOfSelectedBodyParts записываем arrayList
                    // полученный из объекта Bundle по ключу BODY_PART_LIST_KEY
                    val whichBPIsSelected = bundle.getBooleanArray(LIST_BUNDLE_TAG)

                    // Если полученный список не изменился, то перезаписывать данные не будем
                    if (!viewModel.getWhichBPsAreSelected().toBooleanArray()
                            .contentEquals(whichBPIsSelected)
                        || binding.bodyPartSelector.text.isEmpty()
                    ) {
                        viewModel.updateData(requireContext(), whichBPIsSelected!!.toTypedArray())
                        binding.bodyPartSelector.text =
                            getString(
                                R.string.train_on,
                                viewModel.getSelectedBP(requireContext())
                                    .contentToString().dropLast(1).drop(1)
                            )
                        var count: Short = 0
                        viewModel.getWhichMusclesAreSelected().forEach { if (it) count++ }

                        if (viewModel.isSomeMuscleSelected())
                            binding.musclesSelector.text = getString(
                                R.string.number_of_selected_el,
                                count
                            )
                        else binding.musclesSelector.text = null
                        // После выбора списка тренеруемых частей тела
                        // станет доступен выбор мышц
                        binding.musclesIsVisible = true
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
            /** Выбор мышц для выбранных частей тела -- вызов соответствующего окна и обработка */
            binding.musclesSelector.id -> {
                // Если открыт выбор времени вручную, то закроем
                if (textInputIsExpanded()) {
                    hideTextInput()
                }
                // Устанавливаем слушатель для получения списка мышц по ключу SELECT_MUSCLE_DIALOG.
                // Список мышц определяется в зависимости от выбранных частей тела
                setFragmentResultListener(SELECT_MUSCLE_DIALOG) { _, bundle ->
                    val whichMuscleIsSelected = bundle.getBooleanArray(LIST_BUNDLE_TAG)
                    viewModel.saveSelectedMuscles(whichMuscleIsSelected!!.toTypedArray())

                    var count: Short = 0
                    whichMuscleIsSelected.forEach { if (it) count++ }

                    if (viewModel.isSomeMuscleSelected())
                        binding.musclesSelector.text = getString(
                            R.string.number_of_selected_el,
                            count
                        )
                    else binding.musclesSelector.text = null
                }
                if (viewModel.getAllBP(requireContext()).isEmpty())
                    throw Exception("allBodyParts is empty now")
                if (viewModel.getWhichBPsAreSelected().isNullOrEmpty())
                    throw Exception("viewModel.boolBodyPartSelected.value is null or empty now")
                // Запуск диалогового окна с выбором мышц
                if (parentFragmentManager.findFragmentByTag(SELECT_MUSCLE_DIALOG) == null) {
                    MultiChoiceDialog(
                        viewModel.getAvailableMuscles(requireContext()),
                        viewModel.getWhichMusclesAreSelected().toBooleanArray()
                    ).show(parentFragmentManager, SELECT_MUSCLE_DIALOG)
                }
            }
        }
    }

    private fun setDialogSize() {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        dialog?.window?.setLayout(6 * width / 7, 4 * height / 5)
    }

    private fun setWindowAnimation(res: Int) {
        dialog?.window?.setWindowAnimations(res)
    }

    /**
     * Метод для создания спиннера, с загруженными данными из ресурсов;
     * Данный массив из ресурсов содержит места для тренировок
     */
    private fun setUpSpinners() {
        val placeAdapter = PlaceSpinnerAdapter(requireContext(), Place.getList(requireContext()))
        val notificationAdapter =
            PlaceSpinnerAdapter(requireContext(), BreakNotificationMode.getList(requireContext()))
        binding.placeSpinner.adapter = placeAdapter
        binding.placeSpinner.onItemSelectedListener = this
        binding.notificationSpinner.adapter = notificationAdapter
        binding.notificationSpinner.onItemSelectedListener = this
    }

    /** ИНТЕРФЕЙС (spinner)
     * Определеяем действия по выбору объекта из списка spinner
     */
    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        if (parent == binding.placeSpinner) {
            when (pos) {
                0 -> viewModel.trainingPlace = Place.TRAINING_AT_HOME
                1 -> viewModel.trainingPlace = Place.TRAINING_IN_GYM
                2 -> viewModel.trainingPlace = Place.TRAINING_OUTDOORS
            }
        }
        else {
            when (pos) {
                0 -> viewModel.breakNotificationMode = BreakNotificationMode.SOUND
                1 -> viewModel.breakNotificationMode = BreakNotificationMode.VIBRATION
                2 -> viewModel.breakNotificationMode = BreakNotificationMode.ANIMATION
            }
        }
    }

    /** ИНТЕРФЕЙС (spinner)
     * Определеляем действия по отстутствию выбора какого-либо объекта в spinner
     */
    override fun onNothingSelected(parent: AdapterView<*>) {}

    /**
     * Метод предназначен для проверки на заполненность обязательных полей
     * В случае удачи (все нужные поля заполнены) вернется true
     * В случае если не все обязательные поля заполнены, вернется false
     */
    private fun requiredFieldsCompleted(): Boolean {
        return viewModel.getSelectedBP(requireContext()).isNotEmpty()
    }

    /**
     * Метод вызовет error у не заполненных объектов TextView
     */
    private fun requireAllFieldsCompleted() = with(binding) {
        if (viewModel?.getSelectedBP(requireContext())!!.isEmpty()) {
            binding.bodyPartSelector.addError(R.string.error_body_parts, R.drawable.ic_error)
        }
    }

    /**
     * Если пользователь нажав кнопку подтверждения не заполнил все обязательные поля,
     * то на них появится предупреждение (error). Данный метод позволяет отменить предупреждение.
     */
    private fun TextView.removeError() {
        if (error != null) error = null
    }

    /**
     * После изменения конфигурации данный метод позволит восстановить информацию на всех View
     */
    private fun recoverData() {
        // Если список с частями тела не пуст, то восстановим
        if (viewModel.isSomeBPSelected()) {
            binding.bodyPartSelector.text =
                getString(
                    R.string.train_on,
                    viewModel.getSelectedBP(requireContext())
                        .contentToString().dropLast(1).drop(1)
                )
            // Если список с мышцами не пуст, то восстановим
            if (viewModel.isSomeMuscleSelected()) {
                binding.musclesSelector.text =
                    getString(
                        R.string.number_of_selected_el,
                        viewModel.getNumberOfSelectedMuscles()
                    )
                binding.musclesIsVisible = true
            }
        }
        binding.timeSliderValue.text =
            getString(R.string.selected_seconds, viewModel.restTime.value!!)

    }

    private fun TextView.addError(stringRes: Int, drawableRes: Int) {
        val drawable = AppCompatResources.getDrawable(requireContext(), drawableRes)
        drawable?.setBounds(0, 0, textSize.toInt() + 15, textSize.toInt() + 15)
        setError(getString(stringRes), drawable)
    }

    private fun View.focusAndShowKeyboard() {
        /**
         * This is to be called when the window already has focus.
         */
        fun View.showTheKeyboardNow() {
            if (isFocused) {
                post {
                    // We still post the call, just in case we are being notified of the windows focus
                    // but InputMethodManager didn't get properly setup yet.
                    val imm =
                        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                }
            }
        }

        requestFocus()
        if (hasWindowFocus()) {
            // No need to wait for the window to get focus.
            showTheKeyboardNow()
        } else {
            // We need to wait until the window gets focus.
            viewTreeObserver.addOnWindowFocusChangeListener(
                object : ViewTreeObserver.OnWindowFocusChangeListener {
                    override fun onWindowFocusChanged(hasFocus: Boolean) {
                        // This notification will arrive just before the InputMethodManager gets set up.
                        if (hasFocus) {
                            this@focusAndShowKeyboard.showTheKeyboardNow()
                            // It’s very important to remove this listener once we are done.
                            viewTreeObserver.removeOnWindowFocusChangeListener(this)
                        }
                    }
                })
        }
    }

    private fun View.hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager
        imm.hideSoftInputFromWindow(this.windowToken, 0)
    }

    private fun expandTextInput() {
        with(binding) {
            timeSliderValue.visibility = View.GONE
            keyboardImage.visibility = View.GONE
            inputTimeField.visibility = View.VISIBLE
            inputTimeField.setText(viewModel?.restTime?.value!!.toString())
            space.layoutParams = LinearLayout.LayoutParams(
                0, space.height, 0f
            )
            inputTimeManuallyLl.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
        }
    }

    private fun textInputIsExpanded() = binding.inputTimeField.isVisible

    private fun hideTextInput() {
        with(binding) {
            inputTimeField.visibility = View.GONE
            timeSliderValue.visibility = View.VISIBLE
            keyboardImage.visibility = View.VISIBLE
            space.layoutParams = LinearLayout.LayoutParams(
                0, space.height, 1f
            )
            inputTimeManuallyLl.layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
            binding.root.hideKeyboard()
        }
    }

    private fun hideTextInputAndSaveData(seconds: Int) {
        with(binding) {
            viewModel?.restTime?.value = seconds.also {
                timeSliderValue.text = getString(R.string.selected_seconds, it)
            }
            hideTextInput()
            timeSliderValue.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT

            if (seconds in Params.restTimeDefaultRange) {
                if (seconds % 5 == 0)
                    binding.timeSlider.value = seconds.toFloat()
                else
                    binding.timeSlider.value = seconds.toFloat() - seconds % 5
            }
        }
    }

    private fun Slider.toStartPosition() {
        this.value = this.valueFrom
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

}

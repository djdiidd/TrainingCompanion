package com.companion.android.trainingcompanion.dialogs


import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import com.companion.android.trainingcompanion.R
import com.companion.android.trainingcompanion.activities.TAG_MAIN_FRAGMENT
import com.companion.android.trainingcompanion.databinding.DialogStartWorkoutBinding
import com.companion.android.trainingcompanion.objects.BreakNotificationMode
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

        // Инициализация поведения кнопки выбора места тренировки:
        binding.selectWorkoutPlace.setOnClickListener {
            binding.apply {

                if (placeItemsLayout.visibility == View.VISIBLE)
                    placeItemsLayout.visibility = View.GONE
                else {
                    placeItemsLayout.visibility = View.VISIBLE
                }
                itemHome.setOnClickListener {
                    viewModel.trainingPlace = Place.TRAINING_AT_HOME
                    placeItemsLayout.visibility = View.GONE
                }
                itemGym.setOnClickListener {
                    viewModel.trainingPlace = Place.TRAINING_IN_GYM
                    placeItemsLayout.visibility = View.GONE
                }
                itemOutdoors.setOnClickListener {
                    viewModel.trainingPlace = Place.TRAINING_OUTDOORS
                    placeItemsLayout.visibility = View.GONE
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
                    viewModel.breakNotificationMode = BreakNotificationMode.SOUND
                    notifyingItemsLayout.visibility = View.GONE
                }
                itemVibration.setOnClickListener {
                    viewModel.breakNotificationMode = BreakNotificationMode.VIBRATION
                    notifyingItemsLayout.visibility = View.GONE
                }
                itemAnimations.setOnClickListener {
                    viewModel.breakNotificationMode = BreakNotificationMode.ANIMATION
                    notifyingItemsLayout.visibility = View.GONE
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
            binding.openableLayoutSelectRestTime.apply {
                visibility = if (visibility == View.VISIBLE) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
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
                var count: Short = 0
                whichMuscleIsSelected.forEach { if (it) count++ }

//                if (viewModel.isSomeMuscleSelected())
//                    binding.musclesSelector.text = getString(
//                        R.string.number_of_selected_el,
//                        count
//                    )
//                else binding.musclesSelector.text = null
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
            binding.timeSlider.visibility = View.VISIBLE
            binding.inputTimeField.visibility = View.GONE
        }

        binding.selectTimeManuallyButton.setOnClickListener {
            binding.inputTimeField.visibility = View.VISIBLE
            binding.timeSlider.visibility = View.GONE
            binding.inputTimeField.onDone {
                val seconds = binding.inputTimeField.text.toString().toInt()
                if (seconds in Params.restTimeAdvRange) {
                    viewModel.restTime.value = seconds
                    binding.inputTimeField.text = null
                    binding.inputTimeField.visibility = View.GONE
                    binding.inputTimeField.clearFocus()
                    //binding.inputTimeField.removeError()//TODO
                } else {
                }
                //TODO
//                    binding.inputTimeField.addError(
//                        R.string.error_incorrect_time,
//                        R.drawable.ic_error
//                    )

            }
        }

        binding.timeSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                // Уберем предупреждение, если пользователь пытался начать
                // тренировку без выбора времени тренировки
                //TODO//binding.timeSliderDescription.removeError()
            }

            override fun onStopTrackingTouch(slider: Slider) {
                // Сохранить выбранное значение
                viewModel.restTime.value = slider.value.toInt()
//                    TODO;
                //                     binding.timeSliderValue.text =
//                        getString(R.string.selected_seconds, viewModel.restTime.value!!)
            }
        }
        )

        binding.inputTimeField.onFocusChangeListener =
            View.OnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    v.hideKeyboard()
                }
            }

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
        val a: ObjectAnimator = ObjectAnimator.ofInt(
            this,
            "cardBackgroundColor",
            ContextCompat.getColor(requireContext(), R.color.md_theme_primaryContainer),
            ContextCompat.getColor(requireContext(), R.color.md_theme_secondaryContainer)
        )
        a.interpolator = LinearInterpolator()
        a.duration = 800
        a.repeatCount = ValueAnimator.RESTART
        a.repeatMode = ValueAnimator.REVERSE
        a.setEvaluator(ArgbEvaluator())
        val t = AnimatorSet()
        t.play(a)
        t.start()
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
        // Если список с частями тела не пуст, то восстановим
//        if (viewModel.isSomeBPSelected()) {
//            bodyPartSelector.text =
//                getString(
//                    R.string.train_on,
//                    viewModel.getSelectedBP(requireContext())
//                        .contentToString().dropLast(1).drop(1)
//                )
//            // Если список с мышцами не пуст, то восстановим
//            if (viewModel.isSomeMuscleSelected()) {
//                musclesSelector.text =
//                    getString(
//                        R.string.number_of_selected_el,
//                        viewModel.getNumberOfSelectedMuscles()
//                    )
//                musclesIsVisible = true
//            }
//        }
//        timeSliderValue.text =
//            getString(R.string.selected_seconds, viewModel.restTime.value!!)
    }

    interface Callback {
        fun workoutStarted(success: Boolean)
    }
}
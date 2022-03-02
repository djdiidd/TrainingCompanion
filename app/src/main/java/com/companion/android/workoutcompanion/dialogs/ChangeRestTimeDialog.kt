package com.companion.android.workoutcompanion.dialogs

//import android.app.Dialog
//import android.content.Context
//import android.content.DialogInterface
//import android.os.Bundle
//import androidx.databinding.DataBindingUtil
//import androidx.fragment.app.DialogFragment
//import com.companion.android.trainingcompanion.R
//import com.companion.android.trainingcompanion.databinding.DialogRestTimeBinding
//import com.google.android.material.dialog.MaterialAlertDialogBuilder
//import com.google.android.material.slider.LabelFormatter
//import com.google.android.material.slider.Slider
//      /** DEPRECATED */
//class ChangeRestTimeDialog(private val initRestTime: Int)
//    : DialogFragment(), DialogInterface.OnClickListener {
//
//    // Объявление привязки данных
//    private lateinit var binding: DialogRestTimeBinding
//    // Объявление объекта интерфейса
//    private var callback: Callback? = null
//    // Объявление вспомогательных объектов для ползунка
//    private lateinit var sliderTouchListener: Slider.OnSliderTouchListener
//    private lateinit var timeLabelFormatter: LabelFormatter
//
//    /** В момент создания определяем слушатель и форматтер */
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // ОПРЕДЕЛЕНИЕ ПОВЕДЕНИЯ СЛУШАТЕЛЯ ДЛЯ СЛАЙДЕРА С ВРЕМЕНЕМ ОТДЫХА
//        sliderTouchListener = object : Slider.OnSliderTouchListener {
//            override fun onStartTrackingTouch(slider: Slider) {}
//            override fun onStopTrackingTouch(slider: Slider) {
//                binding.selectedTime.text = requireContext()
//                    .getString(R.string.selected_rest_time, slider.value.toInt())
//            }
//        }
//        // ОПРЕДЕЛЕНИЕ ВИДА ОТОБРАЖЕНИЯ ТЕКСТА НА СЛАЙДЕРЕ ВЫБОРА ВРЕМЕНИ
//        timeLabelFormatter = LabelFormatter { value ->
//            if (value % 60f == 0f)  "${value.toInt() / 60} min"
//            else  "${value.toInt()} sec"
//        }
//    }
//    /** Во время создания диалогового окна подключаем данные к слайдеру */
//    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        // Инициализируем привязку данных
//        binding = DataBindingUtil.inflate(layoutInflater, R.layout.dialog_rest_time, null, false)
//        // Применяем объекты к слайдеру времени
//        binding.slider.addOnSliderTouchListener(sliderTouchListener)
//        binding.slider.setLabelFormatter(timeLabelFormatter)
//        // Инициализируем начальными значениями
//        binding.selectedTime.text = requireContext()
//            .getString(R.string.selected_rest_time, initRestTime)
//        binding.slider.value = initRestTime.toFloat()
//        // Создаем диалог
//        val adb: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(requireContext())
//            .setTitle(R.string.title_change_rest_time)
//            .setPositiveButton(R.string.button_dialog_accept, this)
//            .setView(binding.root)
//            .setNegativeButton(R.string.button_dialog_cancel, this)
//        return adb.create()
//    }
//
//    /** Во время старта устанавливаем слушатели */
//    override fun onStart() = with(binding) {
//        super.onStart()
//        add5s.setOnClickListener {
//            if (slider.value == slider.valueTo)
//                add5s.isActivated = false
//            else {
//                updateTime(slider.value+5)
//                sub5s.isActivated = true
//            }
//        }
//        sub5s.setOnClickListener {
//            if (slider.value == slider.valueFrom)
//                sub5s.isActivated = false
//            else {
//                updateTime(slider.value-5f)
//                add5s.isActivated = true
//            }
//        }
//        add5s.setOnLongClickListener {
//            updateTime(slider.valueTo)
//            true
//        }
//        sub5s.setOnLongClickListener {
//            updateTime(slider.valueFrom)
//            true
//        }
//
//    }
//    /* Интерфейс нажатий */
//    /** Определение поведения на нажатия кнопок диалогового окна  */
//    override fun onClick(p0: DialogInterface?, which: Int) {
//        when (which) {
//            Dialog.BUTTON_POSITIVE -> {
//                if (initRestTime != binding.slider.value.toInt())
//                    callback?.newRestTimeSelected(binding.slider.value.toInt())
//                dismiss()
//            }
//            Dialog.BUTTON_NEGATIVE -> dismiss()
//        }
//    }
//    private fun updateTime(time: Float) = with(binding) {
//        slider.value = time
//        selectedTime.text =
//            requireContext().getString (
//                R.string.selected_rest_time,
//                slider.value.toInt()
//            )
//    }
//    // При прикреплении диалогового окна определяем объект интерфейса
//    override fun onAttach(context: Context) {
//        super.onAttach(context)
//        callback = context as Callback
//    }
//    // При откреплении диалогового окна обнуляем объект
//    override fun onDetach() {
//        super.onDetach()
//        callback = null
//    }
//    // При жизненном цикле onPause выключаем диалоговое окно
//    override fun onPause() {
//        super.onPause()
//        dismiss()
//    }
//
//    /**
//     * Объявление интерфейса Callback для передачи значения в MainActivity
//     */
//    interface Callback {
//        fun newRestTimeSelected(time: Int)
//    }
//}
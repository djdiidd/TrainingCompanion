package com.companion.android.workoutcompanion.viewmodels

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.companion.android.workoutcompanion.R
import com.companion.android.workoutcompanion.objects.BreakNotifyingMode
import com.companion.android.workoutcompanion.objects.WorkoutParams
import com.companion.android.workoutcompanion.objects.Place
import com.companion.android.workoutcompanion.objects.WorkoutProcess

/**
 * ViewModel для сохранения необходимых данных, выбранных пользователем
 */
class WorkoutViewModel : ViewModel() {

//-----------------------------------/* Данные */---------------------------------------------------

    val activeProcess = MutableLiveData(WorkoutProcess.NOT_STARTED)
    val restTime = MutableLiveData<Int>() // Время перерыва

    // Хранение массива выбранных объектов из доступного списка (boolean)
    private var whichBodyPartSelected = Array(5) { false }
    private var whichMuscleSelected: Array<Boolean> = arrayOf()

    // Хранение пользовательских настроек
    var breakNotifyingMode: Int? = null
        set(value) {
            if (value in BreakNotifyingMode.SOUND..BreakNotifyingMode.ANIMATION
                || value == null) {
                field = value
            }
        }

    // Хранение места тренировки
    var trainingPlace: Int? = null
        set(value) {
            if (value in Place.TRAINING_AT_HOME..Place.TRAINING_OUTDOORS
                || value == null) {
                field = value
            }
        }


//-----------------------------------/* Получение данных */-----------------------------------------


    fun getWhichBPsAreSelected(): Array<Boolean> = whichBodyPartSelected.copyOf()

    fun getWhichMusclesAreSelected(): Array<Boolean> = whichMuscleSelected.copyOf()

    /** Получение массива с мышцами, соответствуюми выбранным частям тела */
    fun getAvailableMuscles(context: Context): Array<String> {
        if (whichBodyPartSelected.isNullOrEmpty())
            throw NullPointerException(
                "DataViewModel -> getMusclesStringList -> isBodyPartSelected.value is null"
            )
        val appropriateMuscle = mapOf(
            0 to R.array.arms_muscles,  // массив с мышцами рук
            1 to R.array.legs_muscles,  // массив с мышцами ног
            2 to R.array.core_muscles,  // массив с мышцами кора
            3 to R.array.back_muscles,  // массив с мышцами спины
            4 to R.array.chest_muscles, // массив с мышцами груди
        )
        val muscleArray = arrayListOf<String>()
        // Добавляем соответствующий массив со строками из ресурсов
        whichBodyPartSelected.forEachIndexed { i, isSelected ->
            if (isSelected) {
                muscleArray.addAll(context.resources.getStringArray(appropriateMuscle[i]!!))
            }
        }
        return muscleArray.toTypedArray()
    }

    /** Получение полного списка с частями тела (из ресурсов) */
    fun getAllBP(context: Context): Array<String> {
        return context.resources.getStringArray(R.array.body_parts)
    }

    /** Получение массива строк с выбранными частям тела */
    fun getSelectedBP(context: Context): Array<String> {
        if (whichBodyPartSelected.isNullOrEmpty())
            throw java.lang.NullPointerException(
                "DataViewModel -> getStringListOfSelectedBP -> isBodyPartSelected is null or empty"
            )
        val tempList = arrayListOf<String>()
        val fullList = getAllBP(context)
        for (i in whichBodyPartSelected.indices) {
            if (whichBodyPartSelected[i])
                tempList.add(fullList[i])
        }
        return tempList.toTypedArray()
    }

    /** Получение выбранных мышц в виде массива строк */
    fun getSelectedMuscles(context: Context): Array<String> {
        val appropriateMuscle = mapOf(
            0 to R.array.arms_muscles,  // массив с мышцами рук
            1 to R.array.legs_muscles,  // массив с мышцами ног
            2 to R.array.core_muscles,  // массив с мышцами кора
            3 to R.array.back_muscles,  // массив с мышцами спины
            4 to R.array.chest_muscles, // массив с мышцами груди
        )
        // Последний элемент не добавляется вывести все items
        val resArray = arrayListOf<String>() //ok
        for (i in whichBodyPartSelected.indices) {
            if (whichBodyPartSelected[i])
                resArray.addAll(
                    context.resources
                        .getStringArray(appropriateMuscle[i]!!)
                )
        }
        for (i in resArray.indices)
            if (!whichMuscleSelected[i])
                resArray[i] = ""
        resArray.removeAll(arrayOf(""))
        return resArray.toTypedArray()
    }

    /**
     * Получение информации: содержит ли массив с мышцами хотя бы 1 выбранный элемент
     * @return Содержит ли массив с мышцами true
     */
    fun isSomeMuscleSelected(): Boolean {
        if (whichMuscleSelected.isNullOrEmpty()) {
            throw java.lang.NullPointerException(
                "DataViewModel -> someBPSelected -> whichMuscleSelected is null or empty"
            )
        }
        return whichMuscleSelected.contains(true)
    }

    /**
     * Получение информации: содержит ли массив с частями тела хотя бы 1 выбранный элемент
     * @return Содержит ли массив с частями тела true
     */
    fun isSomeBPSelected(): Boolean {
        if (whichBodyPartSelected.isNullOrEmpty()) {
            throw java.lang.NullPointerException(
                "DataViewModel -> someBPSelected -> isBodyPartSelected is null or empty"
            )
        }
        return whichBodyPartSelected.contains(true)
    }

    /**
     * Получение количества выбранных мышц пользователем
     */
    fun getNumberOfSelectedMuscles(): Short {
        var number: Short = 0
        for (i in whichMuscleSelected.indices) {
            if (whichMuscleSelected[i]) {
                number++
            }
        }
        return number
    }

    /**
     * Получение массива, характеризующего не используемые части тела
     */
    fun getWhichBPAreUnused(): Array<Boolean> {
        var shift = 0  // Сдвиг
        val sizes = arrayOf(4, 4, 3, 4, 2)
        val resArray = Array(5) { true }
        for (i in whichBodyPartSelected.indices) {
            if (whichBodyPartSelected[i]) {
                for (j in shift until sizes[i] + shift) {
                    if (whichMuscleSelected[j])
                        resArray[i] = false
                }
                shift += sizes[i]
            } else
                resArray[i] = false
        }
        return resArray
    }

//-----------------------------------/* Сеттеры */--------------------------------------------------

    /** Сохранение переданных мышц */
    fun saveSelectedMuscles(array: Array<Boolean>) {
        whichMuscleSelected = array
    }

    // DANGEROUS -> USE updateData instead
    /** Установка выбранных частей тела */
    fun setWhichBPsAreSelectedForce(bodyParts: Array<Boolean>) {
        whichBodyPartSelected = bodyParts
    }

//-----------------------------------/* Обработка */------------------------------------------------

    /** Очистка всех инкапслуриумых значений */
    fun clearAllData() {
        whichBodyPartSelected = Array(5) { false }
        whichMuscleSelected = emptyArray()
        restTime.value = 0
        breakNotifyingMode = null
        trainingPlace = null
    }

    /**
     * Инициализация массива мышц с необходимым размером
     * @param whichBPSelected Массив с частями тела, по которому будет создан массив с мышцами
     */
    private fun initBooleanMuscleArray(whichBPSelected: Array<Boolean>) {
        var finalSize = 0
        val bodyPartsSizes = arrayOf(
            WorkoutParams.numberOfArmMuscles,  // количество мышц рук
            WorkoutParams.numberOfLegMuscles,  // количество мышц ног
            WorkoutParams.numberOfCoreMuscles, // количество мышц кора
            WorkoutParams.numberOfBackMuscles, // количество мышц спины
            WorkoutParams.numberOfChestMuscles// количество мышц груди
        )
        whichBPSelected.forEachIndexed { i, v ->
            if (v) {
                finalSize += bodyPartsSizes[i]
            }
        }
        // Изначально задаем значениями false
        whichMuscleSelected = Array(finalSize) { false }
    }

    /**
     * Сохранение массива с частями тела и обновление массива с мышцами
     * (Если пользователь выбрал новые части тела, то массив с выбранными мышцами останется)
     * @param context Контекст, необходимый для загрузки строковых ресурсов
     * @param newBPs Массив boolean-значений выбранных частей тела.
     */
    fun updateData(context: Context, newBPs: Array<Boolean>) {
        if (!whichMuscleSelected.contains(true)) {
            initBooleanMuscleArray(newBPs)
            whichBodyPartSelected = newBPs //Переопределяем выбранные части тела на новые
            return
        }
        val muscleStrings = getSelectedMuscles(context) //Текущие выбранные мышцы
        whichBodyPartSelected = newBPs //Переопределяем выбранные части тела на новые
        //Новый список доступных мышц для выбора
        val newAvailableMuscles = getAvailableMuscles(context)
        initBooleanMuscleArray(newBPs) //Пересоздаем массив с мыщцами с false
        //Те элементы, которые не изменились, будут записаны в новый массив
        newAvailableMuscles.forEachIndexed { i, _ ->
            if (muscleStrings.contains(newAvailableMuscles[i])) {
                whichMuscleSelected[i] = true
            }
        }
    }
}

//
//    fun setWhichMusclesAreSelected(muscles: Array<Boolean>) { whichBodyPartSelected = muscles }
//    /** Переопределение массива с мышцами на false значение */
//    fun resetSelectedMuscles() {
//        for (index in whichMuscleSelected.indices) {
//            if (whichMuscleSelected[index])
//                whichMuscleSelected[index] = false
//        }
//    }
///** Сохранение переданных частей тела */
//fun saveSelectedBodyParts(array: Array<Boolean>) {
//    whichBodyPartSelected = array.also {
//        initBooleanMuscleArray(it)
//    }
//}

//    /** Установка состояния для определенной части тела */
//    fun setWhichBPIsSelected(index: Int, value: Boolean) {
//        if (index > 4) throw ArrayIndexOutOfBoundsException(
//            "In WorkoutViewModel -> setWhichBPIsSelected" +
//                    " -> index: $index > size: ${whichBodyPartSelected.size}"
//        )
//        whichBodyPartSelected[index] = value
//    }
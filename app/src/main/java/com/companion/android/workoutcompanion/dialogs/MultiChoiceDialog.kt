package com.companion.android.workoutcompanion.dialogs


import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.companion.android.workoutcompanion.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Диалоговое окно, которое принимает массив из строк и такой же размерности массива Boolean
 * Массив строк заполнит TextView, а второй будет использоваться конструктором onCreateDialog
 */
class MultiChoiceDialog(private val items: Array<String>, private val itemSelectedList: BooleanArray)
    : DialogFragment(), DialogInterface.OnMultiChoiceClickListener{

    // Ранняя инициализация списка с выбранными частями тела
    private val whichItemSelected = itemSelectedList

    /**
     * Процесс создание диалогового окна для выбора с помощью AlertDialog.Builder
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val adb: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(  // Название окна
                if (tag == SELECT_BODY_PART_DIALOG) getString(R.string.title_select_body_parts)
                else getString(R.string.title_select_muscles)
            )
            .setMultiChoiceItems(items, itemSelectedList, this)
            .setNegativeButton(R.string.button_dialog_cancel, null)
            .setPositiveButton(R.string.button_dialog_continue, null)
            .setNeutralButton(R.string.button_dialog_reset_all, null)

        return adb.create()  // Созданное диалоговое окно
    }
    /* ИНТЕРФЕЙС */
    /** Слушатель для выбора частей тела (Multi Choice Items) */
    override fun onClick(dialog: DialogInterface?, which: Int, isChecked: Boolean) {
        // Если выделяем элемент, то добавляем его
        whichItemSelected[which] = isChecked
    }

    override fun onStart() {
        super.onStart()

        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            // Если итоговый список не пуст
            if (whichItemSelected.contains(true)) {
                // Передаем результат в слушатель по тегу.
                setFragmentResult(
                    tag!!, // Тег присваивается во время запуска диалогового окна
                    bundleOf(LIST_BUNDLE_TAG to whichItemSelected) // Создание bundle
                )
                dismiss()
            }
            else {
                Toast.makeText(
                    requireContext(),
                    R.string.warning_nothing_selected,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
            dismiss()
        }

        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            for (i in itemSelectedList.indices) {
                whichItemSelected[i] = false
                (dialog as AlertDialog).listView.setItemChecked(i, false)
            }
        }
    }

    /**
     * Приостановка, которая понадобится для выключения
     * диалогового окна воизбежание ошибок
     */
    override fun onPause() {
        super.onPause()
        this.dismiss()
    }
}
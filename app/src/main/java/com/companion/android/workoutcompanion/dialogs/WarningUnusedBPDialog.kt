package com.companion.android.workoutcompanion.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import androidx.fragment.app.DialogFragment
import com.companion.android.workoutcompanion.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Диалоговое окно, которое предупредит пользователя о том,
 * что он содержит выбранную часть тела, которая не используется
 * (часть тела выбрана, а мышцы для нее - нет)
 */ //todo BottomSheetDialog
class WarningUnusedBPDialog(private val whichAreUnusedBP: Array<Boolean>) : DialogFragment(),
    DialogInterface.OnClickListener {

    private var callback: Callback? = null
    private var arrayOfUnusedBP: ArrayList<String> = arrayListOf()

    override fun onAttach(context: Context) {
        super.onAttach(context)

        callback = context as Callback
        val map = mapOf(
            0 to R.string.array_item_arms,
            1 to R.string.array_item_legs,
            2 to R.string.array_item_core,
            3 to R.string.array_item_back,
            4 to R.string.array_item_chest
        )
        for (i in whichAreUnusedBP.indices)
            if (whichAreUnusedBP[i])
                arrayOfUnusedBP.add(getString(map[i]!!))

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        assert(arrayOfUnusedBP.size >= 1)

        val arrayAsString = arrayOfUnusedBP
            .toString().dropLast(1).drop(1) //Убираем скобочки массива
        val message =
            SpannableString(getString(R.string.warning_dialog_unused_bp_message, arrayAsString))
        message.setSpan(
            StyleSpan(Typeface.ITALIC),
            24, 24 + arrayAsString.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        val adb: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(
            requireContext(),
            R.style.ThemeOverlay_App_MaterialAlertDialog_PositiveColored
        )
            .setTitle(R.string.warning_dialog_unused_bp_title)
            .setMessage(message)
            .setPositiveButton(R.string.button_dialog_remove, this)
            .setNegativeButton(R.string.button_dialog_cancel, this)

        return adb.create()
    }

    override fun onClick(di: DialogInterface?, which: Int) {
        when (which) {
            Dialog.BUTTON_POSITIVE -> {
                callback?.unusedBodyPartsRemoved(whichAreUnusedBP)
                dismiss()
            }
            Dialog.BUTTON_NEGATIVE -> {
                dismiss()
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    interface Callback {
        fun unusedBodyPartsRemoved(whichAreUnusedBP: Array<Boolean>)
    }
}
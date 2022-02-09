package com.companion.android.trainingcompanion.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.companion.android.trainingcompanion.R
import com.companion.android.trainingcompanion.objects.Place
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.graphics.drawable.ColorDrawable
import android.widget.ListView


private const val AT_HOME    = 0
private const val IN_THE_GYM = 1
private const val OUTDOORS   = 2
      /**  DEPRECATED  */
class ChangePlaceDialog(private val selectedItem: Int)
    : DialogFragment(), DialogInterface.OnClickListener {

    private var selection = selectedItem

    private var callback: Callback? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val adb: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.place_changing)
            .setPositiveButton(R.string.button_dialog_accept, this)
            .setNegativeButton(R.string.button_dialog_cancel, this)
            .setSingleChoiceItems(R.array.training_place, selectedItem, this)

        return adb.create()
    }

    override fun onClick(p0: DialogInterface?, which: Int) {
        when (which) {
            AT_HOME    -> { selection = Place.TRAINING_AT_HOME  }
            IN_THE_GYM -> { selection = Place.TRAINING_IN_GYM   }
            OUTDOORS   -> { selection = Place.TRAINING_OUTDOORS }

            Dialog.BUTTON_NEGATIVE -> { dismiss() }
            Dialog.BUTTON_POSITIVE -> { callback?.newWorkoutPlaceSelected(selection) }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as Callback
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    override fun onDestroy() {
        super.onDestroy()
        dismiss()
    }

    interface Callback {
        fun newWorkoutPlaceSelected(place: Int)
    }



}
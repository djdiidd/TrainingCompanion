package com.companion.android.trainingcompanion.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.companion.android.trainingcompanion.R
import com.companion.android.trainingcompanion.databinding.DialogBreakNotificationBinding
import com.companion.android.trainingcompanion.objects.BreakNotificationMode
import com.google.android.material.dialog.MaterialAlertDialogBuilder
      /** DEPRECATED */
class BreakNotificationDialog(private val previousMode: Int)
    : DialogFragment(), DialogInterface.OnClickListener {

    private lateinit var binding: DialogBreakNotificationBinding

    private var lastSelectedMode: Int = previousMode
    private lateinit var modeToString: Map<Int, Int>
    private lateinit var modeToView: Map<Int, View>

    private var callback: Callback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        modeToString = mapOf(
            BreakNotificationMode.SOUND     to R.string.notifying_with_sound,
            BreakNotificationMode.VIBRATION to R.string.notifying_vibration,
            BreakNotificationMode.ANIMATION to R.string.notifying_screen_anim,
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        binding = DataBindingUtil
            .inflate(layoutInflater, R.layout.dialog_break_notification, null, false)

        modeToView = mapOf(
            BreakNotificationMode.SOUND to binding.dialogItemSound,
            BreakNotificationMode.VIBRATION to binding.dialogItemVibration,
            BreakNotificationMode.ANIMATION to binding.dialogItemScreenAnim,
        )

        val adb: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_change_notifying_params)
            .setPositiveButton(R.string.button_dialog_accept, this)
            .setNegativeButton(R.string.button_dialog_cancel, this)
            .setView(binding.root)
        return adb.create()
    }

    override fun onStart() = with(binding) {
        super.onStart()

        Log.d("MyTag", "Passed param is $previousMode")

        breakNoteDescription.text = getString(modeToString[previousMode]!!)
        modeToView[lastSelectedMode]?.setBackgroundResource(R.drawable.bg_selected_break_mode)

        dialogItemSound.setOnClickListener {
            handleClick(BreakNotificationMode.SOUND, ::updateSelectedItemParams)
        }
        dialogItemVibration.setOnClickListener {
            handleClick(BreakNotificationMode.VIBRATION, ::updateSelectedItemParams)
        }
        dialogItemScreenAnim.setOnClickListener {
            handleClick(BreakNotificationMode.ANIMATION, ::updateSelectedItemParams)
        }
    }

    override fun onClick(p0: DialogInterface?, which: Int) {
        when (which) {
            Dialog.BUTTON_POSITIVE -> {
                callback?.newBreakNotificationModeSelected(lastSelectedMode)
            }
            Dialog.BUTTON_NEGATIVE -> {
                dismiss()
            }
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

    private inline fun handleClick(
        item: Int,
        setItem: (item: Int, itemAsString: String) -> Unit
    ) {
        if (item == lastSelectedMode) { return }
        modeToView[lastSelectedMode]?.background = null

        modeToView[item]?.apply {
            setBackgroundResource(R.drawable.bg_selected_break_mode)
            startAnimation(AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_out))
            binding.breakNoteDescription.startAnimation(AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_out))
            setItem(item, getString(modeToString[item]!!))
            startAnimation(AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in))
            binding.breakNoteDescription.startAnimation(AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in))
        }
    }
    private fun updateSelectedItemParams(item: Int, asString: String) {
        lastSelectedMode = item
        binding.breakNoteDescription.text = asString
    }

    override fun onDestroy() {
        super.onDestroy()
        dismiss()
    }

    interface Callback {
        fun newBreakNotificationModeSelected(mode: Int)
    }
}
package com.companion.android.trainingcompanion.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import com.companion.android.trainingcompanion.R
import com.companion.android.trainingcompanion.databinding.FragmentMainBinding
import com.companion.android.trainingcompanion.dialogs.WorkoutStartFSDialog
import com.companion.android.trainingcompanion.objects.WorkoutProcess
import com.companion.android.trainingcompanion.viewmodels.WorkoutViewModel


private const val DIALOG_START = "dialog-start" // Метка диалога

/**
 * Данный фрагмент сопровождает пользователя во время тренировки и является основным
 */
class MainFragment : Fragment(), WorkoutStartFSDialog.Callback {

    private lateinit var binding: FragmentMainBinding               // Объект класса привязки данных
    private val viewModel: WorkoutViewModel by activityViewModels() // Общая ViewModel

    private var callback: FragmentCallback? = null

    private var isLargeLayout = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("LF", "F onCreateView")
        binding = DataBindingUtil // определяем привязку данных
            .inflate(layoutInflater, R.layout.fragment_main, container, false)

        isLargeLayout = resources.getBoolean(R.bool.large_layout)

        // Если тренировка уже началась, то убираем лишнее с экрана;
        if (viewModel.activeProcess.value != WorkoutProcess.NOT_STARTED) {
            binding.startButton.visibility = View.GONE
            binding.setTimerProgress.visibility = View.VISIBLE
            binding.setTimer.visibility = View.VISIBLE
            binding.mainButton.visibility = View.VISIBLE
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("LF", "F onViewCreated")
//        val bounceAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.anim_bounce)
        // Слушатель нажатий для кнопки начала тренировки
        binding.startButton.setOnClickListener {
//            it.startAnimation(bounceAnim)

            if (parentFragmentManager.findFragmentByTag(DIALOG_START) == null) {
                showStartDialog()
                binding.startButton.visibility = View.GONE
            }
//            it.clearAnimation()
        }
        callback?.fragmentUICreated(binding.setTimer, binding.setTimerProgress)
    }

    private fun showStartDialog() {
        if (isLargeLayout) {
            WorkoutStartFSDialog().show(this@MainFragment.parentFragmentManager, DIALOG_START)
        } else {
            // The device is smaller, so show the fragment fullscreen
            val transaction = parentFragmentManager.beginTransaction()
            // For a little polish, specify a transition animation
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            // To make it fullscreen, use the 'content' root view as the container
            // for the fragment, which is always the root view for the activity
            transaction
                .add(android.R.id.content, WorkoutStartFSDialog())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("LF", "F onStart")

        val bounceAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.anim_bounce)

        binding.mainButton.setOnClickListener {
            it.startAnimation(bounceAnim)
            it.isEnabled = false
            callback?.mainButtonClicked()
            Handler(requireContext().mainLooper).postDelayed({
                it.isEnabled = true
            }, 1000)
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("LF", "F onStop")

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("LF", "F onDestroy")
        callback?.fragmentDestroyed()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d("LF", "F onAttach")
        callback = context as FragmentCallback
    }

    override fun onDetach() {
        super.onDetach()
        Log.d("LF", "F onDetach")
        callback = null
    }

    interface FragmentCallback {
        fun mainButtonClicked()
        fun fragmentDestroyed()
        fun fragmentUICreated(textView: TextView, progressBar: ProgressBar)
        fun workoutStarted()
    }

    override fun workoutStarted(success: Boolean) {
        if (success) {
            callback?.workoutStarted()
            binding.startButton.visibility = View.GONE
            binding.setTimerProgress.visibility = View.VISIBLE
            binding.setTimer.visibility = View.VISIBLE
            binding.mainButton.visibility = View.VISIBLE
        } else {
            binding.startButton.visibility = View.VISIBLE
        }
    }

}

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
import androidx.fragment.app.activityViewModels
import com.companion.android.trainingcompanion.R
import com.companion.android.trainingcompanion.databinding.FragmentMainBinding
import com.companion.android.trainingcompanion.dialogs.WorkoutStartDialog
import com.companion.android.trainingcompanion.viewmodels.WorkoutViewModel


private const val DIALOG_START = "dialog-start" // Метка диалога
private const val ARGUMENT_TIMER = "argument-timer" // Ключ для получения таймера из хоста

/**
 * Данный фрагмент сопровождает пользователя во время тренировки и является основным
 */
class MainFragment : Fragment() {

    private lateinit var binding: FragmentMainBinding               // Объект класса привязки данных
    private val viewModel: WorkoutViewModel by activityViewModels() // Общая ViewModel

    private var callback: FragmentCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("LF", "F onCreateView")
        binding = DataBindingUtil // определяем привязку данных
            .inflate(layoutInflater, R.layout.fragment_main, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("LF", "F onViewCreated")
//        val bounceAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.anim_bounce)
        // Слушатель нажатий для кнопки начала тренировки
        binding.startButton.setOnClickListener {
//            it.startAnimation(bounceAnim)
            WorkoutStartDialog().apply {
                show(this@MainFragment.parentFragmentManager, DIALOG_START)
            }
//            it.clearAnimation()
        }
        callback?.fragmentUICreated(binding.setTimer, binding.setTimerProgress)
    }

    override fun onStart() {
        super.onStart()
        Log.d("LF", "F onStart")

        viewModel.workoutSuccessfullyStarted.observe(requireActivity()) { started ->
            if (!started || viewModel.workoutInProgress) {
                return@observe
            }

            Log.d("MyTag", "workoutSuccessfullyStarted success")
            binding.startButton.visibility = View.GONE
            binding.setTimerProgress.visibility = View.VISIBLE
            binding.setTimer.visibility = View.VISIBLE
            binding.mainButton.visibility = View.VISIBLE
        }

        binding.mainButton.setOnClickListener {
            it.isEnabled = false
            callback?.buttonClicked()
            Handler(requireContext().mainLooper).postDelayed({
                it.isEnabled = true
            }, 1000)
        }

        if (viewModel.workoutSuccessfullyStarted.value == true) {
            binding.startButton.visibility = View.GONE
            binding.setTimerProgress.visibility = View.VISIBLE
            binding.setTimer.visibility = View.VISIBLE
            binding.mainButton.visibility = View.VISIBLE
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
        fun buttonClicked()
        fun fragmentDestroyed()
        fun fragmentUICreated(textView: TextView, progressBar: ProgressBar)
    }

}

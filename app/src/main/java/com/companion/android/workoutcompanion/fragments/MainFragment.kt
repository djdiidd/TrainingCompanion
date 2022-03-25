package com.companion.android.workoutcompanion.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.companion.android.workoutcompanion.R
import com.companion.android.workoutcompanion.databinding.FragmentMainBinding
import com.companion.android.workoutcompanion.objects.WorkoutProcess
import com.companion.android.workoutcompanion.viewmodels.WorkoutViewModel


private const val DIALOG_START = "dialog-start" // Метка диалога


/**
 * Данный фрагмент сопровождает пользователя во время тренировки и является основным
 */
class MainFragment : Fragment() {

    private val args: MainFragmentArgs by navArgs()

    private var _binding: FragmentMainBinding? = null  // Объект класса привязки данных
    private val binding get() = _binding!!
    private val viewModel: WorkoutViewModel by activityViewModels() // Общая ViewModel
    private var callback: FragmentCallback? = null

    private var isLargeLayout = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("LF", "F onCreateView")
        _binding = FragmentMainBinding // определяем привязку данных
            .inflate(layoutInflater, container, false)

        isLargeLayout = resources.getBoolean(R.bool.large_layout)

        // Если тренировка уже началась, то убираем лишнее с экрана;
        if (viewModel.activeProcess.value != WorkoutProcess.NOT_STARTED) {
            setProcessViews()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("LF", "F onViewCreated")
        // Слушатель нажатий для кнопки начала тренировки
        binding.startButton.setOnClickListener {

            if (parentFragmentManager.findFragmentByTag(DIALOG_START) == null) {
                showStartDialog()
                binding.startButton.visibility = View.GONE
            }
        }
    }

    override fun onStart() {
        super.onStart()
        callback?.fragmentUICreated(binding.setTimer, binding.setTimerProgress)
        Log.d("LF", "F onStart")

        val bounceAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.anim_bounce)

        // Фиксируем разметку, что позволит вводить текст с клавиатуры без изменений разметки;
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        binding.mainButton.setOnClickListener {
            it.startAnimation(bounceAnim)
            it.isEnabled = false
            callback?.mainButtonClicked()
            Handler(requireContext().mainLooper).postDelayed({
                it.isEnabled = true
            }, 1000)
        }

        requireActivity()
            .onBackPressedDispatcher
            .addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    Toast.makeText(requireContext(), " Attempt to exit...", Toast.LENGTH_SHORT)
                        .show()
                }
            })
        if (viewModel.activeProcess.value == WorkoutProcess.NOT_STARTED) {
            if (args.workoutStartedSuccessfully) {
                callback?.workoutStarted()
                setProcessViews()
            } else {
                setStartViews()
            }
        }

        viewModel.activeProcess.observe(this) {
            if (it == WorkoutProcess.NOT_STARTED && binding.mainButton.isVisible)
                setStartViews()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

    private fun showStartDialog() {
        if (isLargeLayout) {

        } else {
            findNavController().navigate(R.id.navigate_to_workoutStartFSDialog)
        }
    }

    private fun setStartViews() {
        binding.startButton.visibility = View.VISIBLE
        if (binding.setTimer.isVisible) {
            binding.setTimerProgress.visibility = View.GONE
            binding.setTimer.visibility = View.GONE
            binding.mainButton.visibility = View.GONE
        }
    }

    private fun setProcessViews() {
        binding.startButton.visibility = View.GONE
        binding.setTimerProgress.visibility = View.VISIBLE
        binding.setTimer.visibility = View.VISIBLE
        binding.mainButton.visibility = View.VISIBLE
    }

    interface FragmentCallback {
        fun mainButtonClicked()
        fun fragmentDestroyed()
        fun fragmentUICreated(textView: TextView, progressBar: ProgressBar)
        fun workoutStarted()
    }

}

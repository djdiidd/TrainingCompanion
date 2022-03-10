package com.companion.android.workoutcompanion.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.companion.android.workoutcompanion.R
import com.companion.android.workoutcompanion.databinding.FragmentListBinding

class ListFragment: Fragment() {
    // ранняя инициализация объекта класса привязки данных
    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListBinding // определяем привязку данных
            .inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Сообщаем сборщику мусора, что можно очистить;
        _binding = null
    }
}
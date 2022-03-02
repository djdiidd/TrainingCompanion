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
    private lateinit var binding: FragmentListBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil // определяем привязку данных
            .inflate(layoutInflater, R.layout.fragment_list, container, false)
        return binding.root
    }
}
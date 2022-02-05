package com.companion.android.trainingcompanion.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.companion.android.trainingcompanion.R
import com.companion.android.trainingcompanion.databinding.FragmentListBinding

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
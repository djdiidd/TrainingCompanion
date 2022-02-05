package com.companion.android.trainingcompanion.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.companion.android.trainingcompanion.models.SimpleSpinnerItem
import com.companion.android.trainingcompanion.R

class PlaceSpinnerAdapter(
    context: Context,
    values: Array<SimpleSpinnerItem>
) : ArrayAdapter<SimpleSpinnerItem>(context, 0, values) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position, convertView, parent)
    }

    private fun initView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)
        val view = LayoutInflater.from(context)
            .inflate(R.layout.spinner_item, parent, false)
        view.findViewById<ImageView>(R.id.spinner_image).setImageResource(item!!.drawable)
        view.findViewById<TextView>(R.id.spinner_title).text = item.title
        return view
    }

}
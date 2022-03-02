package com.companion.android.workoutcompanion.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.companion.android.workoutcompanion.R
import com.companion.android.workoutcompanion.databinding.SideMenuItemCheckedBinding
import com.companion.android.workoutcompanion.models.SimpleListItem

class SimpleListAdapter(context: Context, val tag: String)
    : ListAdapter<SimpleListItem, SimpleListAdapter.MuscleHolder>(MuscleDiffUtilCallback()) {

    private var callback: Callback = context as Callback

    inner class MuscleHolder(item: View): RecyclerView.ViewHolder(item), View.OnClickListener {
        val binding = SideMenuItemCheckedBinding.bind(item)

        init { binding.root.setOnClickListener(this) }
        fun bind(item: SimpleListItem) {
            binding.dynamicSideMenuItemText.text = item.text
            binding.dynamicSideMenuItemCheckbox.isChecked = item.isChecked
        }

        override fun onClick(v: View) {
            val pos = layoutPosition
            val item = getItem(pos)
            item.isChecked = !item.isChecked
            binding.dynamicSideMenuItemCheckbox.isChecked = item.isChecked
            callback.sideMenuListItemSelected(pos, tag)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MuscleHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.side_menu_item_checked, parent,false)
        return MuscleHolder(view)
    }

    override fun onBindViewHolder(holder: MuscleHolder, position: Int) {
        holder.bind(getItem(position))
    }

    interface Callback {
        fun sideMenuListItemSelected(position: Int, tag: String)
    }
}

class MuscleDiffUtilCallback: DiffUtil.ItemCallback<SimpleListItem>() {
    override fun areItemsTheSame(oldItem: SimpleListItem, newItem: SimpleListItem): Boolean {
        return newItem.text == oldItem.text
    }

    override fun areContentsTheSame(oldItem: SimpleListItem, newItem: SimpleListItem): Boolean {
        return oldItem == newItem
    }
}
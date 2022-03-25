package com.companion.android.workoutcompanion.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.companion.android.workoutcompanion.R
import com.companion.android.workoutcompanion.activities.TAG_BODY_PARTS
import com.companion.android.workoutcompanion.activities.TAG_MUSCLES
import com.companion.android.workoutcompanion.databinding.SideMenuItemCheckedBinding
import com.companion.android.workoutcompanion.models.SimpleListItem
import com.companion.android.workoutcompanion.objects.WorkoutParams

class SimpleListAdapter(context: Context, val tag: String) :
    ListAdapter<SimpleListItem, SimpleListAdapter.MuscleHolder>(MuscleDiffUtilCallback()) {

    private var callback: Callback = context as Callback

    inner class MuscleHolder(item: View) : RecyclerView.ViewHolder(item), View.OnClickListener {
        val binding = SideMenuItemCheckedBinding.bind(item)

        init {
            binding.root.setOnClickListener(this)
        }

        fun bind(item: SimpleListItem) {
            binding.dynamicSideMenuItemText.text = item.text
            binding.dynamicSideMenuItemCheckbox.isChecked = item.isChecked
        }

        private fun isSomeSelected(): Boolean {
            for (i in currentList.indices)
                if (currentList[i].isChecked)
                    return true
            return false
        }

        override fun onClick(v: View) {
            val pos = layoutPosition
            val item = getItem(pos)
            if (tag == TAG_BODY_PARTS) {
                if (item.isChecked) {
                    item.isChecked = false
                    if (isSomeSelected()) {
                        callback.sideMenuListItemSelected(pos, tag)
                        binding.dynamicSideMenuItemCheckbox.isChecked = item.isChecked
                    } else {
                        Toast.makeText(
                            v.context, R.string.toast_need_at_least_one_bp, Toast.LENGTH_LONG
                        ).show()
                        item.isChecked = true
                    }
                } else {
                    item.isChecked = true
                    binding.dynamicSideMenuItemCheckbox.isChecked = item.isChecked
                    callback.sideMenuListItemSelected(pos, tag)
                }
            } else {
                item.isChecked = !item.isChecked
                binding.dynamicSideMenuItemCheckbox.isChecked = item.isChecked
                callback.sideMenuListItemSelected(pos, tag)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MuscleHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.side_menu_item_checked, parent, false)
        return MuscleHolder(view)
    }

    override fun onBindViewHolder(holder: MuscleHolder, position: Int) {
        holder.bind(getItem(position))
    }

    interface Callback {
        fun sideMenuListItemSelected(position: Int, tag: String)
    }
}

class MuscleDiffUtilCallback : DiffUtil.ItemCallback<SimpleListItem>() {
    override fun areItemsTheSame(oldItem: SimpleListItem, newItem: SimpleListItem): Boolean {
        return newItem.text == oldItem.text
    }

    override fun areContentsTheSame(oldItem: SimpleListItem, newItem: SimpleListItem): Boolean {
        return oldItem == newItem
    }
}
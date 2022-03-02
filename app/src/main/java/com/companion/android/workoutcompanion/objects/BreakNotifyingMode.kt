package com.companion.android.workoutcompanion.objects

import android.content.Context
import com.companion.android.workoutcompanion.R
import com.companion.android.workoutcompanion.models.SimpleSpinnerItem

object BreakNotifyingMode {

    const val SOUND    : Int = 111
    const val VIBRATION: Int = 112
    const val ANIMATION: Int = 113

    private val images = intArrayOf(
        R.drawable.ic_sound_outlined,
        R.drawable.ic_vibration_outlined,
        R.drawable.ic_animation_outlined
    )

    private var list: Array<SimpleSpinnerItem> = arrayOf()

    private fun getTitles(context: Context): Array<String> {
        return context.resources.getStringArray(R.array.break_notification)
    }

    fun getList(context: Context): Array<SimpleSpinnerItem> {
        if (list.isNotEmpty())
            return list
        val result = Array(images.size) {SimpleSpinnerItem("", 0)}
        val titles = getTitles(context)
        for (i in images.indices) {
            val image = images[i]
            val title = titles[i]
            result[i] = SimpleSpinnerItem(title, image)
        }
        list = result
        return list
    }

}
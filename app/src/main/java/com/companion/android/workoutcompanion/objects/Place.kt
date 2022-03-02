package com.companion.android.workoutcompanion.objects

import android.content.Context
import com.companion.android.workoutcompanion.R
import com.companion.android.workoutcompanion.models.SimpleSpinnerItem

object Place {

    const val TRAINING_AT_HOME : Int = 0
    const val TRAINING_IN_GYM  : Int = 1
    const val TRAINING_OUTDOORS: Int = 2

    private val images = intArrayOf(
        R.drawable.ic_home_24,
        R.drawable.ic_gym,
        R.drawable.ic_outdoors
    )

    private var list: Array<SimpleSpinnerItem> = arrayOf()

    private fun getTitles(context: Context): Array<String> {
        return context.resources.getStringArray(R.array.training_place)
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
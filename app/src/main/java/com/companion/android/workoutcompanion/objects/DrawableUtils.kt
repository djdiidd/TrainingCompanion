package com.companion.android.workoutcompanion.objects

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable


object DrawableUtils {
    fun setStroke(drawable: Drawable, colorId: Int, width: Int) {
        val gradientDrawable = drawable as GradientDrawable
        gradientDrawable.mutate()
        gradientDrawable.setStroke(width, colorId)
    }
}
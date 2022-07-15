package com.companion.android.workoutcompanion.objects

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.animation.Animation
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

object Anim {

    fun doOnEndOf(animation: Animation, action: () -> Unit) {
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {}
            override fun onAnimationRepeat(p0: Animation?) {}
            override fun onAnimationEnd(p0: Animation?) {
                action.invoke()
                animation.setAnimationListener(null)
            }
        })
    }
    fun getColorChangingAnimation(
        view: View,
        context: Context,
        @ColorRes fromColor: Int,
        @ColorRes toColor: Int
    ) : ValueAnimator {
        return ValueAnimator.ofObject(
            ArgbEvaluator(),
            ContextCompat.getColor(context, fromColor),
            ContextCompat.getColor(context, toColor),
        ).apply {
            duration = 200
            addUpdateListener {
                view.setBackgroundColor(it.animatedValue as Int)
            }
        }
    }

}
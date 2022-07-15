package com.companion.android.workoutcompanion.objects

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager

object Vibration {

    fun getManager(context: Context) =
        context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager


    fun make(vibratorManager: VibratorManager) {
        vibratorManager.defaultVibrator.vibrate(
            VibrationEffect.createOneShot(100, VibrationEffect.EFFECT_HEAVY_CLICK)
        )
    }
}
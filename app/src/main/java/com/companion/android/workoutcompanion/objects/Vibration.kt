package com.companion.android.workoutcompanion.objects

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi

object Vibration {


    fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context
                .getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("Deprecation")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun make(vibrator: Vibrator, millis: Long = 100, amplitude: Int = VibrationEffect.EFFECT_TICK) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(millis, amplitude)
            )
        } else {
            @Suppress("Deprecation")
            vibrator.vibrate(millis)
        }
    }


    fun getManager(context: Context) =
        context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager


    fun make(vibratorManager: VibratorManager) {
        vibratorManager.defaultVibrator.vibrate(
            VibrationEffect.createOneShot(100, VibrationEffect.EFFECT_HEAVY_CLICK)
        )
    }
}
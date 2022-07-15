package com.companion.android.workoutcompanion.objects

object WorkoutParams {

    val restTimeAdvRange = 5..720
    val restTimeDefaultRange = 15..240
    val placeValueRange = 0..2
    val breakNotificationRange = 111..113

    const val numberOfBodyParts = 5

    const val numberOfArmMuscles = 4
    const val numberOfLegMuscles = 4
    const val numberOfCoreMuscles = 3
    const val numberOfBackMuscles = 4
    const val numberOfChestMuscles = 2

    val notifyingSignalAt = arrayOf(30, 15, 10, 7, 5, 3, 2, 1, -1)
}
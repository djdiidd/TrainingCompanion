package com.companion.android.workoutcompanion.objects

import androidx.annotation.Dimension
import com.google.android.material.button.MaterialButton
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel


object ChangeAppearance {
    fun MaterialButton.setCorners(
        @Dimension topLeft: Float,
        @Dimension topRight: Float,
        @Dimension bottomLeft: Float,
        @Dimension bottomRight: Float,
        cornerFamily: Int = CornerFamily.ROUNDED
    ) {
        shapeAppearanceModel = ShapeAppearanceModel.builder()
            .setBottomLeftCorner(cornerFamily, bottomLeft)
            .setBottomRightCorner(cornerFamily, bottomRight)
            .setTopLeftCorner(cornerFamily, topLeft)
            .setTopRightCorner(cornerFamily, topRight)
            .build()
    }
}
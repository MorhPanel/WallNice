package com.morhpanel.nekowall.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

object WallNiceMotion {
    const val fastDurationMillis = 180
    const val standardDurationMillis = 240
    const val emphasisDurationMillis = 300
    const val sharedElementDurationMillis = 380
    const val cardPressDurationMillis = 120
    const val cardReleaseDurationMillis = 260
    const val heartBurstDurationMillis = 320
    const val filterSlideDurationMillis = 200
    const val galleryEntryDurationMillis = 280
    const val galleryStaggerDelayMillis = 28
    const val scanlineDurationMillis = 480
    const val videoPulseDurationMillis = 2000

    val easeStandard = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val easeEmphasized = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val easeDecelerate = LinearOutSlowInEasing
    val easeAccelerateDecelerate = FastOutSlowInEasing

    val springBouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    val springResponsive = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = Spring.StiffnessMedium
    )
    val springSnappy = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
}

package com.licel.samples.core.ui.indication

// Copyright 2023 LICEL CORPORATION
// All Rights Reserved.
//
// NOTICE:  All information contained herein is, and remains
// the property of LICEL CORPORATION and its suppliers,
// if any.  The intellectual and technical concepts contained
// herein are proprietary to LICEL CORPORATION
// and its suppliers and may be covered by U.S. and Foreign Patents,
// patents in process, and are protected by trade secret or copyright law.
// Dissemination of this information or reproduction of this material
// is strictly forbidden unless prior written permission is obtained
// from LICEL CORPORATION.
//

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import kotlinx.coroutines.flow.collectLatest

private class ScaleIndicationInstance : IndicationInstance {
    var currentPressPosition: Offset = Offset.Zero
    val animatedScalePercent = Animatable(1f)

    suspend fun animateToPressed(pressPosition: Offset) {
        currentPressPosition = pressPosition
        animatedScalePercent.animateTo(0.9f, spring())
    }

    suspend fun animateToResting() {
        animatedScalePercent.animateTo(1f, spring())
    }

    override fun ContentDrawScope.drawIndication() {
        scale(
            scale = animatedScalePercent.value,
            pivot = currentPressPosition
        ) {
            this@drawIndication.drawContent()
        }
    }
}

object ScaleIndication : Indication {
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance {
        val instance = remember(interactionSource) { ScaleIndicationInstance() }

        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> instance.animateToPressed(interaction.pressPosition)
                    is PressInteraction.Release -> instance.animateToResting()
                    is PressInteraction.Cancel -> instance.animateToResting()
                }
            }
        }
        return instance
    }
}
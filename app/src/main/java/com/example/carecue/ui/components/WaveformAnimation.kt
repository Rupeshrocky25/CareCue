package com.example.carecue.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberOrangePrimary
import com.example.ui.theme.AmberOrangeSecondary

@Composable
fun WaveformAnimation(
    modifier: Modifier = Modifier,
    isListening: Boolean = false,
    barCount: Int = 5,
    maxHeight: Dp = 28.dp,
    minHeight: Dp = 8.dp,
    activeColor: Color = AmberOrangePrimary,
    inactiveColor: Color = AmberOrangeSecondary.copy(alpha = 0.5f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val baseDelays = listOf(0, 150, 300, 200, 100)
        for (i in 0 until barCount) {
            val delay = baseDelays[i % baseDelays.size]
            val animFraction by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = if (isListening) 1.0f else 0.45f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = if (isListening) 450 + (i * 70) else 1000 + (i * 120),
                        delayMillis = delay,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$i"
            )

            val currentHeight = minHeight + (maxHeight - minHeight) * animFraction
            val color = if (isListening) activeColor else inactiveColor

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(currentHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

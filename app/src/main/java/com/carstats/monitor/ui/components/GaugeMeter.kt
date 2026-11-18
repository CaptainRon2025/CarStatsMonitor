package com.carstats.monitor.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carstats.monitor.ui.theme.CockpitSurfaceVariant
import com.carstats.monitor.ui.theme.ElectricCyan
import com.carstats.monitor.ui.theme.SportRed
import com.carstats.monitor.ui.theme.TextMuted
import com.carstats.monitor.ui.theme.TextPrimary
import com.carstats.monitor.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

/**
 * Анімований цифровий та аналоговий шкалний індикатор (Gauge) для спідометра або тахометра.
 */
@Composable
fun GaugeMeter(
    value: Float,
    minValue: Float = 0f,
    maxValue: Float = 220f,
    title: String,
    unit: String,
    modifier: Modifier = Modifier,
    warningThreshold: Float? = null,
    activeColor: Color = ElectricCyan
) {
    val clampedValue = value.coerceIn(minValue, maxValue)
    val animatedValue by animateFloatAsState(
        targetValue = clampedValue,
        animationSpec = tween(durationMillis = 200),
        label = "gaugeAnimation"
    )

    val progress = (animatedValue - minValue) / (maxValue - minValue)
    val startAngle = 140f
    val sweepAngle = 260f
    val currentSweep = sweepAngle * progress

    val isWarning = warningThreshold != null && animatedValue >= warningThreshold
    val gaugeColor = if (isWarning) SportRed else activeColor

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.width * 0.08f
            val arcSize = size.width - strokeWidth
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            // 1. Фоновa шкала
            drawArc(
                color = CockpitSurfaceVariant,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 2. Активна дуга зі свіченням/градієнтом
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(activeColor, gaugeColor)
                ),
                startAngle = startAngle,
                sweepAngle = currentSweep,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 3. Стрілка-маркер на кінці поточної дуги
            val currentRad = Math.toRadians((startAngle + currentSweep).toDouble())
            val radius = arcSize / 2
            val center = Offset(size.width / 2, size.height / 2)
            val markerX = (center.x + radius * cos(currentRad)).toFloat()
            val markerY = (center.y + radius * sin(currentRad)).toFloat()

            drawCircle(
                color = Color.White,
                radius = strokeWidth * 0.4f,
                center = Offset(markerX, markerY)
            )
        }

        // Цифровий показник по центру
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title.uppercase(),
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = animatedValue.toInt().toString(),
                color = TextPrimary,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp
            )
            Text(
                text = unit,
                color = if (isWarning) SportRed else TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

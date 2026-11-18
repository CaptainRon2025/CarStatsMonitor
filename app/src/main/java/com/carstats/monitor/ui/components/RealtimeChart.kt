package com.carstats.monitor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carstats.monitor.ui.theme.CockpitBorder
import com.carstats.monitor.ui.theme.CockpitSurface
import com.carstats.monitor.ui.theme.ElectricCyan
import com.carstats.monitor.ui.theme.TextMuted
import com.carstats.monitor.ui.theme.TextPrimary
import com.carstats.monitor.ui.theme.TextSecondary

/**
 * Графік динаміки параметра у реальному часі.
 */
@Composable
fun RealtimeChart(
    title: String,
    points: List<Float>,
    currentValue: String,
    unit: String,
    lineColor: Color = ElectricCyan,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CockpitSurface)
            .border(1.dp, CockpitBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "$currentValue $unit",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                if (points.isEmpty()) return@Canvas

                val minVal = (points.minOrNull() ?: 0f).coerceAtLeast(0f)
                val maxVal = ((points.maxOrNull() ?: 100f) * 1.15f).coerceAtLeast(minVal + 10f)
                val range = maxVal - minVal

                val stepX = if (points.size > 1) size.width / (points.size - 1) else size.width

                val linePath = Path()
                val fillPath = Path()

                points.forEachIndexed { index, point ->
                    val x = index * stepX
                    val normalizedY = (point - minVal) / range
                    val y = size.height - (normalizedY * size.height)

                    if (index == 0) {
                        linePath.moveTo(x, y)
                        fillPath.moveTo(x, size.height)
                        fillPath.lineTo(x, y)
                    } else {
                        linePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }

                fillPath.lineTo(size.width, size.height)
                fillPath.close()

                // Градієнтне заповнення під графіком
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.25f), Color.Transparent),
                        startY = 0f,
                        endY = size.height
                    )
                )

                // Основна лінія графіка
                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Останній кружечок поточної точки
                if (points.isNotEmpty()) {
                    val lastX = (points.size - 1) * stepX
                    val lastNormalizedY = ((points.last() - minVal) / range)
                    val lastY = size.height - (lastNormalizedY * size.height)
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(lastX, lastY)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Історія замірів", color = TextMuted, fontSize = 10.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "Зараз", color = TextMuted, fontSize = 10.sp)
            }
        }
    }
}

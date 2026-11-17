package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.data.local.BillReadingEntity
import com.example.domain.HourlyLoadPoint
import com.example.domain.SlabChargeBreakdown
import com.example.ui.theme.ElectricMint
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.MidPeakTeal
import com.example.ui.theme.PeakAmber
import com.example.ui.theme.SurgeCoral
import kotlin.math.max

@Composable
fun TariffSlabProgressBar(
  slabBreakdowns: List<SlabChargeBreakdown>,
  totalMonthlyKwh: Double,
  currencySymbol: String,
  modifier: Modifier = Modifier,
) {
  val slabColors = listOf(ForestEmerald, ElectricMint, PeakAmber, SurgeCoral)
  val totalDenominator = max(totalMonthlyKwh, 1.0)

  Column(modifier = modifier.fillMaxWidth()) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(18.dp)
          .clip(RoundedCornerShape(9.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Canvas(modifier = Modifier.fillMaxWidth().height(18.dp)) {
        if (totalMonthlyKwh <= 0.0) return@Canvas
        var startX = 0f
        slabBreakdowns.forEachIndexed { index, slab ->
          if (slab.kwhUsed > 0.0) {
            val fraction = (slab.kwhUsed / totalDenominator).toFloat().coerceIn(0f, 1f)
            val widthPx = size.width * fraction
            val color = slabColors[index % slabColors.size]
            drawRect(
              color = color,
              topLeft = Offset(startX, 0f),
              size = Size(widthPx, size.height),
            )
            startX += widthPx
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      slabBreakdowns.forEachIndexed { index, slab ->
        val color = slabColors[index % slabColors.size]
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = slab.slabName,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
          Text(
            text =
              "${String.format("%.0f", slab.kwhUsed)} kWh • $currencySymbol${String.format("%.2f", slab.cost)}",
            style = MaterialTheme.typography.labelMedium,
            color =
              if (slab.kwhUsed > 0.0) MaterialTheme.colorScheme.onSurface
              else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
fun HourlyLoadProfileChart(
  points: List<HourlyLoadPoint>,
  modifier: Modifier = Modifier,
) {
  val primaryColor = MaterialTheme.colorScheme.primary
  val peakColor = PeakAmber
  val midColor = MidPeakTeal
  val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ChartLegendDot(color = peakColor, label = "Current Load")
      ChartLegendDot(color = primaryColor, label = "Optimized Load")
    }

    Spacer(modifier = Modifier.height(10.dp))

    Canvas(modifier = Modifier.fillMaxWidth().height(164.dp)) {
      if (points.isEmpty()) return@Canvas
      val maxKw = max(0.5, points.maxOf { max(it.currentKw, it.optimizedKw) } * 1.18)
      val w = size.width
      val h = size.height
      val stepX = w / 23f

      // Shaded Time-of-Use zones (0-5 Off-Peak, 6-16 Mid-Peak, 17-21 Peak, 22-23 Off-Peak)
      drawRect(
        color = primaryColor.copy(alpha = 0.08f),
        topLeft = Offset(0f, 0f),
        size = Size(stepX * 6f, h),
      )
      drawRect(
        color = midColor.copy(alpha = 0.07f),
        topLeft = Offset(stepX * 6f, 0f),
        size = Size(stepX * 11f, h),
      )
      drawRect(
        color = peakColor.copy(alpha = 0.14f),
        topLeft = Offset(stepX * 17f, 0f),
        size = Size(stepX * 5f, h),
      )
      drawRect(
        color = primaryColor.copy(alpha = 0.08f),
        topLeft = Offset(stepX * 22f, 0f),
        size = Size(stepX * 1f, h),
      )

      // Horizontal reference grid lines
      for (i in 1..3) {
        val y = h * (i / 4f)
        drawLine(
          color = gridColor,
          start = Offset(0f, y),
          end = Offset(w, y),
          strokeWidth = 1.dp.toPx(),
        )
      }

      val currentPath = Path()
      val optimizedPath = Path()
      val optimizedFillPath = Path()

      points.forEachIndexed { idx, pt ->
        val x = idx * stepX
        val curY = h - ((pt.currentKw / maxKw).toFloat().coerceIn(0f, 1f) * (h - 12.dp.toPx()))
        val optY = h - ((pt.optimizedKw / maxKw).toFloat().coerceIn(0f, 1f) * (h - 12.dp.toPx()))
        if (idx == 0) {
          currentPath.moveTo(x, curY)
          optimizedPath.moveTo(x, optY)
          optimizedFillPath.moveTo(x, h)
          optimizedFillPath.lineTo(x, optY)
        } else {
          currentPath.lineTo(x, curY)
          optimizedPath.lineTo(x, optY)
          optimizedFillPath.lineTo(x, optY)
        }
        if (idx == points.lastIndex) {
          optimizedFillPath.lineTo(x, h)
          optimizedFillPath.close()
        }
      }

      drawPath(
        path = optimizedFillPath,
        brush =
          Brush.verticalGradient(
            colors = listOf(primaryColor.copy(alpha = 0.28f), primaryColor.copy(alpha = 0.02f))
          ),
      )

      drawPath(
        path = currentPath,
        color = peakColor,
        style =
          Stroke(
            width = 2.5.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
          ),
      )

      drawPath(
        path = optimizedPath,
        color = primaryColor,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(
        "00:00 Off-Peak",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(
        "12:00 Mid",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text("17–22 Peak", style = MaterialTheme.typography.labelSmall, color = PeakAmber)
      Text(
        "23:00",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

@Composable
fun BillHistoryTrendChart(
  readings: List<BillReadingEntity>,
  monthlyBudget: Double,
  currencySymbol: String,
  modifier: Modifier = Modifier,
) {
  val ordered = readings.sortedBy { it.recordedAt }.takeLast(6)
  val barColor = MaterialTheme.colorScheme.primary
  val overBudgetColor = SurgeCoral
  val budgetLineColor = PeakAmber

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ChartLegendDot(color = barColor, label = "Within Budget")
        ChartLegendDot(color = overBudgetColor, label = "Over Budget")
      }
      Text(
        text = "Budget: $currencySymbol${String.format("%.0f", monthlyBudget)}",
        style = MaterialTheme.typography.labelSmall,
        color = budgetLineColor,
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
      if (ordered.isEmpty()) return@Canvas
      val maxVal = max(monthlyBudget * 1.15, ordered.maxOf { it.billedAmount } * 1.15).coerceAtLeast(1.0)
      val slotWidth = size.width / ordered.size
      val barWidth = (slotWidth * 0.48f).coerceAtMost(38.dp.toPx())

      ordered.forEachIndexed { index, bill ->
        val ratio = (bill.billedAmount / maxVal).toFloat().coerceIn(0.05f, 1f)
        val barHeight = size.height * ratio
        val x = index * slotWidth + (slotWidth - barWidth) / 2f
        val y = size.height - barHeight
        val fill = if (bill.billedAmount > monthlyBudget) overBudgetColor else barColor
        drawRoundRect(
          color = fill,
          topLeft = Offset(x, y),
          size = Size(barWidth, barHeight),
          cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
        )
      }

      val budgetY = size.height - ((monthlyBudget / maxVal).toFloat().coerceIn(0f, 1f) * size.height)
      drawLine(
        color = budgetLineColor,
        start = Offset(0f, budgetY),
        end = Offset(size.width, budgetY),
        strokeWidth = 2.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
      ordered.forEach { bill ->
        Text(
          text = bill.monthLabel.take(4),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

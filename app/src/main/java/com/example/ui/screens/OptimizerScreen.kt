package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.OptimizationAction
import com.example.domain.SimulationParams
import com.example.ui.components.HourlyLoadProfileChart
import com.example.ui.viewmodel.WattWiseUiState
import kotlin.math.roundToInt

@Composable
fun OptimizerScreen(
  uiState: WattWiseUiState,
  onSimulationChange: (SimulationParams) -> Unit,
  onApplySimulationToSchedule: () -> Unit,
  onApplyRecommendation: (OptimizationAction) -> Unit,
  onApplyAllRecommendations: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val report = uiState.report
  val sim = uiState.simulationParams
  val currency = uiState.tariffConfig.currencySymbol

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Live What-If Simulator Card
      item {
        Card(
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Filled.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Spacer(Modifier.width(8.dp))
              Text("What-If Bill Simulator", style = MaterialTheme.typography.titleLarge)
            }

            // Before vs Simulated Comparison Box
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
              shape = MaterialTheme.shapes.medium,
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column {
                  Text(
                    "CURRENT BILL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Text(
                    "$currency${String.format("%.2f", report.currentMonthlyBill)}",
                    style = MaterialTheme.typography.titleLarge,
                  )
                  Text(
                    "${String.format("%.0f", report.totalMonthlyKwh)} kWh/mo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = "-$currency${String.format("%.2f", report.potentialMonthlySavings)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  Text(
                    text = "${String.format("%.1f", report.savingsPercent)}% cut",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }

                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    "SIMULATED",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  Text(
                    "$currency${String.format("%.2f", report.optimizedMonthlyBill)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  Text(
                    "${String.format("%.0f", report.optimizedMonthlyKwh)} kWh/mo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                }
              }
            }

            // 1. Off-Peak Shift Toggle
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text("Shift Flexible Loads Off-Peak", style = MaterialTheme.typography.titleMedium)
                Text(
                  "Move shiftable devices to 22:00–06:00 (${String.format("%.2f", uiState.tariffConfig.offPeakMultiplier)}x rate)",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(
                checked = sim.peakShiftRatio >= 0.5f,
                onCheckedChange = { checked ->
                  onSimulationChange(sim.copy(peakShiftRatio = if (checked) 1.0f else 0.0f))
                },
                modifier = Modifier.testTag("sim_peak_shift_switch"),
              )
            }

            // 2. HVAC & Water Heating Runtime Trim Slider
            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text("HVAC & Heating Eco Trim", style = MaterialTheme.typography.labelLarge)
                Text(
                  "-${sim.hvacTrimMinutesPerDay} min/day",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
              Slider(
                value = sim.hvacTrimMinutesPerDay.toFloat(),
                onValueChange = {
                  onSimulationChange(
                    sim.copy(hvacTrimMinutesPerDay = ((it / 15f).roundToInt() * 15).coerceIn(0, 120))
                  )
                },
                valueRange = 0f..120f,
                steps = 7,
                modifier = Modifier.testTag("sim_hvac_trim_slider"),
              )
            }

            // 3. Standby Load Cut Slider
            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text("Standby / Vampire Power Cut", style = MaterialTheme.typography.labelLarge)
                Text(
                  "-${sim.standbyCutPercent}%",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
              Slider(
                value = sim.standbyCutPercent.toFloat(),
                onValueChange = {
                  onSimulationChange(sim.copy(standbyCutPercent = it.roundToInt().coerceIn(0, 15)))
                },
                valueRange = 0f..15f,
                steps = 14,
                modifier = Modifier.testTag("sim_standby_slider"),
              )
            }

            Button(
              onClick = onApplySimulationToSchedule,
              enabled = uiState.appliances.isNotEmpty(),
              modifier = Modifier.fillMaxWidth().testTag("apply_simulation_btn"),
            ) {
              Icon(Icons.Filled.AutoAwesome, contentDescription = null)
              Spacer(Modifier.width(8.dp))
              Text("Commit Simulated Schedule to My Home")
            }
          }
        }
      }

      // Live Curve Preview
      if (uiState.appliances.isNotEmpty()) {
        item {
          Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text("Simulated 24-Hour Impact", style = MaterialTheme.typography.titleLarge)
              HourlyLoadProfileChart(points = report.hourlyProfile)
            }
          }
        }
      }

      // Algorithmic Recommendations List
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Recommended Actions", style = MaterialTheme.typography.titleLarge)
          if (report.recommendations.isNotEmpty()) {
            FilledTonalButton(
              onClick = onApplyAllRecommendations,
              modifier = Modifier.testTag("optimizer_apply_all_btn"),
            ) {
              Text("Apply All (${report.recommendations.size})")
            }
          }
        }
      }

      if (report.recommendations.isEmpty()) {
        item {
          Card(
            shape = MaterialTheme.shapes.medium,
            colors =
              CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
              ),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Text(
                text =
                  if (uiState.appliances.isEmpty())
                    "Add appliances first to generate personalized time-of-use and slab reduction recommendations."
                  else "All shiftable appliances are already scheduled off-peak!",
                style = MaterialTheme.typography.bodyMedium,
              )
            }
          }
        }
      } else {
        items(report.recommendations, key = { it.id }) { action ->
          RecommendationCard(
            action = action,
            currencySymbol = currency,
            onApply = { onApplyRecommendation(action) },
          )
        }
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }
    }
  }
}

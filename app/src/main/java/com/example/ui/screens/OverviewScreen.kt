package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.OptimizationAction
import com.example.ui.components.HourlyLoadProfileChart
import com.example.ui.components.TariffSlabProgressBar
import com.example.ui.theme.PeakAmber
import com.example.ui.theme.SurgeCoral
import com.example.ui.viewmodel.WattWiseUiState
import kotlin.math.min

@Composable
fun OverviewScreen(
  uiState: WattWiseUiState,
  onAddApplianceClick: () -> Unit,
  onLoadDemoClick: () -> Unit,
  onApplyRecommendation: (OptimizationAction) -> Unit,
  onApplyAllClick: () -> Unit,
  onOpenOptimizerTab: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val tariff = uiState.tariffConfig
  val report = uiState.report
  val currency = tariff.currencySymbol

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Hero Banner Card
      item {
        Card(
          shape = MaterialTheme.shapes.large,
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
          Box(modifier = Modifier.fillMaxWidth().height(168.dp)) {
            Image(
              painter = painterResource(id = R.drawable.img_energy_hero),
              contentDescription = stringResource(R.string.cd_hero_banner),
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop,
            )
            Box(
              modifier =
                Modifier.fillMaxSize()
                  .background(
                    Brush.verticalGradient(
                      colors =
                        listOf(
                          Color.Black.copy(alpha = 0.15f),
                          Color(0xFF06261B).copy(alpha = 0.88f),
                        )
                    )
                  )
            )
            Column(
              modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                shape = RoundedCornerShape(999.dp),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = tariff.planName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
              Text(
                text = "WattWise Energy Optimizer",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
              )
              Text(
                text =
                  if (uiState.appliances.isEmpty())
                    "Track appliances, slab brackets & off-peak schedules"
                  else
                    "${uiState.appliances.count { it.isEnabled }} active appliances • ${String.format("%.0f", report.totalMonthlyKwh)} kWh/mo",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
              )
            }
          }
        }
      }

      if (uiState.appliances.isEmpty()) {
        // First-launch / Empty State
        item {
          Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Box(
                modifier =
                  Modifier.size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Filled.ElectricMeter,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(32.dp),
                )
              }
              Text(
                text = stringResource(R.string.empty_title),
                style = MaterialTheme.typography.titleLarge,
              )
              Text(
                text = stringResource(R.string.empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Button(
                  onClick = onAddApplianceClick,
                  modifier = Modifier.weight(1f).testTag("empty_add_appliance_btn"),
                ) {
                  Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(6.dp))
                  Text("Add Appliance")
                }
                OutlinedButton(
                  onClick = onLoadDemoClick,
                  modifier = Modifier.weight(1f).testTag("empty_load_demo_btn"),
                ) {
                  Text("Demo Home")
                }
              }
            }
          }
        }
      } else {
        // Projected vs Optimized Bill Summary Card
        item {
          val budgetRatio =
            if (tariff.monthlyBudget > 0.0)
              (report.currentMonthlyBill / tariff.monthlyBudget).toFloat().coerceIn(0f, 1.5f)
            else 0f
          val overBudget = report.currentMonthlyBill > tariff.monthlyBudget

          Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
              ) {
                Column {
                  Text(
                    text = "PROJECTED MONTHLY BILL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Text(
                    text = "$currency${String.format("%.2f", report.currentMonthlyBill)}",
                    style = MaterialTheme.typography.displayMedium,
                    color =
                      if (overBudget) SurgeCoral else MaterialTheme.colorScheme.onSurface,
                  )
                }

                Surface(
                  color = MaterialTheme.colorScheme.primaryContainer,
                  shape = RoundedCornerShape(14.dp),
                ) {
                  Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.End,
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                      )
                      Spacer(Modifier.width(4.dp))
                      Text(
                        text = "Save $currency${String.format("%.2f", report.potentialMonthlySavings)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                      )
                    }
                    Text(
                      text =
                        "Target: $currency${String.format("%.2f", report.optimizedMonthlyBill)} (-${String.format("%.0f", report.savingsPercent)}%)",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                  }
                }
              }

              // Budget Progress
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = "Monthly Budget (${currency}${String.format("%.0f", tariff.monthlyBudget)})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Text(
                    text =
                      if (overBudget)
                        "Over by $currency${String.format("%.2f", report.currentMonthlyBill - tariff.monthlyBudget)}"
                      else
                        "${String.format("%.0f", budgetRatio * 100)}% used",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (overBudget) SurgeCoral else MaterialTheme.colorScheme.primary,
                  )
                }
                LinearProgressIndicator(
                  progress = { min(1f, budgetRatio) },
                  modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                  color = if (overBudget) SurgeCoral else MaterialTheme.colorScheme.primary,
                  trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
              }

              // Key Metrics Trio
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                MetricPill(
                  icon = Icons.Filled.Schedule,
                  label = "Peak Share",
                  value = "${String.format("%.0f", report.peakUsageSharePercent)}%",
                  tint = if (report.peakUsageSharePercent > 40.0) PeakAmber else MaterialTheme.colorScheme.primary,
                  modifier = Modifier.weight(1f),
                )
                MetricPill(
                  icon = Icons.Filled.Bolt,
                  label = "Marginal Rate",
                  value = "$currency${String.format("%.2f", report.marginalRatePerKwh)}/kWh",
                  tint = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.weight(1f),
                )
                MetricPill(
                  icon = Icons.Filled.Eco,
                  label = "CO₂ / Mo",
                  value = "${String.format("%.0f", report.estimatedMonthlyCo2Kg)} kg",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }
        }

        // Slab Jump Alert Banner (if in Tier 2+)
        if (report.nextLowerSlabThresholdKwh != null && report.kwhIntoCurrentSlab > 0.0) {
          item {
            Surface(
              color = MaterialTheme.colorScheme.tertiaryContainer,
              shape = MaterialTheme.shapes.medium,
              border = BorderStroke(1.dp, PeakAmber.copy(alpha = 0.45f)),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Icon(
                  imageVector = Icons.Outlined.WarningAmber,
                  contentDescription = null,
                  tint = PeakAmber,
                  modifier = Modifier.size(24.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Slab Threshold Opportunity",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                  )
                  Text(
                    text =
                      "You are ${String.format("%.0f", report.kwhIntoCurrentSlab)} kWh into ${report.currentSlabName}. " +
                        "Trimming ${String.format("%.0f", report.kwhIntoCurrentSlab)} kWh drops you below ${String.format("%.0f", report.nextLowerSlabThresholdKwh)} kWh!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                  )
                }
              }
            }
          }
        }

        // Tariff Slab Breakdown Card
        item {
          Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Text("Tariff Slab Breakdown", style = MaterialTheme.typography.titleLarge)
              TariffSlabProgressBar(
                slabBreakdowns = report.slabBreakdowns,
                totalMonthlyKwh = report.totalMonthlyKwh,
                currencySymbol = currency,
              )
              Text(
                text =
                  "Fixed & Tax: $currency${String.format("%.2f", report.currentFixedAndTax)} (${String.format("%.0f", tariff.taxPercent)}% tax + $currency${String.format("%.2f", tariff.fixedMonthlyCharge)} meter fee)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        // 24-Hour Load Profile Chart
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
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text("24-Hour Load Curve", style = MaterialTheme.typography.titleLarge)
                  Text(
                    "Peak shifting flattens 17:00–22:00 demand",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                FilledTonalButton(
                  onClick = onOpenOptimizerTab,
                  modifier = Modifier.testTag("overview_simulate_btn"),
                ) {
                  Text("Simulate")
                }
              }
              HourlyLoadProfileChart(points = report.hourlyProfile)
            }
          }
        }

        // Top Recommendations Preview
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Filled.TipsAndUpdates,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
              Spacer(Modifier.width(8.dp))
              Text("Top Bill-Cutting Actions", style = MaterialTheme.typography.titleLarge)
            }
            AnimatedVisibility(visible = report.recommendations.isNotEmpty()) {
              FilledTonalButton(
                onClick = onApplyAllClick,
                modifier = Modifier.testTag("overview_apply_all_btn"),
              ) {
                Text("Apply All")
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
                  containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                ),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Eco,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text =
                    "Your active schedule is already well-optimized! Try adjusting sliders in the Optimizer tab to explore further cuts.",
                  style = MaterialTheme.typography.bodyMedium,
                )
              }
            }
          }
        } else {
          items(report.recommendations.take(3), key = { it.id }) { action ->
            RecommendationCard(
              action = action,
              currencySymbol = currency,
              onApply = { onApplyRecommendation(action) },
            )
          }
        }
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }
    }
  }
}

@Composable
private fun MetricPill(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  value: String,
  tint: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    shape = RoundedCornerShape(14.dp),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
      )
    }
  }
}

@Composable
fun RecommendationCard(
  action: OptimizationAction,
  currencySymbol: String,
  onApply: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = action.title, style = MaterialTheme.typography.titleMedium)
        Text(
          text = action.description,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text =
            "Saves ~$currencySymbol${String.format("%.2f", action.estimatedMonthlySavings)}/mo" +
              if (action.estimatedKwhSaved > 0.5)
                " (${String.format("%.1f", action.estimatedKwhSaved)} kWh)"
              else "",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
      }
      Button(
        onClick = onApply,
        modifier = Modifier.testTag("apply_action_${action.id}"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
      ) {
        Text("Apply")
      }
    }
  }
}

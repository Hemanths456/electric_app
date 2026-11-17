package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.BillReadingEntity
import com.example.domain.TariffConfig
import com.example.ui.components.BillHistoryTrendChart
import com.example.ui.viewmodel.WattWiseUiState

@Composable
fun TariffAndBillsScreen(
  uiState: WattWiseUiState,
  onSaveTariff: (TariffConfig) -> Unit,
  onOpenAddBillDialog: () -> Unit,
  onDeleteBill: (Long) -> Unit,
  onClearAllData: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val tariff = uiState.tariffConfig
  var currency by remember(tariff) { mutableStateOf(tariff.currencySymbol) }
  var budgetText by remember(tariff) { mutableStateOf(tariff.monthlyBudget.toString()) }
  var fixedChargeText by remember(tariff) { mutableStateOf(tariff.fixedMonthlyCharge.toString()) }
  var taxText by remember(tariff) { mutableStateOf(tariff.taxPercent.toString()) }
  var peakMultText by remember(tariff) { mutableStateOf(tariff.peakMultiplier.toString()) }
  var offPeakMultText by remember(tariff) { mutableStateOf(tariff.offPeakMultiplier.toString()) }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Actual Monthly Bills & Trend Section
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
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Filled.ReceiptLong,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text("Bill & Meter History", style = MaterialTheme.typography.titleLarge)
              }
              Button(
                onClick = onOpenAddBillDialog,
                modifier = Modifier.testTag("open_log_bill_btn"),
              ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Log Bill")
              }
            }

            if (uiState.billReadings.isEmpty()) {
              Text(
                text =
                  "No past meter readings logged yet. Tap 'Log Bill' to track real monthly utility bills against your ${tariff.currencySymbol}${String.format("%.0f", tariff.monthlyBudget)} budget.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            } else {
              BillHistoryTrendChart(
                readings = uiState.billReadings,
                monthlyBudget = tariff.monthlyBudget,
                currencySymbol = tariff.currencySymbol,
              )
            }
          }
        }
      }

      if (uiState.billReadings.isNotEmpty()) {
        items(uiState.billReadings, key = { it.id }) { bill ->
          BillHistoryRow(
            bill = bill,
            currencySymbol = tariff.currencySymbol,
            onDelete = { onDeleteBill(bill.id) },
          )
        }
      }

      // Tariff & Budget Configuration Card
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
                imageVector = Icons.Filled.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Spacer(Modifier.width(8.dp))
              Text("Tariff Plan & Budget", style = MaterialTheme.typography.titleLarge)
            }

            Text(
              "Quick Tariff Presets",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              SuggestionChip(
                onClick = { onSaveTariff(TariffConfig()) },
                label = { Text("Tiered + TOU ($)") },
              )
              SuggestionChip(
                onClick = { onSaveTariff(TariffConfig.flatStandard()) },
                label = { Text("Flat Standard ($)") },
              )
              SuggestionChip(
                onClick = { onSaveTariff(TariffConfig.regionalRupeeSlabs()) },
                label = { Text("Slab Tariff (₹)") },
              )
              SuggestionChip(
                onClick = { onSaveTariff(TariffConfig.euroTouEco()) },
                label = { Text("Euro TOU (€)") },
              )
            }

            Text("Currency Symbol", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf("$", "₹", "€", "£").forEach { sym ->
                FilterChip(
                  selected = currency == sym,
                  onClick = { currency = sym },
                  label = { Text(sym) },
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedTextField(
                value = budgetText,
                onValueChange = { budgetText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Monthly Budget") },
                prefix = { Text(currency) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f).testTag("tariff_budget_input"),
              )
              OutlinedTextField(
                value = fixedChargeText,
                onValueChange = { fixedChargeText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Fixed Meter Fee") },
                prefix = { Text(currency) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedTextField(
                value = peakMultText,
                onValueChange = { peakMultText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Peak Multiplier") },
                suffix = { Text("x") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
              )
              OutlinedTextField(
                value = offPeakMultText,
                onValueChange = { offPeakMultText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Off-Peak Mult.") },
                suffix = { Text("x") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
              )
              OutlinedTextField(
                value = taxText,
                onValueChange = { taxText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Tax %") },
                suffix = { Text("%") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(0.8f),
              )
            }

            // Active Slab Rates Summary
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("Active Slab Brackets", style = MaterialTheme.typography.labelLarge)
              tariff.slabs.forEach { slab ->
                val range =
                  if (slab.maxKwh == null) "${slab.minKwh.toInt()}+ kWh"
                  else "${slab.minKwh.toInt()}–${slab.maxKwh.toInt()} kWh"
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = "${slab.name} ($range)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Text(
                    text = "$currency${String.format("%.2f", slab.ratePerKwh)}/kWh",
                    style = MaterialTheme.typography.labelMedium,
                  )
                }
              }
            }

            Button(
              onClick = {
                val updated =
                  tariff.copy(
                    currencySymbol = currency,
                    monthlyBudget = budgetText.toDoubleOrNull()?.coerceAtLeast(1.0)
                      ?: tariff.monthlyBudget,
                    fixedMonthlyCharge = fixedChargeText.toDoubleOrNull()?.coerceAtLeast(0.0)
                      ?: tariff.fixedMonthlyCharge,
                    taxPercent = taxText.toDoubleOrNull()?.coerceIn(0.0, 50.0)
                      ?: tariff.taxPercent,
                    peakMultiplier = peakMultText.toDoubleOrNull()?.coerceIn(0.5, 4.0)
                      ?: tariff.peakMultiplier,
                    offPeakMultiplier = offPeakMultText.toDoubleOrNull()?.coerceIn(0.2, 2.0)
                      ?: tariff.offPeakMultiplier,
                  )
                onSaveTariff(updated)
              },
              modifier = Modifier.fillMaxWidth().testTag("save_tariff_btn"),
            ) {
              Text("Save Tariff & Budget Settings")
            }

            if (uiState.appliances.isNotEmpty() || uiState.billReadings.isNotEmpty()) {
              OutlinedButton(
                onClick = onClearAllData,
                modifier = Modifier.fillMaxWidth().testTag("clear_all_data_btn"),
              ) {
                Text("Reset All Household Data", color = MaterialTheme.colorScheme.error)
              }
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }
    }
  }
}

@Composable
private fun BillHistoryRow(
  bill: BillReadingEntity,
  currencySymbol: String,
  onDelete: () -> Unit,
) {
  Card(
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "${bill.monthLabel} • ${String.format("%.0f", bill.kwhConsumed)} kWh",
          style = MaterialTheme.typography.titleMedium,
        )
        if (bill.notes.isNotBlank()) {
          Text(
            text = bill.notes,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "$currencySymbol${String.format("%.2f", bill.billedAmount)}",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        IconButton(onClick = onDelete) {
          Icon(
            imageVector = Icons.Filled.DeleteOutline,
            contentDescription = stringResource(R.string.cd_delete_bill),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

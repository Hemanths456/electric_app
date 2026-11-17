package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.BillReadingEntity
import kotlin.math.max

@Composable
fun BillReadingDialog(
  currencySymbol: String,
  onDismiss: () -> Unit,
  onSave: (BillReadingEntity) -> Unit,
) {
  var monthLabel by remember { mutableStateOf("Nov") }
  var prevReading by remember { mutableStateOf("") }
  var currReading by remember { mutableStateOf("") }
  var kwhText by remember { mutableStateOf("") }
  var amountText by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("") }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Log Monthly Bill / Meter", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = monthLabel,
          onValueChange = { monthLabel = it.take(12) },
          label = { Text("Billing Month (e.g., Nov)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("bill_month_input"),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = prevReading,
            onValueChange = { v ->
              prevReading = v.filter { it.isDigit() || it == '.' }
              val p = prevReading.toDoubleOrNull()
              val c = currReading.toDoubleOrNull()
              if (p != null && c != null && c >= p) {
                kwhText = String.format("%.1f", max(0.0, c - p))
              }
            },
            label = { Text("Prev Meter") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = currReading,
            onValueChange = { v ->
              currReading = v.filter { it.isDigit() || it == '.' }
              val p = prevReading.toDoubleOrNull()
              val c = currReading.toDoubleOrNull()
              if (p != null && c != null && c >= p) {
                kwhText = String.format("%.1f", max(0.0, c - p))
              }
            },
            label = { Text("Curr Meter") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
          )
        }
        OutlinedTextField(
          value = kwhText,
          onValueChange = {
            kwhText = it.filter { c -> c.isDigit() || c == '.' }
            errorMsg = null
          },
          label = { Text("Total Units Consumed (kWh)") },
          suffix = { Text("kWh") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth().testTag("bill_kwh_input"),
        )
        OutlinedTextField(
          value = amountText,
          onValueChange = {
            amountText = it.filter { c -> c.isDigit() || c == '.' }
            errorMsg = null
          },
          label = { Text("Billed Amount ($currencySymbol)") },
          prefix = { Text(currencySymbol) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth().testTag("bill_amount_input"),
        )
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it.take(80) },
          label = { Text("Notes (optional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        if (errorMsg != null) {
          Text(
            text = errorMsg!!,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val kwh = kwhText.toDoubleOrNull()
          val amt = amountText.toDoubleOrNull()
          when {
            monthLabel.isBlank() -> errorMsg = "Enter a month label."
            kwh == null || kwh <= 0.0 -> errorMsg = "Enter valid kWh consumed."
            amt == null || amt < 0.0 -> errorMsg = "Enter valid bill amount."
            else ->
              onSave(
                BillReadingEntity(
                  monthLabel = monthLabel.trim(),
                  kwhConsumed = kwh,
                  billedAmount = amt,
                  notes = notes.trim(),
                )
              )
          }
        },
        modifier = Modifier.testTag("save_bill_button"),
      ) {
        Text("Save")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

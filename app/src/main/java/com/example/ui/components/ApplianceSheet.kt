package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.ApplianceEntity
import com.example.domain.ApplianceCategory
import com.example.domain.TimeWindow
import kotlin.math.roundToInt

private data class AppliancePreset(
  val name: String,
  val category: ApplianceCategory,
  val watts: Int,
  val hours: Float,
  val window: TimeWindow,
  val shiftable: Boolean,
)

private val PRESETS =
  listOf(
    AppliancePreset("Split AC", ApplianceCategory.HVAC, 1500, 6f, TimeWindow.PEAK, false),
    AppliancePreset(
      "Water Heater",
      ApplianceCategory.WATER_HEATING,
      2000,
      1.5f,
      TimeWindow.PEAK,
      true,
    ),
    AppliancePreset("Washer/Dryer", ApplianceCategory.LAUNDRY, 1200, 1.5f, TimeWindow.PEAK, true),
    AppliancePreset("Dishwasher", ApplianceCategory.KITCHEN, 1350, 1f, TimeWindow.PEAK, true),
    AppliancePreset("EV Charger", ApplianceCategory.EV_OTHER, 3300, 3f, TimeWindow.OFF_PEAK, true),
    AppliancePreset(
      "Refrigerator",
      ApplianceCategory.KITCHEN,
      180,
      24f,
      TimeWindow.ALWAYS_ON,
      false,
    ),
  )

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ApplianceEditorSheet(
  existing: ApplianceEntity?,
  onDismiss: () -> Unit,
  onSave: (ApplianceEntity) -> Unit,
  onDelete: ((ApplianceEntity) -> Unit)? = null,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
  var wattageText by remember(existing) {
    mutableStateOf(existing?.wattageWatts?.toString() ?: "1000")
  }
  var dailyHours by remember(existing) {
    mutableFloatStateOf(existing?.dailyHours?.toFloat() ?: 2.0f)
  }
  var daysPerWeek by remember(existing) { mutableIntStateOf(existing?.daysPerWeek ?: 7) }
  var category by remember(existing) {
    mutableStateOf(existing?.category ?: ApplianceCategory.HVAC)
  }
  var window by remember(existing) { mutableStateOf(existing?.preferredWindow ?: TimeWindow.PEAK) }
  var isShiftable by remember(existing) { mutableStateOf(existing?.isShiftable ?: true) }
  var errorText by remember { mutableStateOf<String?>(null) }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .navigationBarsPadding()
          .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = if (existing == null) "Add Appliance" else "Edit ${existing.name}",
        style = MaterialTheme.typography.headlineMedium,
      )

      if (existing == null) {
        Text(
          text = "Quick presets",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          PRESETS.forEach { preset ->
            SuggestionChip(
              onClick = {
                name = preset.name
                category = preset.category
                wattageText = preset.watts.toString()
                dailyHours = preset.hours
                window = preset.window
                isShiftable = preset.shiftable
                errorText = null
              },
              label = { Text("${preset.name} (${preset.watts}W)") },
            )
          }
        }
      }

      OutlinedTextField(
        value = name,
        onValueChange = {
          name = it
          errorText = null
        },
        label = { Text("Appliance Name") },
        placeholder = { Text("e.g., Bedroom Air Conditioner") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("appliance_name_input"),
      )

      OutlinedTextField(
        value = wattageText,
        onValueChange = { input ->
          wattageText = input.filter { it.isDigit() }.take(5)
          errorText = null
        },
        label = { Text("Power Rating (Watts)") },
        suffix = { Text("W") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().testTag("appliance_wattage_input"),
      )

      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Daily Usage", style = MaterialTheme.typography.labelLarge)
          Text(
            text = "${String.format("%.1f", dailyHours)} hrs/day",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Slider(
          value = dailyHours,
          onValueChange = { dailyHours = ((it * 2f).roundToInt() / 2f).coerceIn(0.5f, 24f) },
          valueRange = 0.5f..24f,
          modifier = Modifier.testTag("appliance_hours_slider"),
        )
      }

      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Days per Week", style = MaterialTheme.typography.labelLarge)
          Text(
            text = "$daysPerWeek days/wk",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Slider(
          value = daysPerWeek.toFloat(),
          onValueChange = { daysPerWeek = it.roundToInt().coerceIn(1, 7) },
          valueRange = 1f..7f,
          steps = 5,
        )
      }

      Text("Category", style = MaterialTheme.typography.labelLarge)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        ApplianceCategory.entries.forEach { item ->
          FilterChip(
            selected = category == item,
            onClick = { category = item },
            label = { Text(item.label) },
          )
        }
      }

      Text("Usual Time-of-Use Window", style = MaterialTheme.typography.labelLarge)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        TimeWindow.entries.forEach { w ->
          FilterChip(
            selected = window == w,
            onClick = {
              window = w
              if (w == TimeWindow.ALWAYS_ON) {
                isShiftable = false
              }
            },
            label = { Text("${w.shortLabel} (${w.hoursDescription})") },
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
          Text("Flexible / Shiftable Load", style = MaterialTheme.typography.titleMedium)
          Text(
            "Allow WattWise to schedule this device during Off-Peak hours",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Switch(
          checked = isShiftable,
          onCheckedChange = { isShiftable = it },
          enabled = window != TimeWindow.ALWAYS_ON,
          modifier = Modifier.testTag("appliance_shiftable_switch"),
        )
      }

      if (errorText != null) {
        Text(
          text = errorText!!,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.error,
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (existing != null && onDelete != null) {
          TextButton(
            onClick = { onDelete(existing) },
            modifier = Modifier.testTag("delete_appliance_button"),
          ) {
            Text("Delete", color = MaterialTheme.colorScheme.error)
          }
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onDismiss) { Text("Cancel") }
        Button(
          onClick = {
            val watts = wattageText.toIntOrNull()
            when {
              name.isBlank() -> errorText = "Please enter an appliance name."
              watts == null || watts <= 0 -> errorText = "Enter a valid wattage above 0 W."
              else -> {
                val entity =
                  (existing ?: ApplianceEntity(
                    name = name.trim(),
                    category = category,
                    wattageWatts = watts,
                    dailyHours = dailyHours.toDouble(),
                    daysPerWeek = daysPerWeek,
                    preferredWindow = window,
                    isShiftable = isShiftable,
                  ))
                    .copy(
                      name = name.trim(),
                      category = category,
                      wattageWatts = watts,
                      dailyHours = dailyHours.toDouble(),
                      daysPerWeek = daysPerWeek,
                      preferredWindow = window,
                      isShiftable = isShiftable,
                    )
                onSave(entity)
              }
            }
          },
          modifier = Modifier.testTag("save_appliance_button"),
        ) {
          Text(if (existing == null) "Save Appliance" else "Update")
        }
      }
    }
  }
}

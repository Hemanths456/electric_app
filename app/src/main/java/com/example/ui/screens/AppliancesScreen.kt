package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ApplianceEntity
import com.example.domain.ApplianceCategory
import com.example.domain.ApplianceCostSummary
import com.example.domain.TimeWindow
import com.example.ui.theme.MidPeakTeal
import com.example.ui.theme.PeakAmber
import com.example.ui.viewmodel.WattWiseUiState

@Composable
fun AppliancesScreen(
  uiState: WattWiseUiState,
  onSearchChange: (String) -> Unit,
  onCategorySelect: (ApplianceCategory?) -> Unit,
  onAddAppliance: () -> Unit,
  onEditAppliance: (ApplianceEntity) -> Unit,
  onToggleAppliance: (Long, Boolean) -> Unit,
  onDeleteAppliance: (ApplianceEntity) -> Unit,
  onLoadDemo: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val summariesById = uiState.report.applianceSummaries.associateBy { it.applianceId }
  val filtered =
    uiState.appliances.filter { app ->
      val matchesCat = uiState.selectedCategory == null || app.category == uiState.selectedCategory
      val matchesQuery =
        uiState.searchQuery.isBlank() ||
          app.name.contains(uiState.searchQuery, ignoreCase = true) ||
          app.category.label.contains(uiState.searchQuery, ignoreCase = true)
      matchesCat && matchesQuery
    }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxSize()) {
      // Search & Category Chips Header
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = uiState.searchQuery,
          onValueChange = onSearchChange,
          placeholder = { Text("Search appliances (e.g., AC, Washer)") },
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
          trailingIcon = {
            if (uiState.searchQuery.isNotEmpty()) {
              IconButton(onClick = { onSearchChange("") }) {
                Icon(Icons.Filled.Close, contentDescription = "Clear search")
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth().testTag("appliance_search_input"),
        )

        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = uiState.selectedCategory == null,
            onClick = { onCategorySelect(null) },
            label = { Text("All (${uiState.appliances.size})") },
          )
          ApplianceCategory.entries.forEach { cat ->
            FilterChip(
              selected = uiState.selectedCategory == cat,
              onClick = {
                onCategorySelect(if (uiState.selectedCategory == cat) null else cat)
              },
              label = { Text(cat.label) },
            )
          }
        }
      }

      if (uiState.appliances.isEmpty()) {
        Column(
          modifier = Modifier.fillMaxSize().padding(24.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Icon(
            imageVector = Icons.Filled.PowerSettingsNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
          )
          Spacer(Modifier.height(12.dp))
          Text("No appliances added", style = MaterialTheme.typography.titleLarge)
          Spacer(Modifier.height(6.dp))
          Text(
            "Tap + to add your household devices or load a demo home to explore.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(Modifier.height(16.dp))
          OutlinedButton(onClick = onLoadDemo) { Text(stringResource(R.string.action_load_demo)) }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(filtered, key = { it.id }) { appliance ->
            val summary = summariesById[appliance.id]
            ApplianceItemCard(
              appliance = appliance,
              summary = summary,
              currencySymbol = uiState.tariffConfig.currencySymbol,
              onClick = { onEditAppliance(appliance) },
              onToggle = { enabled -> onToggleAppliance(appliance.id, enabled) },
              onDelete = { onDeleteAppliance(appliance) },
            )
          }
        }
      }
    }

    FloatingActionButton(
      onClick = onAddAppliance,
      modifier =
        Modifier.align(Alignment.BottomEnd)
          .padding(20.dp)
          .testTag("add_appliance_fab"),
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
      Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add_appliance))
    }
  }
}

@Composable
private fun ApplianceItemCard(
  appliance: ApplianceEntity,
  summary: ApplianceCostSummary?,
  currencySymbol: String,
  onClick: () -> Unit,
  onToggle: (Boolean) -> Unit,
  onDelete: () -> Unit,
) {
  val windowColor =
    when (appliance.preferredWindow) {
      TimeWindow.PEAK -> PeakAmber
      TimeWindow.MID_PEAK -> MidPeakTeal
      TimeWindow.OFF_PEAK -> MaterialTheme.colorScheme.primary
      TimeWindow.ALWAYS_ON -> MaterialTheme.colorScheme.onSurfaceVariant
    }

  Card(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(onClick = onClick)
        .testTag("appliance_card_${appliance.id}"),
    shape = MaterialTheme.shapes.medium,
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (appliance.isEnabled) MaterialTheme.colorScheme.surface
          else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
          Text(
            text = appliance.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text =
              "${appliance.category.label} • ${appliance.wattageWatts} W • ${String.format("%.1f", appliance.dailyHours)}h/day (${appliance.daysPerWeek}d/wk)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Switch(
          checked = appliance.isEnabled,
          onCheckedChange = onToggle,
          modifier = Modifier.testTag("toggle_appliance_${appliance.id}"),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color = windowColor.copy(alpha = 0.14f),
            shape = RoundedCornerShape(8.dp),
          ) {
            Text(
              text = appliance.preferredWindow.shortLabel,
              style = MaterialTheme.typography.labelSmall,
              color = windowColor,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
          if (appliance.isShiftable) {
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(8.dp),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Filled.SwapHoriz,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                  text = "Shiftable",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (summary != null) {
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "$currencySymbol${String.format("%.2f", summary.effectiveMonthlyCost)}/mo",
                style = MaterialTheme.typography.labelLarge,
                color =
                  if (appliance.isEnabled) MaterialTheme.colorScheme.onSurface
                  else MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = "${String.format("%.1f", summary.monthlyKwh)} kWh",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          IconButton(
            onClick = onDelete,
            modifier = Modifier.testTag("delete_appliance_icon_${appliance.id}"),
          ) {
            Icon(
              imageVector = Icons.Filled.DeleteOutline,
              contentDescription = stringResource(R.string.cd_delete_appliance),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

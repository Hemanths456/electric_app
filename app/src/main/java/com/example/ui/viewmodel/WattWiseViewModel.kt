package com.example.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ApplianceEntity
import com.example.data.local.BillReadingEntity
import com.example.data.repository.EnergyRepository
import com.example.domain.ApplianceCategory
import com.example.domain.BillOptimizerEngine
import com.example.domain.OptimizationAction
import com.example.domain.OptimizationReport
import com.example.domain.SimulationParams
import com.example.domain.TariffConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class WattWiseUiState(
  val isLoading: Boolean = true,
  val appliances: List<ApplianceEntity> = emptyList(),
  val billReadings: List<BillReadingEntity> = emptyList(),
  val tariffConfig: TariffConfig = TariffConfig(),
  val simulationParams: SimulationParams = SimulationParams(),
  val report: OptimizationReport =
    BillOptimizerEngine.computeBill(emptyList(), TariffConfig(), SimulationParams()),
  val selectedCategory: ApplianceCategory? = null,
  val searchQuery: String = "",
)

class WattWiseViewModel(private val repository: EnergyRepository) : ViewModel() {

  private val simulationParamsFlow = MutableStateFlow(SimulationParams())
  private val selectedCategoryFlow = MutableStateFlow<ApplianceCategory?>(null)
  private val searchQueryFlow = MutableStateFlow("")

  private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

  private val dataBundleFlow =
    combine(
      repository.appliancesFlow,
      repository.billReadingsFlow,
      repository.tariffConfigFlow,
      simulationParamsFlow,
    ) { appliances, readings, tariff, sim ->
      DataBundle(appliances, readings, tariff, sim)
    }

  val uiState: StateFlow<WattWiseUiState> =
    combine(dataBundleFlow, selectedCategoryFlow, searchQueryFlow) { bundle, category, query ->
        val report =
          BillOptimizerEngine.computeBill(
            appliances = bundle.appliances,
            tariff = bundle.tariff,
            simulation = bundle.simulation,
          )
        WattWiseUiState(
          isLoading = false,
          appliances = bundle.appliances,
          billReadings = bundle.readings,
          tariffConfig = bundle.tariff,
          simulationParams = bundle.simulation,
          report = report,
          selectedCategory = category,
          searchQuery = query,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = WattWiseUiState(),
      )

  fun setSelectedCategory(category: ApplianceCategory?) {
    selectedCategoryFlow.value = category
  }

  fun setSearchQuery(query: String) {
    searchQueryFlow.value = query
  }

  fun updateSimulationParams(params: SimulationParams) {
    simulationParamsFlow.value = params
  }

  fun saveAppliance(appliance: ApplianceEntity) {
    viewModelScope.launch {
      if (appliance.id == 0L) {
        repository.addAppliance(appliance)
        _snackbarMessages.tryEmit("Added ${appliance.name}")
      } else {
        repository.updateAppliance(appliance)
        _snackbarMessages.tryEmit("Updated ${appliance.name}")
      }
    }
  }

  fun toggleAppliance(id: Long, enabled: Boolean) {
    viewModelScope.launch { repository.setApplianceEnabled(id, enabled) }
  }

  fun deleteAppliance(appliance: ApplianceEntity) {
    viewModelScope.launch {
      repository.deleteAppliance(appliance.id)
      _snackbarMessages.tryEmit("Removed ${appliance.name}")
    }
  }

  fun applyOptimization(action: OptimizationAction, currencySymbol: String) {
    viewModelScope.launch {
      val applied = repository.applyOptimizationAction(action)
      if (applied) {
        val saved = String.format("%.2f", action.estimatedMonthlySavings)
        _snackbarMessages.tryEmit("Applied: ${action.title} (est. -$currencySymbol$saved/mo)")
      }
    }
  }

  fun applyAllRecommendations(actions: List<OptimizationAction>) {
    if (actions.isEmpty()) return
    viewModelScope.launch {
      var count = 0
      for (action in actions) {
        if (repository.applyOptimizationAction(action)) {
          count++
        }
      }
      if (count > 0) {
        _snackbarMessages.tryEmit("Applied $count smart schedule optimizations")
      }
    }
  }

  fun applySimulationToSchedule() {
    viewModelScope.launch {
      val count = repository.applySimulationSchedule(simulationParamsFlow.value)
      if (count > 0) {
        _snackbarMessages.tryEmit("Updated $count appliance schedules to match plan")
      } else {
        _snackbarMessages.tryEmit("Appliances already match the simulated schedule")
      }
    }
  }

  fun saveTariffConfig(config: TariffConfig) {
    viewModelScope.launch {
      repository.saveTariffConfig(config)
      _snackbarMessages.tryEmit("Saved tariff plan: ${config.planName}")
    }
  }

  fun addBillReading(reading: BillReadingEntity) {
    viewModelScope.launch {
      repository.addBillReading(reading)
      _snackbarMessages.tryEmit("Logged ${reading.monthLabel} meter bill")
    }
  }

  fun deleteBillReading(id: Long) {
    viewModelScope.launch {
      repository.deleteBillReading(id)
      _snackbarMessages.tryEmit("Removed bill record")
    }
  }

  fun loadDemoHousehold() {
    viewModelScope.launch {
      repository.loadSampleHousehold()
      _snackbarMessages.tryEmit("Loaded sample household appliances & bill history")
    }
  }

  fun clearAllData() {
    viewModelScope.launch {
      repository.clearAllData()
      _snackbarMessages.tryEmit("Cleared all appliances and bill history")
    }
  }

  private data class DataBundle(
    val appliances: List<ApplianceEntity>,
    val readings: List<BillReadingEntity>,
    val tariff: TariffConfig,
    val simulation: SimulationParams,
  )

  class Factory(private val repository: EnergyRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(WattWiseViewModel::class.java)) {
        return WattWiseViewModel(repository) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
  }
}

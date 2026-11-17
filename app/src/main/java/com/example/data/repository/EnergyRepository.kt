package com.example.data.repository

import com.example.data.local.ApplianceEntity
import com.example.data.local.BillReadingEntity
import com.example.data.local.EnergyDao
import com.example.data.local.TariffPreferencesRepository
import com.example.domain.ApplianceCategory
import com.example.domain.OptimizationAction
import com.example.domain.OptimizationActionType
import com.example.domain.SimulationParams
import com.example.domain.TariffConfig
import com.example.domain.TimeWindow
import kotlinx.coroutines.flow.Flow
import kotlin.math.max

class EnergyRepository(
  private val dao: EnergyDao,
  private val tariffPrefs: TariffPreferencesRepository,
) {
  val appliancesFlow: Flow<List<ApplianceEntity>> = dao.observeAppliances()
  val billReadingsFlow: Flow<List<BillReadingEntity>> = dao.observeBillReadings()
  val tariffConfigFlow: Flow<TariffConfig> = tariffPrefs.tariffConfigFlow

  suspend fun addAppliance(appliance: ApplianceEntity): Long = dao.insertAppliance(appliance)

  suspend fun updateAppliance(appliance: ApplianceEntity) = dao.updateAppliance(appliance)

  suspend fun setApplianceEnabled(id: Long, enabled: Boolean) =
    dao.setApplianceEnabled(id, enabled)

  suspend fun deleteAppliance(id: Long) = dao.deleteApplianceById(id)

  suspend fun addBillReading(reading: BillReadingEntity): Long = dao.insertBillReading(reading)

  suspend fun deleteBillReading(id: Long) = dao.deleteBillReadingById(id)

  suspend fun saveTariffConfig(config: TariffConfig) = tariffPrefs.saveTariffConfig(config)

  suspend fun applyOptimizationAction(action: OptimizationAction): Boolean {
    val targetId = action.applianceId ?: return false
    val current = dao.getAppliancesSnapshot().firstOrNull { it.id == targetId } ?: return false
    val updated =
      when (action.type) {
        OptimizationActionType.SHIFT_TO_OFF_PEAK ->
          current.copy(preferredWindow = action.targetWindow ?: TimeWindow.OFF_PEAK)
        OptimizationActionType.TRIM_SLAB_USAGE ->
          current.copy(dailyHours = action.recommendedDailyHours ?: current.dailyHours)
        OptimizationActionType.REDUCE_STANDBY ->
          current.copy(dailyHours = max(0.5, current.dailyHours * 0.9))
      }
    dao.updateAppliance(updated)
    return true
  }

  suspend fun applySimulationSchedule(simulation: SimulationParams): Int {
    val current = dao.getAppliancesSnapshot()
    var modifiedCount = 0
    for (app in current) {
      if (!app.isEnabled) continue
      var updated = app
      if (app.isShiftable &&
        (app.preferredWindow == TimeWindow.PEAK || app.preferredWindow == TimeWindow.MID_PEAK) &&
        simulation.peakShiftRatio >= 0.5f
      ) {
        updated = updated.copy(preferredWindow = TimeWindow.OFF_PEAK)
      }
      if (simulation.hvacTrimMinutesPerDay > 0 &&
        (app.category == ApplianceCategory.HVAC || app.category == ApplianceCategory.WATER_HEATING)
      ) {
        val trimHours = simulation.hvacTrimMinutesPerDay / 60.0
        updated = updated.copy(dailyHours = max(0.5, updated.dailyHours - trimHours))
      }
      if (updated != app) {
        dao.updateAppliance(updated)
        modifiedCount++
      }
    }
    return modifiedCount
  }

  suspend fun loadSampleHousehold() {
    val now = System.currentTimeMillis()
    val sampleAppliances =
      listOf(
        ApplianceEntity(
          name = "Split Air Conditioner",
          category = ApplianceCategory.HVAC,
          wattageWatts = 1500,
          dailyHours = 6.0,
          daysPerWeek = 7,
          preferredWindow = TimeWindow.PEAK,
          isShiftable = false,
          isEnabled = true,
          createdAt = now - 7000,
        ),
        ApplianceEntity(
          name = "Storage Water Heater",
          category = ApplianceCategory.WATER_HEATING,
          wattageWatts = 2000,
          dailyHours = 1.5,
          daysPerWeek = 7,
          preferredWindow = TimeWindow.PEAK,
          isShiftable = true,
          isEnabled = true,
          createdAt = now - 6000,
        ),
        ApplianceEntity(
          name = "Inverter Refrigerator",
          category = ApplianceCategory.KITCHEN,
          wattageWatts = 180,
          dailyHours = 24.0,
          daysPerWeek = 7,
          preferredWindow = TimeWindow.ALWAYS_ON,
          isShiftable = false,
          isEnabled = true,
          createdAt = now - 5000,
        ),
        ApplianceEntity(
          name = "Front-Load Washer & Dryer",
          category = ApplianceCategory.LAUNDRY,
          wattageWatts = 1200,
          dailyHours = 1.5,
          daysPerWeek = 4,
          preferredWindow = TimeWindow.PEAK,
          isShiftable = true,
          isEnabled = true,
          createdAt = now - 4000,
        ),
        ApplianceEntity(
          name = "Dishwasher Eco Cycle",
          category = ApplianceCategory.KITCHEN,
          wattageWatts = 1350,
          dailyHours = 1.0,
          daysPerWeek = 6,
          preferredWindow = TimeWindow.PEAK,
          isShiftable = true,
          isEnabled = true,
          createdAt = now - 3000,
        ),
        ApplianceEntity(
          name = "Home Workstation & TV",
          category = ApplianceCategory.ELECTRONICS,
          wattageWatts = 280,
          dailyHours = 5.5,
          daysPerWeek = 7,
          preferredWindow = TimeWindow.MID_PEAK,
          isShiftable = false,
          isEnabled = true,
          createdAt = now - 2000,
        ),
        ApplianceEntity(
          name = "LED Home Lighting",
          category = ApplianceCategory.LIGHTING,
          wattageWatts = 110,
          dailyHours = 5.0,
          daysPerWeek = 7,
          preferredWindow = TimeWindow.PEAK,
          isShiftable = false,
          isEnabled = true,
          createdAt = now - 1000,
        ),
      )

    val dayMs = 86_400_000L
    val sampleBills =
      listOf(
        BillReadingEntity(
          monthLabel = "Aug",
          kwhConsumed = 540.0,
          billedAmount = 162.40,
          notes = "Summer cooling peak",
          recordedAt = now - 90 * dayMs,
        ),
        BillReadingEntity(
          monthLabel = "Sep",
          kwhConsumed = 495.0,
          billedAmount = 144.80,
          notes = "Moderate evening usage",
          recordedAt = now - 60 * dayMs,
        ),
        BillReadingEntity(
          monthLabel = "Oct",
          kwhConsumed = 452.0,
          billedAmount = 128.50,
          notes = "Shifted laundry off-peak",
          recordedAt = now - 30 * dayMs,
        ),
      )

    dao.insertAppliances(sampleAppliances)
    dao.insertBillReadings(sampleBills)
  }

  suspend fun clearAllData() {
    dao.deleteAllAppliances()
    dao.deleteAllBillReadings()
  }
}

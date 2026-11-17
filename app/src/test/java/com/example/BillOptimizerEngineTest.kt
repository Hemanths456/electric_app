package com.example

import com.example.data.local.ApplianceEntity
import com.example.domain.ApplianceCategory
import com.example.domain.BillOptimizerEngine
import com.example.domain.SimulationParams
import com.example.domain.TariffConfig
import com.example.domain.TimeWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BillOptimizerEngineTest {

  @Test
  fun emptyAppliancesProducesZeroBill() {
    val report = BillOptimizerEngine.computeBill(emptyList(), TariffConfig())
    assertEquals(0.0, report.totalMonthlyKwh, 0.001)
    assertEquals(0.0, report.currentMonthlyBill, 0.001)
    assertTrue(report.recommendations.isEmpty())
  }

  @Test
  fun shiftingPeakApplianceReducesBillAndGeneratesRecommendation() {
    val geyser =
      ApplianceEntity(
        id = 1L,
        name = "Water Heater",
        category = ApplianceCategory.WATER_HEATING,
        wattageWatts = 2000,
        dailyHours = 2.0,
        daysPerWeek = 7,
        preferredWindow = TimeWindow.PEAK,
        isShiftable = true,
        isEnabled = true,
      )
    val ac =
      ApplianceEntity(
        id = 2L,
        name = "Split AC",
        category = ApplianceCategory.HVAC,
        wattageWatts = 1500,
        dailyHours = 6.0,
        daysPerWeek = 7,
        preferredWindow = TimeWindow.PEAK,
        isShiftable = false,
        isEnabled = true,
      )

    val report =
      BillOptimizerEngine.computeBill(
        appliances = listOf(geyser, ac),
        tariff = TariffConfig(),
        simulation = SimulationParams(peakShiftRatio = 1.0f, hvacTrimMinutesPerDay = 30, standbyCutPercent = 0),
      )

    // Geyser: 2kW * 2h * 30 = 120 kWh; AC: 1.5kW * 6h * 30 = 270 kWh -> 390 kWh total
    assertEquals(390.0, report.totalMonthlyKwh, 0.01)
    assertTrue(report.optimizedMonthlyBill < report.currentMonthlyBill)
    assertTrue(report.potentialMonthlySavings > 0.0)
    assertTrue(report.recommendations.any { it.applianceId == 1L })
    assertEquals(24, report.hourlyProfile.size)
  }
}

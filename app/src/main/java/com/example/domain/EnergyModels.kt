package com.example.domain

import androidx.compose.runtime.Immutable

enum class ApplianceCategory(val label: String) {
  HVAC("HVAC"),
  WATER_HEATING("Water Heat"),
  KITCHEN("Kitchen"),
  LAUNDRY("Laundry"),
  ELECTRONICS("Electronics"),
  LIGHTING("Lighting"),
  EV_OTHER("EV & Heavy"),
}

enum class TimeWindow(
  val label: String,
  val shortLabel: String,
  val hoursDescription: String,
) {
  OFF_PEAK("Off-Peak", "Off-Peak", "22:00 – 06:00"),
  MID_PEAK("Mid-Peak", "Mid-Peak", "06:00 – 17:00"),
  PEAK("Peak Hours", "Peak", "17:00 – 22:00"),
  ALWAYS_ON("24/7 Continuous", "24/7", "All day"),
}

@Immutable
data class TariffSlab(
  val name: String,
  val minKwh: Double,
  val maxKwh: Double?, // null means infinity (last tier)
  val ratePerKwh: Double,
)

@Immutable
data class TariffConfig(
  val planName: String = "Residential Tiered + TOU",
  val currencySymbol: String = "$",
  val monthlyBudget: Double = 150.0,
  val fixedMonthlyCharge: Double = 12.0,
  val taxPercent: Double = 8.0,
  val peakMultiplier: Double = 1.50,
  val midPeakMultiplier: Double = 1.00,
  val offPeakMultiplier: Double = 0.65,
  val slabs: List<TariffSlab> = defaultSlabs(),
) {
  fun multiplierFor(window: TimeWindow): Double =
    when (window) {
      TimeWindow.PEAK -> peakMultiplier
      TimeWindow.MID_PEAK -> midPeakMultiplier
      TimeWindow.OFF_PEAK -> offPeakMultiplier
      TimeWindow.ALWAYS_ON ->
        (peakMultiplier * 5.0 + midPeakMultiplier * 11.0 + offPeakMultiplier * 8.0) / 24.0
    }

  companion object {
    fun defaultSlabs(): List<TariffSlab> =
      listOf(
        TariffSlab("Tier 1 • Essential", 0.0, 150.0, 0.11),
        TariffSlab("Tier 2 • Standard", 150.0, 350.0, 0.17),
        TariffSlab("Tier 3 • High Usage", 350.0, 600.0, 0.25),
        TariffSlab("Tier 4 • Surge", 600.0, null, 0.34),
      )

    fun flatStandard(): TariffConfig =
      TariffConfig(
        planName = "Flat Standard Rate",
        currencySymbol = "$",
        monthlyBudget = 140.0,
        fixedMonthlyCharge = 10.0,
        taxPercent = 7.0,
        peakMultiplier = 1.0,
        midPeakMultiplier = 1.0,
        offPeakMultiplier = 1.0,
        slabs = listOf(TariffSlab("Flat Energy Rate", 0.0, null, 0.16)),
      )

    fun regionalRupeeSlabs(): TariffConfig =
      TariffConfig(
        planName = "Domestic Slab Tariff (₹)",
        currencySymbol = "₹",
        monthlyBudget = 2500.0,
        fixedMonthlyCharge = 110.0,
        taxPercent = 5.0,
        peakMultiplier = 1.25,
        midPeakMultiplier = 1.0,
        offPeakMultiplier = 0.80,
        slabs =
          listOf(
            TariffSlab("0–100 Units", 0.0, 100.0, 3.50),
            TariffSlab("101–250 Units", 100.0, 250.0, 5.80),
            TariffSlab("251–500 Units", 250.0, 500.0, 8.20),
            TariffSlab("500+ Units", 500.0, null, 10.50),
          ),
      )

    fun euroTouEco(): TariffConfig =
      TariffConfig(
        planName = "Euro Smart TOU (€)",
        currencySymbol = "€",
        monthlyBudget = 130.0,
        fixedMonthlyCharge = 14.0,
        taxPercent = 12.0,
        peakMultiplier = 1.60,
        midPeakMultiplier = 1.0,
        offPeakMultiplier = 0.60,
        slabs =
          listOf(
            TariffSlab("Base Allowance", 0.0, 200.0, 0.22),
            TariffSlab("Standard Band", 200.0, 450.0, 0.29),
            TariffSlab("Peak Excess", 450.0, null, 0.38),
          ),
      )
  }
}

@Immutable
data class ApplianceCostSummary(
  val applianceId: Long,
  val name: String,
  val category: ApplianceCategory,
  val wattageWatts: Int,
  val dailyHours: Double,
  val daysPerWeek: Int,
  val window: TimeWindow,
  val isShiftable: Boolean,
  val isEnabled: Boolean,
  val monthlyKwh: Double,
  val effectiveMonthlyCost: Double,
  val sharePercent: Double,
)

@Immutable
data class SlabChargeBreakdown(
  val slabName: String,
  val minKwh: Double,
  val maxKwh: Double?,
  val kwhUsed: Double,
  val baseRate: Double,
  val effectiveAverageRate: Double,
  val cost: Double,
)

@Immutable
data class HourlyLoadPoint(
  val hour: Int, // 0..23
  val currentKw: Double,
  val optimizedKw: Double,
  val window: TimeWindow,
)

enum class OptimizationActionType {
  SHIFT_TO_OFF_PEAK,
  TRIM_SLAB_USAGE,
  REDUCE_STANDBY,
}

@Immutable
data class OptimizationAction(
  val id: String,
  val applianceId: Long?,
  val applianceName: String,
  val type: OptimizationActionType,
  val title: String,
  val description: String,
  val estimatedMonthlySavings: Double,
  val estimatedKwhSaved: Double,
  val targetWindow: TimeWindow? = null,
  val recommendedDailyHours: Double? = null,
)

@Immutable
data class SimulationParams(
  val peakShiftRatio: Float = 1.0f, // 0f..1f of shiftable appliances moved to Off-Peak
  val hvacTrimMinutesPerDay: Int = 30, // 0..120 min reduction on HVAC/Water Heating
  val standbyCutPercent: Int = 10, // 0..15 % phantom load reduction on Electronics/Always-on
)

@Immutable
data class OptimizationReport(
  val totalMonthlyKwh: Double,
  val optimizedMonthlyKwh: Double,
  val currentEnergySubtotal: Double,
  val currentFixedAndTax: Double,
  val currentMonthlyBill: Double,
  val optimizedMonthlyBill: Double,
  val potentialMonthlySavings: Double,
  val savingsPercent: Double,
  val estimatedMonthlyCo2Kg: Double,
  val optimizedMonthlyCo2Kg: Double,
  val peakUsageSharePercent: Double,
  val marginalRatePerKwh: Double,
  val kwhIntoCurrentSlab: Double,
  val currentSlabName: String,
  val nextLowerSlabThresholdKwh: Double?,
  val slabBreakdowns: List<SlabChargeBreakdown>,
  val applianceSummaries: List<ApplianceCostSummary>,
  val hourlyProfile: List<HourlyLoadPoint>,
  val recommendations: List<OptimizationAction>,
)

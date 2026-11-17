package com.example.domain

import com.example.data.local.ApplianceEntity
import kotlin.math.max
import kotlin.math.min

object BillOptimizerEngine {

  private const val DAYS_IN_MONTH = 30.0
  private const val CO2_KG_PER_KWH = 0.42

  fun computeMonthlyKwh(appliance: ApplianceEntity): Double {
    if (!appliance.isEnabled) return 0.0
    val weeklyFactor = appliance.daysPerWeek.coerceIn(1, 7) / 7.0
    return (appliance.wattageWatts.coerceAtLeast(0) / 1000.0) *
      appliance.dailyHours.coerceIn(0.0, 24.0) *
      weeklyFactor *
      DAYS_IN_MONTH
  }

  fun computeBill(
    appliances: List<ApplianceEntity>,
    tariff: TariffConfig,
    simulation: SimulationParams = SimulationParams(),
  ): OptimizationReport {
    val activeAppliances = appliances.filter { it.isEnabled }
    val rawKwhById = activeAppliances.associate { it.id to computeMonthlyKwh(it) }
    val totalMonthlyKwh = rawKwhById.values.sum()

    val currentResult = calculateCostAndSlabs(activeAppliances, tariff)
    val hourlyProfile = buildHourlyProfile(activeAppliances, simulation)
    val recommendations = buildRecommendations(activeAppliances, tariff, currentResult)

    val simulatedAppliances = applySimulation(activeAppliances, simulation)
    val optimizedResult = calculateCostAndSlabs(simulatedAppliances, tariff)

    val optimizedMonthlyKwh = simulatedAppliances.sumOf { computeMonthlyKwh(it) }
    val savings = max(0.0, currentResult.totalBill - optimizedResult.totalBill)
    val savingsPct =
      if (currentResult.totalBill > 0.0) (savings / currentResult.totalBill) * 100.0 else 0.0

    val peakKwh =
      activeAppliances
        .filter { it.preferredWindow == TimeWindow.PEAK }
        .sumOf { computeMonthlyKwh(it) }
    val peakShare = if (totalMonthlyKwh > 0.0) (peakKwh / totalMonthlyKwh) * 100.0 else 0.0

    val summaries =
      appliances
        .map { app ->
          val kwh = computeMonthlyKwh(app)
          val share = if (totalMonthlyKwh > 0.0) (kwh / totalMonthlyKwh) * 100.0 else 0.0
          val avgWeightedEnergyCost =
            if (totalMonthlyKwh > 0.0) {
              val multiplier = tariff.multiplierFor(app.preferredWindow)
              val weightedKwh =
                activeAppliances.sumOf {
                  computeMonthlyKwh(it) * tariff.multiplierFor(it.preferredWindow)
                }
              if (weightedKwh > 0.0) {
                (kwh * multiplier / weightedKwh) * currentResult.energySubtotal
              } else 0.0
            } else 0.0

          ApplianceCostSummary(
            applianceId = app.id,
            name = app.name,
            category = app.category,
            wattageWatts = app.wattageWatts,
            dailyHours = app.dailyHours,
            daysPerWeek = app.daysPerWeek,
            window = app.preferredWindow,
            isShiftable = app.isShiftable,
            isEnabled = app.isEnabled,
            monthlyKwh = kwh,
            effectiveMonthlyCost = avgWeightedEnergyCost * (1.0 + tariff.taxPercent / 100.0),
            sharePercent = share,
          )
        }
        .sortedByDescending { it.effectiveMonthlyCost }

    val sortedSlabs = tariff.slabs.sortedBy { it.minKwh }
    val currentSlabIndex =
      sortedSlabs.indexOfLast { totalMonthlyKwh > it.minKwh }.coerceAtLeast(0)
    val currentSlab = sortedSlabs.getOrNull(currentSlabIndex)
    val kwhIntoCurrentSlab =
      if (currentSlab != null && totalMonthlyKwh > currentSlab.minKwh) {
        totalMonthlyKwh - currentSlab.minKwh
      } else {
        0.0
      }
    val nextLowerThreshold =
      if (currentSlabIndex > 0 && currentSlab != null) currentSlab.minKwh else null

    return OptimizationReport(
      totalMonthlyKwh = totalMonthlyKwh,
      optimizedMonthlyKwh = optimizedMonthlyKwh,
      currentEnergySubtotal = currentResult.energySubtotal,
      currentFixedAndTax =
        tariff.fixedMonthlyCharge +
          (currentResult.energySubtotal + tariff.fixedMonthlyCharge) * (tariff.taxPercent / 100.0),
      currentMonthlyBill = currentResult.totalBill,
      optimizedMonthlyBill = optimizedResult.totalBill,
      potentialMonthlySavings = savings,
      savingsPercent = savingsPct,
      estimatedMonthlyCo2Kg = totalMonthlyKwh * CO2_KG_PER_KWH,
      optimizedMonthlyCo2Kg = optimizedMonthlyKwh * CO2_KG_PER_KWH,
      peakUsageSharePercent = peakShare,
      marginalRatePerKwh = currentResult.marginalRate,
      kwhIntoCurrentSlab = kwhIntoCurrentSlab,
      currentSlabName = currentSlab?.name ?: "Standard",
      nextLowerSlabThresholdKwh = nextLowerThreshold,
      slabBreakdowns = currentResult.breakdowns,
      applianceSummaries = summaries,
      hourlyProfile = hourlyProfile,
      recommendations = recommendations,
    )
  }

  private data class InternalCostResult(
    val energySubtotal: Double,
    val totalBill: Double,
    val marginalRate: Double,
    val breakdowns: List<SlabChargeBreakdown>,
  )

  private fun calculateCostAndSlabs(
    activeAppliances: List<ApplianceEntity>,
    tariff: TariffConfig,
  ): InternalCostResult {
    val totalKwh = activeAppliances.sumOf { computeMonthlyKwh(it) }
    val weightedMultiplier =
      if (totalKwh > 0.0) {
        activeAppliances.sumOf {
          computeMonthlyKwh(it) * tariff.multiplierFor(it.preferredWindow)
        } / totalKwh
      } else {
        1.0
      }

    val sortedSlabs = tariff.slabs.sortedBy { it.minKwh }
    val breakdowns = mutableListOf<SlabChargeBreakdown>()
    var energySubtotal = 0.0
    var marginalRate = sortedSlabs.firstOrNull()?.ratePerKwh ?: 0.0

    for (slab in sortedSlabs) {
      val upper = slab.maxKwh ?: Double.MAX_VALUE
      val kwhInSlab =
        if (totalKwh > slab.minKwh) {
          min(totalKwh, upper) - slab.minKwh
        } else {
          0.0
        }
      val effectiveRate = slab.ratePerKwh * weightedMultiplier
      val slabCost = kwhInSlab * effectiveRate
      if (kwhInSlab > 0.0) {
        marginalRate = effectiveRate
      }
      energySubtotal += slabCost
      breakdowns.add(
        SlabChargeBreakdown(
          slabName = slab.name,
          minKwh = slab.minKwh,
          maxKwh = slab.maxKwh,
          kwhUsed = kwhInSlab,
          baseRate = slab.ratePerKwh,
          effectiveAverageRate = effectiveRate,
          cost = slabCost,
        )
      )
    }

    val preTax =
      if (activeAppliances.isEmpty()) 0.0 else energySubtotal + tariff.fixedMonthlyCharge
    val totalBill = preTax * (1.0 + tariff.taxPercent / 100.0)

    return InternalCostResult(
      energySubtotal = energySubtotal,
      totalBill = totalBill,
      marginalRate = marginalRate,
      breakdowns = breakdowns,
    )
  }

  private fun applySimulation(
    appliances: List<ApplianceEntity>,
    sim: SimulationParams,
  ): List<ApplianceEntity> {
    return appliances.map { app ->
      var updated = app
      // 1. Shiftable appliances moved to Off-Peak proportionally
      if (app.isShiftable &&
        (app.preferredWindow == TimeWindow.PEAK || app.preferredWindow == TimeWindow.MID_PEAK) &&
        sim.peakShiftRatio >= 0.5f
      ) {
        updated = updated.copy(preferredWindow = TimeWindow.OFF_PEAK)
      }
      // 2. HVAC & Water Heating eco runtime trim
      if (sim.hvacTrimMinutesPerDay > 0 &&
        (app.category == ApplianceCategory.HVAC || app.category == ApplianceCategory.WATER_HEATING)
      ) {
        val trimHours = sim.hvacTrimMinutesPerDay / 60.0
        val newHours = max(0.5, app.dailyHours - trimHours)
        updated = updated.copy(dailyHours = newHours)
      }
      // 3. Standby / vampire load reduction on Electronics & Always-On
      if (sim.standbyCutPercent > 0 &&
        (app.category == ApplianceCategory.ELECTRONICS ||
          app.preferredWindow == TimeWindow.ALWAYS_ON)
      ) {
        val factor = 1.0 - (sim.standbyCutPercent / 100.0)
        updated = updated.copy(dailyHours = max(0.25, app.dailyHours * factor))
      }
      updated
    }
  }

  private fun buildRecommendations(
    activeAppliances: List<ApplianceEntity>,
    tariff: TariffConfig,
    baseline: InternalCostResult,
  ): List<OptimizationAction> {
    if (activeAppliances.isEmpty()) return emptyList()
    val actions = mutableListOf<OptimizationAction>()

    // 1. TOU Peak/Mid-Peak Shifting actions
    activeAppliances
      .filter {
        it.isShiftable &&
          (it.preferredWindow == TimeWindow.PEAK || it.preferredWindow == TimeWindow.MID_PEAK)
      }
      .forEach { app ->
        val modified =
          activeAppliances.map {
            if (it.id == app.id) it.copy(preferredWindow = TimeWindow.OFF_PEAK) else it
          }
        val newCost = calculateCostAndSlabs(modified, tariff)
        val saved = max(0.0, baseline.totalBill - newCost.totalBill)
        if (saved >= 0.25) {
          actions.add(
            OptimizationAction(
              id = "shift_${app.id}",
              applianceId = app.id,
              applianceName = app.name,
              type = OptimizationActionType.SHIFT_TO_OFF_PEAK,
              title = "Shift ${app.name} to Off-Peak",
              description =
                "Running ${app.name} after 22:00 cuts its tariff multiplier from " +
                  "${String.format("%.2f", tariff.multiplierFor(app.preferredWindow))}x to " +
                  "${String.format("%.2f", tariff.offPeakMultiplier)}x.",
              estimatedMonthlySavings = saved,
              estimatedKwhSaved = 0.0,
              targetWindow = TimeWindow.OFF_PEAK,
            )
          )
        }
      }

    // 2. Slab-threshold & high-wattage runtime trimming actions
    val totalKwh = activeAppliances.sumOf { computeMonthlyKwh(it) }
    val sortedSlabs = tariff.slabs.sortedBy { it.minKwh }
    val currentSlabIdx = sortedSlabs.indexOfLast { totalKwh > it.minKwh }.coerceAtLeast(0)
    val excessIntoSlab =
      if (currentSlabIdx > 0) totalKwh - sortedSlabs[currentSlabIdx].minKwh else 0.0

    activeAppliances
      .filter {
        it.preferredWindow != TimeWindow.ALWAYS_ON &&
          it.dailyHours >= 1.5 &&
          (it.category == ApplianceCategory.HVAC ||
            it.category == ApplianceCategory.WATER_HEATING ||
            it.category == ApplianceCategory.ELECTRONICS ||
            it.category == ApplianceCategory.EV_OTHER)
      }
      .sortedByDescending { it.wattageWatts * it.dailyHours }
      .take(3)
      .forEach { app ->
        val trimHours = min(1.0, max(0.5, app.dailyHours * 0.2))
        val targetHours = max(0.5, app.dailyHours - trimHours)
        val modified =
          activeAppliances.map { if (it.id == app.id) it.copy(dailyHours = targetHours) else it }
        val newCost = calculateCostAndSlabs(modified, tariff)
        val saved = max(0.0, baseline.totalBill - newCost.totalBill)
        val kwhSaved = computeMonthlyKwh(app) - computeMonthlyKwh(app.copy(dailyHours = targetHours))
        if (saved >= 0.40) {
          val slabHint =
            if (excessIntoSlab > 0.0 && kwhSaved >= excessIntoSlab * 0.6) {
              " Helps drop you out of ${sortedSlabs[currentSlabIdx].name}!"
            } else ""
          actions.add(
            OptimizationAction(
              id = "trim_${app.id}",
              applianceId = app.id,
              applianceName = app.name,
              type = OptimizationActionType.TRIM_SLAB_USAGE,
              title = "Trim ${app.name} by ${(trimHours * 60).toInt()} min/day",
              description =
                "Eco scheduling reduces ${app.name} from ${String.format("%.1f", app.dailyHours)}h " +
                  "to ${String.format("%.1f", targetHours)}h/day, saving ${String.format("%.1f", kwhSaved)} kWh/mo.$slabHint",
              estimatedMonthlySavings = saved,
              estimatedKwhSaved = kwhSaved,
              recommendedDailyHours = targetHours,
            )
          )
        }
      }

    return actions.sortedByDescending { it.estimatedMonthlySavings }
  }

  private fun buildHourlyProfile(
    activeAppliances: List<ApplianceEntity>,
    simulation: SimulationParams,
  ): List<HourlyLoadPoint> {
    val optimizedAppliances = applySimulation(activeAppliances, simulation)
    return (0..23).map { hour ->
      val window = windowForHour(hour)
      val currentKw = activeAppliances.sumOf { kwAtHour(it, hour) }
      val optimizedKw = optimizedAppliances.sumOf { kwAtHour(it, hour) }
      HourlyLoadPoint(
        hour = hour,
        currentKw = currentKw,
        optimizedKw = optimizedKw,
        window = window,
      )
    }
  }

  private fun windowForHour(hour: Int): TimeWindow =
    when {
      hour in 17..21 -> TimeWindow.PEAK
      hour in 6..16 -> TimeWindow.MID_PEAK
      else -> TimeWindow.OFF_PEAK
    }

  private fun kwAtHour(app: ApplianceEntity, hour: Int): Double {
    if (!app.isEnabled) return 0.0
    val weeklyFactor = app.daysPerWeek.coerceIn(1, 7) / 7.0
    val baseKw = (app.wattageWatts / 1000.0) * weeklyFactor
    return when (app.preferredWindow) {
      TimeWindow.ALWAYS_ON -> baseKw * (app.dailyHours.coerceIn(0.0, 24.0) / 24.0)
      TimeWindow.PEAK -> {
        // Peak window spans 17..21 (5 hours); spread or cap inside window
        val windowHours = 5.0
        val activeHoursInPeak = min(app.dailyHours, windowHours)
        val spillHours = max(0.0, app.dailyHours - windowHours)
        when {
          hour in 17..21 -> baseKw * (activeHoursInPeak / windowHours)
          hour in 12..16 && spillHours > 0.0 -> baseKw * min(1.0, spillHours / 5.0)
          else -> 0.0
        }
      }
      TimeWindow.MID_PEAK -> {
        val windowHours = 11.0
        val active = min(app.dailyHours, windowHours)
        if (hour in 6..16) baseKw * (active / windowHours) else 0.0
      }
      TimeWindow.OFF_PEAK -> {
        val windowHours = 8.0
        val active = min(app.dailyHours, windowHours)
        if (hour >= 22 || hour <= 5) baseKw * (active / windowHours) else 0.0
      }
    }
  }
}

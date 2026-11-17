package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.TariffConfig
import com.example.domain.TariffSlab
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tariffDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "wattwise_tariff_prefs")

class TariffPreferencesRepository(private val context: Context) {

  private object Keys {
    val PLAN_NAME = stringPreferencesKey("plan_name")
    val CURRENCY = stringPreferencesKey("currency_symbol")
    val MONTHLY_BUDGET = doublePreferencesKey("monthly_budget")
    val FIXED_CHARGE = doublePreferencesKey("fixed_charge")
    val TAX_PERCENT = doublePreferencesKey("tax_percent")
    val PEAK_MULT = doublePreferencesKey("peak_multiplier")
    val MID_PEAK_MULT = doublePreferencesKey("mid_peak_multiplier")
    val OFF_PEAK_MULT = doublePreferencesKey("off_peak_multiplier")
    val SLABS_SERIALIZED = stringPreferencesKey("slabs_serialized")
  }

  val tariffConfigFlow: Flow<TariffConfig> =
    context.tariffDataStore.data.map { prefs ->
      val defaults = TariffConfig()
      val slabsRaw = prefs[Keys.SLABS_SERIALIZED]
      val parsedSlabs = parseSlabs(slabsRaw) ?: defaults.slabs
      TariffConfig(
        planName = prefs[Keys.PLAN_NAME] ?: defaults.planName,
        currencySymbol = prefs[Keys.CURRENCY] ?: defaults.currencySymbol,
        monthlyBudget = prefs[Keys.MONTHLY_BUDGET] ?: defaults.monthlyBudget,
        fixedMonthlyCharge = prefs[Keys.FIXED_CHARGE] ?: defaults.fixedMonthlyCharge,
        taxPercent = prefs[Keys.TAX_PERCENT] ?: defaults.taxPercent,
        peakMultiplier = prefs[Keys.PEAK_MULT] ?: defaults.peakMultiplier,
        midPeakMultiplier = prefs[Keys.MID_PEAK_MULT] ?: defaults.midPeakMultiplier,
        offPeakMultiplier = prefs[Keys.OFF_PEAK_MULT] ?: defaults.offPeakMultiplier,
        slabs = parsedSlabs,
      )
    }

  suspend fun saveTariffConfig(config: TariffConfig) {
    context.tariffDataStore.edit { prefs ->
      prefs[Keys.PLAN_NAME] = config.planName
      prefs[Keys.CURRENCY] = config.currencySymbol
      prefs[Keys.MONTHLY_BUDGET] = config.monthlyBudget
      prefs[Keys.FIXED_CHARGE] = config.fixedMonthlyCharge
      prefs[Keys.TAX_PERCENT] = config.taxPercent
      prefs[Keys.PEAK_MULT] = config.peakMultiplier
      prefs[Keys.MID_PEAK_MULT] = config.midPeakMultiplier
      prefs[Keys.OFF_PEAK_MULT] = config.offPeakMultiplier
      prefs[Keys.SLABS_SERIALIZED] = serializeSlabs(config.slabs)
    }
  }

  private fun serializeSlabs(slabs: List<TariffSlab>): String =
    slabs.joinToString(";;") { slab ->
      val safeName = slab.name.replace("|", " ").replace(";;", " ")
      val maxStr = slab.maxKwh?.toString() ?: "INF"
      "$safeName|${slab.minKwh}|$maxStr|${slab.ratePerKwh}"
    }

  private fun parseSlabs(raw: String?): List<TariffSlab>? {
    if (raw.isNullOrBlank()) return null
    val list =
      raw.split(";;").mapNotNull { entry ->
        val parts = entry.split("|")
        if (parts.size != 4) return@mapNotNull null
        val name = parts[0].ifBlank { "Tier" }
        val min = parts[1].toDoubleOrNull() ?: return@mapNotNull null
        val max = if (parts[2] == "INF") null else parts[2].toDoubleOrNull()
        val rate = parts[3].toDoubleOrNull() ?: return@mapNotNull null
        TariffSlab(name = name, minKwh = min, maxKwh = max, ratePerKwh = rate)
      }
    return list.takeIf { it.isNotEmpty() }
  }
}

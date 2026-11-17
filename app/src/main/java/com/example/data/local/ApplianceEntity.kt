package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.ApplianceCategory
import com.example.domain.TimeWindow

@Entity(tableName = "appliances")
data class ApplianceEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val category: ApplianceCategory,
  val wattageWatts: Int,
  val dailyHours: Double,
  val daysPerWeek: Int = 7,
  val preferredWindow: TimeWindow,
  val isShiftable: Boolean,
  val isEnabled: Boolean = true,
  val createdAt: Long = System.currentTimeMillis(),
)

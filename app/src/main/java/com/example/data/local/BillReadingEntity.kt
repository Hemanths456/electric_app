package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bill_readings")
data class BillReadingEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val monthLabel: String,
  val kwhConsumed: Double,
  val billedAmount: Double,
  val notes: String = "",
  val recordedAt: Long = System.currentTimeMillis(),
)

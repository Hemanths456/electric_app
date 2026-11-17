package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.domain.ApplianceCategory
import com.example.domain.TimeWindow

class EnergyTypeConverters {
  @TypeConverter
  fun fromCategory(value: ApplianceCategory): String = value.name

  @TypeConverter
  fun toCategory(raw: String): ApplianceCategory =
    ApplianceCategory.entries.firstOrNull { it.name == raw } ?: ApplianceCategory.ELECTRONICS

  @TypeConverter
  fun fromTimeWindow(value: TimeWindow): String = value.name

  @TypeConverter
  fun toTimeWindow(raw: String): TimeWindow =
    TimeWindow.entries.firstOrNull { it.name == raw } ?: TimeWindow.MID_PEAK
}

@Database(
  entities = [ApplianceEntity::class, BillReadingEntity::class],
  version = 1,
  exportSchema = false,
)
@TypeConverters(EnergyTypeConverters::class)
abstract class WattWiseDatabase : RoomDatabase() {
  abstract fun energyDao(): EnergyDao

  companion object {
    @Volatile private var instance: WattWiseDatabase? = null

    fun getInstance(context: Context): WattWiseDatabase =
      instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(
                context.applicationContext,
                WattWiseDatabase::class.java,
                "wattwise_energy.db",
              )
              .build()
              .also { instance = it }
        }
  }
}

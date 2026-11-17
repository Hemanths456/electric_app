package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EnergyDao {
  @Query("SELECT * FROM appliances ORDER BY createdAt DESC")
  fun observeAppliances(): Flow<List<ApplianceEntity>>

  @Query("SELECT * FROM appliances")
  suspend fun getAppliancesSnapshot(): List<ApplianceEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAppliance(appliance: ApplianceEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAppliances(appliances: List<ApplianceEntity>)

  @Update
  suspend fun updateAppliance(appliance: ApplianceEntity)

  @Query("UPDATE appliances SET isEnabled = :enabled WHERE id = :id")
  suspend fun setApplianceEnabled(id: Long, enabled: Boolean)

  @Query("DELETE FROM appliances WHERE id = :id")
  suspend fun deleteApplianceById(id: Long)

  @Query("DELETE FROM appliances")
  suspend fun deleteAllAppliances()

  @Query("SELECT * FROM bill_readings ORDER BY recordedAt DESC")
  fun observeBillReadings(): Flow<List<BillReadingEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBillReading(reading: BillReadingEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBillReadings(readings: List<BillReadingEntity>)

  @Query("DELETE FROM bill_readings WHERE id = :id")
  suspend fun deleteBillReadingById(id: Long)

  @Query("DELETE FROM bill_readings")
  suspend fun deleteAllBillReadings()
}

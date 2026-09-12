package com.umang620.offline_pos.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AddOnDao {
    @Query("SELECT * FROM add_ons ORDER BY category ASC, name ASC")
    fun getAllAddOns(): Flow<List<AddOnEntity>>

    @Query("SELECT * FROM add_ons WHERE isActive = 1 ORDER BY category ASC, name ASC")
    fun getActiveAddOns(): Flow<List<AddOnEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddOn(addOn: AddOnEntity): Long

    @Update
    suspend fun updateAddOn(addOn: AddOnEntity): Int

    @Delete
    suspend fun deleteAddOn(addOn: AddOnEntity): Int

    @Query("UPDATE add_ons SET isActive = :isActive WHERE id = :id")
    suspend fun setAddOnStatus(id: Long, isActive: Boolean): Int

    @Query("DELETE FROM add_ons")
    suspend fun deleteAllAddOns(): Int
}

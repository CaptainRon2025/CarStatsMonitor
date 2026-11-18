package com.carstats.monitor.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity): Long

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Delete
    suspend fun deleteTrip(trip: TripEntity)

    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripById(tripId: Long): TripEntity?

    @Insert
    suspend fun insertLogPoint(point: TripLogPointEntity)

    @Insert
    suspend fun insertLogPoints(points: List<TripLogPointEntity>)

    @Query("SELECT * FROM trip_log_points WHERE tripId = :tripId ORDER BY timestamp ASC")
    suspend fun getLogPointsForTrip(tripId: Long): List<TripLogPointEntity>
}

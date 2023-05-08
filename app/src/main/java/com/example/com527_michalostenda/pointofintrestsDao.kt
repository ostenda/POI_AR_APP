package com.example.com527_michalostenda

import androidx.room.*

@Dao
interface pointofintrestsDao {

    @Query("SELECT * FROM POINTS_OF_INTERESTS WHERE id=:id")
    fun getpoiById(id: Long): POIdata?

    @Query("SELECT * FROM POINTS_OF_INTERESTS")
    fun getAllpois(): List<POIdata>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(POINTS_OF_INTERESTS: POIdata) : Long

    @Update
    fun update(POINTS_OF_INTERESTS: POIdata) : Int

    @Delete
    fun delete(POINTS_OF_INTERESTS: POIdata) : Int
}
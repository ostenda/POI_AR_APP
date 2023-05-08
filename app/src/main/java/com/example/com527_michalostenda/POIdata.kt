package com.example.com527_michalostenda

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName="points_of_interests")

data class POIdata(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name="name") val name: String,
    @ColumnInfo(name="type") val type: String,
    @ColumnInfo(name="description") var description: String,

    @ColumnInfo(name="lat") val lat: Double,
    @ColumnInfo(name="lon") var lon: Double


)

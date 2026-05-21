package com.kyly.picking.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [BipagemPendenteEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bipagemPendenteDao(): BipagemPendenteDao
}

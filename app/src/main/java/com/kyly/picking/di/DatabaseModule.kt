package com.kyly.picking.di

import android.content.Context
import androidx.room.Room
import com.kyly.picking.data.local.AppDatabase
import com.kyly.picking.data.local.BipagemPendenteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "kyly_picking.db")
            .build()

    @Provides
    fun provideBipagemDao(db: AppDatabase): BipagemPendenteDao = db.bipagemPendenteDao()
}

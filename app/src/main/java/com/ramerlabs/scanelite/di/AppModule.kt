package com.ramerlabs.scanelite.di

import android.content.Context
import androidx.room.Room
import com.ramerlabs.scanelite.data.local.DocumentDao
import com.ramerlabs.scanelite.data.local.ScanEliteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDb(@ApplicationContext context: Context): ScanEliteDatabase =
        Room.databaseBuilder(context, ScanEliteDatabase::class.java, "scanelite.db").build()

    @Provides
    fun provideDao(db: ScanEliteDatabase): DocumentDao = db.documentDao()
}



package com.edusolve.com.project.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.edusolve.com.project.db.dao.DAOs
import com.edusolve.com.project.db.entity.HistoryEntity
import com.edusolve.com.project.db.entity.HistoryItemEntity


@TypeConverters(DateConverter::class)
@Database(
    entities = [HistoryEntity::class, HistoryItemEntity::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun historyDao(): DAOs.HistoryDao

    abstract fun historyItemDao(): DAOs.HistoryItemDao
}
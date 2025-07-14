
package com.edusolve.com.project.db.dao

import androidx.room.Dao
import com.edusolve.com.project.db.entity.HistoryEntity
import com.edusolve.com.project.db.entity.HistoryItemEntity

object DAOs {

    @Dao
    abstract class HistoryDao : BaseDao<HistoryEntity>("HistoryEntity")

    @Dao
    abstract class HistoryItemDao : BaseDao<HistoryItemEntity>("HistoryItemEntity")
}
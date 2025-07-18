

package com.edusolve.com.project.content.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.edusolve.com.project.db.dao.DAOs
import com.edusolve.com.project.db.entity.HistoryEntity
import com.edusolve.com.project.util.Constants.db
import kotlinx.coroutines.launch

class HistoryState(
    private val scope: LifecycleCoroutineScope,
    val history: MutableState<List<HistoryEntity>>
) {
    init {
        scope.launch { refreshHistories() }
    }

    private suspend fun refreshHistories() {
        history.value = getAllHistories()
    }

    private suspend fun getAllHistories() = db.historyDao().getAll().sortedByDescending { it.date }

    fun edit(
        id: Long,
        newTitle: String
    ) = scope.launch {
        crudAction(id) { item, dao ->
            dao.update(item.copy(title = newTitle.trim()))
        }
    }

    fun delete(
        id: Long
    ) = scope.launch {
        crudAction(id) { item, dao -> dao.delete(item) }
    }

    private suspend fun crudAction(
        id: Long,
        action: suspend (HistoryEntity, DAOs.HistoryDao) -> Unit
    ) {
        val dao = db.historyDao()
        val item = dao.getById(id)
        if (item != null) action(item, dao)
        refreshHistories()
    }
}

@Composable
fun rememberHistoryState(
    scope: LifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycleScope,
    history: MutableState<List<HistoryEntity>> = remember { mutableStateOf(listOf()) }
) = remember(scope, history) {
    HistoryState(scope, history)
}
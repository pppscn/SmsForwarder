package com.idormy.sms.forwarder.database.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.idormy.sms.forwarder.database.dao.TaskDao
import com.idormy.sms.forwarder.database.entity.Task
import com.idormy.sms.forwarder.database.ext.ioThread
import kotlinx.coroutines.flow.Flow

class TaskViewModel(private val dao: TaskDao) : ViewModel() {
    private var type: String = "sms"

    fun setType(type: String): TaskViewModel {
        this.type = type
        return this
    }

    val allTasks: Flow<PagingData<Task>> = Pager(
        config = PagingConfig(
            pageSize = 10,
            enablePlaceholders = false,
            initialLoadSize = 10
        )
    ) {
        dao.pagingSource(type)
    }.flow.cachedIn(viewModelScope)

    fun insertOrUpdate(task: Task) = ioThread {
        if (task.id > 0) dao.update(task) else dao.insert(task)
    }

    fun delete(id: Long) = ioThread {
        dao.delete(id)
    }
}
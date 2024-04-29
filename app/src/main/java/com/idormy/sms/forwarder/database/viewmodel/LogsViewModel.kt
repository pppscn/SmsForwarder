package com.idormy.sms.forwarder.database.viewmodel

import androidx.lifecycle.ViewModel
import com.idormy.sms.forwarder.database.dao.LogsDao
import com.idormy.sms.forwarder.database.ext.ioThread

class LogsViewModel(private val dao: LogsDao) : ViewModel() {
    //private var type: String = "sms"

    /*fun setType(type: String): LogsViewModel {
        this.type = type
        return this
    }

    val allLogs: Flow<PagingData<LogsAndRuleAndSender>> = Pager(
        config = PagingConfig(
            pageSize = 10,
            enablePlaceholders = false,
            initialLoadSize = 10
        )
    ) {
        dao.pagingSource(type)
    }.flow.cachedIn(viewModelScope)*/

    fun delete(id: Long) = ioThread {
        dao.delete(id)
    }

}
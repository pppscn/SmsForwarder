package com.idormy.sms.forwarder.database.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.idormy.sms.forwarder.database.dao.MsgDao
import com.idormy.sms.forwarder.database.entity.MsgAndLogs
import com.idormy.sms.forwarder.database.ext.ioThread
import kotlinx.coroutines.flow.Flow

class MsgViewModel(private val dao: MsgDao) : ViewModel() {
    private var type: String = "sms"

    fun setType(type: String): MsgViewModel {
        this.type = type
        return this
    }

    val allMsg: Flow<PagingData<MsgAndLogs>> = Pager(
        config = PagingConfig(
            pageSize = 10,
            enablePlaceholders = false,
            initialLoadSize = 10
        )
    ) {
        dao.pagingSource(type)
    }.flow.cachedIn(viewModelScope)

    fun delete(id: Long) = ioThread {
        dao.delete(id)
    }

}
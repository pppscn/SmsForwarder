package com.idormy.sms.forwarder.database.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.idormy.sms.forwarder.database.dao.SenderDao
import com.idormy.sms.forwarder.database.entity.Sender
import com.idormy.sms.forwarder.database.ext.ioThread
import kotlinx.coroutines.flow.Flow

class SenderViewModel(private val dao: SenderDao) : ViewModel() {
    private var status: Int = 1

    fun setStatus(status: Int): SenderViewModel {
        this.status = status
        return this
    }

    val allSenders: Flow<PagingData<Sender>> = Pager(
        config = PagingConfig(
            pageSize = 10,
            enablePlaceholders = false,
            initialLoadSize = 10
        )
    ) {
        dao.pagingSource(status)
    }.flow.cachedIn(viewModelScope)

    fun insertOrUpdate(sender: Sender) = ioThread {
        if (sender.id > 0) dao.update(sender) else dao.insert(sender)
    }

    fun delete(id: Long) = ioThread {
        dao.delete(id)
    }
}
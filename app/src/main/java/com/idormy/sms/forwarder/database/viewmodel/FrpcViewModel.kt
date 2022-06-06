package com.idormy.sms.forwarder.database.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.idormy.sms.forwarder.database.dao.FrpcDao
import com.idormy.sms.forwarder.database.entity.Frpc
import com.idormy.sms.forwarder.database.ext.ioThread
import kotlinx.coroutines.flow.Flow
import java.util.*

class FrpcViewModel(private val dao: FrpcDao) : ViewModel() {

    val allFrpc: Flow<PagingData<Frpc>> = Pager(
        config = PagingConfig(
            pageSize = 10,
            enablePlaceholders = false,
            initialLoadSize = 10
        )
    ) { dao.pagingSource() }.flow.cachedIn(viewModelScope)

    fun insert(name: String, config: String, autorun: Int) = ioThread {
        dao.insert(Frpc(uid = UUID.randomUUID().toString(), name = name, config = config, autorun = autorun))
    }

    fun insert(frpc: Frpc) = ioThread {
        frpc.uid = UUID.randomUUID().toString()
        dao.insert(frpc)
    }

    fun update(frpc: Frpc) = ioThread {
        dao.update(frpc)
    }

    fun delete(frpc: Frpc) = ioThread {
        dao.delete(frpc)
    }
}
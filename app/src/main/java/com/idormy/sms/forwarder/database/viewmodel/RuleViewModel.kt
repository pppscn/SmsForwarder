package com.idormy.sms.forwarder.database.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.idormy.sms.forwarder.database.dao.RuleDao
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.database.ext.ioThread
import kotlinx.coroutines.flow.Flow

class RuleViewModel(private val dao: RuleDao) : ViewModel() {
    private var type: String = "sms"

    fun setType(type: String): RuleViewModel {
        this.type = type
        return this
    }

    val allRules: Flow<PagingData<Rule>> = Pager(
        config = PagingConfig(
            pageSize = 10,
            enablePlaceholders = false,
            initialLoadSize = 10
        )
    ) { dao.pagingSource(type) }.flow.cachedIn(viewModelScope)

    fun insertOrUpdate(rule: Rule) = ioThread {
        if (rule.id > 0) dao.update(rule) else dao.insert(rule)
    }

    fun delete(id: Long) = ioThread {
        dao.delete(id)
    }
}
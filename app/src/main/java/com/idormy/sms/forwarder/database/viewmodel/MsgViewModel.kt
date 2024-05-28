package com.idormy.sms.forwarder.database.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.sqlite.db.SimpleSQLiteQuery
import com.idormy.sms.forwarder.database.dao.MsgDao
import com.idormy.sms.forwarder.database.entity.MsgAndLogs
import com.idormy.sms.forwarder.database.ext.ioThread
import com.idormy.sms.forwarder.utils.Log
import com.xuexiang.xutil.data.DateUtils
import kotlinx.coroutines.flow.Flow

class MsgViewModel(private val dao: MsgDao) : ViewModel() {
    private var type: String = "sms"
    private var filter: MutableMap<String, Any> = mutableMapOf()

    fun setType(type: String): MsgViewModel {
        this.type = type
        return this
    }

    fun setFilter(filter: MutableMap<String, Any>): MsgViewModel {
        this.filter = filter
        return this
    }

    val allMsg: Flow<PagingData<MsgAndLogs>> = Pager(
        config = PagingConfig(
            pageSize = 10,
            enablePlaceholders = false,
            initialLoadSize = 10
        )
    ) {
        if (filter.isEmpty()) {
            dao.pagingSource(type)
        } else {
            val sb = StringBuilder().apply {
                append("SELECT * FROM Msg WHERE type = '$type'")
                append(getOtherCondition())
                append(" ORDER BY id DESC")
            }

            //Log.d("MsgViewModel", "sql: $sb")
            val query = SimpleSQLiteQuery(sb.toString())
            dao.pagingSource(query)
        }

    }.flow.cachedIn(viewModelScope)

    fun delete(id: Long) = ioThread {
        dao.delete(id)
    }

    fun deleteAll() = ioThread {
        val sb = StringBuilder().apply {
            append("DELETE FROM Msg WHERE type = '$type'")
            if (filter.isNotEmpty()) {
                append(getOtherCondition())
            }
        }

        Log.d("MsgViewModel", "sql: $sb")
        val query = SimpleSQLiteQuery(sb.toString())
        dao.deleteAll(query)
    }

    private fun getOtherCondition(): String {
        return StringBuilder().apply {
            filter["from"]?.toString()?.takeIf { it.isNotEmpty() }?.let { append(" AND `from` LIKE '%$it%'") }
            filter["content"]?.toString()?.takeIf { it.isNotEmpty() }?.let { append(" AND content LIKE '%$it%'") }
            filter["title"]?.toString()?.takeIf { it.isNotEmpty() }?.let { append(" AND sim_info LIKE '%$it%'") }
            filter["start_time"]?.toString()?.takeIf { it.isNotEmpty() }?.let {
                val date = DateUtils.string2Date(it, DateUtils.yyyyMMddHHmmss.get())
                append(" AND time >= '${date.time}'")
            }
            filter["end_time"]?.toString()?.takeIf { it.isNotEmpty() }?.let {
                val date = DateUtils.string2Date(it, DateUtils.yyyyMMddHHmmss.get())
                append(" AND time <= '${date.time}'")
            }
            if (filter["sim_slot"] is Int && filter["sim_slot"] != -1) {
                append(" AND sim_slot = ${filter["sim_slot"]}")
            }
            val callTypeFilter = filter["call_type"] as? MutableList<*>
            if (!callTypeFilter.isNullOrEmpty()) {
                val callTypeString = callTypeFilter.joinToString(",") { it.toString() }
                append(" AND call_type IN ($callTypeString)")
            }
            val forwardStatusFilter = filter["forward_status"] as? MutableList<*>
            if (!forwardStatusFilter.isNullOrEmpty()) {
                val forwardStatusString = forwardStatusFilter.joinToString(",") { it.toString() }
                val subSql = "SELECT DISTINCT msg_id FROM Logs WHERE type = '$type' and forward_status IN ($forwardStatusString)"
                append(" AND id in ($subSql)")
            }
        }.toString()
    }

}
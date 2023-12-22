package com.idormy.sms.forwarder.database.repository

import androidx.annotation.WorkerThread
import com.idormy.sms.forwarder.database.dao.LogsDao
import com.idormy.sms.forwarder.database.entity.Logs

class LogsRepository(private val logsDao: LogsDao) {

    @WorkerThread
    suspend fun insert(logs: Logs): Long = logsDao.insert(logs)

    @WorkerThread
    fun delete(id: Long) = logsDao.delete(id)

    fun deleteAll() = logsDao.deleteAll()

    @WorkerThread
    fun updateStatus(id: Long, status: Int, response: String): Int = logsDao.updateStatus(id, status, response)

    fun getOne(id: Long) = logsDao.getOne(id)

}
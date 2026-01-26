package cn.ppps.forwarder.database.repository

import androidx.annotation.WorkerThread
import androidx.sqlite.db.SimpleSQLiteQuery
import cn.ppps.forwarder.database.dao.LogsDao
import cn.ppps.forwarder.database.entity.Logs

class LogsRepository(private val logsDao: LogsDao) {

    @WorkerThread
    suspend fun insert(logs: Logs): Long = logsDao.insert(logs)

    @WorkerThread
    fun delete(id: Long) = logsDao.delete(id)

    fun deleteAll() = logsDao.deleteAll()

    @WorkerThread
    fun updateStatus(id: Long, status: Int, response: String): Int = logsDao.updateStatus(id, status, response)

    @WorkerThread
    fun updateResponse(id: Long, response: String): Int = logsDao.updateResponse(id, response)

    fun getOne(id: Long) = logsDao.getOne(id)

    fun getIdsByTimeAndStatus(hours: Int, statusList: List<Int>): List<Logs> {
        var sql = "SELECT * FROM Logs WHERE 1=1"
        if (hours > 0) {
            val time = System.currentTimeMillis() - hours * 3600000
            sql += " AND time>=$time"
        }
        if (statusList.isNotEmpty()) {
            val statusListStr = statusList.joinToString(",")
            sql += " AND forward_status IN ($statusListStr)"
        }
        sql += " ORDER BY id ASC"

        val query = SimpleSQLiteQuery(sql)
        return logsDao.getLogsRaw(query)
    }

}
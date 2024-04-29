package com.idormy.sms.forwarder.database.repository

import androidx.annotation.WorkerThread
import androidx.sqlite.db.SimpleSQLiteQuery
import com.idormy.sms.forwarder.database.dao.TaskDao
import com.idormy.sms.forwarder.database.entity.Task
import io.reactivex.Single
import java.util.Date

class TaskRepository(private val taskDao: TaskDao) {

    @WorkerThread
    fun insert(task: Task): Long = taskDao.insert(task)

    @WorkerThread
    fun delete(id: Long) = taskDao.delete(id)

    fun deleteAll() = taskDao.deleteAll()

    fun update(task: Task) = taskDao.update(task)

    fun updateExecTime(taskId: Long, lastExecTime: Date, nextExecTime: Date, status: Int) = taskDao.updateExecTime(taskId, lastExecTime, nextExecTime, status)

    fun updateStatusByIds(ids: List<Long>, status: Int) = taskDao.updateStatusByIds(ids, status)

    fun get(id: Long) = taskDao.get(id)

    suspend fun getOne(id: Long) = taskDao.getOne(id)

    fun getAll(): Single<List<Task>> = taskDao.getAll()

    fun getAllNonCache(): List<Task> {
        val query = SimpleSQLiteQuery("SELECT * FROM Task ORDER BY id ASC")
        return taskDao.getAllRaw(query)
    }

    fun getByType(type: Int): List<Task> = taskDao.getByType(type)

}
package com.idormy.sms.forwarder.database.repository

import androidx.annotation.WorkerThread
import androidx.sqlite.db.SimpleSQLiteQuery
import com.idormy.sms.forwarder.database.dao.TaskDao
import com.idormy.sms.forwarder.database.entity.Task

class TaskRepository(private val taskDao: TaskDao) {

    @WorkerThread
    fun insert(task: Task) = taskDao.insert(task)

    fun getOne(id: Long) = taskDao.getOne(id)

    fun update(task: Task) = taskDao.update(task)

    fun getAllNonCache(): List<Task> {
        val query = SimpleSQLiteQuery("SELECT * FROM Task ORDER BY id ASC")
        return taskDao.getAllRaw(query)
    }

    @WorkerThread
    fun delete(id: Long) {
        taskDao.delete(id)
    }

    fun deleteAll() {
        taskDao.deleteAll()
    }
}
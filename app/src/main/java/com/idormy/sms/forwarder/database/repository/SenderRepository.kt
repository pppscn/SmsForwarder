package com.idormy.sms.forwarder.database.repository

import androidx.annotation.WorkerThread
import com.idormy.sms.forwarder.database.dao.SenderDao
import com.idormy.sms.forwarder.database.entity.Sender
import kotlinx.coroutines.flow.Flow
import androidx.sqlite.db.SimpleSQLiteQuery

class SenderRepository(private val senderDao: SenderDao) {

    private var listener: Listener? = null

    @WorkerThread
    fun insert(sender: Sender) = senderDao.insert(sender)

    @WorkerThread
    fun delete(id: Long) {
        listener?.onDelete(id)
        senderDao.delete(id)
    }

    fun get(id: Long) = senderDao.get(id)

    fun getOne(id: Long) = senderDao.getOne(id)

    fun update(sender: Sender) = senderDao.update(sender)

    fun getAllNonCache(): List<Sender> {
        val query = SimpleSQLiteQuery("SELECT * FROM Sender ORDER BY id ASC")
        return senderDao.getAllRaw(query)
    }

    val count: Flow<Long> = senderDao.getOnCount()

    fun deleteAll() {
        senderDao.deleteAll()
    }
}
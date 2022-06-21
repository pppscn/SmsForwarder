package com.idormy.sms.forwarder.database.repository

import androidx.annotation.WorkerThread
import com.idormy.sms.forwarder.database.dao.SenderDao
import com.idormy.sms.forwarder.database.entity.Sender
import kotlinx.coroutines.flow.Flow

class SenderRepository(private val senderDao: SenderDao) {

    var listener: Listener? = null

    @WorkerThread
    fun insert(sender: Sender) = senderDao.insert(sender)

    @WorkerThread
    fun delete(id: Long) {
        listener?.onDelete(id)
        senderDao.delete(id)
    }

    fun get(id: Long) = senderDao.get(id)

    fun update(sender: Sender) = senderDao.update(sender)

    val count: Flow<Long> = senderDao.getOnCount()

    //TODO:允许主线程访问，后面再优化
    val all: List<Sender> = senderDao.getAll2()

    fun deleteAll() {
        senderDao.deleteAll()
    }

}
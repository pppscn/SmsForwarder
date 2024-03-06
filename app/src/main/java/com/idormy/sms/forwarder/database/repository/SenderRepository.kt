package com.idormy.sms.forwarder.database.repository

import androidx.annotation.WorkerThread
import androidx.sqlite.db.SimpleSQLiteQuery
import com.idormy.sms.forwarder.database.dao.SenderDao
import com.idormy.sms.forwarder.database.entity.Sender
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow

class SenderRepository(private val senderDao: SenderDao) {

    private var listener: Listener? = null

    @WorkerThread
    fun insert(sender: Sender) = senderDao.insert(sender)

    @WorkerThread
    fun delete(id: Long) {
        listener?.onDelete(id)
        senderDao.delete(id)
    }

    fun deleteAll() = senderDao.deleteAll()

    fun update(sender: Sender) = senderDao.update(sender)

    fun updateStatusByIds(ids: List<Long>, status: Int) = senderDao.updateStatusByIds(ids, status)

    fun get(id: Long) = senderDao.get(id)

    fun getOne(id: Long) = senderDao.getOne(id)

    fun getByIds(ids: List<Long>, instr: String): List<Sender> {
        val senders = senderDao.getByIds(ids)
        // 将结果按照 instr() 的顺序进行排序
        return senders.sortedBy { instr.indexOf(it.id.toString()) }
    }

    fun getAllNonCache(): List<Sender> {
        val query = SimpleSQLiteQuery("SELECT * FROM Sender ORDER BY id ASC")
        return senderDao.getAllRaw(query)
    }

    fun getAll(): Single<List<Sender>> = senderDao.getAll()

    val count: Flow<Long> = senderDao.getOnCount()

}
package com.idormy.sms.forwarder.database.repository

import androidx.annotation.WorkerThread
import androidx.sqlite.db.SimpleSQLiteQuery
import com.idormy.sms.forwarder.database.dao.FrpcDao
import com.idormy.sms.forwarder.database.entity.Frpc
import io.reactivex.Single

class FrpcRepository(private val frpcDao: FrpcDao) {

    @WorkerThread
    fun insert(frpc: Frpc) = frpcDao.insert(frpc)

    @WorkerThread
    fun delete(uid: String) = frpcDao.delete(uid)

    fun deleteAll() = frpcDao.deleteAll()

    @WorkerThread
    fun update(frpc: Frpc) = frpcDao.update(frpc)

    @WorkerThread
    fun get(uid: String) = frpcDao.get(uid)

    fun getAllNonCache(): List<Frpc> {
        val query = SimpleSQLiteQuery("SELECT * FROM Frpc ORDER BY time DESC")
        return frpcDao.getAllRaw(query)
    }

    fun getAll(): Single<List<Frpc>> = frpcDao.getAll()

    fun getAutorun(): List<Frpc> = frpcDao.getAutorun()

    fun getByUids(uids: List<String>, instr: String): List<Frpc> {
        val frpcs = frpcDao.getByUids(uids)
        // 将结果按照 instr() 的顺序进行排序
        return frpcs.sortedBy { instr.indexOf(it.uid) }
    }

}
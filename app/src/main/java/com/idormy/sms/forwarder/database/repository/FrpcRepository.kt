package com.idormy.sms.forwarder.database.repository

import androidx.annotation.WorkerThread
import androidx.sqlite.db.SimpleSQLiteQuery
import com.idormy.sms.forwarder.database.dao.FrpcDao
import com.idormy.sms.forwarder.database.entity.Frpc

class FrpcRepository(
    private val frpcDao: FrpcDao,
) {

    @WorkerThread
    fun insert(frpc: Frpc) {
        frpcDao.insert(frpc)
    }

    @WorkerThread
    fun delete(uid: String) {
        frpcDao.delete(uid)
    }

    @WorkerThread
    fun get(uid: String) = frpcDao.get(uid)

    @WorkerThread
    fun update(frpc: Frpc) = frpcDao.update(frpc)

    fun getAllNonCache(): List<Frpc> {
        val query = SimpleSQLiteQuery("SELECT * FROM Frpc ORDER BY time DESC")
        return frpcDao.getAllRaw(query)
    }

    fun deleteAll() {
        frpcDao.deleteAll()
    }

}
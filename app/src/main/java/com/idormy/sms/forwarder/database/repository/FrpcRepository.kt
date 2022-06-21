package com.idormy.sms.forwarder.database.repository

import androidx.annotation.WorkerThread
import com.idormy.sms.forwarder.database.dao.FrpcDao
import com.idormy.sms.forwarder.database.entity.Frpc

class FrpcRepository(
    private val frpcDao: FrpcDao,
) {

    var listener: Listener? = null

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

    //TODO:允许主线程访问，后面再优化
    val all: List<Frpc> = frpcDao.getAll()

    fun deleteAll() {
        frpcDao.deleteAll()
    }

}
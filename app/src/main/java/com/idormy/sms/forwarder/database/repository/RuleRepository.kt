package com.idormy.sms.forwarder.database.repository

import androidx.annotation.WorkerThread
import com.idormy.sms.forwarder.database.dao.RuleDao
import com.idormy.sms.forwarder.database.entity.Rule

class RuleRepository(
    private val ruleDao: RuleDao,
) {

    var listener: Listener? = null

    @WorkerThread
    fun insert(rule: Rule) {
        ruleDao.insert(rule)
    }

    @WorkerThread
    fun delete(id: Long) {
        listener?.onDelete(id)
        ruleDao.delete(id)
    }

    @WorkerThread
    fun get(id: Long) = ruleDao.get(id)

    suspend fun getRuleAndSender(type: String, status: Int, simSlot: String) = ruleDao.getRuleAndSender(type, status, simSlot)

    fun getRuleList(type: String, status: Int, simSlot: String) = ruleDao.getRuleList(type, status, simSlot)

    @WorkerThread
    fun update(rule: Rule) = ruleDao.update(rule)

    //TODO:允许主线程访问，后面再优化
    val all: List<Rule> = ruleDao.getAll()
}
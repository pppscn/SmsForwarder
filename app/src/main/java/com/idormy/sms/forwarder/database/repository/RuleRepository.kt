package com.idormy.sms.forwarder.database.repository

import androidx.annotation.WorkerThread
import androidx.sqlite.db.SimpleSQLiteQuery
import com.idormy.sms.forwarder.database.dao.RuleDao
import com.idormy.sms.forwarder.database.entity.Rule

class RuleRepository(
    private val ruleDao: RuleDao,
) {

    private var listener: Listener? = null

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

    @WorkerThread
    fun getOne(id: Long) = ruleDao.getOne(id)

    fun getRuleList(type: String, status: Int, simSlot: String) = ruleDao.getRuleList(type, status, simSlot)

    @WorkerThread
    fun update(rule: Rule) = ruleDao.update(rule)

    fun getAllNonCache(): List<Rule> {
        val query = SimpleSQLiteQuery("SELECT * FROM Rule ORDER BY id ASC")
        return ruleDao.getAllRaw(query)
    }

    fun deleteAll() {
        ruleDao.deleteAll()
    }
}
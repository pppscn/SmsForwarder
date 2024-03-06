package com.idormy.sms.forwarder.database.repository

import androidx.annotation.WorkerThread
import androidx.sqlite.db.SimpleSQLiteQuery
import com.idormy.sms.forwarder.database.dao.RuleDao
import com.idormy.sms.forwarder.database.entity.Rule
import io.reactivex.Single

class RuleRepository(private val ruleDao: RuleDao) {

    private var listener: Listener? = null

    @WorkerThread
    fun insert(rule: Rule) = ruleDao.insert(rule)

    @WorkerThread
    fun delete(id: Long) {
        listener?.onDelete(id)
        ruleDao.delete(id)
    }

    fun deleteAll() = ruleDao.deleteAll()

    @WorkerThread
    fun update(rule: Rule) = ruleDao.update(rule)

    fun updateStatusByIds(ids: List<Long>, status: Int) = ruleDao.updateStatusByIds(ids, status)

    @WorkerThread
    fun get(id: Long) = ruleDao.get(id)

    @WorkerThread
    fun getOne(id: Long) = ruleDao.getOne(id)

    fun getAll(): Single<List<Rule>> = ruleDao.getAll()

    fun getAllNonCache(): List<Rule> {
        val query = SimpleSQLiteQuery("SELECT * FROM Rule ORDER BY id ASC")
        return ruleDao.getAllRaw(query)
    }

    fun getRuleList(type: String, status: Int, simSlot: String) = ruleDao.getRuleList(type, status, simSlot)

}
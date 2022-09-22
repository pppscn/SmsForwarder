package com.idormy.sms.forwarder.database.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.database.entity.RuleAndSender
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface RuleDao {

    @Insert
    fun insert(rule: Rule)

    @Delete
    fun delete(rule: Rule): Completable

    @Query("DELETE FROM Rule where id=:id")
    fun delete(id: Long)

    @Update
    fun update(rule: Rule)

    @Query("SELECT * FROM Rule where id=:id")
    fun get(id: Long): Single<Rule>

    @Query("SELECT count(*) FROM Rule where type=:type and status=:status")
    fun count(type: String, status: Int): Single<Int>

    /*@Query(
        "SELECT Rule.*," +
                "Sender.name as sender_name,Sender.type as sender_type" +
                " FROM Rule" +
                " LEFT JOIN Sender ON Rule.sender_id = Sender.id" +
                " where Rule.type=:type" +
                " ORDER BY Rule.time DESC"
    )
    fun pagingSource(type: String): PagingSource<Int, Rule>*/

    @Transaction
    @Query("SELECT * FROM Rule where type=:type ORDER BY id DESC")
    fun pagingSource(type: String): PagingSource<Int, RuleAndSender>

    @Transaction
    @Query("SELECT * FROM Rule where type=:type and status=:status and (sim_slot='ALL' or sim_slot=:simSlot)")
    suspend fun getRuleAndSender(type: String, status: Int, simSlot: String): List<RuleAndSender>

    @Transaction
    @Query("SELECT * FROM Rule where type=:type and status=:status and (sim_slot='ALL' or sim_slot=:simSlot)")
    fun getRuleList(type: String, status: Int, simSlot: String): List<Rule>

    //TODO:允许主线程访问，后面再优化
    @Query("SELECT * FROM Rule ORDER BY id ASC")
    fun getAll(): List<Rule>
}
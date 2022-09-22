package com.idormy.sms.forwarder.database.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.idormy.sms.forwarder.database.entity.Logs
import com.idormy.sms.forwarder.database.entity.LogsAndRuleAndSender
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface LogsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(logs: Logs): Long

    @Delete
    fun delete(logs: Logs): Completable

    @Query("DELETE FROM Logs where id=:id")
    fun delete(id: Long)

    @Query("DELETE FROM Logs where type=:type")
    fun deleteAll(type: String): Completable

    @Query("DELETE FROM Logs where time<:time")
    fun deleteTimeAgo(time: Long)

    @Update
    fun update(logs: Logs): Completable

    @Query("SELECT * FROM Logs where id=:id")
    fun get(id: Long): Single<Logs>

    @Query("SELECT count(*) FROM Logs where type=:type and forward_status=:forwardStatus")
    fun count(type: String, forwardStatus: Int): Single<Int>

    @Transaction
    @Query("SELECT * FROM Logs WHERE type = :type ORDER BY id DESC")
    fun pagingSource(type: String): PagingSource<Int, LogsAndRuleAndSender>

    @Query(
        "UPDATE Logs SET forward_status=:status" +
                ",forward_response=CASE WHEN (trim(forward_response) = '' or trim(forward_response) = 'ok')" +
                " THEN :response" +
                " ELSE forward_response || '\n--------------------\n' || :response" +
                " END" +
                " where id=:id"
    )
    fun updateStatus(id: Long, status: Int, response: String): Int
}
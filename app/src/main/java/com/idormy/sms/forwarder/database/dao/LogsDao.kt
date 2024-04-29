package com.idormy.sms.forwarder.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
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

    @Query("DELETE FROM Logs")
    fun deleteAll()

    @Update
    fun update(logs: Logs): Completable

    @Query(
        "UPDATE Logs SET forward_status=:status" +
                ",forward_response=CASE WHEN (trim(forward_response) = '' or trim(forward_response) = 'ok')" +
                " THEN :response" +
                " ELSE forward_response || '\n--------------------\n' || :response" +
                " END" +
                " where id=:id"
    )
    fun updateStatus(id: Long, status: Int, response: String): Int

    @Query(
        "UPDATE Logs SET forward_response=CASE WHEN (trim(forward_response) = '' or trim(forward_response) = 'ok')" +
                " THEN :response" +
                " ELSE forward_response || '\n' || :response" +
                " END" +
                " where id=:id"
    )
    fun updateResponse(id: Long, response: String): Int

    @Query("SELECT * FROM Logs where id=:id")
    fun get(id: Long): Single<Logs>

    @Transaction
    @Query("SELECT * FROM Logs where id=:id")
    fun getOne(id: Long): LogsAndRuleAndSender

    @Query("SELECT count(*) FROM Logs where type=:type and forward_status=:forwardStatus")
    fun count(type: String, forwardStatus: Int): Single<Int>

    @Transaction
    @Query("SELECT * FROM Logs WHERE type = :type ORDER BY id DESC")
    fun pagingSource(type: String): PagingSource<Int, LogsAndRuleAndSender>

    @Transaction
    @RawQuery(observedEntities = [Logs::class])
    fun getLogsRaw(query: SupportSQLiteQuery): List<Logs>
}
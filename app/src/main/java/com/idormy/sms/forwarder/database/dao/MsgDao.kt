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
import com.idormy.sms.forwarder.database.entity.Msg
import com.idormy.sms.forwarder.database.entity.MsgAndLogs
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface MsgDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(msg: Msg): Long

    @Delete
    fun delete(msg: Msg): Completable

    @Query("DELETE FROM Msg where id=:id")
    fun delete(id: Long)

    @RawQuery
    fun deleteAll(sql: SupportSQLiteQuery): Int

    @Query("DELETE FROM Msg")
    fun deleteAll()

    @Query("DELETE FROM Msg where time<:time")
    fun deleteTimeAgo(time: Long)

    @Update
    fun update(msg: Msg): Completable

    @Query("SELECT * FROM Msg where id=:id")
    fun get(id: Long): Single<Msg>

    @Query("SELECT count(*) FROM Msg where type=:type")
    fun count(type: String): Single<Int>

    @Transaction
    @Query("SELECT * FROM Msg WHERE type = :type ORDER BY id DESC")
    fun pagingSource(type: String): PagingSource<Int, MsgAndLogs>

    @Transaction
    @RawQuery(observedEntities = [MsgAndLogs::class])
    fun pagingSource(query: SupportSQLiteQuery): PagingSource<Int, MsgAndLogs>

}
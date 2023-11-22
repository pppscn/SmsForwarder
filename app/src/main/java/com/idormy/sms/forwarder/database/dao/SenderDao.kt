package com.idormy.sms.forwarder.database.dao

import androidx.paging.PagingSource
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.idormy.sms.forwarder.database.entity.Sender
import io.reactivex.Completable
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow

@Dao
interface SenderDao {

    @Insert
    fun insert(sender: Sender)

    @Delete
    fun delete(sender: Sender): Completable

    @Query("DELETE FROM Sender where id=:id")
    fun delete(id: Long)

    @Update
    fun update(sender: Sender)

    @Query("SELECT * FROM Sender where id=:id")
    fun get(id: Long): Single<Sender>

    @Query("SELECT * FROM Sender where id=:id")
    fun getOne(id: Long): Sender

    @Query("SELECT count(*) FROM Sender where type=:type and status=:status")
    fun count(type: String, status: Int): Single<Int>

    @Query("SELECT * FROM Sender where status=:status ORDER BY id DESC")
    fun pagingSource(status: Int): PagingSource<Int, Sender>

    @Query("SELECT * FROM Sender ORDER BY id DESC")
    fun getAll(): Single<List<Sender>>

    @Transaction
    @RawQuery(observedEntities = [Sender::class])
    fun getAllRaw(query: SupportSQLiteQuery): List<Sender>

    @Query("SELECT COUNT(id) FROM Sender WHERE status = 1")
    fun getOnCount(): Flow<Long>

    @Query("DELETE FROM Sender")
    fun deleteAll()
}
package com.idormy.sms.forwarder.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
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

    @Query("DELETE FROM Sender")
    fun deleteAll()

    @Update
    fun update(sender: Sender)

    @Query("UPDATE Sender SET status=:status WHERE id IN (:ids)")
    fun updateStatusByIds(ids: List<Long>, status: Int)

    @Query("SELECT * FROM Sender where id=:id")
    fun get(id: Long): Single<Sender>

    @Query("SELECT * FROM Sender where id=:id")
    fun getOne(id: Long): Sender

    //使用 ORDER BY 子句和 instr() 函数按照列表中 ID 的顺序返回结果
    //@Query("SELECT * FROM Sender WHERE id IN (:ids) ORDER BY instr(:instr, id)")
    //fun getByIds(ids: List<Long>, instr: String): List<Sender>
    @Query("SELECT * FROM Sender WHERE id IN (:ids)")
    fun getByIds(ids: List<Long>): List<Sender>

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

}
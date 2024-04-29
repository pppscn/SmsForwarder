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
import com.idormy.sms.forwarder.database.entity.Frpc
import io.reactivex.Single

@Dao
interface FrpcDao {

    @Insert
    fun insert(frpc: Frpc)

    @Delete
    fun delete(frpc: Frpc)

    @Query("DELETE FROM Frpc where uid=:uid")
    fun delete(uid: String)

    @Query("DELETE FROM Frpc")
    fun deleteAll()

    @Update
    fun update(frpc: Frpc)

    @Query("SELECT * FROM Frpc where uid=:uid")
    fun get(uid: String): Single<Frpc>

    //TODO:允许主线程访问，后面再优化
    @Query("SELECT * FROM Frpc where uid=:uid")
    fun getOne(uid: String): Frpc

    @Query("SELECT * FROM Frpc where autorun=1")
    fun getAutorun(): List<Frpc>

    //使用 ORDER BY 子句和 instr() 函数按照列表中 uid 的顺序返回结果
    //@Query("SELECT * FROM Frpc WHERE uid IN (:uids) ORDER BY instr(:instr, uid)")
    //fun getByUids(uids: List<String>, instr: String): List<Frpc>
    @Query("SELECT * FROM Frpc WHERE uid IN (:uids)")
    fun getByUids(uids: List<String>): List<Frpc>

    @Query("SELECT * FROM Frpc ORDER BY time DESC")
    fun pagingSource(): PagingSource<Int, Frpc>

    @Query("SELECT * FROM Frpc ORDER BY time DESC")
    fun getAll(): Single<List<Frpc>>

    @Transaction
    @RawQuery(observedEntities = [Frpc::class])
    fun getAllRaw(query: SupportSQLiteQuery): List<Frpc>

}
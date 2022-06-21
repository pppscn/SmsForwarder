package com.idormy.sms.forwarder.database.dao

import androidx.paging.PagingSource
import androidx.room.*
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

    @Update
    fun update(frpc: Frpc)

    @Query("SELECT * FROM Frpc where uid=:uid")
    fun get(uid: String): Single<Frpc>

    //TODO:允许主线程访问，后面再优化
    @Query("SELECT * FROM Frpc where autorun=1")
    fun getAutorun(): List<Frpc>

    @Query("SELECT * FROM Frpc ORDER BY time DESC")
    fun pagingSource(): PagingSource<Int, Frpc>

    //TODO:允许主线程访问，后面再优化
    @Query("SELECT * FROM Frpc ORDER BY time ASC")
    fun getAll(): List<Frpc>

    @Query("DELETE FROM Frpc")
    fun deleteAll()
}
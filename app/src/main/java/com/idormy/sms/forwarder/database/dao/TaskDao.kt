package com.idormy.sms.forwarder.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.idormy.sms.forwarder.database.entity.Task
import io.reactivex.Single
import java.util.Date

@Dao
interface TaskDao {

    @Query("SELECT * FROM Task where id=:id")
    fun get(id: Long): Single<Task>

    @Query("SELECT * FROM Task where id=:id")
    fun getOne(id: Long): Task

    @Query("SELECT * FROM Task ORDER BY id DESC")
    fun getAll(): List<Task>

    @Query("SELECT * FROM Task where type = 1000 ORDER BY id DESC")
    fun getAllCron(): List<Task>

    @Query("SELECT * FROM Task where type < 1000 ORDER BY id DESC")
    fun pagingSourceFixed(): PagingSource<Int, Task>

    @Query("SELECT * FROM Task where type >= 1000 ORDER BY id DESC")
    fun pagingSourceMine(): PagingSource<Int, Task>

    @Transaction
    @RawQuery(observedEntities = [Task::class])
    fun getAllRaw(query: SupportSQLiteQuery): List<Task>

    @Query("SELECT * FROM Task WHERE type = :taskType")
    fun getByType(taskType: Int): List<Task>

    //TODO:根据条件查询，不推荐使用
    @Query("SELECT * FROM Task WHERE type = :taskType AND conditions LIKE '%' || :conditionKey || '%' AND conditions LIKE '%' || :conditionValue || '%'")
    fun getByCondition(taskType: String, conditionKey: String, conditionValue: String): List<Task>

    @Insert
    fun insert(task: Task)

    @Update
    fun update(task: Task)

    @Query("UPDATE Task SET last_exec_time = :lastExecTime, next_exec_time = :nextExecTime, status = :status WHERE id = :taskId")
    fun updateExecTime(taskId: Long, lastExecTime: Date, nextExecTime: Date, status: Int)

    @Query("DELETE FROM Task WHERE id = :taskId")
    fun delete(taskId: Long)

    @Query("DELETE FROM Task")
    fun deleteAll()

}

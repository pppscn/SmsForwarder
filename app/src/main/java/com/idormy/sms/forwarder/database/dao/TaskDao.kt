package com.idormy.sms.forwarder.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(task: Task): Long

    @Query("DELETE FROM Task WHERE id = :taskId")
    fun delete(taskId: Long)

    @Query("DELETE FROM Task")
    fun deleteAll()

    @Update
    fun update(task: Task)

    @Query("UPDATE Task SET last_exec_time = :lastExecTime, next_exec_time = :nextExecTime, status = :status WHERE id = :taskId")
    fun updateExecTime(taskId: Long, lastExecTime: Date, nextExecTime: Date, status: Int)

    @Query("UPDATE Task SET status = :status WHERE id = :id")
    fun updateStatus(id: Long, status: Int)

    @Query("UPDATE Task SET status=:status WHERE id IN (:ids)")
    fun updateStatusByIds(ids: List<Long>, status: Int)

    @Query("SELECT * FROM Task where id=:id")
    fun get(id: Long): Single<Task>

    @Query("SELECT * FROM Task where id=:id")
    suspend fun getOne(id: Long): Task?

    @Query("SELECT * FROM Task where type < 1000 ORDER BY id DESC")
    fun pagingSourceFixed(): PagingSource<Int, Task>

    @Query("SELECT * FROM Task where type >= 1000 ORDER BY id DESC")
    fun pagingSourceMine(): PagingSource<Int, Task>

    @Query("SELECT * FROM Task ORDER BY id DESC")
    fun getAll(): Single<List<Task>>

    @Transaction
    @RawQuery(observedEntities = [Task::class])
    fun getAllRaw(query: SupportSQLiteQuery): List<Task>

    @Query("SELECT * FROM Task WHERE status = 1 AND type = :taskType")
    fun getByType(taskType: Int): List<Task>

}

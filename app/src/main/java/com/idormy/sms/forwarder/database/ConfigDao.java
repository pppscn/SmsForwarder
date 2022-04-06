package com.idormy.sms.forwarder.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;

@Dao
public interface ConfigDao {
    @Query("SELECT * FROM config")
    Single<List<Config>> getAll();

    @Query("SELECT * FROM config where uid=:uid")
    Single<Config> getConfigByUid(String uid);

    @Update
    Completable update(Config config);

    @Insert
    Completable insert(Config config);

    @Delete
    Completable delete(Config config);
}
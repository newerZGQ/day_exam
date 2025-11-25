package com.gorden.dayexam.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gorden.dayexam.db.entity.Config

@Dao
interface ConfigDao {
    @Insert
    fun insert(config: Config): Long

    @Query("SELECT * FROM config")
    fun getEntity(): Config

    @Query("SELECT * FROM config")
    fun get(): LiveData<Config>

    @Update
    fun update(config: Config)
}
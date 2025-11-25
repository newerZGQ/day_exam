package com.gorden.dayexam.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.gorden.dayexam.db.entity.DContext

@Dao
interface DContextDao {
    @Insert
    fun insert(dContext: DContext)

    @Update
    fun update(dContext: DContext)

    @Query("SELECT * FROM d_context WHERE id = 1")
    fun getDContext(): LiveData<DContext>

    @Query("SELECT * FROM d_context WHERE id = 1")
    fun getDContextEntity(): DContext

    // 非业务查询，用于将wal内容写入数据库，备份前调用
    @RawQuery
    fun checkpoint(supportSQLiteQuery: SupportSQLiteQuery): Int
}
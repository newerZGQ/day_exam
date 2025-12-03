package com.gorden.dayexam.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.gorden.dayexam.db.AppDatabase
import com.gorden.dayexam.db.converter.DateConverter
import com.gorden.dayexam.db.entity.*
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.executor.AppExecutors
import com.gorden.dayexam.repository.model.*
import java.util.*

object DataRepository {

    private lateinit var mDatabase: AppDatabase

    fun init(mDatabase: AppDatabase) {
        this.mDatabase = mDatabase
    }

    /**
     * Dao层状态相关
     */
    fun isDatabaseCreated(): LiveData<Boolean> {
        return this.mDatabase.isDatabaseCreated
    }

    fun isDatabaseOpened(): LiveData<Boolean> {
        return this.mDatabase.isDatabaseOpened
    }


    fun updateDContext(paperId: Int, questionId: Int) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val dContext = mDatabase.dContextDao().getDContextEntity()
                dContext.curPaperId = paperId
                dContext.curQuestionId = questionId
                mDatabase.dContextDao().update(dContext)
            }
        }
    }

    fun getDContext(): LiveData<DContext> {
        return mDatabase.dContextDao().getDContext()
    }

    /**
     * study status相关
     */

    fun getStudyStatus(type: Int, contentId: Int): LiveData<StudyStatus> {
        return mDatabase.studyStatusDao().queryByTypeAndContentId(type, contentId)
    }

    /**
     * paper相关
     */
    fun insertPaper(title: String, desc: String, path: String, questionCount: Int) {
        AppExecutors.diskIO().execute {
            val maxOrder = mDatabase.paperDao().getMaxPosition()
            val paperInfo = PaperInfo(
                title = title,
                path = path,
                hash = "",
                position = maxOrder + 1,
                lastStudyPosition = 0,
                questionCount = questionCount
            )
            val paperId = mDatabase.paperDao().insert(paperInfo).toInt()
            val paperStatus = StudyStatus(PaperStatus, paperId, 0)
            mDatabase.studyStatusDao().insert(paperStatus)
        }
    }

    fun insertPaperWithHash(title: String, desc: String, path: String, hash: String, questionCount: Int) {
        AppExecutors.diskIO().execute {
            val maxOrder = mDatabase.paperDao().getMaxPosition()
            val paperInfo = PaperInfo(
                title = title,
                path = path,
                hash = hash,
                position = maxOrder + 1,
                lastStudyPosition = 0,
                questionCount = questionCount
            )
            val paperId = mDatabase.paperDao().insert(paperInfo).toInt()
            val paperStatus = StudyStatus(PaperStatus, paperId, 0)
            mDatabase.studyStatusDao().insert(paperStatus)
        }
    }

    fun getPaperByHash(hash: String): PaperInfo? {
        return mDatabase.paperDao().getPaperByHash(hash)
    }

    fun currentPaper(): LiveData<PaperInfo> {
        val dContext = mDatabase.dContextDao().getDContext()
        return Transformations.switchMap(dContext) {
            dContext.value?.let {
                mDatabase.paperDao().getById(it.curPaperId)
            }
        }
    }

    fun updatePapers(paperInfos: List<PaperInfo>) {
        mDatabase.paperDao().update(paperInfos)
    }

    fun updatePaper(paperInfo: PaperInfo) {
        AppExecutors.diskIO().execute {
            mDatabase.paperDao().update(paperInfo)
        }
    }

    fun deletePaper(paperInfo: PaperInfo) {
        AppExecutors.diskIO().execute {
            mDatabase.paperDao().delete(paperInfo.id)
        }
    }

    /**
     * question相关
     */

    // 更新试卷状态以及dontext的当前question
     fun updatePaperStatus(paperId: Int, questionPosition: Int) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val paperStatus = mDatabase.studyStatusDao().queryEntityByTypeAndContentId(PaperStatus, paperId)
                mDatabase.studyStatusDao().update(paperStatus)
            }
        }
    }

    fun getAllPapers(): LiveData<List<PaperInfo>> {
        return mDatabase.paperDao().getAllPapers()
    }

    /**
     * StudyRecord相关
     */
    fun insertStudyRecord(studyRecord: StudyRecord) {
        AppExecutors.diskIO().execute {
            mDatabase.studyRecordDao().insert(studyRecord)
        }
    }

    fun todayStudyCount(): LiveData<Long> {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        val todayZero = DateConverter().toTimestamp(today.time)
        return mDatabase.studyRecordDao().getStudyCountAfter(todayZero)
    }

    /**
     * Config相关
     */
    fun getConfig(): LiveData<Config> {
        return mDatabase.configDao().get()
    }

    fun updateRememberMode(opened: Boolean) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val config = mDatabase.configDao().getEntity()
                config.rememberMode = opened
                mDatabase.configDao().update(config)
            }
        }
    }

    fun updateFocusMode(opened: Boolean) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val config = mDatabase.configDao().getEntity()
                config.focusMode = opened
                mDatabase.configDao().update(config)
            }
        }
    }

    fun updateOnlyFavoriteMode(opened: Boolean) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val config = mDatabase.configDao().getEntity()
                config.onlyFavorite = opened
                mDatabase.configDao().update(config)
            }
        }
    }

    fun updateSortAccuracyMode(opened: Boolean) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val config = mDatabase.configDao().getEntity()
                config.sortByAccuracy = opened
                mDatabase.configDao().update(config)
            }
        }
    }

    /**
     * 搜索search相关
     */
    fun searchByScopeAndKey(scope: Int, key: String, liveSearchItems: MutableLiveData<List<SearchItem>>) {

    }

}


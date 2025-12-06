package com.gorden.dayexam.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.gorden.dayexam.ContextHolder
import com.gorden.dayexam.db.AppDatabase
import com.gorden.dayexam.db.converter.DateConverter
import com.gorden.dayexam.db.entity.*
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.executor.AppExecutors
import com.gorden.dayexam.repository.model.*
import com.gorden.dayexam.utils.SharedPreferenceUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.*

object DataRepository {

    private lateinit var mDatabase: AppDatabase
    private val curPaperIdLiveData = MutableLiveData<Int>()

    fun init(mDatabase: AppDatabase) {
        this.mDatabase = mDatabase
        val paperId = SharedPreferenceUtil.getInt("cur_paper_id", -1)
        curPaperIdLiveData.value = paperId
    }

    fun updateCurPaperId(paperId: Int) {
        SharedPreferenceUtil.setInt("cur_paper_id", paperId)
        curPaperIdLiveData.value = paperId
    }

    fun getCurPaperId(): LiveData<Int> {
        return curPaperIdLiveData
    }

    fun getCurPaperInfo(): PaperInfo? {
        return getPaperById(curPaperIdLiveData.value ?: -1)
    }

    /**
     * study status相关
     */

    fun getStudyStatus(type: Int, contentId: Int): LiveData<StudyStatus> {
        return mDatabase.studyStatusDao().queryByTypeAndContentId(type, contentId)
    }

    fun insertPaperWithHash(title: String, path: String, hash: String, questionCount: Int) {
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

    fun getPaperByHash(hash: String): PaperInfo? {
        return mDatabase.paperDao().getPaperByHash(hash)
    }

    fun getPaperById(id: Int): PaperInfo? {
        return mDatabase.paperDao().getEntityById(id)
    }

    fun currentPaper(): LiveData<PaperInfo> {
        return Transformations.switchMap(curPaperIdLiveData) { paperId ->
            paperId?.let {
                mDatabase.paperDao().getById(it)
            }
        }
    }

    fun updatePapers(paperInfos: List<PaperInfo>) {
        mDatabase.paperDao().update(paperInfos)
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

    /**
     * 问题相关
     */
    fun getQuestionsByPaperId(paperId: Int): List<QuestionDetail> {
        val paperInfo = getPaperById(paperId) ?: return emptyList()
        return getQuestionsFromCache(paperInfo.hash)
    }

    fun getPaperDetailById(paperId: Int): PaperDetail? {
        val paperInfo = getPaperById(paperId) ?: return null
        val questions = getQuestionsFromCache(paperInfo.hash)
        if (questions.isEmpty()) return null
        return PaperDetail(paperInfo, PaperStudyInfo(0, null), questions)
    }

    private fun getQuestionsFromCache(paperHash: String): List<QuestionDetail> {
        return try {
            val jsonFile = File(ContextHolder.application.cacheDir, "$paperHash/questions.json")
            if (!jsonFile.exists()) {
                emptyList()
            } else {
                val jsonString = jsonFile.readText()
                val gson = Gson()
                val type = object : TypeToken<List<QuestionDetail>>() {}.type
                gson.fromJson(jsonString, type) ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 搜索search相关
     */
    fun searchByScopeAndKey(scope: Int, key: String, liveSearchItems: MutableLiveData<List<SearchItem>>) {

    }

}


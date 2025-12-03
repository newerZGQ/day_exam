package com.gorden.dayexam.ui.paper

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.repository.DataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PaperListViewModel(application: Application): AndroidViewModel(application) {

    private val allPapers = DataRepository.getAllPapers()

    fun getAllPapers(): LiveData<List<PaperInfo>> {
        return allPapers
    }

    /**
     * 使用协程在 IO 线程更新试卷标题，返回是否成功
     */
    suspend fun updatePaperTitle(paperInfo: PaperInfo, newTitle: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                paperInfo.title = newTitle
                DataRepository.updatePaper(paperInfo)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * 使用协程在 IO 线程删除试卷，返回是否成功
     */
    suspend fun deletePaper(paperInfo: PaperInfo): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                DataRepository.deletePaper(paperInfo)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * 根据当前列表顺序更新试卷 position，用于拖拽排序后持久化
     */
    suspend fun updatePaperOrder(papers: List<PaperInfo>) {
        withContext(Dispatchers.IO) {
            try {
                papers.forEachIndexed { index, paper ->
                    paper.position = index
                }
                DataRepository.updatePapers(papers)
            } catch (e: Exception) {
                // 排序失败这里先忽略，必要时可以加日志或上报
            }
        }
    }
}
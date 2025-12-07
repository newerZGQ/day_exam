package com.gorden.dayexam.ui.sheet.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.repository.PaperDetailCache
import com.gorden.dayexam.repository.model.SearchItem

class SearchViewModel: ViewModel() {
    private val searchResult = MutableLiveData<List<SearchItem>>()

    fun search(key: String) {
        // 如果搜索关键词为空，清空结果
        if (key.isBlank()) {
            searchResult.value = emptyList()
            return
        }
        
        val curPaperId = DataRepository.getCurPaperId().value ?: -1
        
        // 如果没有当前 paper，清空结果
        if (curPaperId < 0) {
            searchResult.value = emptyList()
            return
        }
        
        // 从缓存获取 PaperDetail
        val paperDetail = PaperDetailCache.get(curPaperId)
        if (paperDetail == null) {
            searchResult.value = emptyList()
            return
        }
        
        val questions = paperDetail.question
        val paperInfo = paperDetail.paperInfo
        val results = mutableListOf<SearchItem>()
        
        // 遍历所有问题
        questions.forEachIndexed { index, question ->
            // 将 body 中所有 TEXT 类型的元素合并成一个字符串
            val bodyText = question.body
                .filter { it.elementType == com.gorden.dayexam.repository.model.Element.TEXT }
                .joinToString(" ") { it.content }
            
            // 检查合并后的文本是否包含关键词
            if (bodyText.contains(key, ignoreCase = true)) {
                results.add(
                    SearchItem(
                        paperId = paperInfo.id,
                        paperTitle = paperInfo.title,
                        questionIndex = index,
                        questionType = question.type,
                        elementType = com.gorden.dayexam.repository.model.Element.TEXT,
                        elementContent = bodyText  // 使用合并后的完整文本
                    )
                )
            }
        }
        
        // 更新搜索结果
        searchResult.value = results
    }

    fun searchResult(): LiveData<List<SearchItem>> {
        return searchResult
    }
}
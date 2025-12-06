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
            // 检查问题的 body 中是否包含关键词
            val matchedElements = question.body.filter { element ->
                element.elementType == com.gorden.dayexam.repository.model.Element.TEXT &&
                element.content.contains(key, ignoreCase = true)
            }
            
            // 如果找到匹配的元素，为每个匹配的元素创建一个 SearchItem
            matchedElements.forEach { element ->
                results.add(
                    SearchItem(
                        paperId = paperInfo.id,
                        paperTitle = paperInfo.title,
                        questionId = index,  // 使用问题的索引作为 ID
                        questionType = question.type,
                        elementType = element.elementType,
                        elementContent = element.content
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
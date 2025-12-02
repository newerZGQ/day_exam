package com.gorden.dayexam.repository.model

data class Element (
    val elementType: Int,
    val content: String,
    var parentId: Int,
    var position: Int,
    var hash: String = "") {
    companion object {
        const val TEXT = 0
        const val PICTURE = 1
    }
}
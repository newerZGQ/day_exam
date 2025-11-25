package com.gorden.dayexam.repository.model

import com.gorden.dayexam.db.entity.Book
import com.gorden.dayexam.db.entity.Paper
import java.util.*

data class BookWithPaper (
    val book: Book,
    var papers: List<PaperWithQuestion> )

data class PaperWithQuestion (
    val paper: Paper,
    val curQuestionId: Int,
    val questionCount: Int,
    val studyInfo: PaperStudyInfo,
    val question: QuestionWithElement? )

data class PaperStudyInfo (
    val studyCount: Int,
    val lastStudyDate: Date? )
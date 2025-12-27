package com.gorden.dayexam.ui.home.viewholder

import android.view.View
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.db.entity.StudyRecord
import com.gorden.dayexam.repository.model.Answer
import com.gorden.dayexam.repository.model.Element
import com.gorden.dayexam.repository.model.QuestionDetail
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.ui.widget.AnswerCardView
import com.gorden.dayexam.utils.showOrGone
import com.jeremyliao.liveeventbus.LiveEventBus

class TrueFalseViewHolder(itemView: View): BaseQuestionViewHolder(itemView) {

    private val correctOption: View = itemView.findViewById(R.id.action_correct)
    private val inCorrectOption: View = itemView.findViewById(R.id.action_incorrect)

    private val answerContainer: View = itemView.findViewById(R.id.answer_container)
    private val answer: AnswerCardView = itemView.findViewById(R.id.answer)

    override fun setContent(
        paperInfo: PaperInfo,
        question: QuestionDetail,
        isRememberMode: Boolean
    ) {
        if (isRememberMode) {
            question.realAnswer = null
        }
        setOptionsView(paperInfo, question, isRememberMode)
        setAnswerView(paperInfo, question, isRememberMode)
    }

    private fun setOptionsView(paperInfo: PaperInfo, question: QuestionDetail, isRememberMode: Boolean) {
        if (isRememberMode) {
            switchToRememberOptionsView(paperInfo, question)
            return
        }
        if (question.realAnswer != null) {
            switchToAnsweredOptionsView(paperInfo, question)
        } else {
            switchToCommonOptionsView(paperInfo, question)
        }
    }

    private fun setAnswerView(paperInfo: PaperInfo, question: QuestionDetail, isRememberMode: Boolean) {
        if (isRememberMode) {
            answerContainer.showOrGone(false)
            return
        }
        if (question.realAnswer != null) {
            answerContainer.showOrGone(true)
            genAnswerView(paperInfo, question)
        } else {
            answerContainer.showOrGone(false)
        }
    }

    private fun switchToCommonOptionsView(paperInfo: PaperInfo, question: QuestionDetail) {
        val resources = itemView.resources
        correctOption.setBackgroundColor(resources.getColor(R.color.state_default))
        inCorrectOption.setBackgroundColor(resources.getColor(R.color.state_default))
        correctOption.setOnClickListener {
            question.realAnswer = Answer(tfAnswer = true)
            resetAllStatus()
            val eventTag = getAnswerEventTag(question)
            LiveEventBus.get(EventKey.ANSWER_EVENT, EventKey.AnswerEventModel::class.java)
                .post(EventKey.AnswerEventModel(eventTag))
        }
        inCorrectOption.setOnClickListener {
            question.realAnswer = Answer(tfAnswer = false)
            resetAllStatus()
            val eventTag = getAnswerEventTag(question)
            LiveEventBus.get(EventKey.ANSWER_EVENT, EventKey.AnswerEventModel::class.java)
                .post(EventKey.AnswerEventModel(eventTag))
        }
    }

    private fun switchToAnsweredOptionsView(paperInfo: PaperInfo, question: QuestionDetail) {
        val context = itemView.context
        val answer = question.answer.tfAnswer
        if (question.realAnswer != null) {
            if (question.realAnswer?.tfAnswer == answer){
                if (answer) {
                    correctOption.setBackgroundColor(context.getColor(R.color.state_success))
                    inCorrectOption.setBackgroundColor(context.getColor(R.color.state_default))
                } else {
                    correctOption.setBackgroundColor(context.getColor(R.color.state_default))
                    inCorrectOption.setBackgroundColor(context.getColor(R.color.state_success))
                }
            } else {
                if (answer) {
                    correctOption.setBackgroundColor(context.getColor(R.color.state_success))
                    inCorrectOption.setBackgroundColor(context.getColor(R.color.state_error))
                } else {
                    correctOption.setBackgroundColor(context.getColor(R.color.state_error))
                    inCorrectOption.setBackgroundColor(context.getColor(R.color.state_success))
                }
            }
        }
        // 避免点击该view时触发翻页
        correctOption.setOnClickListener {}
        inCorrectOption.setOnClickListener {}
    }

    private fun switchToRememberOptionsView(paperInfo: PaperInfo, question: QuestionDetail) {
        val answer = question.answer.tfAnswer
        val resources = itemView.resources
        if (answer) {
            correctOption.setBackgroundColor(resources.getColor(R.color.state_success))
            inCorrectOption.setBackgroundColor(resources.getColor(R.color.state_default))
        } else {
            correctOption.setBackgroundColor(resources.getColor(R.color.state_default))
            inCorrectOption.setBackgroundColor(resources.getColor(R.color.state_success))
        }
        correctOption.setOnClickListener(null)
        inCorrectOption.setOnClickListener(null)
    }

    private fun genAnswerView(paperInfo: PaperInfo, question: QuestionDetail) {
        val resources = itemView.resources
        val answerText = if (question.answer.tfAnswer) resources.getString(R.string.correct) else resources.getString(R.string.incorrect)
        val elements = listOf(Element(content = answerText, elementType = Element.TEXT))
        val realAnswer = if (question.realAnswer?.tfAnswer == true) resources.getString(R.string.correct) else resources.getString(R.string.incorrect)
        answer.setElements(
            paperInfo = paperInfo, elements = elements, "", realAnswer,
            listener = { target, elements ->

            },
        )

    }

    private fun getAnswerEventTag(question: QuestionDetail): Int {
        val realAnswer = question.realAnswer?.tfAnswer ?: false
        return if (realAnswer) {
            StudyRecord.CORRECT
        } else {
            StudyRecord.IN_CORRECT
        }
    }

}
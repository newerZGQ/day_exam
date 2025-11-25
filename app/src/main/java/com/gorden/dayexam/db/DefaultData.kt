package com.gorden.dayexam.db

class DefaultData {
    companion object {
        const val DEFAULT_IMAGE = "default_data_image_xingkong.png"

        const val FILL_IN_BODY_1 =
            """
The following is an example of the format of completion. Completion has only two parts, the stem and the answer. These two parts can be in the format of mixed text and image. In word, edit the format of your completion question, and then you can analyze it normally. Pay attention to add && in front of the question type and answer

>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
&&Completion
How old are you?
&&Answer
18
            """
        const val FILL_IN_BODY_2 = DEFAULT_IMAGE
        const val FILL_IN_BODY_3 = "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<"
        const val FILL_IN_ANSWER = "18"



        const val TRUE_FALSE_BODY_1 =
            """
The following is an example of the true-false question. The true-false question has only two parts: the stem and the answer. These two parts can be in the format of mixed text and image. Pay attention to add && in front of the true-false and answer, and the content of the answer can only be true or false

>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
&&true false
Are you 18?
&&Answer
True
            """
        const val TRUE_FALSE_BODY_2 = DEFAULT_IMAGE
        const val TRUE_FALSE_BODY_3 = "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<"
        const val TRUE_FALSE_ANSWER = "True"


        const val SINGLE_CHOICE_BODY_1 =
            """
The following is an example of the single answer questions. The single answer questions have three parts: stem, options, and answers. These three parts can be in the format of mixed text and image. Note that the front of the question type, answer and options should be added && , and the content of the answers can only be a capital English character. Pictures can also be inserted into the options

>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
&&Single Answer
How old are you
&&Option
15
&&Option
16
&&Option
17
            """
        const val SINGLE_CHOICE_BODY_2 = DEFAULT_IMAGE
        const val SINGLE_CHOICE_BODY_3 =
            """
&&Option
18
&&Answer
B
            """
        const val SINGLE_CHOICE_BODY_4 = "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<"
        const val SINGLE_CHOICE_OPTION_1 = "15"
        const val SINGLE_CHOICE_OPTION_2 = "16"
        const val SINGLE_CHOICE_OPTION_3 = "17"
        const val SINGLE_CHOICE_OPTION_3_IMAGE = DEFAULT_IMAGE
        const val SINGLE_CHOICE_OPTION_4 = "18"
        const val SINGLE_CHOICE_ANSWER = "B"


        const val MULTI_CHOICE_BODY_1 =
            """
The following is an example of the multiple answer question. Multiple answer questions have three parts: stem, options and answers. These two parts can be in the format of mixed text and image. Note that question type, option and answer should be preceded by &&, and the content of the answer can only be one or more uppercase English characters. Pictures can also be inserted into the options

>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
&&Multiple Answer
Which telephone do you like
&&Option
Apple
&&Option
Android
&&Option
Nokia
            """
        const val MULTI_CHOICE_BODY_2 = DEFAULT_IMAGE
        const val MULTI_CHOICE_BODY_3 =
            """
&&Option
Other
&&Answer
BD
            """
        const val MULTI_CHOICE_BODY_4 = "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<"
        const val MULTI_CHOICE_OPTION_1 = "Apple"
        const val MULTI_CHOICE_OPTION_2 = "Android"
        const val MULTI_CHOICE_OPTION_3 = "Nokia"
        const val MULTI_CHOICE_OPTION_3_IMAGE = DEFAULT_IMAGE
        const val MULTI_CHOICE_OPTION_4 = "Other"
        const val MULTI_CHOICE_ANSWER = "BD"



        const val ESSAY_BODY_1 =
            """
The following is an example of discussion essays. The question and answer only has two parts, which can be in the format of mixed text and image. Note that && should be added in front of the question type and answer

>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
&&Discussion Essays
Who are you
&&Answer
I am DayExam
            """
        const val ESSAY_BODY_2 = DEFAULT_IMAGE
        const val ESSAY_BODY_3 = "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<"
        const val ESSAY_ANSWER =
            """
I am DayExam
            """
    }
}
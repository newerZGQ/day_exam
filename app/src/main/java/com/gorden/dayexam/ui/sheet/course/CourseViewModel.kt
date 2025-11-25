package com.gorden.dayexam.ui.sheet.course

import android.app.Application
import androidx.lifecycle.*
import com.gorden.dayexam.db.entity.DContext
import com.gorden.dayexam.db.entity.Course
import com.gorden.dayexam.repository.DataRepository

class CourseViewModel(application: Application) : AndroidViewModel(application) {

    private val dContext = DataRepository.getDContext()
    private var courses: LiveData<List<Course>> =
        Transformations.switchMap(dContext) {
            dContext.value?.let {
                DataRepository.getAllCourse()
        }
    }

    fun getAllCourse(): LiveData<List<Course>> {
        return courses
    }

    fun getDContext(): LiveData<DContext> {
        return dContext
    }

}
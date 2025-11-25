package com.gorden.dayexam

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextHolder.onActivityCreated(this, savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        ContextHolder.onActivityStarted(this)
    }

    override fun onResume() {
        super.onResume()
        ContextHolder.onActivityResumed(this)
    }
}
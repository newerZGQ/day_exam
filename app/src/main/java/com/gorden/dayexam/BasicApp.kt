package com.gorden.dayexam

import android.app.Application
import com.gorden.dayexam.db.AppDatabase
import com.gorden.dayexam.executor.AppExecutors
import com.gorden.dayexam.repository.DataRepository
import com.jeremyliao.liveeventbus.LiveEventBus

class BasicApp: Application() {

    override fun onCreate() {
        super.onCreate()
        ContextHolder.application = this
        LiveEventBus
            .config()
            .setContext(this)
        DataRepository.init(AppDatabase.getInstance(this, AppExecutors))
    }

}
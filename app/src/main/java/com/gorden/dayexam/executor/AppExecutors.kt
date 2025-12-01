package com.gorden.dayexam.executor

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper

import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Global executor pools for the whole application.
 *
 *
 * Grouping tasks like this avoids the effects of task starvation (e.g. disk reads don't wait behind
 * webservice requests).
 */
object AppExecutors {

    private val mDiskIO = Executors.newSingleThreadExecutor()
    private val mNetworkIO = Executors.newFixedThreadPool(3)
    private val mMainThread = MainThreadExecutor()

    fun diskIO(): Executor {
        return mDiskIO
    }

    fun networkIO(): Executor {
        return mNetworkIO
    }

    fun mainThread(): MainThreadExecutor {
        return mMainThread
    }

    class MainThreadExecutor : Executor {
        private val mainThreadHandler = Handler(Looper.getMainLooper())
        override fun execute(command: Runnable) {
            mainThreadHandler.post(command)
        }

        fun executeDelay(command: Runnable, delayTime: Long) {
            mainThreadHandler.postDelayed(command, delayTime)
        }

        fun removeCommand(command: Runnable) {
            mainThreadHandler.removeCallbacks(command)
        }
    }
}
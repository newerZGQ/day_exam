package com.gorden.dayexam

import android.app.Activity
import android.app.Application
import android.content.res.Resources
import android.os.Bundle
import java.lang.ref.WeakReference

object ContextHolder: Application.ActivityLifecycleCallbacks {
    lateinit var application: Application
    private var currentActivity = WeakReference<Activity>(null)

    fun currentActivity(): Activity? {
        return currentActivity.get()
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {
        currentActivity = WeakReference<Activity>(p0)
    }

    override fun onActivityStarted(p0: Activity) {
        currentActivity = WeakReference<Activity>(p0)
    }

    override fun onActivityResumed(p0: Activity) {
        currentActivity = WeakReference<Activity>(p0)
    }

    override fun onActivityPaused(p0: Activity) {

    }

    override fun onActivityStopped(p0: Activity) {

    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {

    }

    override fun onActivityDestroyed(p0: Activity) {

    }
}
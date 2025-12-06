package com.gorden.dayexam.utils

import android.content.res.Resources
import android.graphics.Rect
import android.view.View
import android.view.Window
import com.gorden.dayexam.ContextHolder

object ScreenUtils {

    private var screenWidth = 0
    private var screenHeight = 0
    private var density = 0f

    init {
        screenWidth = Resources.getSystem().displayMetrics.widthPixels
        screenHeight = Resources.getSystem().displayMetrics.heightPixels
        density = Resources.getSystem().displayMetrics.density
    }

    fun screenWidth(): Int {
        return screenWidth
    }

    fun screenHeight(): Int {
        return screenHeight
    }

    fun dp2px(dp: Float): Int {
        return (dp * density + 0.5f).toInt()
    }

    fun getRawX(view: View): Int {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return location[0]
    }

    fun getRawY(view: View): Int {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return location[1]
    }

    fun getStatusBarHeight(): Int {
        val rectangle = Rect()
        val window: Window = ContextHolder.currentActivity()!!.window
        window.decorView.getWindowVisibleDisplayFrame(rectangle)
        return rectangle.top
    }
}

fun View.showOrGone(show: Boolean) {
    this.visibility = if (show) View.VISIBLE else View.GONE
}
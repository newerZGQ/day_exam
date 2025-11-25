package com.gorden.dayexam.utils

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.gorden.dayexam.ContextHolder
import com.gorden.dayexam.R


object FontUtils {
    private val fontCache: MutableMap<String, Typeface> = mutableMapOf()

    const val XWWK_FONT = "xwwk"

    operator fun get(name: String): Typeface? {
        var tf: Typeface? = fontCache[name]
        if (tf == null) {
            when (name) {
                XWWK_FONT -> {
                    val font = ResourcesCompat.getFont(ContextHolder.application, R.font.xwwk)
                    font?.let {
                        fontCache[name] = it
                    }
                }
                else -> {

                }
            }

        }
        return fontCache[name]
    }
}
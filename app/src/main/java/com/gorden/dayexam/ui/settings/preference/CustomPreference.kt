package com.gorden.dayexam.ui.settings.preference

import android.R
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.gorden.dayexam.utils.FontUtils


open class CustomPreference: Preference {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        val titleView = holder?.itemView?.findViewById(R.id.title) as TextView?
        titleView?.setTextColor(Color.WHITE)
        titleView?.typeface = FontUtils[FontUtils.XWWK_FONT]
        val summaryView = holder?.itemView?.findViewById(R.id.summary) as TextView?
        summaryView?.setTextColor(context.resources.getColor(com.gorden.dayexam.R.color.setting_preference_summary_color))
        summaryView?.typeface = FontUtils[FontUtils.XWWK_FONT]
    }
}
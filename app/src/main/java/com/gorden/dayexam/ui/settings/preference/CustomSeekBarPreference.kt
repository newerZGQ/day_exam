package com.gorden.dayexam.ui.settings.preference

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import androidx.preference.SeekBarPreference
import android.R
import android.widget.SeekBar
import com.gorden.dayexam.utils.FontUtils

class CustomSeekBarPreference: SeekBarPreference {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        val seekBarValue = holder?.findViewById(androidx.preference.R.id.seekbar_value) as TextView?
        seekBarValue?.setTextColor(Color.WHITE)
        seekBarValue?.typeface = FontUtils[FontUtils.XWWK_FONT]
        val titleView = holder?.findViewById(R.id.title) as TextView?
        titleView?.setTextColor(Color.WHITE)
        titleView?.typeface = FontUtils[FontUtils.XWWK_FONT]
        val seekBar = holder?.findViewById(androidx.preference.R.id.seekbar) as SeekBar?
        seekBar?.max = 9
        val summaryView = holder?.itemView?.findViewById(R.id.summary) as TextView?
        summaryView?.setTextColor(context.resources.getColor(com.gorden.dayexam.R.color.setting_preference_summary_color))
        summaryView?.typeface = FontUtils[FontUtils.XWWK_FONT]
    }
}
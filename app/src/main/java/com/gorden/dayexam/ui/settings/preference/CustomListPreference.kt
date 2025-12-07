package com.gorden.dayexam.ui.settings.preference

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.ListPreference
import androidx.preference.PreferenceViewHolder
import com.gorden.dayexam.R
import com.gorden.dayexam.utils.FontUtils

class CustomListPreference : ListPreference {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        val titleView: TextView? = holder?.itemView?.findViewById(android.R.id.title)
        titleView?.setTextColor(Color.WHITE)
        titleView?.typeface = FontUtils[FontUtils.XWWK_FONT]
        val summaryView: TextView? = holder?.itemView?.findViewById(android.R.id.summary)
        summaryView?.setTextColor(context.resources.getColor(R.color.setting_preference_summary_color, null))
        summaryView?.typeface = FontUtils[FontUtils.XWWK_FONT]

        val iconView = holder?.itemView?.findViewById(android.R.id.icon) as? android.widget.ImageView
        iconView?.setColorFilter(context.resources.getColor(R.color.setting_preference_summary_color, null))
    }
}

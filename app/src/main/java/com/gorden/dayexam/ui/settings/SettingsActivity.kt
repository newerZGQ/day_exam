package com.gorden.dayexam.ui.settings

import android.os.Bundle
import com.gorden.dayexam.BaseActivity
import com.gorden.dayexam.R
import kotlinx.android.synthetic.main.app_bar_main.*

class SettingsActivity: BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        initToolbar()
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, SettingsFragment())
            .commit()
    }

    private fun initToolbar() {
        toolbar.setTitleTextAppearance(this, R.style.XWWKBoldTextAppearance)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.title = resources.getString(R.string.settings)
    }
}
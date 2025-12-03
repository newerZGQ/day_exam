package com.gorden.dayexam.ui.settings

import android.os.Bundle
import com.gorden.dayexam.BaseActivity
import com.gorden.dayexam.R
import com.gorden.dayexam.databinding.ActivitySettingsBinding

class SettingsActivity: BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initToolbar()
        supportFragmentManager.beginTransaction()
            .add(binding.fragmentContainer.id, SettingsFragment())
            .commit()
    }

    private fun initToolbar() {
        binding.toolbar.setTitleTextAppearance(this, R.style.XWWKBoldTextAppearance)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.toolbar.title = resources.getString(R.string.settings)
    }
}
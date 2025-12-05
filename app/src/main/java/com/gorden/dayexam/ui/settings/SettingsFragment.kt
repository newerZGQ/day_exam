package com.gorden.dayexam.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle

import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
 
import com.gorden.dayexam.R
import com.gorden.dayexam.ui.dialog.EditTextDialog

class SettingsFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preference, rootKey)
        findPreference<Preference>(resources.getString(R.string.privacy_key))?.setOnPreferenceClickListener {
            EditTextDialog(requireActivity(),
                requireActivity().resources.getString(R.string.privacy_dialog_title),
                requireActivity().resources.getString(R.string.privacy_dialog_content),
                editCallBack = object : EditTextDialog.EditCallBack {
                    override fun onConfirmContent(
                        dialog: EditTextDialog,
                        content: String,
                        subContent: String
                    ) {}
                }).show()
            return@setOnPreferenceClickListener true
        }
//        findPreference<Preference>(resources.getString(R.string.bilibili_video_help_key))?.setOnPreferenceClickListener {
//            val uri = Uri.parse("")
//            val intent = Intent(Intent.ACTION_VIEW, uri)
//            startActivity(intent)
//            return@setOnPreferenceClickListener true
//        }
        findPreference<Preference>(resources.getString(R.string.demo_template_key))?.setOnPreferenceClickListener {
            val uri = Uri.parse(resources.getString(R.string.demo_template_link))
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
            return@setOnPreferenceClickListener true
        }
    }
}
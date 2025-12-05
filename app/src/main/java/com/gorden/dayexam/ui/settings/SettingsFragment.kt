package com.gorden.dayexam.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle

import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.gorden.dayexam.utils.SharedPreferenceUtil
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
        findPreference<Preference>(resources.getString(R.string.demo_template_key))?.setOnPreferenceClickListener {
            val uri = Uri.parse(resources.getString(R.string.demo_template_link))
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
            return@setOnPreferenceClickListener true
        }

        // Gemini API key
        val geminiKey = resources.getString(R.string.gemini_api_key)
        val geminiPref = findPreference<Preference>(geminiKey)
        geminiPref?.let { pref ->
            // set initial summary
            val saved = SharedPreferenceUtil.getString(geminiKey)
            pref.summary = if (saved.isNotEmpty()) "${resources.getString(R.string.gemini_api_title)}(已设置)" else getString(R.string.gemini_api_summary)
            pref.setOnPreferenceClickListener {
                EditTextDialog(requireActivity(), resources.getString(R.string.gemini_api_title), "", saved, getString(R.string.please_input), editCallBack = object : EditTextDialog.EditCallBack {
                    override fun onConfirmContent(dialog: EditTextDialog, content: String, subContent: String) {
                        SharedPreferenceUtil.setString(geminiKey, content)
                        pref.summary = if (content.isNotEmpty()) "${resources.getString(R.string.gemini_api_title)}(已设置)" else getString(R.string.gemini_api_summary)
                    }
                }).show()
                return@setOnPreferenceClickListener true
            }
        }

        // Deepseek API key
        val deepseekKey = resources.getString(R.string.deepseek_api_key)
        val deepseekPref = findPreference<Preference>(deepseekKey)
        deepseekPref?.let { pref ->
            val saved = SharedPreferenceUtil.getString(deepseekKey)
            pref.summary = if (saved.isNotEmpty()) "${resources.getString(R.string.deepseek_api_title)}(已设置)" else getString(R.string.deepseek_api_summary)
            pref.setOnPreferenceClickListener {
                EditTextDialog(requireActivity(), resources.getString(R.string.deepseek_api_title), "", saved, getString(R.string.please_input), editCallBack = object : EditTextDialog.EditCallBack {
                    override fun onConfirmContent(dialog: EditTextDialog, content: String, subContent: String) {
                        SharedPreferenceUtil.setString(deepseekKey, content)
                        pref.summary = if (content.isNotEmpty()) "${resources.getString(R.string.deepseek_api_title)}(已设置)" else getString(R.string.deepseek_api_summary)
                    }
                }).show()
                return@setOnPreferenceClickListener true
            }
        }
    }
}
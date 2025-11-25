package com.gorden.dayexam.ui.action

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.gorden.dayexam.ContextHolder
import com.gorden.dayexam.MainActivity
import com.gorden.dayexam.R
import com.gorden.dayexam.backup.BackupManager
import com.gorden.dayexam.db.AppDatabase
import com.gorden.dayexam.executor.AppExecutors
import com.gorden.dayexam.ui.dialog.EditTextDialog
import java.io.File
import kotlin.system.exitProcess


class RecoverBackupAction(val context: Context): Action {

    override fun start() {
        selectCourse()
    }

    private fun selectCourse() {
        AppExecutors.diskIO().execute {
            val backups = BackupManager.getAllBackupFiles()
            AppExecutors.mainThread().execute {
                val titles = backups.map {
                    it.nameWithoutExtension
                }
                AlertDialog.Builder(context, R.style.MyAlertDialogTheme)
                    .setTitle(context.resources.getString(R.string.please_select_backup))
                    .setItems(
                        titles.toTypedArray()
                    ) { p0, p1 ->
                        confirm(backups[p1])
                        p0.dismiss()
                    }.show()
            }
        }
    }

    private fun confirm(backup: File) {
        EditTextDialog(context,
            context.resources.getString(R.string.confirm_recover),
            "确定要恢复" + backup.nameWithoutExtension + "的备份么,恢复后会自动重启",
            editCallBack = object : EditTextDialog.EditCallBack {
                override fun onConfirmContent(
                    dialog: EditTextDialog,
                    content: String,
                    subContent: String
                ) {
                    BackupManager.recover(backup, object : BackupManager.BackupListener {
                        override fun onRecoverEnd(success: Boolean, msg: String) {
                            if (success) {
                                System.exit(0)
                            } else {
                                Toast.makeText(
                                    ContextHolder.application,
                                    msg,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                    })
                }
            }).show()
    }
}
package com.gorden.dayexam.ui.paper

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gorden.dayexam.R
import com.gorden.dayexam.databinding.FragmentPaperListLayoutBinding
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.executor.AppExecutors
import com.gorden.dayexam.parser.PaperParser
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.ui.dialog.EditTextDialog
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class PaperListFragment : Fragment() {

    private var _binding: FragmentPaperListLayoutBinding? = null
    private val binding get() = _binding!!

    private lateinit var paperListViewModel: PaperListViewModel
    val adapter = PaperListAdapter()
    private var curPaperId: Int = 0

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    result.data?.data?.let { uri ->
                        LiveEventBus.get(EventKey.START_PROGRESS_BAR, Int::class.java).post(0)
                        importPaperFromUri(uri)
                    }
                } catch (e: Exception) {
                    // 异常处理（例如 Toast 或日志）
                    LiveEventBus.get(EventKey.END_PROGRESS_BAR, Int::class.java).post(0)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaperListLayoutBinding.inflate(inflater, container, false)
        initPaperList()
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    private fun initPaperList() {
        binding.paperList.adapter = adapter
        binding.paperList.layoutManager = LinearLayoutManager(this.context)

        // 顶部布局里的 addPaper 按钮，点击直接打开系统文件选择器
        binding.addPaper.setOnClickListener {
            Log.d("PaperListFragment", "click add")
            toFileBrowser()
        }

        paperListViewModel = ViewModelProvider(this).get(PaperListViewModel::class.java)

        // 试卷列表
        paperListViewModel.getAllPapers().observe(viewLifecycleOwner) {
            if (it == null) {
                curPaperId = 0
                adapter.setData(listOf(), curPaperId)

            } else {
                adapter.setData(it, curPaperId)
            }
        }
        registerPaperClickedEvent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun editPaper(paperInfo: PaperInfo) {
        val ctx = context ?: return
        EditTextDialog(
            ctx,
            ctx.resources.getString(R.string.dialog_edit_paper_title),
            ctx.resources.getString(R.string.dialog_edit_paper_subTitle),
            paperInfo.title,
            ctx.resources.getString(R.string.dialog_create_paper_hint),
            editCallBack = object : EditTextDialog.EditCallBack {
                override fun onConfirmContent(
                    dialog: EditTextDialog,
                    content: String,
                    subContent: String
                ) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val success = paperListViewModel.updatePaperTitle(paperInfo, content)
                        val ctxInner = context ?: return@launch
                        Toast.makeText(
                            ctxInner,
                            ctxInner.getString(
                                if (success) R.string.toast_edit_paper_success
                                else R.string.toast_edit_paper_failed
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        ).show()
    }

    private fun deletePaper(paperInfo: PaperInfo) {
        val ctx = context ?: return
        EditTextDialog(
            ctx,
            ctx.resources.getString(R.string.dialog_delete_paper_title),
            ctx.getString(R.string.dialog_delete_paper_message, paperInfo.title),
            editCallBack = object : EditTextDialog.EditCallBack {
                override fun onConfirmContent(
                    dialog: EditTextDialog,
                    content: String,
                    subContent: String
                ) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val success = paperListViewModel.deletePaper(paperInfo)
                        val ctxInner = context ?: return@launch
                        Toast.makeText(
                            ctxInner,
                            ctxInner.getString(
                                if (success) R.string.toast_delete_paper_success
                                else R.string.toast_delete_paper_failed
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        ).show()
    }

    private fun toFileBrowser() {
        Log.d("paperList", "toFileBrowser")
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        filePickerLauncher.launch(intent)
    }

    /**
     * 将系统文件选择器返回的 Uri 拷贝到应用缓存目录，再交给 PaperParser 解析
     */
    private fun importPaperFromUri(uri: Uri) {
        AppExecutors.diskIO().execute {
            try {
                val context = requireContext()
                val fileName = queryDisplayName(uri) ?: "paper_${System.currentTimeMillis()}.docx"
                val destDir = File(context.cacheDir, "imported_papers")
                if (!destDir.exists()) {
                    destDir.mkdirs()
                }
                val destFile = File(destDir, fileName)

                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // 使用 PaperParser 解析拷贝到缓存目录的文件
                PaperParser.parseFromFile(destFile.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                AppExecutors.mainThread().execute {
                    LiveEventBus.get(EventKey.END_PROGRESS_BAR, Int::class.java).post(0)
                }
            }
        }
    }

    /**
     * 通过 ContentResolver 查询 Uri 对应的文件名
     */
    private fun queryDisplayName(uri: Uri): String? {
        val context = context ?: return null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index != -1 && it.moveToFirst()) {
                return it.getString(index)
            }
        }
        return null
    }

    private fun registerPaperClickedEvent() {
        // paper操作
        LiveEventBus.get(EventKey.PAPER_MENU_EDIT_PAPER, PaperInfo::class.java)
            .observe(this, {
                editPaper(it)
            })
        LiveEventBus.get(EventKey.PAPER_MENU_DELETE_PAPER, PaperInfo::class.java)
            .observe(this, {
                deletePaper(it)
            })
    }

}
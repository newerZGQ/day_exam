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
import androidx.recyclerview.widget.ItemTouchHelper
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
    private lateinit var adapter: PaperListAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    private var curPaperId: Int = 0
    private var isInEditMode: Boolean = false

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    result.data?.data?.let { uri ->
                        showProgress()
                        importPaperFromUri(uri)
                    }
                } catch (e: Exception) {
                    // 异常处理（例如 Toast 或日志）
                    hideProgress()
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
        paperListViewModel = ViewModelProvider(this).get(PaperListViewModel::class.java)

        adapter = PaperListAdapter(object : PaperListAdapter.Listener {
            override fun onItemLongPressed(holder: PaperViewHolder, paperInfo: PaperInfo) {
                // 进入编辑模式，并开始拖拽
                enterEditMode()
                itemTouchHelper.startDrag(holder)
            }

            override fun onItemDeleteClicked(paperInfo: PaperInfo) {
                deletePaper(paperInfo)
            }
        })

        binding.paperList.adapter = adapter
        binding.paperList.layoutManager = LinearLayoutManager(this.context)

        // 启用拖拽排序
        val dragCallback = DragCallback().apply {
            listener = object : DragCallback.OnItemTouchListener {
                override fun onMove(fromPosition: Int, toPosition: Int) {
                    adapter.onItemMove(fromPosition, toPosition)
                }

                override fun clearView() {
                    // 拖拽结束后，持久化新的排序
                    val papers = adapter.getPapers()
                    viewLifecycleOwner.lifecycleScope.launch {
                        paperListViewModel.updatePaperOrder(papers)
                    }
                }
            }
        }
        itemTouchHelper = ItemTouchHelper(dragCallback)
        itemTouchHelper.attachToRecyclerView(binding.paperList)

        // 顶部布局里的 addPaper 按钮，普通模式：打开文件选择器；编辑模式：退出编辑模式
        binding.addPaper.setOnClickListener {
            Log.d("PaperListFragment", "click add")
            if (isInEditMode) {
                exitEditMode()
            } else {
                toFileBrowser()
            }
        }

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

    private fun showProgress() {
        binding.parsingProgress.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        binding.root.postDelayed({
            binding.parsingProgress.visibility = View.GONE
        }, 1000)
    }

    private fun enterEditMode() {
        if (isInEditMode) return
        isInEditMode = true
        adapter.setEditMode(true)
        binding.addPaper.animate()
            .rotation(45f)
            .setDuration(200)
            .start()
    }

    private fun exitEditMode() {
        if (!isInEditMode) return
        isInEditMode = false
        adapter.setEditMode(false)
        binding.addPaper.animate()
            .rotation(0f)
            .setDuration(200)
            .start()
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
                    hideProgress()
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
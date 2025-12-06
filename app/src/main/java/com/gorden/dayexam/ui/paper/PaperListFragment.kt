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
import androidx.activity.result.ActivityResultLauncher
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
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.ui.dialog.EditTextDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class PaperListFragment : Fragment() {

    private var _binding: FragmentPaperListLayoutBinding? = null
    private val binding get() = _binding!!

    private val paperListViewModel: PaperListViewModel by lazy {
        ViewModelProvider(this).get(PaperListViewModel::class.java)
    }
    private lateinit var adapter: PaperListAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    private var isInEditMode: Boolean = false
    private var currentPaperInfo: PaperInfo? = null

    // 导入原始文档 - 使用AI解析
    private val rawDocumentPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    result.data?.data?.let { uri ->
                        showProgress()
                        importRawDocumentFromUri(uri)
                    }
                } catch (e: Exception) {
                    hideProgress()
                }
            }
        }

    // 导入格式化文档 - 使用模版
    private val formattedDocumentPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    result.data?.data?.let { uri ->
                        showProgress()
                        importFormattedDocumentFromUri(uri)
                    }
                } catch (e: Exception) {
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initPaperList()
        initData()
    }

    @SuppressLint("SetTextI18n")
    private fun initPaperList() {
        adapter = PaperListAdapter(object : PaperListAdapter.Listener {
            override fun onItemClicked(paperInfo: PaperInfo) {
                if (isInEditMode) {
                    exitEditMode()
                    return
                }
                // 记录当前选中的 paperInfo
                currentPaperInfo = paperInfo
                // 通知适配器更新以显示绿色标题
                adapter.setData(adapter.getPapers(), paperInfo.id)
                // 通过 EventBus 发送事件
                DataRepository.updateCurPaperId(paperInfo.id)
            }
            
            override fun onItemLongPressed(holder: PaperViewHolder, paperInfo: PaperInfo) {
                enterEditMode()
                // 进入编辑模式，并开始拖拽
                binding.root.post {
                    itemTouchHelper.startDrag(holder)
                }
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
                showImportPaperDialog()
            }
        }

    }


    private fun initData() {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                currentPaperInfo = DataRepository.getCurPaperInfo()
            }
            // 试卷列表
            withContext(Dispatchers.Main) {
                paperListViewModel.getAllPapers().observe(viewLifecycleOwner) {
                    adapter.setData(it, currentPaperInfo?.id ?: -1)
                }
            }
            currentPaperInfo?.let {
                DataRepository.updateCurPaperId(it.id)
            }
        }
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

    private fun showImportPaperDialog() {
        val ctx = context ?: return
        val bottomSheetDialog = BottomSheetDialog(ctx)
        val view = layoutInflater.inflate(R.layout.dialog_import_paper_bottom_sheet, null)
        bottomSheetDialog.setContentView(view)

        // 导入原始文档 - 使用AI解析
        view.findViewById<View>(R.id.import_raw_document).setOnClickListener {
            bottomSheetDialog.dismiss()
            openFileBrowser(rawDocumentPickerLauncher)
        }

        // 导入格式化文档 - 使用模版
        view.findViewById<View>(R.id.import_formatted_document).setOnClickListener {
            bottomSheetDialog.dismiss()
            openFileBrowser(formattedDocumentPickerLauncher)
        }

        bottomSheetDialog.show()
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

    private fun openFileBrowser(launcher: ActivityResultLauncher<Intent>) {
        Log.d("paperList", "openFileBrowser")
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        launcher.launch(intent)
    }

    /**
     * 导入原始文档 - 使用AI解析试题
     * 将系统文件选择器返回的 Uri 拷贝到应用缓存目录，再交给 PaperParser 解析
     */
    private fun importRawDocumentFromUri(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // IO operations in IO dispatcher
                withContext(Dispatchers.IO) {
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
                    // Check if paper already exists before parsing
                    if (PaperParser.checkExist(destFile.absolutePath)) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_paper_already_exists),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@withContext
                    }

                    // TODO: 使用 AI 解析原始文档
                    // 暂时使用 PaperParser 解析拷贝到缓存目录的文件
                    PaperParser.parseFromFile(destFile.absolutePath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                hideProgress()
            }
        }
    }

    /**
     * 导入格式化文档 - 使用模版格式解析
     * 将系统文件选择器返回的 Uri 拷贝到应用缓存目录，再交给 PaperParser 解析
     */
    private fun importFormattedDocumentFromUri(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // IO operations in IO dispatcher
                withContext(Dispatchers.IO) {
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
                    // Check if paper already exists before parsing
                    if (PaperParser.checkExist(destFile.absolutePath)) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_paper_already_exists),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@withContext
                    }

                    // 使用模版格式解析
                    PaperParser.parseFromFile(destFile.absolutePath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                hideProgress()
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

}
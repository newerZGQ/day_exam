# Day Exam Version 3 - 产品需求文档 (PRD)

## 1. 概述

### 1.1 版本目标
构建轻量级试卷学习应用,采用"文件驱动+内存处理"的简洁架构,专注于核心学习功能。

### 1.2 核心特性
- **简洁架构**: 单层试卷管理,无书籍/课程层级
- **文件驱动**: 试题数据实时解析,不存储到数据库
- **轻量设计**: 数据库仅存储试卷元数据(名称、路径、排序)
- **专注学习**: 聚焦核心功能,无冗余特性

---

## 2. 功能需求详细说明

### 2.1 侧边栏试卷管理

#### 2.1.1 功能描述
采用单层结构设计,侧边栏直接展示所有试卷列表,简洁高效。

#### 2.1.2 UI/UX 设计要求
- **侧边栏布局**:
  - 顶部: 添加试卷按钮 (FloatingActionButton 或 ImageButton)
  - 中间: 试卷列表 (RecyclerView)
  - 每个试卷项显示: 试卷名称、最后编辑时间
  - 支持长按拖拽排序 (ItemTouchHelper)
  
- **交互行为**:
  - 点击试卷项 → 打开试卷详情页
  - 长按试卷项 → 显示上下文菜单 (重命名、删除)
  - 拖拽试卷项 → 调整顺序,实时保存到数据库

#### 2.1.3 技术实现细节

**数据库 Schema 变更**:

```kotlin
@Entity(tableName = "paper")
data class Paper(
    @PrimaryKey(autoGenerate = true) 
    var id: Int = 0,
    
    var title: String,              // 试卷名称
    var filePath: String,           // 文件路径 (唯一约束)
    var position: Int,              // 排序位置
    var createTime: Date = Date(),
    var editTime: Date = Date()
)
```

**关键字段说明**:
- `filePath`: 存储试卷文件的绝对路径,用于唯一标识试卷
- `position`: 用户自定义的排序位置,支持拖拽调整
- `title`: 试卷名称,默认从文件名提取
- `createTime` / `editTime`: 创建和编辑时间戳

**PaperDao 设计**:

```kotlin
@Dao
interface PaperDao {
    @Insert
    fun insert(paper: Paper): Long
    
    @Update
    fun update(paper: Paper)
    
    @Delete
    fun delete(paper: Paper)
    
    @Query("SELECT * FROM paper ORDER BY position ASC")
    fun getAllOrderByPosition(): LiveData<List<Paper>>
    
    @Query("SELECT * FROM paper WHERE filePath = :filePath")
    fun getByFilePath(filePath: String): Paper?
    
    @Query("SELECT MAX(position) FROM paper")
    fun getMaxPosition(): Int?
    
    @Query("UPDATE paper SET position = :position WHERE id = :id")
    fun updatePosition(id: Int, position: Int)
}
```

#### 2.1.4 添加试卷功能

**流程**:
1. 用户点击"添加试卷"按钮
2. 打开系统文件选择器 (Intent.ACTION_OPEN_DOCUMENT)
3. 用户选择 `.docx` 文件
4. 检查文件路径是否已存在于数据库
   - 如果存在 → 弹出 Toast 提示 "该试卷已添加"
   - 如果不存在 → 继续
5. 提取文件名作为试卷名称 (去除 `.docx` 后缀)
6. 创建 Paper 对象,position = maxPosition + 1
7. 插入数据库
8. 刷新侧边栏列表

**代码示例**:

```kotlin
// MainActivity.kt 或 PaperManagementFragment.kt
private fun addPaper() {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    }
    startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT)
}

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_CODE_OPEN_DOCUMENT && resultCode == RESULT_OK) {
        data?.data?.let { uri ->
            val filePath = getRealPathFromURI(uri)
            
            // 检查是否已存在
            if (paperDao.getByFilePath(filePath) != null) {
                Toast.makeText(this, "该试卷已添加", Toast.LENGTH_SHORT).show()
                return
            }
            
            // 提取文件名
            val fileName = File(filePath).nameWithoutExtension
            
            // 创建 Paper 对象
            val maxPosition = paperDao.getMaxPosition() ?: 0
            val paper = Paper(
                title = fileName,
                filePath = filePath,
                position = maxPosition + 1
            )
            
            // 插入数据库
            paperDao.insert(paper)
            Toast.makeText(this, "试卷添加成功", Toast.LENGTH_SHORT).show()
        }
    }
}
```

#### 2.1.5 重命名试卷功能

**实现方式**: 直接修改本地文件名,同时更新数据库记录

**流程**:
1. 用户长按试卷项 → 选择"重命名"
2. 弹出输入框,显示当前试卷名
3. 用户输入新名称
4. 验证新名称合法性 (非空、不包含特殊字符)
5. 重命名本地文件: `oldFile.renameTo(newFile)`
6. 更新数据库中的 `title` 和 `filePath`
7. 刷新列表

**代码示例**:

```kotlin
private fun renamePaper(paper: Paper) {
    val builder = AlertDialog.Builder(this)
    val input = EditText(this).apply {
        setText(paper.title)
        selectAll()
    }
    
    builder.setTitle("重命名试卷")
        .setView(input)
        .setPositiveButton("确定") { _, _ ->
            val newTitle = input.text.toString().trim()
            if (newTitle.isEmpty()) {
                Toast.makeText(this, "试卷名不能为空", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            
            val oldFile = File(paper.filePath)
            val newFile = File(oldFile.parent, "$newTitle.docx")
            
            if (oldFile.renameTo(newFile)) {
                paper.title = newTitle
                paper.filePath = newFile.absolutePath
                paper.editTime = Date()
                paperDao.update(paper)
                Toast.makeText(this, "重命名成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "重命名失败", Toast.LENGTH_SHORT).show()
            }
        }
        .setNegativeButton("取消", null)
        .show()
}
```

#### 2.1.6 拖拽排序功能

**实现**: 使用 `ItemTouchHelper` + RecyclerView

**代码示例**:

```kotlin
val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
    ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
) {
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val fromPosition = viewHolder.adapterPosition
        val toPosition = target.adapterPosition
        
        // 交换列表中的位置
        Collections.swap(paperList, fromPosition, toPosition)
        adapter.notifyItemMoved(fromPosition, toPosition)
        
        return true
    }
    
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // 不处理滑动
    }
    
    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        
        // 拖拽结束后,更新数据库中的 position
        paperList.forEachIndexed { index, paper ->
            paper.position = index
            paperDao.updatePosition(paper.id, index)
        }
    }
})

itemTouchHelper.attachToRecyclerView(recyclerView)
```

---

### 2.2 试题数据管理设计

#### 2.2.1 功能描述
试题数据采用"实时解析"模式,每次打开试卷时解析 `.docx` 文档,数据仅保存在内存中,不持久化到数据库。

#### 2.2.2 数据库设计

**数据库表设计** (仅包含必要的表):
- `paper` 表: 试卷元数据
- `study_record` 表: 学习记录
- `study_status` 表: 学习状态
- `config` 表: 应用配置

**不需要的表** (无需实现):
- ~~`question` 表~~
- ~~`content` 表~~
- ~~`element` 表~~
- ~~`book` 表~~
- ~~`course` 表~~
- ~~`dcontext` 表~~

**DAO 设计**:
- `PaperDao`: 试卷元数据操作
- `StudyRecordDao`: 学习记录操作
- `StudyStatusDao`: 学习状态操作
- `ConfigDao`: 配置操作

#### 2.2.3 新的数据流程

**打开试卷流程**:
1. 用户点击试卷项
2. 从数据库获取 Paper 对象 (包含 filePath)
3. 使用 `BookParser.parse(filePath)` 解析文档
4. 返回 `List<PQuestion>` (内存对象)
5. 将数据传递给 ViewModel
6. UI 层从 ViewModel 获取数据并展示

**BookParser 实现**:

```kotlin
object BookParser {
    /**
     * 解析试卷文档,返回试题列表 (内存对象)
     * @param filePath 试卷文件路径
     * @param onProgress 进度回调 (0-100)
     * @return 试题列表
     */
    fun parse(filePath: String, onProgress: ((Int) -> Unit)? = null): List<PQuestion> {
        val inputStream = FileInputStream(filePath)
        val document = XWPFDocument(inputStream)
        val paragraphs = document.paragraphs
        
        onProgress?.invoke(10) // 文档加载完成
        
        // 按题型分组
        val groupedParagraphs = splitByTitleSeparator(paragraphs)
        onProgress?.invoke(30) // 分组完成
        
        // 解析试题
        val questions = mutableListOf<PQuestion>()
        val totalGroups = groupedParagraphs.size
        
        groupedParagraphs.forEachIndexed { index, group ->
            val parsedQuestions = parseParagraphToQuestion(group)
            questions.addAll(parsedQuestions)
            
            // 更新进度 (30% - 90%)
            val progress = 30 + ((index + 1) * 60 / totalGroups)
            onProgress?.invoke(progress)
        }
        
        onProgress?.invoke(95) // 解析完成
        
        inputStream.close()
        onProgress?.invoke(100) // 全部完成
        
        return questions
    }
    
    // ... 其他解析方法保持不变 ...
}
```

**ViewModel 实现** (使用协程异步解析):

```kotlin
class PaperDetailViewModel : ViewModel() {
    // 试题数据
    private val _questions = MutableLiveData<List<PQuestion>>()
    val questions: LiveData<List<PQuestion>> = _questions
    
    // 加载状态
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // 错误信息
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    // 解析进度 (0-100)
    private val _progress = MutableLiveData<Int>()
    val progress: LiveData<Int> = _progress
    
    /**
     * 加载试卷数据
     * @param filePath 试卷文件路径
     */
    fun loadPaper(filePath: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _progress.value = 0
            
            try {
                // 在 IO 线程执行解析
                val questionList = withContext(Dispatchers.IO) {
                    BookParser.parse(filePath) { currentProgress ->
                        // 更新解析进度
                        _progress.postValue(currentProgress)
                    }
                }
                
                // 解析成功，更新数据
                _questions.value = questionList
                _progress.value = 100
            } catch (e: FileNotFoundException) {
                _error.value = "文件不存在: ${e.message}"
                Log.e("PaperDetailViewModel", "文件不存在", e)
            } catch (e: IOException) {
                _error.value = "文件读取失败: ${e.message}"
                Log.e("PaperDetailViewModel", "文件读取失败", e)
            } catch (e: Exception) {
                _error.value = "解析失败: ${e.message}"
                Log.e("PaperDetailViewModel", "解析试卷失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _error.value = null
    }
}
```

#### 2.2.4 性能优化与协程使用

**优化策略**:
1. **协程异步解析**: 使用 Kotlin 协程在后台线程解析,避免阻塞 UI 线程
2. **缓存机制**: 在 ViewModel 中缓存已解析的试题数据,避免重复解析
3. **进度反馈**: 解析过程中实时更新进度,提升用户体验
4. **错误处理**: 完善的异常捕获和错误提示
5. **懒加载**: 如果试题数量较多,可以考虑分页加载 (可选)

**依赖配置** (build.gradle):

```gradle
dependencies {
    // Kotlin 协程
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
    
    // Lifecycle (包含 viewModelScope)
    implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2"
    implementation "androidx.lifecycle:lifecycle-livedata-ktx:2.6.2"
}
```

**带缓存和进度的完整示例**:

```kotlin
class PaperDetailViewModel : ViewModel() {
    // 缓存已解析的试卷数据
    private val cache = mutableMapOf<String, List<PQuestion>>()
    
    private val _questions = MutableLiveData<List<PQuestion>>()
    val questions: LiveData<List<PQuestion>> = _questions
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _progress = MutableLiveData<Int>()
    val progress: LiveData<Int> = _progress
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    /**
     * 加载试卷，优先使用缓存
     */
    fun loadPaper(filePath: String, forceRefresh: Boolean = false) {
        // 检查缓存
        if (!forceRefresh && cache.containsKey(filePath)) {
            _questions.value = cache[filePath]
            return
        }
        
        // 使用协程异步解析
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _progress.value = 0
            
            try {
                val questionList = withContext(Dispatchers.IO) {
                    BookParser.parse(filePath) { progress ->
                        _progress.postValue(progress)
                    }
                }
                
                // 缓存结果
                cache[filePath] = questionList
                
                // 更新 UI
                _questions.value = questionList
                _progress.value = 100
            } catch (e: Exception) {
                _error.value = "解析失败: ${e.message}"
                Log.e("PaperDetailViewModel", "解析失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        cache.clear()
    }
    
    /**
     * 清除指定试卷的缓存
     */
    fun clearCache(filePath: String) {
        cache.remove(filePath)
    }
}
```

**UI 层使用示例**:

```kotlin
class PaperDetailFragment : Fragment() {
    private val viewModel: PaperDetailViewModel by viewModels()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 观察加载状态
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // 观察解析进度
        viewModel.progress.observe(viewLifecycleOwner) { progress ->
            progressBar.progress = progress
            progressText.text = "$progress%"
        }
        
        // 观察错误信息
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
        
        // 观察试题数据
        viewModel.questions.observe(viewLifecycleOwner) { questions ->
            // 更新 UI 显示试题
            adapter.submitList(questions)
        }
        
        // 加载试卷
        val filePath = arguments?.getString("filePath") ?: return
        viewModel.loadPaper(filePath)
    }
}
```

---

### 2.3 删除废纸篓功能

#### 2.3.1 影响范围

**需要删除的代码**:
- 废纸篓相关的 UI 组件 (Fragment, Activity, Menu Item)
- 废纸篓相关的数据库逻辑 (如果有 `isDeleted` 字段)
- "移动到废纸篓"的菜单项和逻辑

**需要修改的逻辑**:
- 删除试题时,直接从数据库删除 (如果还有数据库存储)
- 删除试卷时,直接删除 Paper 记录,可选择是否删除本地文件

#### 2.3.2 删除试卷逻辑

**流程**:
1. 用户长按试卷项 → 选择"删除"
2. 弹出确认对话框: "是否删除试卷?"
3. 用户确认后:
   - 从数据库删除 Paper 记录
   - (可选) 删除本地文件
4. 刷新列表

**代码示例**:

```kotlin
private fun deletePaper(paper: Paper) {
    AlertDialog.Builder(this)
        .setTitle("删除试卷")
        .setMessage("确定要删除试卷 \"${paper.title}\" 吗?")
        .setPositiveButton("删除") { _, _ ->
            // 删除数据库记录
            paperDao.delete(paper)
            
            // (可选) 删除本地文件
            // File(paper.filePath).delete()
            
            Toast.makeText(this, "试卷已删除", Toast.LENGTH_SHORT).show()
        }
        .setNegativeButton("取消", null)
        .show()
}
```

---

### 2.4 删除备份功能

#### 2.4.1 影响范围

**需要删除的代码**:
- `BackupManager.kt`
- 备份相关的 UI (设置页面中的备份选项)
- 备份相关的 Menu Item

**理由**: 
- 试题数据不再存储到数据库,备份的意义降低
- 用户可以直接备份试卷文件 (`.docx` 文件)

---

### 2.5 删除全局搜索功能

#### 2.5.1 功能调整

**移除**:
- 跨试卷的全局搜索功能
- 搜索相关的 UI (SearchView, SearchActivity)

**保留**:
- 试卷内搜索功能 (在内存中搜索)

#### 2.5.2 试卷内搜索实现

**流程**:
1. 用户在试卷详情页点击搜索图标
2. 显示 SearchView
3. 用户输入关键词
4. 在 ViewModel 的 `questions` 列表中过滤
5. 更新 UI 显示匹配的试题

**代码示例**:

```kotlin
class PaperDetailViewModel : ViewModel() {
    private val _allQuestions = MutableLiveData<List<PQuestion>>()
    private val _filteredQuestions = MutableLiveData<List<PQuestion>>()
    val questions: LiveData<List<PQuestion>> = _filteredQuestions
    
    fun search(keyword: String) {
        val allQuestions = _allQuestions.value ?: return
        
        if (keyword.isEmpty()) {
            _filteredQuestions.value = allQuestions
            return
        }
        
        val filtered = allQuestions.filter { question ->
            question.body.contains(keyword, ignoreCase = true) ||
            question.options.any { it.contains(keyword, ignoreCase = true) } ||
            question.answer.contains(keyword, ignoreCase = true)
        }
        
        _filteredQuestions.value = filtered
    }
}
```

---

## 3. 数据库设计

### 3.1 AppDatabase 配置

```kotlin
@Database(
    entities = [Paper::class, StudyRecord::class, StudyStatus::class, Config::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun paperDao(): PaperDao
    abstract fun studyRecordDao(): StudyRecordDao
    abstract fun studyStatusDao(): StudyStatusDao
    abstract fun configDao(): ConfigDao
    
    companion object {
        const val DATABASE_NAME = "day-exam-db"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }
        
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            ).build()
        }
    }
}
```

### 3.2 数据表结构

详见各功能模块的 Entity 设计。

---

## 4. 核心组件清单

### 4.1 数据层组件

**Entity**:
- `Paper.kt`: 试卷元数据实体
- `StudyRecord.kt`: 学习记录实体
- `StudyStatus.kt`: 学习状态实体
- `Config.kt`: 配置实体

**DAO**:
- `PaperDao.kt`: 试卷数据访问对象
- `StudyRecordDao.kt`: 学习记录数据访问对象
- `StudyStatusDao.kt`: 学习状态数据访问对象
- `ConfigDao.kt`: 配置数据访问对象

**Database**:
- `AppDatabase.kt`: 数据库配置
- `DateConverter.kt`: 日期类型转换器

### 4.2 业务逻辑层组件

**Parser**:
- `BookParser.kt`: 试卷文档解析器 (支持进度回调)
- `PQuestion.kt`: 试题内存模型
- `PBody.kt`: 题干内存模型
- `ImageCacheManager.kt`: 图片缓存管理

**Repository**:
- `DataRepository.kt`: 数据仓库
- `PaperRepository.kt`: 试卷数据仓库 (可选)

**协程配置**:
- Kotlin Coroutines: 异步解析和后台任务
- Dispatchers.IO: 文件 I/O 操作
- viewModelScope: ViewModel 生命周期绑定的协程作用域

### 4.3 UI 层组件

**Activity**:
- `MainActivity.kt`: 主界面

**Fragment**:
- `PaperListFragment.kt`: 试卷列表
- `PaperDetailFragment.kt`: 试卷详情

**ViewModel**:
- `PaperListViewModel.kt`: 试卷列表视图模型
- `PaperDetailViewModel.kt`: 试卷详情视图模型

**Adapter**:
- `PaperListAdapter.kt`: 试卷列表适配器
- `QuestionPagerAdapter.kt`: 试题翻页适配器

**Layout**:
- `activity_main.xml`: 主界面布局
- `fragment_paper_list.xml`: 试卷列表布局
- `fragment_paper_detail.xml`: 试卷详情布局
- `item_paper.xml`: 试卷列表项布局

---

## 5. 开发任务分解

### 5.1 Phase 1: 数据库设计与实现 (优先级: 高)
- [ ] 设计并实现 `Paper.kt` Entity
- [ ] 实现 `PaperDao.kt`
- [ ] 实现 `StudyRecord.kt` 和 `StudyRecordDao.kt`
- [ ] 实现 `StudyStatus.kt` 和 `StudyStatusDao.kt`
- [ ] 实现 `Config.kt` 和 `ConfigDao.kt`
- [ ] 配置 `AppDatabase.kt`
- [ ] 编写数据库单元测试

### 5.2 Phase 2: 解析逻辑实现 (优先级: 高)
- [ ] 配置 Kotlin 协程依赖 (kotlinx-coroutines-core, kotlinx-coroutines-android)
- [ ] 配置 Lifecycle 依赖 (lifecycle-viewmodel-ktx, lifecycle-livedata-ktx)
- [ ] 实现 `BookParser.kt`,解析 .docx 文档
- [ ] 在 `BookParser.parse()` 中添加进度回调参数
- [ ] 定义 `PQuestion` 等内存模型
- [ ] 实现 `PaperDetailViewModel`,使用协程异步加载
- [ ] 在 ViewModel 中添加加载状态、进度、错误管理
- [ ] 实现缓存机制,避免重复解析
- [ ] 添加完善的异常处理 (FileNotFoundException, IOException 等)

### 5.3 Phase 3: UI 实现 (优先级: 中)
- [ ] 设计并实现侧边栏布局
- [ ] 实现试卷列表 RecyclerView 和 Adapter
- [ ] 实现添加试卷功能 (文件选择器)
- [ ] 实现重命名试卷功能
- [ ] 实现拖拽排序功能 (ItemTouchHelper)
- [ ] 实现删除试卷功能
- [ ] 实现试卷详情页
- [ ] 实现试题展示和翻页

### 5.4 Phase 4: 辅助功能实现 (优先级: 中)
- [ ] 实现试卷内搜索功能
- [ ] 实现学习进度记录
- [ ] 实现学习状态管理
- [ ] 实现应用配置管理

### 5.5 Phase 5: 测试与优化 (优先级: 高)
- [ ] 单元测试: PaperDao, BookParser
- [ ] 集成测试: 添加/删除/重命名试卷流程
- [ ] UI 测试: 拖拽排序,搜索功能
- [ ] 性能测试: 大文件解析性能
- [ ] 用户验收测试

---

## 6. 验收标准

### 6.1 功能验收
- [ ] 侧边栏展示试卷列表,结构简洁清晰
- [ ] 可以添加本地 `.docx` 文件作为试卷
- [ ] 重复添加同一文件时弹出提示
- [ ] 可以重命名试卷,本地文件名同步修改
- [ ] 可以拖拽调整试卷顺序,顺序持久化
- [ ] 可以删除试卷,有确认提示
- [ ] 打开试卷时实时解析,显示加载进度
- [ ] 试题展示正确,支持图片、选项、答案
- [ ] 试卷内搜索功能正常,结果准确
- [ ] 学习进度正确记录和显示

### 6.2 性能验收
- [ ] 解析 100 道题的试卷耗时 < 2 秒
- [ ] 拖拽排序流畅,无卡顿
- [ ] 搜索响应时间 < 500ms

### 6.3 兼容性验收
- [ ] 支持 Android 6.0+ 系统
- [ ] 支持不同屏幕尺寸和分辨率
- [ ] 支持深色模式 (可选)
- [ ] 文件路径处理兼容不同存储位置

---

## 7. 风险与注意事项

### 7.1 性能风险
- **风险**: 大文件解析可能导致 UI 卡顿
- **缓解措施**: 
  - 使用协程异步解析
  - 显示加载进度
  - 添加缓存机制

### 7.2 文件管理风险
- **风险**: 用户手动删除/移动文件后,数据库记录失效
- **缓解措施**: 
  - 打开试卷前检查文件是否存在
  - 如果文件不存在,提示用户并提供"重新选择文件"或"删除记录"选项

---

## 8. 后续优化方向

### 8.1 云同步
- 支持将试卷文件上传到云端 (Google Drive, Dropbox)
- 多设备同步试卷列表

### 8.2 试卷模板
- 提供试卷模板,帮助用户快速创建试卷

### 8.3 统计分析
- 记录学习进度 (StudyRecord 表保留)
- 提供学习报告和统计图表

---

## 附录: 关键代码文件清单

### A.1 数据库相关
- `app/src/main/java/com/gorden/dayexam/db/AppDatabase.kt`
- `app/src/main/java/com/gorden/dayexam/db/entity/Paper.kt`
- `app/src/main/java/com/gorden/dayexam/db/dao/PaperDao.kt`

### A.2 解析相关
- `app/src/main/java/com/gorden/dayexam/parser/BookParser.kt`
- `app/src/main/java/com/gorden/dayexam/parser/model/PQuestion.kt`

### A.3 UI 相关
- `app/src/main/java/com/gorden/dayexam/MainActivity.kt`
- `app/src/main/java/com/gorden/dayexam/ui/home/HomeFragment.kt`
- `app/src/main/res/layout/activity_main.xml`

### A.4 Repository 相关
- `app/src/main/java/com/gorden/dayexam/repository/DataRepository.kt`

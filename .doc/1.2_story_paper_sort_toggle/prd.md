# PRD: 学习试卷 Pager 页面排序切换

## 背景

当前学习试卷的 ViewPager2 页面，试题严格按照原始试卷的序号顺序排列。用户希望对同一类型的题目集中练习（例如先做完所有单选题，再做多选题），需要一个在「序号排序」和「按类型分组排序」之间切换的能力。

## 需求描述

在学习试卷的 pager 页面增加排序切换交互，支持两种排序模式：

| 模式 | 说明 |
|------|------|
| **按序号排序（默认）** | 保持原始试卷题目顺序，与当前行为一致 |
| **按类型分组排序** | 按题目类型分组，类型之间按固定顺序排列（填空 → 判断 → 单选 → 多选 → 简答），同一类型内部保持原始相对顺序 |

## 交互设计

- 在首页右上角「更多」弹窗（ShortCutSheetDialog）中新增一个排序切换开关（Switch）
- 开关关闭 = 按序号排序（默认），开关开启 = 按类型分组排序
- 切换时，ViewPager2 定位到当前题目在新排序下的位置（尽力匹配，不严格保证）
- 排序偏好通过 SharedPreferences 持久化

## 功能要求

### F1: 排序模式状态管理
- HomeFragment 持有 `sortMode` 状态（enum: `SEQUENCE` / `TYPE_GROUP`）
- 维护两份列表映射：`originalQuestions`（不变）和 `displayQuestions`（派生）
- 提供 `displayToOriginal(position)` 和 `originalToDisplay(position)` 两个位置映射方法

### F2: Pager 适配
- QuestionPagerAdapter 无需修改，接收 displayQuestions 即可
- 切换排序时重新 setData

### F3: 更多弹窗适配
- ShortCutSheetDialog 新增排序开关行（icon + 文字 + Switch），样式与背诵模式开关一致
- 开关状态读写 SharedPreferences

### F4: 底部面板适配
- 列表视图（QuestionListAdapter）：跟随 displayQuestions
- 网格视图（AnswerStatusAdapter）：显示原始题号（`originalIndex + 1`），而非位置号

### F5: 位置追踪
- `lastStudyPosition` 保存原始列表位置，不受排序模式影响
- 搜索跳转结果使用原始索引，调用 `originalToDisplay()` 转换后再跳转
- 前后翻页（NAVIGATE_QUESTION）在 display 列表中移动，行为不变

## 涉及文件

| 文件 | 变更类型 |
|------|----------|
| `app/src/main/java/com/gorden/dayexam/ui/home/HomeFragment.kt` | 主要修改 — 增加排序状态、列表映射 |
| `app/src/main/java/com/gorden/dayexam/ui/sheet/shortcut/ShortCutSheetDialog.kt` | 新增排序开关行 |
| `app/src/main/java/com/gorden/dayexam/ui/sheet/status/AnswerStatusAdapter.kt` | 显示原始题号 |
| `app/src/main/java/com/gorden/dayexam/ui/sheet/question/QuestionListBottomSheet.kt` | 列表/网格视图传递 displayQuestions |
| `app/src/main/res/layout/short_cut_sheet_layout.xml` | 新增排序开关行布局 |
| `app/src/main/res/values/strings.xml` | 排序相关字符串 |
| `app/src/main/java/com/gorden/dayexam/utils/SharedPreferenceUtil.kt` | 排序偏好读写（间接使用） |

## 不包含

- 类型分组模式下的 section header（后续版本）
- 拖拽自定义排序

## 验收标准

1. 打开试卷，默认按序号排序，与当前行为一致
2. 打开排序开关，切换为类型分组排序，题目重新排列
3. 网格答题卡中的编号始终显示原始题号
4. 排序模式下做题并前后翻页，行为正常
5. 搜索结果跳转在两种排序模式下均正确定位
6. 切换排序不丢失当前做题状态（realAnswer 保留）

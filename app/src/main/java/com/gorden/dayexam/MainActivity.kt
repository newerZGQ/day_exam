package com.gorden.dayexam

import android.Manifest
import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Color.blue
import android.os.Bundle
import android.provider.MediaStore
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.URLSpan
import android.util.Log
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceManager
import com.gorden.dayexam.db.DefaultDataGenerator
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.ui.Constants
import com.gorden.dayexam.ui.Constants.Companion.HAS_AGREE_PRIVACY_KEY
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.ui.book.BooksFragment
import com.gorden.dayexam.ui.dialog.element.ImageElementEditCard
import com.gorden.dayexam.ui.home.HomeFragment
import com.gorden.dayexam.ui.home.shortcut.FastQuestionSelectActivity
import com.gorden.dayexam.ui.home.shortcut.FastQuestionSelectActivity.Companion.CURRENT_POSITION
import com.gorden.dayexam.ui.home.shortcut.FastQuestionSelectActivity.Companion.PAPER_ID_KEY
import com.gorden.dayexam.ui.home.shortcut.SimpleQuestionViewHolder
import com.gorden.dayexam.ui.sheet.course.CourseSheetDialog
import com.gorden.dayexam.ui.sheet.search.SearchSheetDialog
import com.gorden.dayexam.ui.sheet.shortcut.ShortCutSheetDialog
import com.gorden.dayexam.utils.SharedPreferenceUtil
import com.jeremyliao.liveeventbus.LiveEventBus
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView
import kotlinx.android.synthetic.main.app_bar_main.*
import java.io.IOException


class MainActivity : BaseActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private val courseSheet = CourseSheetDialog()
    private val shortCutSheet = ShortCutSheetDialog()
    private val searchSheet = SearchSheetDialog()
    private val homeFragment = HomeFragment()
    private val bookListFragment = BooksFragment()
    private var curCourseId = 0
    private var curCourseTitle = ""
    private var curPaperId = 0
    private var curQuestionId = 0
    private var lastHomepagePosition = -1

    // config相关
    private var isFocusMode = false

    private lateinit var todayCount: TextView

    private var photoSelectCallback: ImageElementEditCard.PhotoSelectCallback? = null

    companion object {
        const val SELECT_QUESTION_REQUEST_CODE = 201
        const val SELECT_QUESTION_RESULT_CODE = 202

        const val SELECT_PHOTO_REQUEST_CODE = 301
    }

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawerLayout = findViewById(R.id.drawer_layout)
        initToolBar()
        initFab()
        initFragment()
        registerEvent()
        observeDatabase()
        observeCurrentCourse()
        observeDContext()
        observeConfig()
        observeTodayStudyCount()
        checkScreenLight()
        checkPrivacyDialog()
    }

    fun closeDrawerLayout() {
        drawerLayout.closeDrawer(Gravity.LEFT)
    }

    private fun initToolBar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.setTitleTextAppearance(this, R.style.XWWKBoldTextAppearance)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            0,
            0
        )
        toggle.isDrawerIndicatorEnabled = true
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        toolbar.findViewById<TextView>(R.id.title).setOnClickListener {
            showCourseSheet()
        }
        toolbar.findViewById<ImageView>(R.id.title_drop_down).setOnClickListener {
            showCourseSheet()
        }
        todayCount = toolbar.findViewById(R.id.today_study_count)
    }

    private fun showCourseSheet() {
        courseSheet.show(
            supportFragmentManager,
            "course"
        )
    }

    private fun initFab() {
        val fab: SpeedDialView = findViewById(R.id.fab)
        fab.addActionItem(
            SpeedDialActionItem.Builder(
                R.id.float_button_reset_question_item,
                R.drawable.ic_baseline_refresh_24
            )
                .setFabBackgroundColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.colorPrimaryDark,
                        theme
                    )
                )
                .setFabImageTintColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.float_button_item_icon_color,
                        theme
                    )
                )
                .setLabel(getString(R.string.reset_current_question))
                .setLabelClickable(false)
                .setTheme(R.style.FloatButtonTextAppearance)
                .create()
        )
        fab.addActionItem(
            SpeedDialActionItem.Builder(
                R.id.float_button_list_question_item,
                R.drawable.ic_outline_format_list_numbered_24
            )
                .setFabBackgroundColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.colorPrimaryDark,
                        theme
                    )
                )
                .setFabImageTintColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.float_button_item_icon_color,
                        theme
                    )
                )
                .setLabel(getString(R.string.list_question))
                .setLabelClickable(false)
                .setTheme(R.style.FloatButtonTextAppearance)
                .create()
        )
        fab.setOnActionSelectedListener {
            when (it.id) {
                R.id.float_button_reset_question_item -> {
                    LiveEventBus.get(EventKey.REFRESH_QUESTION, Int::class.java).post(0)
                    fab.close()
                }
                R.id.float_button_favorite_question_item -> {
                    LiveEventBus.get(EventKey.FAVORITE_QUESTION, Int::class.java).post(0)
                    fab.close()
                }
                R.id.float_button_list_question_item -> {
                    val intent = Intent(this, FastQuestionSelectActivity::class.java)
                    intent.putExtra(PAPER_ID_KEY, curPaperId)
                    lastHomepagePosition = homeFragment.currentPosition()
                    intent.putExtra(CURRENT_POSITION, lastHomepagePosition)
                    startActivityForResult(intent, SELECT_QUESTION_REQUEST_CODE)
                    fab.close()
                }
            }
            return@setOnActionSelectedListener true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SELECT_QUESTION_REQUEST_CODE && resultCode == SELECT_QUESTION_RESULT_CODE) {
            val selectPosition =
                data?.getIntExtra(SimpleQuestionViewHolder.SELECT_POSITION, -1) ?: -1
            if (selectPosition == -1 || selectPosition == lastHomepagePosition) {
                return
            }
            homeFragment.setCurrentPosition(selectPosition)
        } else if (requestCode == SELECT_PHOTO_REQUEST_CODE) {
            val selectedImage = data?.data
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImage)
                photoSelectCallback?.onSelect(bitmap)
            } catch (e: IOException) {
                Log.e("", "")
            }
        }
    }

    private fun initFragment() {
        supportFragmentManager
            .beginTransaction()
            .add(R.id.fragment_content, homeFragment)
            .add(R.id.book_list_container, bookListFragment)
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                shortCutSheet.show(
                    supportFragmentManager,
                    "shortcut"
                )
            }
        }
        return true
    }

    private fun registerEvent() {
        LiveEventBus.get(EventKey.SEARCH_CLICKED, Int::class.java)
            .observe(this, {
                drawerLayout.closeDrawers()
                searchSheet.show(
                    supportFragmentManager,
                    "Search"
                )
            })

        LiveEventBus.get(
            EventKey.EDIT_SELECT_PHOTO_CLICK,
            ImageElementEditCard.PhotoSelectCallback::class.java
        )
            .observe(this, {
                this.photoSelectCallback = it
                selectPhoto()
            })
        LiveEventBus.get(EventKey.START_PROGRESS_BAR, Int::class.java).observe(this, {
            findViewById<View>(R.id.parsing_progress).visibility = View.VISIBLE
        })
        LiveEventBus.get(EventKey.END_PROGRESS_BAR, Int::class.java).observe(this, {
            drawerLayout.postDelayed({
                findViewById<View>(R.id.parsing_progress).visibility = View.GONE
            }, 1000)
        })
        LiveEventBus.get(
            EventKey.PAPER_MENU_ADD_QUESTION_FROM_FILE,
            EventKey.QuestionAddEventModel::class.java
        )
            .observe(this, {
                drawerLayout.closeDrawers()
            })
    }

    private fun observeDatabase() {
        DataRepository.isDatabaseCreated().observe(this, {
            if (it) {
                DefaultDataGenerator.generate()
            }
        })
    }

    private fun observeCurrentCourse() {
        DataRepository.currentCourse().observe(this, {
            if (it == null) {
                toolbar.findViewById<TextView>(R.id.title).text = ""
                return@observe
            }
            if (it != null && it.id != curCourseId) {
                toolbar.findViewById<TextView>(R.id.title).text = it.title
                curCourseId = it.id
                curCourseTitle = it.title
            }
            if (it != null && it.title != curCourseTitle) {
                toolbar.findViewById<TextView>(R.id.title).text = it.title
                curCourseTitle = it.title
            }
        })
    }

    private fun observeDContext() {
        DataRepository.getDContext().observe(this, {
            if (it != null) {
                curPaperId = it.curPaperId
                curQuestionId = it.curQuestionId
            }
        })
    }

    private fun observeConfig() {
        DataRepository.getConfig().observe(this, { config ->
            config?.let {
                if (it.focusMode && supportActionBar != null) {
                    toFocusMode()
                } else if (this.isFocusMode && supportActionBar != null) {
                    exitFocusMode()
                }
                this.isFocusMode = it.focusMode
            }
        })
    }

    private fun toFocusMode() {
        supportActionBar?.hide()
        val animator = ValueAnimator.ofInt(supportActionBar?.height!!, 0)
        animator.interpolator = AccelerateInterpolator(2.toFloat())
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            val layoutParams =
                (fragment_content.layoutParams as ConstraintLayout.LayoutParams)
            layoutParams.topMargin = value
            fragment_content.layoutParams = layoutParams
        }
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator?) {

            }

            override fun onAnimationEnd(p0: Animator?) {
                if (!SharedPreferenceUtil.getBoolean(Constants.HAS_GUIDE_FOCUS, false)) {
                    tryShowFocusGuide()
                    SharedPreferenceUtil.setBoolean(Constants.HAS_GUIDE_FOCUS, true)
                }
            }

            override fun onAnimationCancel(p0: Animator?) {

            }

            override fun onAnimationRepeat(p0: Animator?) {

            }

        })
        animator.start()
        fab.visibility = View.GONE
    }

    private fun exitFocusMode() {
        supportActionBar?.show()
        val animator = ValueAnimator.ofInt(0, supportActionBar?.height!!)
        animator.interpolator = AccelerateInterpolator(2.toFloat())
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            val layoutParams =
                (fragment_content.layoutParams as ConstraintLayout.LayoutParams)
            layoutParams.topMargin = value
            fragment_content.layoutParams = layoutParams
        }
        animator.start()
        fab.visibility = View.VISIBLE
    }

    private fun tryShowFocusGuide() {
        // 引导功能原先依赖第三方高亮库，
        // 现在去掉该库后，暂时不再展示高亮引导，仅保留占位方法以避免逻辑改动过大。
    }

    private fun observeTodayStudyCount() {
        DataRepository.todayStudyCount().observe(this, {
            todayCount.text = it.toString()
        })
    }

    private fun selectPhoto() {
        //动态申请权限
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission
                    .WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1
            )
        } else {
            //执行启动相册的方法
            openAlbum();
        }
    }

    //启动相册的方法
    private fun openAlbum() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        ActivityCompat.startActivityForResult(this, intent, SELECT_PHOTO_REQUEST_CODE, null)
    }

    private fun checkScreenLight() {
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(ContextHolder.currentActivity())
        val keepScreenOnKey =
            ContextHolder.application.resources.getString(R.string.keep_screen_light_key)
        val keepScreenOn = sharedPreferences.getBoolean(keepScreenOnKey, false)
        if (keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun checkPrivacyDialog() {
        val hasShowPrivacy = SharedPreferenceUtil.getBoolean(HAS_AGREE_PRIVACY_KEY, false)
        if (!hasShowPrivacy) {
            val spannableString = SpannableStringBuilder(getString(R.string.privacy_message_dialog))
            val privacyLink = getString(R.string.privacy_link)
            val linkSpannable = SpannableString(privacyLink)
            spannableString.append(linkSpannable)
            AlertDialog.Builder(this)
                .setMessage(spannableString)
                .setCancelable(false)
                .setPositiveButton(
                    getString(R.string.agree)
                ) { p0, p1 -> SharedPreferenceUtil.setBoolean(HAS_AGREE_PRIVACY_KEY, true) }
                .create().show()
        }
    }

}
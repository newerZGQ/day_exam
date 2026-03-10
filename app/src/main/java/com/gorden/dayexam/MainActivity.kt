package com.gorden.dayexam

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.drawerlayout.widget.DrawerLayout
import com.gorden.dayexam.databinding.ActivityMainBinding
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.repository.PaperDetailCache
import com.gorden.dayexam.ui.Constants
import com.gorden.dayexam.ui.Constants.Companion.HAS_AGREE_PRIVACY_KEY
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.ui.home.HomeFragment
import com.gorden.dayexam.ui.home.shortcut.SimpleQuestionViewHolder
import com.gorden.dayexam.ui.paper.PaperListFragment
import com.gorden.dayexam.ui.sheet.search.SearchSheetDialog
import com.gorden.dayexam.ui.sheet.shortcut.ShortCutSheetDialog
import com.gorden.dayexam.utils.SharedPreferenceUtil
import com.jeremyliao.liveeventbus.LiveEventBus
import com.leinardi.android.speeddial.SpeedDialView


class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var drawerLayout: DrawerLayout
    private val shortCutSheet = ShortCutSheetDialog()
    private val searchSheet = SearchSheetDialog()
    private val homeFragment = HomeFragment()
    private val paperListFragment = PaperListFragment()
    private var curPaperId = 0
    private var lastHomepagePosition = -1

    // config相关
    private var isFocusMode = false

    private lateinit var todayCount: TextView
    private lateinit var toolbarTitle: TextView

    companion object {
        const val SELECT_QUESTION_REQUEST_CODE = 201
        const val SELECT_QUESTION_RESULT_CODE = 202
    }

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        drawerLayout = binding.drawerLayout
        initDrawerWidth()
        initToolBar()
        initFab()
        initFragment()
        registerEvent()
        observeDContext()
        observeTodayStudyCount()
        checkScreenLight()
        observeCurrentPaper()
        checkPrivacyDialog()
    }

    fun closeDrawerLayout() {
        drawerLayout.closeDrawer(Gravity.LEFT)
    }

    private fun initDrawerWidth() {
        val screenWidth = resources.displayMetrics.widthPixels
        val drawerParams = binding.paperListContainer.layoutParams
        drawerParams.width = screenWidth * 4 / 5
        binding.paperListContainer.layoutParams = drawerParams
    }

    private fun initToolBar() {
        val toolbar: Toolbar = binding.homeMainLayout.toolbar
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
        todayCount = toolbar.findViewById(R.id.today_study_count)
        toolbarTitle = toolbar.findViewById(R.id.title)
    }

    private fun initFab() {
        val fab: SpeedDialView = binding.homeMainLayout.fab
        // Remove all action items to make it a single button
        fab.clearActionItems()
        
        // Set OnChangeListener to handle main FAB click when it doesn't expand
        // OR better: set OnMainFabChangeListener
        fab.setOnChangeListener(object : SpeedDialView.OnChangeListener {
            override fun onMainActionSelected(): Boolean {
                val detail = PaperDetailCache.get(curPaperId)
                if (detail != null) {
                     val sheet = com.gorden.dayexam.ui.sheet.question.QuestionListBottomSheet
                        .newInstance(curPaperId, homeFragment.currentPosition())
                     sheet.setData(detail)
                     sheet.show(supportFragmentManager, "QuestionList")
                }
                return false // Return false to keep it closed or true? Usually false if we handle it.
            }

            override fun onToggleChanged(isOpen: Boolean) {
                // Do nothing
            }
        })
    }

    private fun initFragment() {
        supportFragmentManager
            .beginTransaction()
            .add(binding.homeMainLayout.fragmentContent.id, homeFragment)
            .add(binding.paperListContainer.id, paperListFragment)
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
            .observe(this) {
                drawerLayout.closeDrawers()
                searchSheet.show(
                    supportFragmentManager,
                    "Search"
                )
            }

        // 监听试卷点击事件
        LiveEventBus.get(EventKey.PAPER_CONTAINER_CLICKED, EventKey.PaperClickEventModel::class.java)
            .observe(this) { event ->
                closeDrawerLayout()
            }

        // 监听试卷点击事件
        LiveEventBus.get(EventKey.KEEP_SCREEN_ON, Boolean::class.java)
            .observe(this) { event ->
                checkScreenLight()
            }
    }


    private fun observeDContext() {
        DataRepository.getCurPaperId().observe(this) {
            if (it != null) {
                curPaperId = it
            }
            closeDrawerLayout()
        }
    }

    private fun observeCurrentPaper() {
        DataRepository.currentPaper().observe(this) { paperInfo ->
            if (paperInfo != null) {
                toolbarTitle.text = paperInfo.title
            } else {
                toolbarTitle.text = getString(R.string.app_name)
            }
        }
    }

    private fun toFocusMode() {
        supportActionBar?.hide()
        val animator = ValueAnimator.ofInt(supportActionBar?.height!!, 0)
        animator.interpolator = AccelerateInterpolator(2.toFloat())
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            val layoutParams =
                (binding.homeMainLayout.fragmentContent.layoutParams as ConstraintLayout.LayoutParams)
            layoutParams.topMargin = value
            binding.homeMainLayout.fragmentContent.layoutParams = layoutParams
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
        binding.homeMainLayout.fab.visibility = View.GONE
    }

    private fun exitFocusMode() {
        supportActionBar?.show()
        val animator = ValueAnimator.ofInt(0, supportActionBar?.height!!)
        animator.interpolator = AccelerateInterpolator(2.toFloat())
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            val layoutParams =
                (binding.homeMainLayout.fragmentContent.layoutParams as ConstraintLayout.LayoutParams)
            layoutParams.topMargin = value
            binding.homeMainLayout.fragmentContent.layoutParams = layoutParams
        }
        animator.start()
        binding.homeMainLayout.fab.visibility = View.VISIBLE
    }

    private fun tryShowFocusGuide() {
        // 引导功能原先依赖第三方高亮库，
        // 现在去掉该库后，暂时不再展示高亮引导，仅保留占位方法以避免逻辑改动过大。
    }

    private fun observeTodayStudyCount() {
        DataRepository.todayStudyCount().observe(this) {
            todayCount.text = it.toString()
        }
    }

    private fun checkScreenLight() {
        val keepScreenOnKey =
            ContextHolder.application.resources.getString(R.string.keep_screen_light_key)
        val keepScreenOn = SharedPreferenceUtil.getBoolean(keepScreenOnKey, false)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SELECT_QUESTION_REQUEST_CODE && resultCode == SELECT_QUESTION_RESULT_CODE) {
            val selectPosition =
                data?.getIntExtra(SimpleQuestionViewHolder.SELECT_POSITION, -1) ?: -1
            if (selectPosition == -1 || selectPosition == lastHomepagePosition) {
                return
            }
            homeFragment.setCurrentPosition(selectPosition)
        }
    }

}
package com.example.socialroboticslab

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.socialroboticslab.util.PrefsManager

class SettingsActivity : AppCompatActivity() {

    // ── Input fields ──────────────────────────────────────────────────────────
    private lateinit var etHttpUrl: EditText
    private lateinit var etWsAudioUrl: EditText
    private lateinit var etWebrtcStreamUrl: EditText
    private lateinit var etVideoCallUrl: EditText
    private lateinit var etStunUrl: EditText
    private lateinit var etTurnUrl: EditText
    private lateinit var etTurnUser: EditText
    private lateinit var etTurnPass: EditText
    private lateinit var etWozUrl: EditText

    // ── Status indicators ────────────────────────────────────────────────────
    private lateinit var statusHttpUrl: TextView
    private lateinit var statusWsAudioUrl: TextView
    private lateinit var statusWebrtcStreamUrl: TextView
    private lateinit var statusVideoCallUrl: TextView
    private lateinit var statusWozUrl: TextView
    private lateinit var statusStunUrl: TextView
    private lateinit var statusTurnUrl: TextView
    private lateinit var statusTurnUser: TextView
    private lateinit var statusTurnPass: TextView

    // ── Navigation ───────────────────────────────────────────────────────────
    private enum class Section { CH2, CH3, CH4, CH5, CH6, WEBRTC }

    private data class NavIds(val navId: Int, val accentId: Int, val titleId: Int, val subId: Int)
    private data class NavViews(val nav: LinearLayout, val accent: View, val title: TextView, val sub: TextView)

    private val navDefs = mapOf(
        Section.CH2    to NavIds(R.id.navCh2,    R.id.navCh2Accent,    R.id.navCh2Title,    R.id.navCh2Sub),
        Section.CH3    to NavIds(R.id.navCh3,    R.id.navCh3Accent,    R.id.navCh3Title,    R.id.navCh3Sub),
        Section.CH4    to NavIds(R.id.navCh4,    R.id.navCh4Accent,    R.id.navCh4Title,    R.id.navCh4Sub),
        Section.CH5    to NavIds(R.id.navCh5,    R.id.navCh5Accent,    R.id.navCh5Title,    R.id.navCh5Sub),
        Section.CH6    to NavIds(R.id.navCh6,    R.id.navCh6Accent,    R.id.navCh6Title,    R.id.navCh6Sub),
        Section.WEBRTC to NavIds(R.id.navWebrtc, R.id.navWebrtcAccent, R.id.navWebrtcTitle, R.id.navWebrtcSub),
    )

    private val sectionIds = mapOf(
        Section.CH2    to R.id.sectionCh2,
        Section.CH3    to R.id.sectionCh3,
        Section.CH4    to R.id.sectionCh4,
        Section.CH5    to R.id.sectionCh5,
        Section.CH6    to R.id.sectionCh6,
        Section.WEBRTC to R.id.sectionWebrtc,
    )

    // Cached view references, populated in onCreate
    private val navViews = mutableMapOf<Section, NavViews>()

    private val autoSaveHandler = Handler(Looper.getMainLooper())

    // ── Stratos colour tokens ────────────────────────────────────────────────
    private val colorActive   = 0xFF005FB8.toInt()   // Primary Blue
    private val colorHighlight= 0xFFDBEAFE.toInt()   // Blue 100
    private val colorTextPrim = 0xFF0F172A.toInt()   // Slate 900
    private val colorTextSec  = 0xFF475569.toInt()   // Slate 600
    private val colorTextAct  = 0xFF005FB8.toInt()   // active subtitle = primary
    private val colorTransp   = Color.TRANSPARENT

    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.title = "Server Settings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        bindViews()
        cacheNavViews()
        loadSettings()
        setupAutoSave()
        setupNavigation()

        findViewById<Button>(R.id.btnReset).setOnClickListener { resetDefaults() }
    }

    // ── View binding ─────────────────────────────────────────────────────────

    private fun bindViews() {
        etHttpUrl         = findViewById(R.id.etHttpUrl)
        etWsAudioUrl      = findViewById(R.id.etWsAudioUrl)
        etWebrtcStreamUrl = findViewById(R.id.etWebrtcStreamUrl)
        etVideoCallUrl    = findViewById(R.id.etVideoCallUrl)
        etStunUrl         = findViewById(R.id.etStunUrl)
        etTurnUrl         = findViewById(R.id.etTurnUrl)
        etTurnUser        = findViewById(R.id.etTurnUser)
        etTurnPass        = findViewById(R.id.etTurnPass)
        etWozUrl          = findViewById(R.id.etWozUrl)

        statusHttpUrl         = findViewById(R.id.statusHttpUrl)
        statusWsAudioUrl      = findViewById(R.id.statusWsAudioUrl)
        statusWebrtcStreamUrl = findViewById(R.id.statusWebrtcStreamUrl)
        statusVideoCallUrl    = findViewById(R.id.statusVideoCallUrl)
        statusWozUrl          = findViewById(R.id.statusWozUrl)
        statusStunUrl         = findViewById(R.id.statusStunUrl)
        statusTurnUrl         = findViewById(R.id.statusTurnUrl)
        statusTurnUser        = findViewById(R.id.statusTurnUser)
        statusTurnPass        = findViewById(R.id.statusTurnPass)
    }

    private fun cacheNavViews() {
        navDefs.forEach { (section, ids) ->
            navViews[section] = NavViews(
                nav    = findViewById(ids.navId),
                accent = findViewById(ids.accentId),
                title  = findViewById(ids.titleId),
                sub    = findViewById(ids.subId),
            )
        }
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    private fun setupNavigation() {
        Section.values().forEach { section ->
            navViews[section]?.nav?.setOnClickListener { showSection(section) }
        }
    }

    private fun showSection(section: Section) {
        saveAll()   // 切換分頁前先儲存

        // 隱藏所有 sections，重置所有 nav 樣式
        sectionIds.forEach { (_, id) -> findViewById<View>(id).visibility = View.GONE }
        navViews.forEach { (_, v) -> applyNavStyle(v, selected = false) }

        // 顯示目標 section，高亮目標 nav
        sectionIds[section]?.let { findViewById<View>(it).visibility = View.VISIBLE }
        navViews[section]?.let { applyNavStyle(it, selected = true) }
    }

    private fun applyNavStyle(v: NavViews, selected: Boolean) {
        if (selected) {
            v.nav.setBackgroundColor(colorHighlight)
            v.accent.setBackgroundColor(colorActive)
            v.title.setTextColor(colorTextPrim)
            v.sub.setTextColor(colorTextAct)
        } else {
            v.nav.setBackgroundColor(colorTransp)
            v.accent.setBackgroundColor(colorTransp)
            v.title.setTextColor(colorTextPrim)
            v.sub.setTextColor(colorTextSec)
        }
    }

    // ── Auto-save (輸入停止 800ms 後觸發) ────────────────────────────────────

    private fun setupAutoSave() {
        addAutoSave(etHttpUrl,         { PrefsManager.setHttpUrl(this, it) },         statusHttpUrl)
        addAutoSave(etWsAudioUrl,      { PrefsManager.setWsAudioUrl(this, it) },      statusWsAudioUrl)
        addAutoSave(etWebrtcStreamUrl, { PrefsManager.setWebrtcStreamUrl(this, it) }, statusWebrtcStreamUrl)
        addAutoSave(etVideoCallUrl,    { PrefsManager.setVideoCallUrl(this, it) },    statusVideoCallUrl)
        addAutoSave(etWozUrl,          { PrefsManager.setWozUrl(this, it) },          statusWozUrl)
        addAutoSave(etStunUrl,         { PrefsManager.setStunUrl(this, it) },         statusStunUrl)
        addAutoSave(etTurnUrl,         { PrefsManager.setTurnUrl(this, it) },         statusTurnUrl)
        addAutoSave(etTurnUser,        { PrefsManager.setTurnUser(this, it) },        statusTurnUser)
        addAutoSave(etTurnPass,        { PrefsManager.setTurnPass(this, it) },        statusTurnPass)
    }

    private fun addAutoSave(et: EditText, save: (String) -> Unit, status: TextView) {
        et.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                autoSaveHandler.removeCallbacksAndMessages(et)
                autoSaveHandler.postDelayed({
                    save(s?.toString()?.trim() ?: "")
                    flashSaved(status)
                }, 800L)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun flashSaved(status: TextView) {
        status.visibility = View.VISIBLE
        status.postDelayed({ status.visibility = View.INVISIBLE }, 2000L)
    }

    // ── Explicit save-all（切換分頁 / 退出頁面時呼叫） ────────────────────────

    private fun saveAll() {
        PrefsManager.setHttpUrl(this,         etHttpUrl.text.toString().trim())
        PrefsManager.setWsAudioUrl(this,      etWsAudioUrl.text.toString().trim())
        PrefsManager.setWebrtcStreamUrl(this, etWebrtcStreamUrl.text.toString().trim())
        PrefsManager.setVideoCallUrl(this,    etVideoCallUrl.text.toString().trim())
        PrefsManager.setWozUrl(this,          etWozUrl.text.toString().trim())
        PrefsManager.setStunUrl(this,         etStunUrl.text.toString().trim())
        PrefsManager.setTurnUrl(this,         etTurnUrl.text.toString().trim())
        PrefsManager.setTurnUser(this,        etTurnUser.text.toString().trim())
        PrefsManager.setTurnPass(this,        etTurnPass.text.toString().trim())
    }

    // ── Load / Reset ─────────────────────────────────────────────────────────

    private fun loadSettings() {
        etHttpUrl.setText(PrefsManager.getHttpUrl(this))
        etWsAudioUrl.setText(PrefsManager.getWsAudioUrl(this))
        etWebrtcStreamUrl.setText(PrefsManager.getWebrtcStreamUrl(this))
        etVideoCallUrl.setText(PrefsManager.getVideoCallUrl(this))
        etStunUrl.setText(PrefsManager.getStunUrl(this))
        etTurnUrl.setText(PrefsManager.getTurnUrl(this))
        etTurnUser.setText(PrefsManager.getTurnUser(this))
        etTurnPass.setText(PrefsManager.getTurnPass(this))
        etWozUrl.setText(PrefsManager.getWozUrl(this))
    }

    private fun resetDefaults() {
        etHttpUrl.setText(PrefsManager.DEFAULT_HTTP_URL)
        etWsAudioUrl.setText(PrefsManager.DEFAULT_WS_AUDIO_URL)
        etWebrtcStreamUrl.setText(PrefsManager.DEFAULT_WEBRTC_STREAM_URL)
        etVideoCallUrl.setText(PrefsManager.DEFAULT_VIDEO_CALL_URL)
        etStunUrl.setText(PrefsManager.DEFAULT_STUN_URL)
        etTurnUrl.setText(PrefsManager.DEFAULT_TURN_URL)
        etTurnUser.setText(PrefsManager.DEFAULT_TURN_USER)
        etTurnPass.setText(PrefsManager.DEFAULT_TURN_PASS)
        etWozUrl.setText(PrefsManager.DEFAULT_WOZ_URL)
        // TextWatcher 自動觸發儲存
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onPause() {
        super.onPause()
        saveAll()   // 退出頁面時儲存
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        autoSaveHandler.removeCallbacksAndMessages(null)
    }
}

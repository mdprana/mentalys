package com.mentalys.app.ui.activities

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.CONSUMED
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.etebarian.meowbottomnavigation.MeowBottomNavigation
import com.mentalys.app.R
import com.mentalys.app.databinding.ActivityMainBinding
import com.mentalys.app.ui.article.ArticleFragment
import com.mentalys.app.ui.home.HomeFragment
import com.mentalys.app.ui.mental.history.MentalHistoryFragment
import com.mentalys.app.ui.onboarding.OnboardingActivity
import com.mentalys.app.ui.profile.ProfileFragment
import com.mentalys.app.ui.profile.ProfileLoggedOutFragment
import com.mentalys.app.ui.viewmodels.MainViewModel
import com.mentalys.app.ui.viewmodels.ViewModelFactory
import com.mentalys.app.di.Injection
import com.mentalys.app.utils.BackgroundMusicPlayer
import com.mentalys.app.utils.SettingsPreferences
import com.mentalys.app.utils.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels {
        ViewModelFactory.getInstance(this@MainActivity)
    }

    private var isLoggedIn: Boolean? = false

    // Fragment instances
    private val homeFragment = HomeFragment()
    private val articleFragment = ArticleFragment()
    private val mentalHistoryFragment = MentalHistoryFragment()
    private val profileFragment = ProfileFragment()
    private val profileLoggedOutFragment = ProfileLoggedOutFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Check if onboarding has been shown
        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("isFirstRun", true)

        if (isFirstRun) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        // Must be synchronous so the container view exists before onStart() restores fragments
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            return@setOnApplyWindowInsetsListener CONSUMED
        }

        lifecycleScope.launch {
            isLoggedIn = SettingsPreferences.getInstance(dataStore).getIsLoginSetting().first()

            val savedLanguage =
                SettingsPreferences.getInstance(dataStore).getLanguageSetting().firstOrNull()
                    ?: "English"
            com.mentalys.app.MentalysApplication.currentLanguage = savedLanguage
            getSharedPreferences(
                com.mentalys.app.MentalysApplication.PREFS_NAME, MODE_PRIVATE
            ).edit().putString(
                com.mentalys.app.MentalysApplication.KEY_LANGUAGE, savedLanguage
            ).apply()
            val locale = when (savedLanguage) {
                "Bahasa Indonesia" -> Locale("in")
                else -> Locale("en")
            }
            Locale.setDefault(locale)
            val config = Configuration(resources.configuration)
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)

            // Theme is applied synchronously in MentalysApplication.onCreate().
            // Do NOT call setDefaultNightMode here — it triggers an Activity recreation loop.

            observeBackgroundMusic()
            init()
        }

    }

    private fun observeBackgroundMusic() {
        lifecycleScope.launch {
            val preferences = SettingsPreferences.getInstance(dataStore)
            preferences.getBackgroundMusicSetting().collect { enabled ->
                if (!enabled) {
                    BackgroundMusicPlayer.stop()
                    return@collect
                }

                // If a track is already loaded (e.g. user came back from Generate Result
                // / Fusion / any activity), keep playing it instead of replacing with a
                // new random track. Music must only stop when the user toggles it off.
                if (BackgroundMusicPlayer.hasActiveTrack()) {
                    return@collect
                }

                playRandomBackgroundMusic()
            }
        }
    }

    private fun playRandomBackgroundMusic() {
        lifecycleScope.launch {
            val previewUrl = Injection.provideMusicRepository(this@MainActivity)
                .getBackgroundPreviewUrl(excludeUrl = BackgroundMusicPlayer.currentPreviewUrl())
                if (previewUrl != null) {
                    BackgroundMusicPlayer.play(this@MainActivity, previewUrl) {
                        playRandomBackgroundMusic()
                    }
                }
        }
    }

    private fun init() {
        initializeFragments()

        val bottomNavigation = findViewById<MeowBottomNavigation>(R.id.bottom_nav)
        bottomNavigation.add(MeowBottomNavigation.Model(1, R.drawable.ic_round_home))
        bottomNavigation.add(MeowBottomNavigation.Model(2, R.drawable.ic_round_book))
        bottomNavigation.add(MeowBottomNavigation.Model(3, R.drawable.ic_round_reports))
        bottomNavigation.add(MeowBottomNavigation.Model(4, R.drawable.ic_round_person))
        bottomNavigation.setOnClickMenuListener { model ->
            when (model.id) {
                1 -> showFragment(homeFragment)
                2 -> showFragment(articleFragment)
                3 -> showFragment(mentalHistoryFragment)
                4 -> {
                    if (isLoggedIn == true) showFragment(profileFragment)
                    else showFragment(profileLoggedOutFragment)
                }
            }
        }
        bottomNavigation.setOnShowListener { }
        bottomNavigation.setOnReselectListener { }
        bottomNavigation.show(1, true)
    }

    private fun initializeFragments() {
        // On recreation (e.g. after dark-mode change), the fragment manager already
        // restored the fragments from saved state — don't add duplicates.
        if (supportFragmentManager.findFragmentByTag("Home") != null) return
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.nav_host_fragment, homeFragment, "Home")
        transaction.add(R.id.nav_host_fragment, articleFragment, "Education").hide(articleFragment)
        transaction.add(R.id.nav_host_fragment, mentalHistoryFragment, "Reports").hide(mentalHistoryFragment)
        if (isLoggedIn == true) {
            transaction.add(R.id.nav_host_fragment, profileFragment, "Profile").hide(profileFragment)
        } else {
            transaction.add(R.id.nav_host_fragment, profileLoggedOutFragment, "Profile").hide(profileLoggedOutFragment)
        }
        transaction.commit()
    }

    private fun showFragment(fragment: Fragment) {
        // After recreation the field instances are new objects not in the FM;
        // resolve by tag so we always show the correct (possibly restored) fragment.
        val tag = when (fragment) {
            homeFragment               -> "Home"
            articleFragment            -> "Education"
            mentalHistoryFragment      -> "Reports"
            profileFragment,
            profileLoggedOutFragment   -> "Profile"
            else                       -> null
        }
        val target = if (tag != null) supportFragmentManager.findFragmentByTag(tag) ?: fragment else fragment
        val transaction = supportFragmentManager.beginTransaction()
        supportFragmentManager.fragments.forEach { transaction.hide(it) }
        transaction.show(target).commit()
    }

}

package com.mentalys.app.ui.activities

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.mentalys.app.R
import com.mentalys.app.databinding.ActivitySettingsBinding
import com.mentalys.app.ui.about.AboutActivity
import com.mentalys.app.ui.about.PrivacyPolicyActivity
import com.mentalys.app.ui.about.TermOfServiceActivity
import com.mentalys.app.ui.viewmodels.MainViewModel
import com.mentalys.app.ui.viewmodels.ViewModelFactory
import com.mentalys.app.utils.SettingsPreferences
import com.mentalys.app.utils.dataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settingsPreferences: SettingsPreferences
    private val viewModel: MainViewModel by viewModels {
        ViewModelFactory.getInstance(this@SettingsActivity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize settingsPreferences here
        settingsPreferences = SettingsPreferences.getInstance(dataStore, this)

        // Theme — save to DataStore (and its SharedPreferences mirror via saveThemeSetting).
        // Apply immediately here since this is intentional user action, not a cold-start load.
        binding.darkModeSwitch.setOnCheckedChangeListener { _, isDark ->
            viewModel.saveThemeSetting(isDark)
            AppCompatDelegate.setDefaultNightMode(
                if (isDark) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // Back button
        binding.backButton.setOnClickListener {
            finish()
        }

        // Notifications
        binding.notificationSwitch.setOnCheckedChangeListener { _, isEnabled ->
            viewModel.saveNotificationSetting(isEnabled)
        }

        binding.backgroundMusicSwitch.setOnCheckedChangeListener { _, isEnabled ->
            viewModel.saveBackgroundMusicSetting(isEnabled)
        }

        // Language
        binding.languageButton.setOnClickListener {
            showLanguageSelectionDialog()
        }

        // Sync switch state with DataStore on launch (read-only, no setDefaultNightMode side-effect).
        lifecycleScope.launch {
            settingsPreferences.getThemeSetting().collect { isDark ->
                settingsPreferences.saveThemeSettingSync(isDark)
                // Remove listener to avoid feedback loop, then restore it.
                binding.darkModeSwitch.setOnCheckedChangeListener(null)
                binding.darkModeSwitch.isChecked = isDark
                binding.darkModeSwitch.setOnCheckedChangeListener { _, newIsDark ->
                    viewModel.saveThemeSetting(newIsDark)
                    AppCompatDelegate.setDefaultNightMode(
                        if (newIsDark) AppCompatDelegate.MODE_NIGHT_YES
                        else AppCompatDelegate.MODE_NIGHT_NO
                    )
                }
            }
        }

        lifecycleScope.launch {
            settingsPreferences.getNotificationSetting().collect { isEnabled ->
                binding.notificationSwitch.isChecked = isEnabled
            }
        }

        lifecycleScope.launch {
            settingsPreferences.getBackgroundMusicSetting().collect { isEnabled ->
                binding.backgroundMusicSwitch.isChecked = isEnabled
            }
        }

        //About us
        binding.buttonAbout.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }

        binding.buttonPrivacyPolicy.setOnClickListener {
            val intent = Intent(this, PrivacyPolicyActivity::class.java)
            startActivity(intent)
        }

        binding.buttonTermOfService.setOnClickListener {
            val intent = Intent(this, TermOfServiceActivity::class.java)
            startActivity(intent)
        }



    }

    private fun showLanguageSelectionDialog() {
        val languages = arrayOf("English", "Bahasa Indonesia")
        lifecycleScope.launch {
            val savedLanguage = settingsPreferences.getLanguageSetting().firstOrNull() ?: "English"
            var selectedLanguage = languages.indexOf(savedLanguage).takeIf { it >= 0 } ?: 0

            AlertDialog.Builder(this@SettingsActivity)
                .setTitle(getString(R.string.select_language))
                .setSingleChoiceItems(languages, selectedLanguage) { _, which ->
                    selectedLanguage = which
                }
                .setPositiveButton(getString(R.string.ok)) { _, _ ->
                    val chosenLanguage = languages[selectedLanguage]
                    lifecycleScope.launch {
                        settingsPreferences.saveLanguageSetting(chosenLanguage)
                        applyLanguage(chosenLanguage)
                    }
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }

    private fun applyLanguage(language: String) {
        val locale = when (language) {
            "Bahasa Indonesia" -> Locale("in")
            else -> Locale("en")
        }
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        restartApp()
    }

    private fun restartApp() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

}

package com.mentalys.app.ui.onboarding

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager.widget.ViewPager
import com.mentalys.app.R
import com.mentalys.app.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private val autoSlideHandler = Handler(Looper.getMainLooper())
    private val autoSlideRunnable = object : Runnable {
        override fun run() {
            if (!::binding.isInitialized) return
            val currentPage = binding.liquidPager.currentItem
            if (currentPage < LAST_PAGE_INDEX) {
                binding.liquidPager.setCurrentItem(currentPage + 1, true)
                autoSlideHandler.postDelayed(this, AUTO_SLIDE_DELAY_MS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.apply {

            liquidPager.adapter = OnboardingAdapter(supportFragmentManager)
            liquidPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

                override fun onPageScrollStateChanged(state: Int) {}

                override fun onPageSelected(position: Int) {
                    when (position) {
                        0 -> liquidPager.setButtonDrawable(R.drawable.ic_button_onboarding1)
                        1 -> liquidPager.setButtonDrawable(R.drawable.ic_button_onboarding2)
                        2 -> liquidPager.setButtonDrawable(R.drawable.ic_button_onboarding1)
                        3 -> liquidPager.setButtonDrawable(R.drawable.ic_button_onboarding2)
                        else -> ContextCompat.getColor(this@OnboardingActivity, R.color.primary)
                    }
                }

                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                }

            })

        }
        startAutoSlide()

    }

    override fun onResume() {
        super.onResume()
        startAutoSlide()
    }

    override fun onPause() {
        stopAutoSlide()
        super.onPause()
    }

    override fun onDestroy() {
        stopAutoSlide()
        super.onDestroy()
    }

    private fun startAutoSlide() {
        if (!::binding.isInitialized || binding.liquidPager.currentItem >= LAST_PAGE_INDEX) return
        autoSlideHandler.removeCallbacks(autoSlideRunnable)
        autoSlideHandler.postDelayed(autoSlideRunnable, AUTO_SLIDE_DELAY_MS)
    }

    private fun stopAutoSlide() {
        autoSlideHandler.removeCallbacks(autoSlideRunnable)
    }

    companion object {
        private const val AUTO_SLIDE_DELAY_MS = 15000L
        private const val LAST_PAGE_INDEX = 2
    }
}

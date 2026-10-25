package com.mentalys.app.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.mentalys.app.R
import com.mentalys.app.databinding.FragmentHomeBinding
import com.mentalys.app.ui.viewmodels.ViewModelFactory
import com.mentalys.app.utils.Resource
import com.mentalys.app.utils.showToast
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.mentalys.app.ui.chat.AiChatActivity
import com.mentalys.app.ui.clinic.ClinicActivity
import com.mentalys.app.ui.specialist.SpecialistHomeAdapter
import com.mentalys.app.ui.dailytips.DailyTips
import com.mentalys.app.ui.dailytips.DailyTipsAdapter
import com.mentalys.app.ui.mental.test.MentalTestActivity
import com.mentalys.app.ui.music.MusicActivity
import com.mentalys.app.ui.specialist.SpecialistActivity
import com.mentalys.app.ui.specialist.SpecialistViewModel
import com.mentalys.app.utils.SettingsPreferences
import com.mentalys.app.utils.dataStore
import com.mentalys.app.utils.openAndroidXBrowser
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val specialistViewModel: SpecialistViewModel by viewModels {
        ViewModelFactory.getInstance(requireContext())
    }

    private lateinit var specialistAdapter: SpecialistHomeAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val carouselItems = listOf(
            DailyTips(
                title = getString(R.string.mindfulness_tip_title),
                description = getString(R.string.mindfulness_tip_description),
                imageResource = R.drawable.icon_calm
            ),
            DailyTips(
                title = getString(R.string.positive_thinking_title),
                description = getString(R.string.positive_thinking_description),
                imageResource = R.drawable.icon_happy
            ),
            DailyTips(
                title = getString(R.string.stress_relief_title),
                description = getString(R.string.stress_relief_description),
                imageResource = R.drawable.icon_nature
            ),
            DailyTips(
                title = getString(R.string.gratitude_exercise_title),
                description = getString(R.string.gratitude_exercise_description),
                imageResource = R.drawable.icon_book
            ),
            DailyTips(
                title = getString(R.string.physical_activity_tip_title),
                description = getString(R.string.physical_activity_tip_description),
                imageResource = R.drawable.icon_exercise
            ),
            DailyTips(
                title = getString(R.string.sleep_hygiene_title),
                description = getString(R.string.sleep_hygiene_description),
                imageResource = R.drawable.icon_bed
            )
        )

        setupSpecialist()
        setupDailyTipsRecyclerView(carouselItems)

        // Set greeting and name
        observeGreeting()

        // Set banner images
        Glide.with(requireActivity()).load(R.drawable.image_banner_music).into(binding.imageView)
        Glide.with(requireActivity()).load(R.drawable.image_banner_meditation)
            .into(binding.mainBannerImageView)

        // Setup click listeners and other initializations
        setupClickListeners()
        // Note: setupSpecialist() is already called above (line 96) - do NOT call it again here

    }

    private fun observeGreeting() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                SettingsPreferences.getInstance(requireContext().dataStore)
                    .getFullNameSetting()
                    .collect { fullName ->
                        val firstName = fullName
                            .takeIf { it.isNotBlank() && !it.equals("Guest", ignoreCase = true) }
                            ?.split(" ")
                            ?.firstOrNull()
                            ?: getString(R.string.guest_user)

                        binding.greetingTextView.text = getGreetingMessage()
                        binding.timeIcon.setImageResource(getIconResource())
                        binding.nameTextView.text = getString(R.string.greeting_user, firstName)
                    }
            }
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            screeningLayout.setOnClickListener {
                startActivity(Intent(requireContext(), MentalTestActivity::class.java))
            }

            clinicLayout.setOnClickListener {
                startActivity(Intent(requireContext(), ClinicActivity::class.java))
            }

            aiChatLayout.setOnClickListener {
                startActivity(Intent(requireContext(), AiChatActivity::class.java).apply {
                    putExtra(AiChatActivity.EXTRA_INCLUDE_HISTORY, true)
                })
            }

            specialistViewAllLabel.setOnClickListener {
                startActivity(Intent(requireContext(), SpecialistActivity::class.java))
            }

            musicBanner.setOnClickListener {
                startActivity(Intent(requireContext(), MusicActivity::class.java))
            }

        }
    }

    private fun setupDailyTipsRecyclerView(carouselItems: List<DailyTips>) {
        // Set up RecyclerView with horizontal layout manager
        binding.dailyTipsRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.dailyTipsRecyclerView.adapter = DailyTipsAdapter(carouselItems)
        // SnapHelper to enable snapping
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.dailyTipsRecyclerView)
        // Add indicator dots
        addIndicatorDots(carouselItems.size)
        // Ensure the initial indicator is set after layout is ready
        binding.dailyTipsRecyclerView.post {
            updateIndicator(binding.dailyTipsRecyclerView)
        }
        // Listener for scroll state changes
        binding.dailyTipsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                updateIndicator(recyclerView)
            }
        })
    }

    private fun addIndicatorDots(count: Int) {
        for (i in 0 until count) {
            val dot = ImageView(requireContext())
            dot.setImageResource(R.drawable.indicator_dot) // Create a drawable for your indicator dot
            val params = LinearLayout.LayoutParams(16, 16)
            params.setMargins(8, 0, 8, 0)
            dot.layoutParams = params
            binding.dailyTipsIndicator.addView(dot)
        }
    }

    private fun updateIndicator(recyclerView: RecyclerView) {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val snapHelper = LinearSnapHelper() // Ensure you are using the same SnapHelper
        val snappedView = snapHelper.findSnapView(layoutManager)
        val position =
            snappedView?.let { layoutManager.getPosition(it) } ?: RecyclerView.NO_POSITION

        // Update the indicator's selected dot
        for (i in 0 until binding.dailyTipsIndicator.childCount) {
            val dot = binding.dailyTipsIndicator.getChildAt(i) as ImageView
            dot.setImageResource(if (i == position) R.drawable.indicator_dot_selected else R.drawable.indicator_dot)
        }
    }

    private fun setupSpecialist() {

        specialistAdapter = SpecialistHomeAdapter()

        // Trigger fetching of specialists
        specialistViewModel.getSpecialists()

        // Observe specialists LiveData
        specialistViewModel.specialists.observe(requireActivity()) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    specialistAdapter.setLoadingState(true)
                }

                is Resource.Success -> {
                    specialistAdapter.setLoadingState(false)
                    specialistAdapter.submitList(resource.data)
                }

                is Resource.Error -> {
                    showToast(requireContext(), resource.error)
                }
            }
        }

        binding.specialistRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = specialistAdapter
            // Smooth snap between specialist cards
            if (onFlingListener == null) {
                LinearSnapHelper().attachToRecyclerView(this)
            }
        }

    }

    private fun getGreetingMessage(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        return when (hour) {
            in 5..11 -> getString(R.string.good_morning)
            in 12..16 -> getString(R.string.good_afternoon)
            in 17..20 -> getString(R.string.good_evening)
            else -> getString(R.string.good_night)
        }
    }

    private fun getIconResource(): Int {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        return when (hour) {
            in 5..20 -> R.drawable.ic_outline_sun // Replace with your sun icon resource
            else -> R.drawable.ic_outline_dark // Replace with your moon icon resource
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}

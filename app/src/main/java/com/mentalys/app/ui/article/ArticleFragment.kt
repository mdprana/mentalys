package com.mentalys.app.ui.article

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.carousel.CarouselSnapHelper
import com.mentalys.app.R
import com.mentalys.app.databinding.FragmentArticleBinding
import com.mentalys.app.ui.viewmodels.ViewModelFactory
import com.mentalys.app.utils.Resource
import com.mentalys.app.utils.showToast

class ArticleFragment : Fragment() {

    private var _binding: FragmentArticleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ArticleViewModel by viewModels {
        ViewModelFactory.getInstance(requireContext())
    }

    private lateinit var articleAdapter: ArticleAdapter
    private lateinit var foodAdapter: FoodAdapter
    private val carouselHandler = Handler(Looper.getMainLooper())
    private var carouselPosition = 0
    private var carouselItemCount = 0
    private val carouselAutoSwipe = object : Runnable {
        override fun run() {
            if (_binding == null || isHidden || carouselItemCount <= 1) return
            carouselPosition = (carouselPosition + 1) % carouselItemCount
            binding.carouselRecyclerView.smoothScrollToPosition(carouselPosition)
            carouselHandler.postDelayed(this, CAROUSEL_AUTO_SWIPE_DELAY_MS)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentArticleBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val carouselItems = listOf(
            Item(R.drawable.image_carousel_1),
            Item(R.drawable.image_carousel_2),
            Item(R.drawable.image_carousel_3),
            Item(R.drawable.image_carousel_4),
            Item(R.drawable.image_carousel_5),
        )
        carouselItemCount = carouselItems.size

        // Set up the carousel adapter
        val carouselAdapter = CarouselAdapter(carouselItems)
        val snapHelper = CarouselSnapHelper()
        snapHelper.attachToRecyclerView(binding.carouselRecyclerView)
        binding.carouselRecyclerView.adapter = carouselAdapter
        startCarouselAutoSwipe()

        // ============================== FOODS ============================== //
        foodAdapter = FoodAdapter()
        foodAdapter.setLoadingState(true)

        // Trigger fetching of foods
        viewModel.get4Foods()

        // Observe foods LiveData
        viewModel.foods.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    foodAdapter.setLoadingState(true)
                }

                is Resource.Success -> {
                    foodAdapter.setLoadingState(false)
                    foodAdapter.submitList(resource.data)
                }

                is Resource.Error -> {
                    showToast(requireContext(), resource.error)
                }
            }
        }

        binding.foodRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = foodAdapter
        }

        binding.foodViewAll.setOnClickListener {
            val intent = Intent(requireContext(), AllFoodsActivity::class.java)
            startActivity(intent)
        }

        // ============================== ARTICLES ============================== //
        articleAdapter = ArticleAdapter()
        articleAdapter.setLoadingState(true)

        // Trigger fetching of articles
        viewModel.get3ListArticle()

        // Observe articles LiveData
        viewModel.articles.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    articleAdapter.setLoadingState(true)
                }

                is Resource.Success -> {
                    articleAdapter.setLoadingState(false)
                    articleAdapter.submitList(resource.data)
                }

                is Resource.Error -> {
                    showToast(requireContext(), resource.error)
                }
            }
        }

        binding.articleRecyclerView.apply {
            layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
            adapter = articleAdapter
        }

        binding.articleViewAll.setOnClickListener {
            val intent = Intent(requireContext(), AllArticleActivity::class.java)
            startActivity(intent)
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopCarouselAutoSwipe()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        startCarouselAutoSwipe()
    }

    override fun onPause() {
        stopCarouselAutoSwipe()
        super.onPause()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) stopCarouselAutoSwipe() else startCarouselAutoSwipe()
    }

    private fun startCarouselAutoSwipe() {
        if (_binding == null || isHidden || carouselItemCount <= 1) return
        carouselHandler.removeCallbacks(carouselAutoSwipe)
        carouselHandler.postDelayed(carouselAutoSwipe, CAROUSEL_AUTO_SWIPE_DELAY_MS)
    }

    private fun stopCarouselAutoSwipe() {
        carouselHandler.removeCallbacks(carouselAutoSwipe)
    }

    companion object {
        private const val CAROUSEL_AUTO_SWIPE_DELAY_MS = 3500L
    }

}

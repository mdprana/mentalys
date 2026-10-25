package com.mentalys.app.ui.mental.test.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.mentalys.app.R
import com.mentalys.app.databinding.FragmentPhq9QuizBinding
import com.mentalys.app.ml.fusion.RiskMapper
import com.mentalys.app.utils.showToast

class PHQ9QuizFragment : Fragment() {

    private var _binding: FragmentPhq9QuizBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PHQ9QuizViewModel by viewModels()

    private val radioGroups: List<RadioGroup> by lazy {
        listOf(
            binding.rgQ1, binding.rgQ2, binding.rgQ3,
            binding.rgQ4, binding.rgQ5, binding.rgQ6,
            binding.rgQ7, binding.rgQ8, binding.rgQ9
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhq9QuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        radioGroups.forEachIndexed { index, group ->
            group.setOnCheckedChangeListener { _, checkedId ->
                val score = when (checkedId) {
                    group.getChildAt(0).id -> 0
                    group.getChildAt(1).id -> 1
                    group.getChildAt(2).id -> 2
                    group.getChildAt(3).id -> 3
                    else -> -1
                }
                viewModel.setAnswer(index, score)
            }
        }

        binding.btnSubmitPhq9.setOnClickListener { onSubmit() }
    }

    private fun onSubmit() {
        if (!viewModel.allAnswered()) {
            showToast(requireContext(), getString(R.string.phq9_unanswered))
            return
        }
        val rawScore = viewModel.totalScore()
        val phqNorm  = RiskMapper.normalizePHQ(rawScore)
        val category = RiskMapper.phqCategory(rawScore)
        val q9       = viewModel.q9Answer()

        (activity as? PHQ9ResultListener)?.onPHQ9Complete(rawScore, phqNorm, category, q9)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface PHQ9ResultListener {
        fun onPHQ9Complete(rawScore: Int, normScore: Float, category: String, q9Answer: Int)
    }
}

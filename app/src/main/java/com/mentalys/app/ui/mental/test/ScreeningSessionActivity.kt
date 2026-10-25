package com.mentalys.app.ui.mental.test

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mentalys.app.R
import com.mentalys.app.databinding.ActivityScreeningSessionBinding
import com.mentalys.app.ui.mental.test.quiz.PHQ9QuizFragment

class ScreeningSessionActivity : AppCompatActivity(), PHQ9QuizFragment.PHQ9ResultListener {

    companion object {
        const val RESULT_PHQ_RAW      = "result_phq_raw"
        const val RESULT_PHQ_NORM     = "result_phq_norm"
        const val RESULT_PHQ_CATEGORY = "result_phq_category"
        const val RESULT_Q9_ANSWER    = "result_q9_answer"
    }

    private lateinit var binding: ActivityScreeningSessionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityScreeningSessionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.backButton.setOnClickListener { finish() }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PHQ9QuizFragment())
                .commit()
        }
    }

    override fun onPHQ9Complete(rawScore: Int, normScore: Float, category: String, q9Answer: Int) {
        val data = Intent()
            .putExtra(RESULT_PHQ_RAW, rawScore)
            .putExtra(RESULT_PHQ_NORM, normScore)
            .putExtra(RESULT_PHQ_CATEGORY, category)
            .putExtra(RESULT_Q9_ANSWER, q9Answer)
        setResult(RESULT_OK, data)
        finish()
    }
}

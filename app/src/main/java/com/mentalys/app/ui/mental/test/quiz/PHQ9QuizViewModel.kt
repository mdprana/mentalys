package com.mentalys.app.ui.mental.test.quiz

import androidx.lifecycle.ViewModel

class PHQ9QuizViewModel : ViewModel() {
    val answers = IntArray(9) { -1 }

    fun setAnswer(questionIndex: Int, value: Int) {
        answers[questionIndex] = value
    }

    fun allAnswered(): Boolean = answers.all { it >= 0 }

    fun totalScore(): Int = answers.filter { it >= 0 }.sum()

    fun q9Answer(): Int = answers[8].coerceAtLeast(0)
}

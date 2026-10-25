package com.mentalys.app.ui.mental.test

import androidx.lifecycle.ViewModel

class ScreeningSessionViewModel : ViewModel() {

    // PHQ-9 — null means not yet completed
    var phqRawScore: Int? = null
    var phqNorm: Float? = null
    var phqCategory: String? = null
    var q9Answer: Int = 0

    // Face — null means not yet completed
    var faceRiskScore: Float? = null

    // Voice — null means not yet completed
    var audioRiskScore: Float? = null

    fun isPhqDone() = phqRawScore != null
    fun isFaceDone() = faceRiskScore != null
    fun isVoiceDone() = audioRiskScore != null
    fun isAnyDone() = isPhqDone() || isFaceDone() || isVoiceDone()

    fun modalitiesUsed(): String = buildList {
        if (isPhqDone()) add("phq")
        if (isFaceDone()) add("face")
        if (isVoiceDone()) add("voice")
    }.joinToString(",")
}

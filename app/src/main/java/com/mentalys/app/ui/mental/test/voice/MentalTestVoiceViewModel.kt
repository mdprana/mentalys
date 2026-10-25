package com.mentalys.app.ui.mental.test.voice

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mentalys.app.ml.fusion.RiskMapper
import com.mentalys.app.ml.voice.AudioPreprocessor
import com.mentalys.app.ml.voice.VoiceInterpreter
import com.mentalys.app.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MentalTestVoiceViewModel : ViewModel() {

    private val _audioFileUri = MutableLiveData<Uri?>()
    val audioFileUri: LiveData<Uri?> = _audioFileUri

    private val _isRecording = MutableLiveData(false)
    val isRecording: LiveData<Boolean> = _isRecording

    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> = _isPlaying

    private val _inferenceState = MutableLiveData<Resource<Float>>()
    val inferenceState: LiveData<Resource<Float>> = _inferenceState

    private var voiceInterpreter: VoiceInterpreter? = null

    fun setAudioFileUri(uri: Uri?) { _audioFileUri.value = uri }
    fun setRecordingStatus(status: Boolean) { _isRecording.value = status }
    fun setPlayingStatus(status: Boolean) { _isPlaying.value = status }

    fun runLocalInference(context: Context, wavFile: File) {
        _inferenceState.value = Resource.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val interpreter = voiceInterpreter
                    ?: VoiceInterpreter(context).also { voiceInterpreter = it }

                // Sample multiple overlapping windows and average the risk score.
                // A confident classifier on a single 3 s window saturates to 0.00 / 1.00;
                // averaging 5 windows produces a continuous, calibrated score.
                val windows = AudioPreprocessor.processWindows(context, wavFile, maxWindows = 5)
                val perWindowRisk = mutableListOf<Float>()
                val avgProbs = FloatArray(VoiceInterpreter.LABELS.size)
                for (features in windows) {
                    val probs = interpreter.run(features)
                    perWindowRisk += RiskMapper.computeAudioRisk(probs)
                    for (i in avgProbs.indices) avgProbs[i] += probs.getOrElse(i) { 0f }
                }
                if (windows.isNotEmpty()) {
                    for (i in avgProbs.indices) avgProbs[i] /= windows.size
                }

                val averagedRisk = if (perWindowRisk.isNotEmpty())
                    perWindowRisk.average().toFloat() else 0f

                Log.d(
                    "VoiceTest",
                    "windows=${windows.size} per_window_risk=$perWindowRisk averaged=$averagedRisk " +
                        "avgProbs=${VoiceInterpreter.LABELS.zip(avgProbs.toList()).joinToString { "${it.first}=${"%.3f".format(it.second)}" }}"
                )
                _inferenceState.postValue(Resource.Success(averagedRisk))
            } catch (e: Exception) {
                _inferenceState.postValue(Resource.Error("Voice inference failed: ${e.message}"))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceInterpreter?.close()
    }
}

package com.mentalys.app.ui.mental.test.voice

import com.mentalys.app.utils.AudioRecorder
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mentalys.app.R
import com.mentalys.app.databinding.ActivityMentalTestVoiceBinding
import com.mentalys.app.utils.AudioUtils
import com.mentalys.app.utils.BackgroundMusicPlayer
import com.mentalys.app.utils.Resource
import com.mentalys.app.utils.showToast
import java.io.File

class MentalTestVoiceActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_AUDIO_RISK = "extra_audio_risk"
    }

    private lateinit var binding: ActivityMentalTestVoiceBinding
    private val viewModel: MentalTestVoiceViewModel by viewModels()

    private var audioRecorder: AudioRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioFile: File? = null
    private var audioUri: Uri? = null

    private var isRecording = false
    private var isPlaying = false
    private var recordingDuration = 0L
    private var recordingTimer: CountDownTimer? = null

    private val minRecordingDuration = 3000L
    private val maxRecordingDuration = 30000L
    private var recordingStartTime: Long = 0
    private var isBackButtonDisabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMentalTestVoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        requestPermissions()
        observeViewModel()
        setupListeners()
        updateUI()
        setupInferenceObserver()
        resetTimerDisplay()
    }

    @SuppressLint("SetTextI18n")
    private fun resetTimerDisplay() {
        binding.chronometer.text = "00:30"
    }

    private fun requestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(Manifest.permission.RECORD_AUDIO)
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (!granted.all { it.value }) {
                showToast(this, getString(R.string.need_permission_to_continure))
                finish()
            }
        }.launch(permissions)
    }

    private fun observeViewModel() {
        viewModel.audioFileUri.observe(this) { uri ->
            audioUri = uri
            audioFile = uri?.path?.let { File(it) }
            updateUI()
        }
        viewModel.isRecording.observe(this) { isRecording = it; updateUI() }
        viewModel.isPlaying.observe(this) { isPlaying = it; updateUI() }
    }

    private fun setupListeners() {
        binding.backButton.setOnClickListener { finish() }
        binding.buttonRecordAudio.setOnClickListener {
            when {
                isRecording -> stopRecording()
                isPlaying -> stopPlayback()
                audioFile != null || audioUri != null -> playAudio()
                else -> startRecording()
            }
        }
        binding.buttonResetAudio.setOnClickListener { resetAudio() }
        binding.buttonSendAudio.setOnClickListener { submitForInference() }
    }

    private fun updateUI() {
        binding.buttonResetAudio.isEnabled = audioFile != null || audioUri != null
        binding.buttonSendAudio.isEnabled = audioFile != null || audioUri != null
        binding.buttonRecordAudio.setImageResource(
            when {
                isRecording -> R.drawable.ic_stop
                isPlaying -> R.drawable.ic_stop
                audioFile != null || audioUri != null -> R.drawable.ic_play
                else -> R.drawable.ic_outline_voice
            }
        )
    }

    private fun startRecording() {
        val timestamp = System.currentTimeMillis()
        audioFile = AudioUtils.createAudioFile(this, timestamp) ?: return
        try {
            BackgroundMusicPlayer.pauseForRecording()
            audioRecorder = AudioRecorder(this, audioFile!!.absolutePath)
            audioRecorder?.startRecording()
            startRecordingTimer()
            recordingStartTime = SystemClock.elapsedRealtime()
            viewModel.setAudioFileUri(Uri.fromFile(audioFile))
            viewModel.setRecordingStatus(true)
        } catch (e: Exception) {
            BackgroundMusicPlayer.resumeAfterRecording()
            Log.e("VoiceTest", "Error starting recording: ${e.message}", e)
        }
    }

    private fun startRecordingTimer() {
        recordingTimer?.cancel()
        recordingTimer = object : CountDownTimer(maxRecordingDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.chronometer.text = String.format("00:%02d", millisUntilFinished / 1000)
            }
            override fun onFinish() { stopRecording() }
        }.start()
    }

    private fun stopRecording() {
        try {
            val elapsed = SystemClock.elapsedRealtime() - recordingStartTime
            if (elapsed < minRecordingDuration) {
                showToast(this, getString(R.string.record_min_15s))
                return
            }
            audioRecorder?.stopRecording()
            audioRecorder = null
            recordingTimer?.cancel()
            recordingDuration = elapsed
            binding.chronometer.text = String.format("00:%02d", elapsed / 1000)
            viewModel.setRecordingStatus(false)
            BackgroundMusicPlayer.resumeAfterRecording()
            showToast(this, getString(R.string.finish_recordin))
        } catch (e: Exception) {
            BackgroundMusicPlayer.resumeAfterRecording()
            Log.e("VoiceTest", "Error stopping recording: ${e.message}", e)
        }
    }

    private fun playAudio() {
        try {
            if (audioFile == null || !audioFile!!.exists()) return
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile!!.absolutePath)
                prepare()
                start()
                setOnCompletionListener { stopPlayback() }
            }
            recordingTimer = object : CountDownTimer(recordingDuration, 1000) {
                override fun onTick(ms: Long) { binding.chronometer.text = String.format("00:%02d", ms / 1000) }
                override fun onFinish() { stopPlayback() }
            }.start()
            viewModel.setPlayingStatus(true)
        } catch (e: Exception) {
            Log.e("VoiceTest", "Error playing audio: ${e.message}", e)
        }
    }

    private fun stopPlayback() {
        try {
            mediaPlayer?.apply { stop(); release() }
            mediaPlayer = null
            recordingTimer?.cancel()
            binding.chronometer.text = String.format("00:%02d", recordingDuration / 1000)
            viewModel.setPlayingStatus(false)
        } catch (e: Exception) {
            Log.e("VoiceTest", "Error stopping playback: ${e.message}", e)
        }
    }

    private fun resetAudio() {
        try {
            audioFile?.let { AudioUtils.deleteRecording(it) }
            audioFile = null
            viewModel.setAudioFileUri(null)
            resetTimerDisplay()
            recordingTimer?.cancel()
            viewModel.setRecordingStatus(false)
            viewModel.setPlayingStatus(false)
            showToast(this, "Audio reset")
        } catch (e: Exception) {
            Log.e("VoiceTest", "Error resetting audio: ${e.message}", e)
        }
    }

    private fun submitForInference() {
        val file = audioFile
        if (file == null || !file.exists()) {
            showToast(this, getString(R.string.alert_empty_audio))
            return
        }
        showLoading(true)
        viewModel.runLocalInference(this, file)
    }

    private fun setupInferenceObserver() {
        viewModel.inferenceState.observe(this) { result ->
            when (result) {
                is Resource.Loading -> showLoading(true)
                is Resource.Success -> {
                    val audioRisk = result.data
                    Log.d("VoiceTest", "audio_risk_score=$audioRisk")
                    setResult(RESULT_OK, Intent().putExtra(EXTRA_AUDIO_RISK, audioRisk))
                    finish()
                }
                is Resource.Error -> {
                    showLoading(false)
                    Log.e("VoiceTest", result.error)
                    showToast(this, result.error)
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        val loadingScreen = findViewById<View>(R.id.voice_loading_layout)
        loadingScreen.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.appBar.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.layoutVoiceTest.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.bottomIcons.visibility = if (isLoading) View.GONE else View.VISIBLE
        isBackButtonDisabled = isLoading
    }

    override fun onBackPressed() {
        if (isBackButtonDisabled) {
            showToast(this, getString(R.string.be_patient_please_wait))
            return
        }
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecorder?.stopRecording()
        BackgroundMusicPlayer.resumeAfterRecording()
        mediaPlayer?.release()
        recordingTimer?.cancel()
    }
}

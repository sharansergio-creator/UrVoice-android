package com.urvoice.app.ui.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.urvoice.app.network.UrVoiceApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

enum class VoiceSetupStep {
    INTRO, ENVIRONMENT_CHECK, RECORDING, PROCESSING, SUCCESS, ERROR
}

data class VoiceSetupState(
    val step: VoiceSetupStep = VoiceSetupStep.INTRO,
    val isRecording: Boolean = false,
    val recordingSeconds: Int = 0,
    val audioLevel: Float = 0f,
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val hasExistingVoice: Boolean = false,
    val existingVoiceId: String? = null
)

class AiVoiceSetupViewModel : ViewModel() {
    private val _state = MutableStateFlow(VoiceSetupState())
    val state: StateFlow<VoiceSetupState> = _state

    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var recordingJob: kotlinx.coroutines.Job? = null

    val VOICE_SCRIPT = "Hello, thank you for calling. I'm here to help you with bookings, questions, and anything you need. Whether you're planning a stay, looking for activities, or just want to know more about us — I've got you covered. Please feel free to ask me anything at all. We look forward to welcoming you very soon."

    val MIN_RECORDING_SECONDS = 20
    val MAX_RECORDING_SECONDS = 45

    init {
        checkExistingVoice()
    }

    private fun checkExistingVoice() {
        viewModelScope.launch {
            try {
                val uid = Firebase.auth.currentUser?.uid ?: return@launch
                val doc = Firebase.firestore.collection("users").document(uid).get().await()
                val voiceId = doc.getString("elevenLabsVoiceId")
                _state.value = _state.value.copy(
                    hasExistingVoice = voiceId != null,
                    existingVoiceId = voiceId
                )
            } catch (e: Exception) {
                android.util.Log.e("AiVoiceVM", "checkExistingVoice error: ${e.message}")
            }
        }
    }

    fun goToEnvironmentCheck() {
        _state.value = _state.value.copy(step = VoiceSetupStep.ENVIRONMENT_CHECK)
    }

    fun startRecording(context: Context) {
        viewModelScope.launch {
            try {
                val file = File(context.cacheDir, "voice_sample_${System.currentTimeMillis()}.m4a")
                recordingFile = file

                mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioSamplingRate(44100)
                    setAudioEncodingBitRate(128000)
                    setOutputFile(file.absolutePath)
                    prepare()
                    start()
                }

                _state.value = _state.value.copy(
                    step = VoiceSetupStep.RECORDING,
                    isRecording = true,
                    recordingSeconds = 0,
                    audioLevel = 0f
                )

                // Timer + audio level monitor
                recordingJob = viewModelScope.launch {
                    var seconds = 0
                    while (_state.value.isRecording) {
                        kotlinx.coroutines.delay(1000)
                        seconds++
                        val maxAmplitude = mediaRecorder?.maxAmplitude ?: 0
                        val level = (maxAmplitude / 32767f).coerceIn(0f, 1f)
                        _state.value = _state.value.copy(
                            recordingSeconds = seconds,
                            audioLevel = level
                        )
                        // Auto-stop at max duration
                        if (seconds >= MAX_RECORDING_SECONDS) {
                            stopRecordingAndAnalyze()
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AiVoiceVM", "startRecording error: ${e.message}")
                _state.value = _state.value.copy(
                    step = VoiceSetupStep.ERROR,
                    errorMessage = "Could not access microphone. Please check permissions."
                )
            }
        }
    }

    fun stopRecordingAndAnalyze() {
        viewModelScope.launch {
            try {
                recordingJob?.cancel()
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null

                val seconds = _state.value.recordingSeconds
                _state.value = _state.value.copy(isRecording = false)

                // Quality checks
                if (seconds < MIN_RECORDING_SECONDS) {
                    _state.value = _state.value.copy(
                        step = VoiceSetupStep.ERROR,
                        errorMessage = "Recording too short. Please read the full script — at least ${MIN_RECORDING_SECONDS} seconds needed.",
                        retryCount = _state.value.retryCount + 1
                    )
                    return@launch
                }

                val file = recordingFile
                if (file == null || !file.exists() || file.length() < 10000) {
                    _state.value = _state.value.copy(
                        step = VoiceSetupStep.ERROR,
                        errorMessage = "Recording seems too quiet. Please move closer to your phone and try again.",
                        retryCount = _state.value.retryCount + 1
                    )
                    return@launch
                }

                // All checks passed — upload
                uploadVoiceSample(file)

            } catch (e: Exception) {
                android.util.Log.e("AiVoiceVM", "stopRecording error: ${e.message}")
                _state.value = _state.value.copy(
                    step = VoiceSetupStep.ERROR,
                    errorMessage = "Something went wrong. Please try again."
                )
            }
        }
    }

    private suspend fun uploadVoiceSample(file: File) {
        _state.value = _state.value.copy(step = VoiceSetupStep.PROCESSING)
        try {
            val uid = Firebase.auth.currentUser?.uid ?: return
            val requestFile = file.asRequestBody("audio/m4a".toMediaTypeOrNull())
            val audioPart = MultipartBody.Part.createFormData("audio", file.name, requestFile)
            val userIdPart = uid.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = UrVoiceApi.service.cloneVoice(userIdPart, audioPart)
            if (response.isSuccessful) {
                val body = response.body()
                val voiceId = body?.get("voiceId") as? String
                if (voiceId != null) {
                    _state.value = _state.value.copy(
                        step = VoiceSetupStep.SUCCESS,
                        hasExistingVoice = true,
                        existingVoiceId = voiceId
                    )
                } else {
                    _state.value = _state.value.copy(
                        step = VoiceSetupStep.ERROR,
                        errorMessage = "Voice cloning failed. Please try again in a quieter environment.",
                        retryCount = _state.value.retryCount + 1
                    )
                }
            } else {
                _state.value = _state.value.copy(
                    step = VoiceSetupStep.ERROR,
                    errorMessage = "Upload failed. Check your internet connection and try again.",
                    retryCount = _state.value.retryCount + 1
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("AiVoiceVM", "uploadVoiceSample error: ${e.message}")
            _state.value = _state.value.copy(
                step = VoiceSetupStep.ERROR,
                errorMessage = "Connection error. Please try again.",
                retryCount = _state.value.retryCount + 1
            )
        }
    }

    fun deleteVoice() {
        viewModelScope.launch {
            try {
                val uid = Firebase.auth.currentUser?.uid ?: return@launch
                val voiceId = _state.value.existingVoiceId ?: return@launch
                UrVoiceApi.service.deleteVoice(mapOf("user_id" to uid, "voice_id" to voiceId))
                _state.value = _state.value.copy(
                    hasExistingVoice = false,
                    existingVoiceId = null,
                    step = VoiceSetupStep.INTRO
                )
            } catch (e: Exception) {
                android.util.Log.e("AiVoiceVM", "deleteVoice error: ${e.message}")
            }
        }
    }

    fun retry() {
        _state.value = _state.value.copy(
            step = VoiceSetupStep.RECORDING,
            errorMessage = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        mediaRecorder?.release()
        mediaRecorder = null
        recordingJob?.cancel()
    }
}

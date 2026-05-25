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
    LANGUAGE_SELECT, SCRIPT_SELECT, ENVIRONMENT_CHECK, RECORDING, PROCESSING, SUCCESS, ERROR
}

data class LanguageVoiceStatus(
    val code: String,
    val displayName: String,
    val flag: String,
    val voiceId: String? = null
)

data class VoiceSetupState(
    val step: VoiceSetupStep = VoiceSetupStep.LANGUAGE_SELECT,
    val languages: List<LanguageVoiceStatus> = emptyList(),
    val selectedLanguage: LanguageVoiceStatus? = null,
    val selectedScriptOption: String = "original", // "original" or "phonetic"
    val isRecording: Boolean = false,
    val recordingSeconds: Int = 0,
    val audioLevel: Float = 0f,
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val successMessage: String? = null
)

class AiVoiceSetupViewModel : ViewModel() {
    private val _state = MutableStateFlow(VoiceSetupState())
    val state: StateFlow<VoiceSetupState> = _state

    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var recordingJob: kotlinx.coroutines.Job? = null

    val MIN_RECORDING_SECONDS = 35
    val MAX_RECORDING_SECONDS = 60

    // Scripts for each language — original and phonetic versions
    val scripts = mapOf(
        "en" to mapOf(
            "original" to "Hello, and thank you so much for calling. I'm really happy to assist you today. Whether you're planning a short getaway or a longer vacation, we have something special waiting for you here. Our resort offers beautiful rooms, exciting outdoor activities, delicious food, and warm hospitality that you'll remember for a long time. We have options for families, couples, and corporate groups as well. You can enjoy swimming, outdoor sports, nature walks, and much more during your stay with us. If you're looking for a peaceful escape from the busy city life, this is exactly the right place for you. Please feel free to ask me anything about our availability, pricing, special packages, or any other details. I'm here to help make your experience as smooth and enjoyable as possible. We truly look forward to welcoming you very soon.",
            "phonetic" to "Hello, and thank you so much for calling. I'm really happy to assist you today. Whether you're planning a short getaway or a longer vacation, we have something special waiting for you here. Our resort offers beautiful rooms, exciting outdoor activities, delicious food, and warm hospitality that you'll remember for a long time. We have options for families, couples, and corporate groups as well. You can enjoy swimming, outdoor sports, nature walks, and much more during your stay with us. If you're looking for a peaceful escape from the busy city life, this is exactly the right place for you. Please feel free to ask me anything about our availability, pricing, special packages, or any other details. I'm here to help make your experience as smooth and enjoyable as possible. We truly look forward to welcoming you very soon."
        ),
        "kn" to mapOf(
            "original" to "ನಮಸ್ಕಾರ, ನಿಮ್ಮ ಕರೆಗೆ ತುಂಬಾ ಧನ್ಯವಾದಗಳು. ನಾನು ನಿಮಗೆ ಸಹಾಯ ಮಾಡಲು ಸಂತೋಷಪಡುತ್ತೇನೆ. ನೀವು ರೂಮ್ ಬುಕಿಂಗ್, ಚಟುವಟಿಕೆಗಳು, ಅಥವಾ ಬೆಲೆ ಮಾಹಿತಿ ಬೇಕಾದರೆ ಕೇಳಬಹುದು. ನಾವು ಕೂರ್ಗ್‌ನಲ್ಲಿ ಇದ್ದೇವೆ ಮತ್ತು ನಿಮ್ಮ ಸ್ವಾಗತಕ್ಕೆ ಸದಾ ಸಿದ್ಧರಿದ್ದೇವೆ. ದಯವಿಟ್ಟು ನಿಮ್ಮ ಹೆಸರು ಮತ್ತು ಅಗತ್ಯತೆ ತಿಳಿಸಿ. ನಾವು ನಿಮ್ಮ ಪ್ರಶ್ನೆಗಳಿಗೆ ತಕ್ಷಣ ಉತ್ತರಿಸುತ್ತೇವೆ. ವಿಶ್ಮಾ ರೆಸಾರ್ಟ್‌ಗೆ ನಿಮ್ಮನ್ನು ಸ್ವಾಗತಿಸಲು ನಾವು ತುಂಬಾ ಸಂತೋಷಪಡುತ್ತೇವೆ.",
            "phonetic" to "Namaskara, nimma kareige tumba dhanyavadagalu. Naanu nimage sahaya madalu santoshapaDuttene. Neevu room booking, chatuvaTikegalu, athava bele mahiti bekadare keLabahudu. Naavu Coorginalliddeeve mattu nimma swagatakke sada siddhariddeeve. Dayavitta nimma hesaru mattu agatayte tilisi. Naavu nimage takshaNA uttarisutteve. Vishma Resortage nimmanu swagatisalu naavu tumba santoshapaDutteve."
        ),
        "hi" to mapOf(
            "original" to "नमस्ते, आपके कॉल के लिए बहुत धन्यवाद। मैं आपकी मदद करके खुश हूँ। आप रूम बुकिंग, गतिविधियों या कीमत की जानकारी के लिए पूछ सकते हैं। हम कूर्ग में स्थित हैं और आपका स्वागत करने के लिए हमेशा तैयार हैं। कृपया अपना नाम और ज़रूरत बताएं। हम आपके सवालों का तुरंत जवाब देंगे। विश्मा रिसॉर्ट में आपका स्वागत करते हुए हमें बहुत खुशी होगी।",
            "phonetic" to "Namaste, aapke call ke liye bahut dhanyavaad. Main aapki madad karke khush hoon. Aap room booking, gatividhiyon ya keemat ki jaankari ke liye pooch sakte hain. Hum Coorg mein sthit hain aur aapka swagat karne ke liye hamesha taiyaar hain. Kripya apna naam aur zaroorat batayen. Hum aapke sawaalon ka turant jawaab denge. Vishma Resort mein aapka swagat karte hue hume bahut khushi hogi."
        ),
        "ta" to mapOf(
            "original" to "வணக்கம், உங்கள் அழைப்பிற்கு மிக்க நன்றி. உங்களுக்கு உதவ மகிழ்ச்சியாக இருக்கிறேன். அறை முன்பதிவு, செயல்பாடுகள் அல்லது விலை தகவல் வேண்டுமானால் கேட்கலாம். நாங்கள் கூர்கில் இருக்கிறோம், உங்களை வரவேற்க எப்போதும் தயாராக இருக்கிறோம். உங்கள் பெயர் மற்றும் தேவையை சொல்லுங்கள். உங்கள் கேள்விகளுக்கு உடனே பதில் சொல்கிறோம். விஷ்மா ரிசார்ட்டில் உங்களை வரவேற்பதில் மிக்க மகிழ்ச்சி.",
            "phonetic" to "Vanakkam, ungal azhaippiRku mikka nandri. Ungalukku udava magizhchiyaaga irukkiren. Arai munpatiivu, seyalpaadugal alladu vilai thagaval vendumaanal ketkalaam. Naangal Coorgil irukkiRom, ungalai varavERka eppozhudum thaiyaaraaga irukkiRom. Ungal peyar matrum thaevayai sollungal. Ungal kELvigaLukku udanE pathil solgiRom. Vishma Resortil ungalai varavERpatil mikka magizhchi."
        )
    )

    init {
        resetAndCheck()
    }

    fun resetAndCheck() {
        _state.value = VoiceSetupState()
        loadVoiceClones()
    }

    private fun buildLanguageList(voiceClones: Map<String, String> = emptyMap(), legacyVoiceId: String? = null) = listOf(
        LanguageVoiceStatus("en", "English", "🇬🇧", voiceClones["en"] ?: legacyVoiceId),
        LanguageVoiceStatus("kn", "Kannada", "🇮🇳", voiceClones["kn"]),
        LanguageVoiceStatus("hi", "Hindi", "🇮🇳", voiceClones["hi"]),
        LanguageVoiceStatus("ta", "Tamil", "🇮🇳", voiceClones["ta"])
    )

    private fun loadVoiceClones() {
        viewModelScope.launch {
            val uid = Firebase.auth.currentUser?.uid
            if (uid == null) {
                _state.value = _state.value.copy(languages = buildLanguageList())
                return@launch
            }
            try {
                val doc = Firebase.firestore.collection("users").document(uid).get().await()
                val data = doc.data ?: emptyMap<String, Any>()

                @Suppress("UNCHECKED_CAST")
                val voiceClones = data["voiceClones"] as? Map<String, String> ?: emptyMap()
                val legacyVoiceId = data["elevenLabsVoiceId"] as? String

                _state.value = _state.value.copy(
                    step = VoiceSetupStep.LANGUAGE_SELECT,
                    languages = buildLanguageList(voiceClones, legacyVoiceId)
                )
            } catch (e: Exception) {
                android.util.Log.e("AiVoiceVM", "loadVoiceClones error: ${e.message}")
                _state.value = _state.value.copy(languages = buildLanguageList())
            }
        }
    }

    fun selectLanguage(language: LanguageVoiceStatus) {
        _state.value = _state.value.copy(
            selectedLanguage = language,
            step = VoiceSetupStep.SCRIPT_SELECT
        )
    }

    fun selectScript(option: String) {
        _state.value = _state.value.copy(
            selectedScriptOption = option,
            step = VoiceSetupStep.ENVIRONMENT_CHECK
        )
    }

    fun goToEnvironmentCheck() {
        _state.value = _state.value.copy(step = VoiceSetupStep.ENVIRONMENT_CHECK)
    }

    fun startRecording(context: Context) {
        viewModelScope.launch {
            try {
                val file = File(context.cacheDir, "voice_${_state.value.selectedLanguage?.code}_${System.currentTimeMillis()}.m4a")
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
                mediaRecorder?.apply { stop(); release() }
                mediaRecorder = null

                val seconds = _state.value.recordingSeconds
                _state.value = _state.value.copy(isRecording = false)

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

                uploadVoiceSample(file)

            } catch (e: Exception) {
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
            val langCode = _state.value.selectedLanguage?.code ?: "en"

            val requestFile = file.asRequestBody("audio/m4a".toMediaTypeOrNull())
            val audioPart = MultipartBody.Part.createFormData("audio", file.name, requestFile)
            val userIdPart = uid.toRequestBody("text/plain".toMediaTypeOrNull())
            val languagePart = langCode.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = UrVoiceApi.service.cloneVoice(userIdPart, languagePart, audioPart)
            if (response.isSuccessful) {
                val body = response.body()
                val voiceId = body?.get("voiceId") as? String
                if (voiceId != null) {
                    val langName = _state.value.selectedLanguage?.displayName ?: "Voice"
                    _state.value = _state.value.copy(
                        step = VoiceSetupStep.SUCCESS,
                        successMessage = "$langName voice is ready!"
                    )
                    loadVoiceClones() // Refresh language status
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
            _state.value = _state.value.copy(
                step = VoiceSetupStep.ERROR,
                errorMessage = "Connection error. Please try again.",
                retryCount = _state.value.retryCount + 1
            )
        }
    }

    fun deleteVoice(languageCode: String, voiceId: String) {
        viewModelScope.launch {
            try {
                val uid = Firebase.auth.currentUser?.uid ?: return@launch
                UrVoiceApi.service.deleteVoice(mapOf(
                    "user_id" to uid,
                    "voice_id" to voiceId,
                    "language" to languageCode
                ))
                loadVoiceClones()
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

    fun backToLanguageSelect() {
        _state.value = _state.value.copy(
            step = VoiceSetupStep.LANGUAGE_SELECT,
            selectedLanguage = null,
            errorMessage = null,
            retryCount = 0
        )
        loadVoiceClones()
    }

    override fun onCleared() {
        super.onCleared()
        mediaRecorder?.release()
        mediaRecorder = null
        recordingJob?.cancel()
    }
}

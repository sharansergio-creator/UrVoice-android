package com.urvoice.app.ui.voice

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

val PremiumPurple = Color(0xFF7C3AED)
val PremiumPurpleLight = Color(0xFFBB86FC)
val DarkBackground = Color(0xFF0A0A0F)
val DarkSurface = Color(0xFF141420)
val GoldAccent = Color(0xFFFFD700)

@Composable
fun AiVoiceSetupScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    viewModel: AiVoiceSetupViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith
                slideOutHorizontally { -it } + fadeOut()
            }
        ) { step ->
            when (step) {
                VoiceSetupStep.LANGUAGE_SELECT -> LanguageSelectStep(
                    state = state,
                    onLanguageSelected = { viewModel.selectLanguage(it) },
                    onDeleteVoice = { langCode, voiceId -> viewModel.deleteVoice(langCode, voiceId) },
                    onDone = onComplete
                )
                VoiceSetupStep.SCRIPT_SELECT -> ScriptSelectStep(
                    language = state.selectedLanguage,
                    onScriptSelected = { viewModel.selectScript(it) },
                    onBack = { viewModel.backToLanguageSelect() }
                )
                VoiceSetupStep.ENVIRONMENT_CHECK -> EnvironmentCheckStep(
                    onReady = {
                        viewModel.startRecording(context)
                    },
                    onBack = { viewModel.backToLanguageSelect() },
                    requestPermission = true
                )
                VoiceSetupStep.RECORDING -> RecordingStep(
                    state = state,
                    script = viewModel.scripts[state.selectedLanguage?.code]
                        ?.get(state.selectedScriptOption) ?: "",
                    onStopRecording = { viewModel.stopRecordingAndAnalyze() }
                )
                VoiceSetupStep.PROCESSING -> ProcessingStep(
                    language = state.selectedLanguage?.displayName ?: "Voice"
                )
                VoiceSetupStep.SUCCESS -> SuccessStep(
                    message = state.successMessage ?: "Your Voice is Ready!",
                    languages = state.languages,
                    onSetupAnother = { viewModel.backToLanguageSelect() },
                    onComplete = onComplete
                )
                VoiceSetupStep.ERROR -> ErrorStep(
                    errorMessage = state.errorMessage ?: "Something went wrong",
                    retryCount = state.retryCount,
                    onRetry = { viewModel.startRecording(context) },
                    onSkip = { viewModel.backToLanguageSelect() }
                )
            }
        }
    }
}

@Composable
private fun LanguageSelectStep(
    state: VoiceSetupState,
    onLanguageSelected: (LanguageVoiceStatus) -> Unit,
    onDeleteVoice: (String, String) -> Unit,
    onDone: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf<LanguageVoiceStatus?>(null) }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            containerColor = DarkSurface,
            title = { Text("Delete Voice Clone?", color = Color.White) },
            text = {
                Text(
                    "Delete ${showDeleteConfirm?.displayName} voice clone? The AI will use the default voice for this language.",
                    color = Color.White.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val lang = showDeleteConfirm!!
                    onDeleteVoice(lang.code, lang.voiceId!!)
                    showDeleteConfirm = null
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.5f))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Premium badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = GoldAccent.copy(alpha = 0.15f),
            modifier = Modifier.border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
        ) {
            Text(
                "✨ PREMIUM FEATURE",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                color = GoldAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "AI Voice Clones",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Your callers hear YOU in their language",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Language cards
        if (state.languages.isEmpty()) {
            CircularProgressIndicator(color = PremiumPurple)
        } else {
            state.languages.forEach { lang ->
                LanguageCard(
                    language = lang,
                    onSetup = { onLanguageSelected(lang) },
                    onDelete = { showDeleteConfirm = lang }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        val activeCount = state.languages.count { it.voiceId != null }
        if (activeCount > 0) {
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Done ($activeCount voice${if (activeCount > 1) "s" else ""} active)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            TextButton(onClick = onDone) {
                Text("Skip for Now", color = Color.White.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun LanguageCard(
    language: LanguageVoiceStatus,
    onSetup: () -> Unit,
    onDelete: () -> Unit
) {
    val isActive = language.voiceId != null

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isActive) PremiumPurple.copy(alpha = 0.15f) else DarkSurface,
        border = if (isActive) BorderStroke(1.dp, PremiumPurple.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(language.flag, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    language.displayName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (isActive) "✅ Voice Active" else "⚪ Not recorded",
                    color = if (isActive) Color(0xFF1DB954) else Color.White.copy(alpha = 0.4f),
                    fontSize = 13.sp
                )
            }
            val currentPlan by com.urvoice.app.data.PlanManager.currentPlan.collectAsState()
            val isLocked = language.code != "en" && currentPlan == "basic"
            if (isActive) {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = Color.Red.copy(alpha = 0.7f), fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onSetup) {
                    Text("Re-record", color = PremiumPurpleLight, fontSize = 12.sp)
                }
            } else if (isLocked) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFD700).copy(alpha = 0.1f)
                ) {
                    Text(
                        "✨ Premium",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = Color(0xFFFFD700),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = onSetup,
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Set Up", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ScriptSelectStep(
    language: LanguageVoiceStatus?,
    onScriptSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            "${language?.flag ?: ""} ${language?.displayName ?: ""} Voice",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "How would you like to read the script?",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Option A — Original script
        Surface(
            onClick = { onScriptSelected("original") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            border = BorderStroke(1.dp, PremiumPurple.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(language?.flag ?: "🌐", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Original Script",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (language?.code == "en") "Read in English"
                            else "Read in ${language?.displayName} characters",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "✅ Best quality — most accurate clone",
                    color = Color(0xFF1DB954),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Option B — Phonetic (only show for non-English)
        if (language?.code != "en") {
            Surface(
                onClick = { onScriptSelected("phonetic") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔤", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "English Phonetic Version",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Same words written in English letters",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 13.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "⚠️ Use if you can't read ${language?.displayName} script",
                        color = GoldAccent.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(onClick = onBack) {
            Text("Go Back", color = Color.White.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun EnvironmentCheckStep(
    onReady: () -> Unit,
    onBack: () -> Unit,
    requestPermission: Boolean = false
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) onReady()
    }

    val checks = listOf(
        "🔇" to "Find a quiet room with no background noise",
        "📱" to "Hold your phone 15cm from your mouth",
        "🗣️" to "Speak naturally — don't rush or whisper",
        "📖" to "Read the full script shown on the next screen",
        "🔁" to "You can retry if the quality isn't perfect"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text("Before You Record", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Follow these steps for the best voice clone", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(40.dp))

        checks.forEachIndexed { index, (emoji, text) ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(index * 150L)
                visible = true
            }
            AnimatedVisibility(visible = visible, enter = slideInHorizontally { -it } + fadeIn()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(emoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("I'm Ready — Start Recording", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onBack) { Text("Go Back", color = Color.White.copy(alpha = 0.4f)) }
    }
}

@Composable
private fun RecordingStep(
    state: VoiceSetupState,
    script: String,
    onStopRecording: () -> Unit
) {
    val progress = (state.recordingSeconds / 60f).coerceIn(0f, 1f)
    val infiniteTransition = rememberInfiniteTransition()
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f + (state.audioLevel * 0.3f),
        animationSpec = infiniteRepeatable(animation = tween(300), repeatMode = RepeatMode.Reverse)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "${state.selectedLanguage?.flag ?: ""} Recording ${state.selectedLanguage?.displayName ?: ""}...",
            color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold
        )
        Text("Read the script below clearly", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)

        Spacer(modifier = Modifier.height(32.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(160.dp),
                color = PremiumPurple,
                trackColor = PremiumPurple.copy(alpha = 0.15f),
                strokeWidth = 6.dp
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(110.dp).scale(ringScale).background(PremiumPurple.copy(alpha = 0.2f), CircleShape)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(90.dp).background(PremiumPurple, CircleShape)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("${state.recordingSeconds}s / 60s", color = PremiumPurpleLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(24.dp))

        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = DarkSurface) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("📖 Read this aloud:", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(script, color = Color.White, fontSize = 15.sp, lineHeight = 24.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (state.recordingSeconds >= 35) {
            Button(
                onClick = onStopRecording,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Done — Analyze My Voice", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Surface(modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), color = DarkSurface) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Keep reading... (${35 - state.recordingSeconds}s more)", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun ProcessingStep(language: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(80.dp), color = PremiumPurple, strokeWidth = 6.dp)
        Spacer(modifier = Modifier.height(32.dp))
        Text("Cloning Your $language Voice", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Analyzing audio patterns...", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text("This takes about 10–15 seconds", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
    }
}

@Composable
private fun SuccessStep(
    message: String,
    languages: List<LanguageVoiceStatus>,
    onSetupAnother: () -> Unit,
    onComplete: () -> Unit
) {
    val nextLanguage = languages.firstOrNull { it.voiceId == null }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF1DB954),
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(message, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Your AI assistant will answer calls in this voice.",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Progress summary
        val activeCount = languages.count { it.voiceId != null }
        Text(
            "$activeCount / ${languages.size} languages set up",
            color = PremiumPurpleLight,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (nextLanguage != null) {
            Button(
                onClick = onSetupAnother,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Set Up ${nextLanguage.flag} ${nextLanguage.displayName} Next", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (nextLanguage != null) DarkSurface else PremiumPurple
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Go to Dashboard", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ErrorStep(
    errorMessage: String,
    retryCount: Int,
    onRetry: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("❌", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Let's Try Again", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = Color(0xFFFF3B30).copy(alpha = 0.1f)) {
            Text(errorMessage, modifier = Modifier.padding(16.dp), color = Color(0xFFFF6B6B), fontSize = 14.sp, textAlign = TextAlign.Center)
        }
        if (retryCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Attempt ${retryCount + 1} — Find a quieter spot and try again", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple), shape = RoundedCornerShape(16.dp)) {
            Text("Try Again", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onSkip) { Text("Back to Languages", color = Color.White.copy(alpha = 0.4f)) }
    }
}

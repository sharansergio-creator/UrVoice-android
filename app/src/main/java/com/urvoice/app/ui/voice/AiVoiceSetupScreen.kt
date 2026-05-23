package com.urvoice.app.ui.voice

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
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
                VoiceSetupStep.INTRO -> IntroStep(
                    hasExistingVoice = state.hasExistingVoice,
                    onGetStarted = { viewModel.goToEnvironmentCheck() },
                    onSkip = onSkip
                )
                VoiceSetupStep.ENVIRONMENT_CHECK -> EnvironmentCheckStep(
                    onReady = { viewModel.startRecording(context) },
                    onBack = { onSkip() }
                )
                VoiceSetupStep.RECORDING -> RecordingStep(
                    state = state,
                    script = viewModel.VOICE_SCRIPT,
                    onStopRecording = { viewModel.stopRecordingAndAnalyze() }
                )
                VoiceSetupStep.PROCESSING -> ProcessingStep()
                VoiceSetupStep.SUCCESS -> SuccessStep(onComplete = onComplete)
                VoiceSetupStep.ERROR -> ErrorStep(
                    errorMessage = state.errorMessage ?: "Something went wrong",
                    retryCount = state.retryCount,
                    onRetry = { viewModel.startRecording(context) },
                    onSkip = onSkip
                )
            }
        }
    }
}

@Composable
private fun IntroStep(
    hasExistingVoice: Boolean,
    onGetStarted: () -> Unit,
    onSkip: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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

        Spacer(modifier = Modifier.height(32.dp))

        // Pulsing mic icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(140.dp)
                .scale(pulseScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(PremiumPurple.copy(alpha = 0.3f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(PremiumPurple, PremiumPurpleLight)
                        ),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            if (hasExistingVoice) "Your Voice is Active" else "Clone Your Voice",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            if (hasExistingVoice)
                "Your callers already hear your voice. Re-record to update it."
            else
                "Your callers will hear YOU — not a robot.\nA 30-second recording is all it takes.",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PremiumPurple
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                if (hasExistingVoice) "Re-record Voice" else "Set Up My Voice",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onSkip) {
            Text(
                if (hasExistingVoice) "Keep Current Voice" else "Skip for Now",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun EnvironmentCheckStep(
    onReady: () -> Unit,
    onBack: () -> Unit
) {
    val checks = listOf(
        "🔇" to "Find a quiet room with no background noise",
        "📱" to "Hold your phone 15cm from your mouth",
        "🗣️" to "Speak naturally — don't rush or whisper",
        "📖" to "Read the script shown on the next screen",
        "🔁" to "You can retry if the quality isn't perfect"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            "Before You Record",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Follow these steps for the best voice clone",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        checks.forEach { (emoji, text) ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(checks.indexOf(emoji to text) * 150L)
                visible = true
            }
            AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally { -it } + fadeIn()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(emoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onReady,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("I'm Ready — Start Recording", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBack) {
            Text("Go Back", color = Color.White.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun RecordingStep(
    state: VoiceSetupState,
    script: String,
    onStopRecording: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f + (state.audioLevel * 0.3f),
        animationSpec = infiniteRepeatable(
            animation = tween(300),
            repeatMode = RepeatMode.Reverse
        )
    )
    val progress = (state.recordingSeconds / 45f).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "Recording...",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Read the script below clearly",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Circular progress ring with mic
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
                modifier = Modifier
                    .size(110.dp)
                    .scale(ringScale)
                    .background(PremiumPurple.copy(alpha = 0.2f), CircleShape)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(90.dp)
                        .background(PremiumPurple, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "${state.recordingSeconds}s / 45s",
            color = PremiumPurpleLight,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Script card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "📖 Read this aloud:",
                    color = GoldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    script,
                    color = Color.White,
                    fontSize = 16.sp,
                    lineHeight = 26.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (state.recordingSeconds >= 20) {
            Button(
                onClick = onStopRecording,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Done — Analyze My Voice", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "Keep reading... (${20 - state.recordingSeconds}s more)",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ProcessingStep() {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing))
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
            CircularProgressIndicator(
                modifier = Modifier.size(160.dp),
                color = PremiumPurple,
                trackColor = PremiumPurple.copy(alpha = 0.1f),
                strokeWidth = 6.dp
            )
            Icon(
                Icons.Default.Mic,
                contentDescription = null,
                tint = PremiumPurpleLight,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Cloning Your Voice", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Analyzing audio patterns...", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text("This takes about 10–15 seconds", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
    }
}

@Composable
private fun SuccessStep(onComplete: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF1DB954),
            modifier = Modifier.size(100.dp).scale(scale)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Your Voice is Ready!", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Your AI assistant will now answer calls in your voice. Your callers won't know it's AI.",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
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

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFFF3B30).copy(alpha = 0.1f)
        ) {
            Text(
                errorMessage,
                modifier = Modifier.padding(16.dp),
                color = Color(0xFFFF6B6B),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }

        if (retryCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Attempt ${retryCount + 1} — Find a quieter spot and try again",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PremiumPurple),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Try Again", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onSkip) {
            Text("Skip for Now", color = Color.White.copy(alpha = 0.4f))
        }
    }
}

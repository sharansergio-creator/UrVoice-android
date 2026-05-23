package com.urvoice.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.urvoice.app.ui.business.BusinessSetupScreen
import com.urvoice.app.ui.dashboard.DashboardScreen
import com.urvoice.app.ui.onboarding.OnboardingScreen
import com.urvoice.app.ui.settings.CallHandlingScreen
import com.urvoice.app.ui.settings.SettingsScreen
import com.urvoice.app.ui.voice.AiVoiceSetupScreen
import kotlinx.coroutines.tasks.await

enum class Screen { ONBOARDING, BUSINESS_SETUP, AI_VOICE_SETUP, DASHBOARD, SETTINGS, BUSINESS_EDIT, CALL_HANDLING }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannels()
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            UrVoiceTheme {
                val context = LocalContext.current

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    Log.d("UrVoice", "Notification permission granted: $isGranted")
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                val auth = FirebaseAuth.getInstance()
                val currentUser = auth.currentUser

                // Start on ONBOARDING; if already signed in resolve the correct screen first
                var screen by remember {
                    mutableStateOf(if (currentUser != null) null else Screen.ONBOARDING)
                }

                // When the app launches with an existing session, check Firestore once
                LaunchedEffect(currentUser?.uid) {
                    if (currentUser != null && screen == null) {
                        screen = resolveScreenForUser(currentUser.uid)
                    }
                }

                when (screen) {
                    Screen.ONBOARDING -> OnboardingScreen(
                        onNavigateToDashboard = {
                            // OTP verified; set screen to null so the LaunchedEffect
                            // below queries Firestore and picks BUSINESS_SETUP or DASHBOARD
                            screen = null
                        }
                    )

                    Screen.BUSINESS_SETUP -> BusinessSetupScreen(
                        onNavigateToDashboard = { screen = Screen.AI_VOICE_SETUP }
                    )

                    Screen.AI_VOICE_SETUP -> AiVoiceSetupScreen(
                        onComplete = { screen = Screen.DASHBOARD },
                        onSkip = { screen = Screen.DASHBOARD }
                    )

                    Screen.SETTINGS -> SettingsScreen(
                        onBack = { screen = Screen.DASHBOARD },
                        onEditProfile = { screen = Screen.BUSINESS_EDIT },
                        onCallHandling = { screen = Screen.CALL_HANDLING },
                        onSignOut = { screen = Screen.ONBOARDING }
                    )

                    Screen.CALL_HANDLING -> CallHandlingScreen(
                        onBack = { screen = Screen.SETTINGS }
                    )

                    Screen.BUSINESS_EDIT -> BusinessSetupScreen(
                        isEditMode = true,
                        onNavigateToDashboard = { screen = Screen.SETTINGS }
                    )

                    Screen.DASHBOARD -> DashboardScreen(
                        onNavigateToSettings = { screen = Screen.SETTINGS }
                    )

                    // null = resolving (spinner shown while Firestore is queried)
                    null -> CircularProgressIndicator()
                }

                // After OTP success the screen is temporarily null; resolve once uid is set
                LaunchedEffect(auth.currentUser?.uid, screen) {
                    if (screen == null) {
                        val uid = auth.currentUser?.uid
                        screen = if (uid != null) resolveScreenForUser(uid) else Screen.ONBOARDING
                    }
                }
            }
        }
    }
}

/** Checks Firestore for an existing business profile and returns the right destination. */
private suspend fun resolveScreenForUser(uid: String): Screen {
    // Register / refresh the FCM token now that we have a confirmed signed-in uid
    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnSuccessListener
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(currentUid)
            .set(
                mapOf("fcmToken" to token, "userId" to currentUid),
                com.google.firebase.firestore.SetOptions.merge()
            )
        Log.d("UrVoice", "FCM token saved for user: $currentUid, token: $token")
    }

    return try {
        val doc = FirebaseFirestore.getInstance()
            .collection("business_context")
            .document(uid)
            .get()
            .await()
        if (doc.exists() && doc.getString("businessName")?.isNotBlank() == true) {
            Screen.DASHBOARD
        } else {
            Screen.BUSINESS_SETUP
        }
    } catch (e: Exception) {
        // On error fall back to business setup so the user can still proceed
        Screen.BUSINESS_SETUP
    }
}

private fun MainActivity.createNotificationChannels() {
    val nm = getSystemService(NotificationManager::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        nm.createNotificationChannel(
            NotificationChannel(
                "urvoice_calls",
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "UrVoice call notifications" }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                "urvoice_updates",
                "Call Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }
}

private val UrVoiceDarkColorScheme = darkColorScheme(
    primary = Color(0xFF6C63FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3D3594),
    onPrimaryContainer = Color.White,
    background = Color(0xFF0A0A0A),
    onBackground = Color.White,
    surface = Color(0xFF1A1A1A),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2E2E2E),
    onSurfaceVariant = Color(0xFFBBBBBB),
    error = Color(0xFFFF5252),
    onError = Color.White,
)

@Composable
fun UrVoiceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = UrVoiceDarkColorScheme,
        content = content
    )
}

package com.urvoice.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private val Background   = Color(0xFF0A0A0A)
private val Surface      = Color(0xFF1A1A1A)
private val Accent       = Color(0xFF6C63FF)
private val TextPrimary  = Color.White
private val TextSecondary = Color(0xFFBBBBBB)
private val Border       = Color(0xFF2E2E2E)
private val ErrorColor   = Color(0xFFFF5252)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onCallHandling: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToVoiceSetup: () -> Unit = {},
) {
    var showSignOutDialog by remember { mutableStateOf(false) }
    var hasVoice by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val doc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .get()
                    .await()
                hasVoice = doc.getString("elevenLabsVoiceId") != null
            }
        } catch (_: Exception) {}
    }

    BackHandler { onBack() }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out", color = TextPrimary) },
            text  = { Text("Are you sure you want to sign out?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    FirebaseAuth.getInstance().signOut()
                    onSignOut()
                }) {
                    Text("Sign Out", color = ErrorColor, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Surface,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Settings",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(8.dp))

        // ── Business section ─────────────────────────────────────────────────
        SettingsSectionHeader("Business")
        SettingsItem(
            icon       = Icons.Default.Edit,
            iconTint   = Accent,
            title      = "Edit Business Profile",
            subtitle   = "Update name, hours, services and more",
            onClick    = onEditProfile
        )

        Spacer(Modifier.height(16.dp))

        // ── Call handling section ────────────────────────────────────────────
        SettingsSectionHeader("Call Handling")
        SettingsItem(
            icon     = Icons.Default.Phone,
            iconTint = Accent,
            title    = "Call Handling",
            subtitle = "Configure when and how UrVoice answers calls",
            onClick  = onCallHandling
        )

        Spacer(Modifier.height(16.dp))
        // ── AI Voice section ─────────────────────────────────────────────────────────────────
        SettingsSectionHeader("AI Voice")
        SettingsItem(
            icon = Icons.Default.Mic,
            iconTint = Color(0xFF7C3AED),
            title = "AI Voice Clone",
            subtitle = if (hasVoice) "● Voice Active" else "Not configured",
            subtitleColor = if (hasVoice) Color(0xFF1DB954) else TextSecondary,
            onClick = onNavigateToVoiceSetup
        )

        Spacer(Modifier.height(16.dp))
        // ── Account section ──────────────────────────────────────────────────
        SettingsSectionHeader("Account")
        SettingsItem(
            icon     = Icons.AutoMirrored.Filled.Logout,
            iconTint = ErrorColor,
            title    = "Sign Out",
            subtitle = "Sign out of your UrVoice account",
            onClick  = { showSignOutDialog = true },
            titleColor = ErrorColor
        )

        Spacer(Modifier.weight(1f))

        // ── App version footer ───────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("UrVoice v1.0", color = TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconTint: Color = TextPrimary,
    title: String,
    subtitle: String = "",
    titleColor: Color = TextPrimary,
    subtitleColor: Color = TextSecondary,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = titleColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (subtitle.isNotBlank()) {
                Text(text = subtitle, color = subtitleColor, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF444444),
            modifier = Modifier.size(20.dp)
        )
    }
    HorizontalDivider(color = Border, thickness = 0.5.dp)
}

@file:OptIn(ExperimentalMaterial3Api::class)

package com.urvoice.app.ui.business

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

// ─────────────────────────── colours ───────────────────────────────────────

private val Background = Color(0xFF0A0A0A)
private val Surface = Color(0xFF1A1A1A)
private val SurfaceDeep = Color(0xFF1E1E1E)
private val Accent = Color(0xFF6C63FF)
private val AccentDim = Accent.copy(alpha = 0.5f)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFBBBBBB)
private val Border = Color(0xFF2E2E2E)
private val ErrorColor = Color(0xFFFF5252)

// ─────────────────────────── screen ────────────────────────────────────────

@Composable
fun BusinessSetupScreen(
    onNavigateToDashboard: () -> Unit,
    isEditMode: Boolean = false,
    viewModel: BusinessSetupViewModel = viewModel()
) {
    val businessName by viewModel.businessName.collectAsState()
    val businessType by viewModel.businessType.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val businessHours by viewModel.businessHours.collectAsState()
    val googleBusinessUrl by viewModel.googleBusinessUrl.collectAsState()
    val websiteUrl by viewModel.websiteUrl.collectAsState()
    val qaAnswers by viewModel.qaAnswers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val error by viewModel.error.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val completionProgress by viewModel.completionProgress.collectAsState()
    val fetchState by viewModel.fetchState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // System back in edit mode → return to Settings instead of closing the app
    BackHandler(enabled = isEditMode) { onNavigateToDashboard() }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            if (isEditMode) {
                snackbarHostState.showSnackbar("Business profile updated successfully")
            }
            onNavigateToDashboard()
        }
    }

    LaunchedEffect(fetchState) {
        when (fetchState) {
            is FetchState.Success -> {
                snackbarHostState.showSnackbar("Business details fetched successfully! Review and save.")
                viewModel.resetFetchState()
            }
            is FetchState.Error -> {
                snackbarHostState.showSnackbar("Could not fetch details. Please fill in manually.")
                viewModel.resetFetchState()
            }
            else -> Unit
        }
    }

    // Time picker dialog state
    var showTimePicker by remember { mutableStateOf(false) }
    var timePickerDay by remember { mutableStateOf("") }
    var timePickerIsOpen by remember { mutableStateOf(true) }

    // Show time picker dialog when requested
    if (showTimePicker) {
        val target = businessHours.find { it.day == timePickerDay }
        val rawTime = if (timePickerIsOpen) target?.openTime else target?.closeTime
        val parts = rawTime?.split(":") ?: listOf("9", "0")
        val initHour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val initMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        TimePickerDialog(
            initialHour = initHour,
            initialMinute = initMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                val formatted = "%02d:%02d".format(hour, minute)
                if (timePickerIsOpen) viewModel.onOpenTimeChange(timePickerDay, formatted)
                else viewModel.onCloseTimeChange(timePickerDay, formatted)
                showTimePicker = false
            }
        )
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Accent)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 40.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // ── Header ──────────────────────────────────────────────────────────
        if (isEditMode) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            ) {
                androidx.compose.material3.IconButton(onClick = onNavigateToDashboard) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Text(
                    text = "Edit Business Profile",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Business Setup",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Help UrVoice represent your business",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
            Icon32(Icons.Default.Business, "Business", Accent)
        }
        }

        Spacer(Modifier.height(20.dp))

        // ── Progress bar ─────────────────────────────────────────────────────
        val progressPercent = (completionProgress * 100).toInt()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Setup progress", color = TextSecondary, fontSize = 12.sp)
            Text(
                "$progressPercent%",
                color = Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { completionProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Accent,
            trackColor = Border
        )

        Spacer(Modifier.height(24.dp))

        // ── Section 1 — Basic Info ───────────────────────────────────────────
        SectionCard(title = "Basic Info") {
            OutlinedTextField(
                value = businessName,
                onValueChange = viewModel::onBusinessNameChange,
                label = { Text("Business Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )

            Spacer(Modifier.height(12.dp))

            var dropdownExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = businessType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Business Type") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    colors = fieldColors()
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.background(SurfaceDeep)
                ) {
                    BUSINESS_TYPES.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type, color = TextPrimary) },
                            onClick = {
                                viewModel.onBusinessTypeChange(type)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = viewModel::onPhoneNumberChange,
                label = { Text("Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Section 2 — Business Hours ───────────────────────────────────────
        SectionCard(title = "Business Hours") {
            businessHours.forEachIndexed { index, hours ->
                DayHoursRow(
                    hours = hours,
                    onToggle = { viewModel.onHourToggle(hours.day, it) },
                    onOpenTimeTap = {
                        timePickerDay = hours.day
                        timePickerIsOpen = true
                        showTimePicker = true
                    },
                    onCloseTimeTap = {
                        timePickerDay = hours.day
                        timePickerIsOpen = false
                        showTimePicker = true
                    }
                )
                if (index < businessHours.size - 1) {
                    HorizontalDivider(
                        color = Border,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Section 3 — Online Presence ──────────────────────────────────────
        SectionCard(title = "Online Presence") {
            OutlinedTextField(
                value = googleBusinessUrl,
                onValueChange = viewModel::onGoogleBusinessUrlChange,
                label = { Text("Google Business Profile URL (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = websiteUrl,
                onValueChange = viewModel::onWebsiteUrlChange,
                label = { Text("Website URL (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )

            Spacer(Modifier.height(14.dp))

            val isFetching = fetchState is FetchState.Loading
            val canFetch = !isFetching && (googleBusinessUrl.isNotBlank() || websiteUrl.isNotBlank())
            Button(
                onClick = { viewModel.fetchFromWeb(googleBusinessUrl, websiteUrl) },
                enabled = canFetch,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6C63FF),
                    disabledContainerColor = Color(0xFF6C63FF).copy(alpha = 0.35f)
                )
            ) {
                if (isFetching) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Fetching…", color = Color.White, fontSize = 14.sp)
                } else {
                    Text("✨ Auto-fetch Business Details", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Section 4 — Custom Q&A ───────────────────────────────────────────
        SectionCard(title = "Custom Q&A") {
            Text(
                text = "Help the AI answer common questions about your business",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))

            QA_QUESTIONS.forEachIndexed { index, question ->
                Text(
                    text = "${index + 1}. $question",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = qaAnswers[question] ?: "",
                    onValueChange = { viewModel.onQaAnswerChange(question, it) },
                    placeholder = { Text("Your answer…", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = fieldColors()
                )
                if (index < QA_QUESTIONS.size - 1) {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Error message ────────────────────────────────────────────────────
        if (error != null) {
            Text(
                text = error!!,
                color = ErrorColor,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        // ── Save button ──────────────────────────────────────────────────────
        Button(
            onClick = viewModel::save,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            enabled = !isSaving,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                disabledContainerColor = AccentDim
            )
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    color = TextPrimary,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (isEditMode) "Save Changes" else "Save & Continue",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
            snackbar = { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF2A2A2A),
                    contentColor = Color.White
                )
            }
        )
    } // end Box
}

// ─────────────────────────── sub-composables ───────────────────────────────

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DayHoursRow(
    hours: BusinessHours,
    onToggle: (Boolean) -> Unit,
    onOpenTimeTap: () -> Unit,
    onCloseTimeTap: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = hours.day,
            color = if (hours.enabled) TextPrimary else TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(40.dp)
        )
        Switch(
            checked = hours.enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextPrimary,
                checkedTrackColor = Accent,
                uncheckedTrackColor = Border
            )
        )
        if (hours.enabled) {
            Spacer(Modifier.weight(1f))
            TimeChip(time = hours.openTime, label = "Open", onClick = onOpenTimeTap)
            Text(
                text = "–",
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            TimeChip(time = hours.closeTime, label = "Close", onClick = onCloseTimeTap)
        }
    }
}

@Composable
private fun TimeChip(time: String, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        androidx.compose.material3.Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = label,
            tint = Accent,
            modifier = Modifier.size(14.dp)
        )
        Text(text = time, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select time", color = TextPrimary) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text("OK", color = Accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = Surface
    )
}

@Composable
private fun Icon32(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color
) {
    androidx.compose.material3.Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(32.dp)
    )
}

@Composable
private fun fieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Accent,
    unfocusedBorderColor = Border,
    focusedLabelColor = Accent,
    unfocusedLabelColor = TextSecondary,
    cursorColor = Accent,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedPlaceholderColor = TextSecondary,
    unfocusedPlaceholderColor = TextSecondary
)

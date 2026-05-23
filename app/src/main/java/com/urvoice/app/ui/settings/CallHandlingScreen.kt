package com.urvoice.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

// ── Design tokens ──────────────────────────────────────────────────────────────
private val ChBg         = Color(0xFF0A0A0A)
private val ChSurface    = Color(0xFF1A1A1A)
private val ChAccent     = Color(0xFF6C63FF)
private val ChBorder     = Color(0xFF2A2A2A)
private val ChTextPri    = Color.White
private val ChTextSec    = Color(0xFF888888)
private val ChSuccess    = Color(0xFF4CAF50)
private val ChError      = Color(0xFFFF5252)
private val ChWarn       = Color(0xFFFFD700)

private const val TWILIO_NUMBER = "+16206596566"

private fun dialCodeForMode(mode: String): String? = when (mode) {
    "ALWAYS"    -> "**21*$TWILIO_NUMBER#"
    "BUSY_ONLY" -> "**61*$TWILIO_NUMBER#"
    else        -> null
}

private fun formatHour(h: Int): String {
    val period = if (h < 12) "AM" else "PM"
    val h12    = when { h == 0 -> 12; h <= 12 -> h; else -> h - 12 }
    return "$h12:00 $period"
}

// ── Screen ─────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHandlingScreen(
    onBack: () -> Unit,
    viewModel: CallHandlingViewModel = viewModel()
) {
    val state            by viewModel.state.collectAsState()
    val context          = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState      = rememberScrollState()

    var copiedCode by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    LaunchedEffect(copiedCode) {
        if (copiedCode) { delay(2000L); copiedCode = false }
    }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = "Call Handling",
                        color      = ChTextPri,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = ChTextPri)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ChBg)
            )
        },
        containerColor = ChBg
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ChAccent, strokeWidth = 3.dp)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                // ── 1. Answer Mode ────────────────────────────────────────────
                AnswerModeCard(
                    selectedMode = state.answerMode,
                    onModeChange = viewModel::setAnswerMode
                )

                // ── 2. No Answer Delay (BUSY_ONLY only) ───────────────────────
                AnimatedVisibility(visible = state.answerMode == "BUSY_ONLY") {
                    NoAnswerDelayCard(
                        delaySeconds  = state.noAnswerDelaySeconds,
                        onDelayChange = viewModel::setNoAnswerDelay
                    )
                }

                // ── 3. Schedule (SCHEDULED only) ──────────────────────────────
                AnimatedVisibility(visible = state.answerMode == "SCHEDULED") {
                    ScheduleCard(
                        startHour     = state.scheduleStartHour,
                        endHour       = state.scheduleEndHour,
                        onStartChange = viewModel::setScheduleStartHour,
                        onEndChange   = viewModel::setScheduleEndHour
                    )
                }

                // ── 4. Forwarding Setup ───────────────────────────────────────
                ForwardingSetupCard(
                    answerMode   = state.answerMode,
                    twilioNumber = state.forwardingNumber,
                    copiedCode   = copiedCode,
                    onCopy       = { code ->
                        clipboardManager.setText(AnnotatedString(code))
                        copiedCode = true
                    },
                    onDial       = { code ->
                        val tel = code.replace("#", "%23")
                        try { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel"))) }
                        catch (_: Exception) {}
                    }
                )

                // ── Error ─────────────────────────────────────────────────────
                if (state.error != null) {
                    Text(
                        text     = state.error!!,
                        color    = ChError,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ChError.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    )
                }

                // ── 5. Save button ────────────────────────────────────────────
                Button(
                    onClick  = viewModel::save,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled  = !state.isSaving,
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = ChAccent)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            color       = Color.White,
                            modifier    = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ── 1. Answer Mode card ────────────────────────────────────────────────────────
@Composable
private fun AnswerModeCard(selectedMode: String, onModeChange: (String) -> Unit) {
    val options = listOf(
        Triple("ALWAYS",    "Always",          "Answer all incoming calls immediately"),
        Triple("BUSY_ONLY", "Busy / No Answer", "Answer only when you don't pick up (recommended)"),
        Triple("SCHEDULED", "Scheduled",        "Answer during set business hours only"),
        Triple("NEVER",     "Disabled",         "Handle all calls yourself")
    )
    ChCard(title = "When should UrVoice answer?") {
        Column {
            options.forEachIndexed { i, (mode, label, desc) ->
                RadioOptionRow(
                    selected = selectedMode == mode,
                    label    = label,
                    desc     = desc,
                    onClick  = { onModeChange(mode) }
                )
                if (i < options.lastIndex) {
                    HorizontalDivider(color = ChBorder, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun RadioOptionRow(
    selected: Boolean,
    label:    String,
    desc:     String,
    onClick:  () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick  = onClick,
            colors   = RadioButtonDefaults.colors(
                selectedColor   = ChAccent,
                unselectedColor = ChTextSec
            )
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = label,
                color      = ChTextPri,
                fontSize   = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(text = desc, color = ChTextSec, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

// ── 2. No Answer Delay card ────────────────────────────────────────────────────
@Composable
private fun NoAnswerDelayCard(delaySeconds: Int, onDelayChange: (Int) -> Unit) {
    ChCard(title = "Ring before UrVoice answers") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Delay", color = ChTextSec, fontSize = 14.sp)
                Text(
                    text       = "$delaySeconds seconds",
                    color      = ChAccent,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Slider(
                value         = delaySeconds.toFloat(),
                onValueChange = { onDelayChange(it.roundToInt()) },
                valueRange    = 10f..30f,
                steps         = 19,
                colors        = SliderDefaults.colors(
                    thumbColor          = ChAccent,
                    activeTrackColor    = ChAccent,
                    inactiveTrackColor  = ChBorder
                )
            )
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("10 sec", color = ChTextSec, fontSize = 12.sp)
                Text("30 sec", color = ChTextSec, fontSize = 12.sp)
            }
        }
    }
}

// ── 3. Schedule card ───────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleCard(
    startHour:     Int,
    endHour:       Int,
    onStartChange: (Int) -> Unit,
    onEndChange:   (Int) -> Unit
) {
    ChCard(title = "Active hours") {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HourPickerField(
                label        = "Start time",
                hour         = startHour,
                onHourChange = onStartChange,
                modifier     = Modifier.weight(1f)
            )
            HourPickerField(
                label        = "End time",
                hour         = endHour,
                onHourChange = onEndChange,
                modifier     = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HourPickerField(
    label:        String,
    hour:         Int,
    onHourChange: (Int) -> Unit,
    modifier:     Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded         = expanded,
        onExpandedChange = { expanded = it },
        modifier         = modifier
    ) {
        OutlinedTextField(
            value         = formatHour(hour),
            onValueChange = {},
            readOnly      = true,
            label         = { Text(label, color = ChTextSec, fontSize = 12.sp) },
            modifier      = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            shape         = RoundedCornerShape(10.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedContainerColor   = Color(0xFF1E1E1E),
                unfocusedContainerColor = Color(0xFF1E1E1E),
                focusedBorderColor      = ChAccent,
                unfocusedBorderColor    = ChBorder,
                focusedTextColor        = ChTextPri,
                unfocusedTextColor      = ChTextPri
            ),
            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false },
            containerColor   = Color(0xFF252525)
        ) {
            (0..23).forEach { h ->
                DropdownMenuItem(
                    text    = {
                        Text(
                            text  = formatHour(h),
                            color = if (h == hour) ChAccent else ChTextPri
                        )
                    },
                    onClick = { onHourChange(h); expanded = false }
                )
            }
        }
    }
}

// ── 4. Forwarding Setup card ───────────────────────────────────────────────────
@Composable
private fun ForwardingSetupCard(
    answerMode:   String,
    twilioNumber: String,
    copiedCode:   Boolean,
    onCopy:       (String) -> Unit,
    onDial:       (String) -> Unit
) {
    val dialCode = dialCodeForMode(answerMode)

    ChCard(
        title    = "Call Forwarding Setup",
        subtitle = "Set up forwarding on your phone so UrVoice can answer"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // ── Twilio number display ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141414), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Your UrVoice Number", color = ChTextSec, fontSize = 12.sp)
                    Text(
                        text       = twilioNumber,
                        color      = ChTextPri,
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box(
                    modifier         = Modifier
                        .size(36.dp)
                        .background(ChAccent.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Phone, null, tint = ChAccent, modifier = Modifier.size(18.dp))
                }
            }

            // ── Mode-specific instructions ────────────────────────────────────
            when (answerMode) {
                "ALWAYS", "BUSY_ONLY" -> if (dialCode != null) {
                    Text(
                        text     = if (answerMode == "ALWAYS")
                            "To forward all calls to UrVoice, dial this code on your phone:"
                        else
                            "To forward unanswered calls to UrVoice, dial this code:",
                        color    = ChTextSec,
                        fontSize = 13.sp
                    )

                    // ── Dial code box ─────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E1E1E), RoundedCornerShape(10.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text       = dialCode,
                            color      = ChAccent,
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier   = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick  = { onCopy(dialCode) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector        = if (copiedCode) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = if (copiedCode) "Copied" else "Copy",
                                tint               = if (copiedCode) ChSuccess else ChTextSec,
                                modifier           = Modifier.size(18.dp)
                            )
                        }
                    }

                    // ── Call to Setup button ──────────────────────────────────
                    Button(
                        onClick  = { onDial(dialCode) },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = ChAccent.copy(alpha = 0.12f),
                            contentColor   = ChAccent
                        )
                    ) {
                        Icon(Icons.Default.Call, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Call to Setup", fontWeight = FontWeight.SemiBold)
                    }
                }

                "SCHEDULED" -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ChWarn.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Info, null, tint = ChWarn, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text     = "Set up manually during scheduled hours",
                        color    = ChWarn,
                        fontSize = 13.sp
                    )
                }

                "NEVER" -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ChError.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Block, null, tint = ChError, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text     = "UrVoice is disabled. No call forwarding needed.",
                        color    = ChError,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ── Base card ──────────────────────────────────────────────────────────────────
@Composable
private fun ChCard(
    title:    String,
    subtitle: String  = "",
    content:  @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChSurface, RoundedCornerShape(16.dp))
            .border(1.dp, ChBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text       = title,
                color      = ChTextPri,
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle.isNotBlank()) {
                Text(text = subtitle, color = ChTextSec, fontSize = 13.sp)
            }
        }
        content()
    }
}

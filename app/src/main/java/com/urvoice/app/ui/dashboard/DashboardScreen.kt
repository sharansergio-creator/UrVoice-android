package com.urvoice.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urvoice.app.data.model.CallSession
import com.urvoice.app.data.model.Exchange
import com.urvoice.app.ui.contacts.ContactsContent
import com.urvoice.app.ui.contacts.ContactsTopBar
import com.urvoice.app.ui.contacts.ContactsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BackgroundColor = Color(0xFF0A0A0A)
private val AccentColor = Color(0xFF6C63FF)
private val AccentLight = Color(0xFF9C94FF)
private val SurfaceColor = Color(0xFF1A1A1A)
private val DividerColor = Color(0xFF1E1E1E)
private val TextSecondary = Color(0xFF888888)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToContacts: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel(),
    contactsViewModel: ContactsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedSession by remember { mutableStateOf<CallSession?>(null) }
    var showTestCallSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) isRefreshing = false
    }

    selectedSession?.let { session ->
        CallSessionDetailSheet(session = session, onDismiss = { selectedSession = null })
    }

    if (showTestCallSheet) {
        TestCallSheet(
            businessName = uiState.businessName.ifBlank { "your business" },
            onDismiss = { showTestCallSheet = false }
        )
    }

    Scaffold(
        topBar = {
            when (selectedTab) {
                1 -> ContactsTopBar()
                else -> DashboardTopBar(onSettingsClick = onNavigateToSettings)
            }
        },
        bottomBar = {
            DashboardBottomNav(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    selectedTab = tab
                    if (tab == 2) onNavigateToSettings()
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showTestCallSheet = true },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null
                        )
                    },
                    text = { Text("Test Call", fontWeight = FontWeight.SemiBold) },
                    containerColor = AccentColor,
                    contentColor = Color.White
                )
            }
        },
        containerColor = BackgroundColor
    ) { paddingValues ->
        when (selectedTab) {
            1 -> ContactsContent(
                paddingValues = paddingValues,
                onNavigateBack = { selectedTab = 0 },
                viewModel     = contactsViewModel
            )
            else -> PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    viewModel.refresh()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
            when {
                uiState.isLoading && uiState.sessions.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = AccentColor,
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }

                uiState.error != null -> {
                    ErrorContent(
                        message = uiState.error!!,
                        onRetry = { viewModel.refresh() }
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        item {
                            StatsSection(stats = uiState.stats)
                        }
                        item {
                            RecentCallsHeader()
                        }
                        item {
                            FilterChipsRow(
                                selectedFilter = uiState.selectedFilter,
                                onFilterChange = viewModel::setFilter
                            )
                        }
                        if (uiState.filteredSessions.isEmpty()) {
                            item {
                                EmptyCallsContent()
                            }
                        } else {
                            items(
                                items = uiState.filteredSessions,
                                key = { it.sessionId ?: it.hashCode().toString() }
                            ) { session ->
                                CallSessionItem(session = session, onClick = { selectedSession = session })
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                                    color = DividerColor
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(onSettingsClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "UrVoice",
                style = TextStyle(
                    brush = Brush.horizontalGradient(listOf(AccentLight, AccentColor)),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BackgroundColor
        )
    )
}

@Composable
private fun DashboardBottomNav(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF111111),
        tonalElevation = 0.dp
    ) {
        listOf(
            Triple(Icons.Default.Home, "Dashboard", 0),
            Triple(Icons.Default.Person, "Contacts", 1),
            Triple(Icons.Default.Settings, "Settings", 2)
        ).forEach { (icon, label, index) ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentColor,
                    selectedTextColor = AccentColor,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = AccentColor.copy(alpha = 0.15f)
                )
            )
        }
    }
}

@Composable
private fun StatsSection(stats: DashboardStats) {
    Column(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
        Text(
            text = "TODAY'S SUMMARY",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 10.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Total Calls",
                value = stats.totalToday.toString(),
                valueColor = AccentColor
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Customers",
                value = stats.customers.toString(),
                valueColor = Color(0xFF4CAF50)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Spam Blocked",
                value = stats.spamBlocked.toString(),
                valueColor = Color(0xFFFF5252)
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = valueColor,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun FilterChipsRow(
    selectedFilter: String,
    onFilterChange: (String) -> Unit
) {
    val filters = listOf(
        "ALL"         to "All",
        "CUSTOMER"    to "Customers",
        "SPAM"        to "Spam",
        "BLOCKED"     to "Blocked",
        "AFTER_HOURS" to "After Hours"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (key, label) ->
            val selected = selectedFilter == key
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) AccentColor else SurfaceColor)
                    .clickable { onFilterChange(key) }
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                Text(
                    text       = label,
                    color      = if (selected) Color.White else TextSecondary,
                    fontSize   = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun RecentCallsHeader() {
    Text(
        text = "RECENT CALLS",
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun CallSessionItem(session: CallSession, onClick: () -> Unit) {
    val firstTranscript = session.exchanges.firstOrNull()?.transcript
    val displayName = when {
        !session.callerName.isNullOrBlank() -> session.callerName!!
        !session.callerNumber.isNullOrBlank() && !session.callerNumber.equals("unknown", ignoreCase = true) -> session.callerNumber!!
        else -> "Unknown Caller"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(AccentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = null,
                tint = AccentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            StatusChip(session.status)
            if (!firstTranscript.isNullOrBlank()) {
                Text(
                    text = firstTranscript,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatTime(session.startTime),
                color = Color(0xFF555555),
                fontSize = 12.sp
            )
            val count = session.totalExchanges ?: session.exchanges.size
            if (count > 0) {
                Text(
                    text = "$count exchange${if (count != 1) "s" else ""}",
                    color = Color(0xFF555555),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: String?) {
    val (label, bgColor) = when (status?.lowercase()) {
        "active"    -> "ACTIVE"    to Color(0xFF1B5E20)
        "completed" -> "COMPLETED" to Color(0xFF2A2A2A)
        else        -> "UNKNOWN"   to Color(0xFF333333)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun EmptyCallsContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 72.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(AccentColor.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = null,
                tint = AccentColor.copy(alpha = 0.35f),
                modifier = Modifier.size(46.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "No calls yet.",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "UrVoice is ready to answer.",
            color = TextSecondary,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = Color(0xFFFF5252),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = Color(0xFFFF5252),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            onClick = onRetry,
            border = BorderStroke(1.dp, AccentColor),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentColor)
        ) {
            Text("Retry", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────

private fun formatCallerNumber(number: String?): String {
    if (number.isNullOrBlank() || number.equals("unknown", ignoreCase = true)) return "Unknown Caller"
    return number
}

private fun formatTime(date: Date?): String {
    if (date == null) return ""
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
}

private fun formatFullTimestamp(date: Date?): String {
    if (date == null) return "Unknown time"
    return SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(date)
}

private fun formatExchangeTime(isoString: String): String {
    return runCatching {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = sdf.parse(isoString)
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(date!!)
    }.getOrElse { isoString }
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}

// ── Test Call Bottom Sheet ───────────────────────────────────────────────

private const val TWILIO_NUMBER_DISPLAY = "+1 620 659 6566"
private const val TWILIO_NUMBER_DIAL    = "tel:+16206596566"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestCallSheet(businessName: String, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context    = LocalContext.current
    val scope      = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = SurfaceColor,
        tonalElevation   = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Icon ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AccentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = AccentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Test Your UrVoice Assistant",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Call this number to test how your AI assistant handles customer calls",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(24.dp))

            // ── Number display ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E2E))
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = TWILIO_NUMBER_DISPLAY,
                    color = AccentLight,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Call Now button ───────────────────────────────────────────
            Button(
                onClick = {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_DIAL,
                        android.net.Uri.parse(TWILIO_NUMBER_DIAL)
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Call Now", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(10.dp))

            // ── Copy Number button ────────────────────────────────────────
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                            as android.content.ClipboardManager
                    clipboard.setPrimaryClip(
                        android.content.ClipData.newPlainText("UrVoice Number", TWILIO_NUMBER_DISPLAY)
                    )
                    android.widget.Toast.makeText(context, "Number copied", android.widget.Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF444444)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Copy Number", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(20.dp))

            // ── Note ──────────────────────────────────────────────────────
            Text(
                text = "Your AI will answer as $businessName and log the call to your dashboard",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

// ── Detail Bottom Sheet ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallSessionDetailSheet(session: CallSession, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceColor,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AccentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = AccentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    val hasName = !session.callerName.isNullOrBlank()
                    val titleText = when {
                        hasName -> session.callerName!!
                        !session.callerNumber.isNullOrBlank() && !session.callerNumber.equals("unknown", ignoreCase = true) -> session.callerNumber!!
                        else -> "Unknown Caller"
                    }
                    Text(
                        text = titleText,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (hasName && !session.callerNumber.isNullOrBlank() && !session.callerNumber.equals("unknown", ignoreCase = true)) {
                        Text(
                            text = session.callerNumber!!,
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = formatFullTimestamp(session.startTime),
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
                StatusChip(session.status)
            }

            HorizontalDivider(color = DividerColor)

            // Meta chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val count = session.totalExchanges ?: session.exchanges.size
                DetailChip("Exchanges", "$count")
                session.endTime?.let { DetailChip("Ended", formatTime(it)) }
            }

            // Conversation thread
            if (session.exchanges.isNotEmpty()) {
                Text(
                    text = "CONVERSATION",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
                session.exchanges.forEach { exchange ->
                    ExchangeBubblePair(exchange = exchange)
                }
            }

            // Close button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
            ) {
                Text("Close", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun ExchangeBubblePair(exchange: Exchange) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Caller bubble — left-aligned grey
        if (!exchange.transcript.isNullOrBlank()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
                        .background(Color(0xFF2A2A2A))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text("Caller", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = exchange.transcript!!,
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        // AI bubble — right-aligned purple
        if (!exchange.aiResponse.isNullOrBlank()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 4.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
                        .background(Color(0xFF3D3680))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text("UrVoice", color = AccentLight, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = exchange.aiResponse!!,
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        // Exchange timestamp
        if (!exchange.timestamp.isNullOrBlank()) {
            Text(
                text = formatExchangeTime(exchange.timestamp!!),
                color = Color(0xFF444444),
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DetailChip(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BackgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = label, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}



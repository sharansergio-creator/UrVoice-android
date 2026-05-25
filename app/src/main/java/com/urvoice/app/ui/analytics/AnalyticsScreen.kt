package com.urvoice.app.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

val ChartPurple = Color(0xFF7C3AED)
val ChartBlue = Color(0xFF3B82F6)
val ChartGreen = Color(0xFF10B981)
val ChartAmber = Color(0xFFF59E0B)
val ChartRed = Color(0xFFEF4444)
val ChartTeal = Color(0xFF14B8A6)
val DarkBg = Color(0xFF0A0A0F)
val DarkCard = Color(0xFF141420)

@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = ChartPurple)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading analytics...", color = Color.White.copy(alpha = 0.5f))
            }
        } else if (state.error != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("⚠️", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(state.error ?: "Error", color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { viewModel.loadAnalytics() },
                    colors = ButtonDefaults.buttonColors(containerColor = ChartPurple)) {
                    Text("Retry")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Analytics",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Last 30 days",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Summary stats row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Total Calls",
                        value = state.totalCalls.toString(),
                        icon = "📞",
                        color = ChartPurple
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Avg Duration",
                        value = formatDuration(state.avgDurationSeconds),
                        icon = "⏱️",
                        color = ChartBlue
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Daily calls — last 7 days
                AnalyticsCard(title = "📅 Calls This Week") {
                    BarChart(
                        data = state.dailyCalls.map { it.day to it.count },
                        color = ChartPurple
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Peak hours
                AnalyticsCard(title = "🕐 Peak Hours") {
                    val hourlyData = state.peakHours.map {
                        val label = when {
                            it.hour == 0 -> "12a"
                            it.hour < 12 -> "${it.hour}a"
                            it.hour == 12 -> "12p"
                            else -> "${it.hour - 12}p"
                        }
                        label to it.count
                    }
                    // Show only every 3 hours for readability
                    BarChart(
                        data = hourlyData.filterIndexed { index, _ -> index % 3 == 0 },
                        color = ChartBlue,
                        barWidth = 24.dp
                    )
                    val peakHour = state.peakHours.maxByOrNull { it.count }
                    if (peakHour != null && peakHour.count > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val peakLabel = when {
                            peakHour.hour == 0 -> "12 AM"
                            peakHour.hour < 12 -> "${peakHour.hour} AM"
                            peakHour.hour == 12 -> "12 PM"
                            else -> "${peakHour.hour - 12} PM"
                        }
                        Text(
                            "Peak: $peakLabel (${peakHour.count} calls)",
                            color = ChartBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Language breakdown + Caller categories side by side
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AnalyticsCard(
                        title = "🌐 Languages",
                        modifier = Modifier.weight(1f)
                    ) {
                        val colors = listOf(ChartPurple, ChartBlue, ChartGreen, ChartAmber, ChartTeal)
                        state.languageBreakdown.entries.toList().forEachIndexed { index, (lang, count) ->
                            LegendRow(
                                label = lang,
                                value = count.toString(),
                                color = colors[index % colors.size]
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        if (state.languageBreakdown.isEmpty()) {
                            Text("No data yet", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
                        }
                    }
                    AnalyticsCard(
                        title = "👥 Callers",
                        modifier = Modifier.weight(1f)
                    ) {
                        val catColors = mapOf(
                            "VIP" to ChartAmber,
                            "CUSTOMER" to ChartGreen,
                            "UNKNOWN" to Color.White.copy(alpha = 0.4f),
                            "BLOCKED" to ChartRed
                        )
                        state.callerCategories.entries.toList().forEach { (cat, count) ->
                            LegendRow(
                                label = cat.lowercase().replaceFirstChar { it.uppercase() },
                                value = count.toString(),
                                color = catColors[cat] ?: ChartPurple
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        if (state.callerCategories.isEmpty()) {
                            Text("No data yet", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Common questions
                AnalyticsCard(title = "🤖 Common Questions (AI Analysis)") {
                    if (state.commonQuestionsLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = ChartPurple,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyzing with Gemini...", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                        }
                    } else {
                        state.commonQuestions.forEachIndexed { index, question ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    "${index + 1}.",
                                    color = ChartPurple,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(20.dp)
                                )
                                Text(
                                    question,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = DarkCard
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = color, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun AnalyticsCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DarkCard
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun BarChart(
    data: List<Pair<String, Int>>,
    color: Color,
    barWidth: androidx.compose.ui.unit.Dp = 32.dp
) {
    val maxValue = data.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1
    val chartHeight = 80.dp
    val labelHeight = 20.dp
    val valueHeight = 16.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(chartHeight + labelHeight + valueHeight),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { (label, value) ->
            val barFraction = value.toFloat() / maxValue
            val barHeightDp = (barFraction * 80f).coerceAtLeast(if (value > 0) 3f else 1f).dp

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.height(chartHeight + labelHeight + valueHeight)
            ) {
                // Value label — fixed height slot at top
                Box(modifier = Modifier.height(valueHeight), contentAlignment = Alignment.BottomCenter) {
                    if (value > 0) {
                        Text(
                            value.toString(),
                            color = color,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Bar — grows from bottom
                Box(
                    modifier = Modifier
                        .width(barWidth)
                        .height(barHeightDp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (value > 0) color else color.copy(alpha = 0.12f))
                )
                // Label — fixed height slot at bottom
                Box(modifier = Modifier.height(labelHeight), contentAlignment = Alignment.TopCenter) {
                    Text(
                        label,
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatDuration(seconds: Int): String {
    return when {
        seconds < 60 -> "${seconds}s"
        else -> "${seconds / 60}m ${seconds % 60}s"
    }
}

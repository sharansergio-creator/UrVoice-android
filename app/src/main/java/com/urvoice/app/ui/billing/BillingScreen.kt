package com.urvoice.app.ui.billing

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import com.urvoice.app.BuildConfig
import com.urvoice.app.network.UrVoiceApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

// Colors
val BillingDark = Color(0xFF0A0A0F)
val BillingCard = Color(0xFF141420)
val BillingPurple = Color(0xFF7C3AED)
val BillingGold = Color(0xFFFFD700)
val BillingGreen = Color(0xFF10B981)

data class BillingState(
    val isLoading: Boolean = false,
    val currentPlan: String = "free",
    val selectedPlan: String = "basic",
    val error: String? = null,
    val success: String? = null
)

class BillingViewModel : ViewModel() {
    private val _state = MutableStateFlow(BillingState())
    val state: StateFlow<BillingState> = _state

    var pendingOrderId: String? = null
    var pendingPlan: String? = null

    fun selectPlan(plan: String) {
        _state.value = _state.value.copy(selectedPlan = plan)
    }

    fun createOrder(plan: String, onOrderCreated: (orderId: String, amount: Int) -> Unit) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)
                val uid = Firebase.auth.currentUser?.uid ?: return@launch

                val response = UrVoiceApi.service.createOrder(
                    mapOf("userId" to uid, "plan" to plan)
                )

                if (response.isSuccessful) {
                    val body = response.body() ?: return@launch
                    val orderId = body["orderId"] as? String ?: return@launch
                    val amount = (body["amount"] as? Double)?.toInt()
                        ?: (body["amount"] as? Long)?.toInt()
                        ?: 99900

                    pendingOrderId = orderId
                    pendingPlan = plan
                    _state.value = _state.value.copy(isLoading = false)
                    onOrderCreated(orderId, amount)
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to create order. Try again."
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }

    fun verifyPayment(paymentId: String, signature: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true)
                val uid = Firebase.auth.currentUser?.uid ?: return@launch
                val orderId = pendingOrderId ?: return@launch
                val plan = pendingPlan ?: return@launch

                val response = UrVoiceApi.service.verifyPayment(
                    mapOf(
                        "userId" to uid,
                        "plan" to plan,
                        "razorpayOrderId" to orderId,
                        "razorpayPaymentId" to paymentId,
                        "razorpaySignature" to signature
                    )
                )

                if (response.isSuccessful) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        currentPlan = plan,
                        success = "🎉 ${if (plan == "basic") "Basic" else "Premium"} plan activated!"
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Payment verification failed"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Verification error: ${e.message}"
                )
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, success = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    onBack: () -> Unit,
    viewModel: BillingViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val mainActivity = activity as? com.urvoice.app.MainActivity
    LaunchedEffect(Unit) {
        mainActivity?.setBillingViewModel(viewModel)
    }

    // Show success/error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.success, state.error) {
        state.success?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Upgrade Plan", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BillingDark)
            )
        },
        containerColor = BillingDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Header
            Text("Choose Your Plan", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                "Upgrade to unlock full potential of UrVoice",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Basic Plan Card
            PlanCard(
                title = "Basic",
                price = "₹999",
                period = "/month",
                color = BillingPurple,
                isSelected = state.selectedPlan == "basic",
                isCurrent = state.currentPlan == "basic",
                features = listOf(
                    "AI answers all calls 24/7",
                    "1 voice clone (English)",
                    "Call history & transcripts",
                    "Smart caller recognition",
                    "Up to 200 calls/month"
                ),
                onClick = { viewModel.selectPlan("basic") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Premium Plan Card
            PlanCard(
                title = "Premium",
                price = "₹2,499",
                period = "/month",
                color = BillingGold,
                isSelected = state.selectedPlan == "premium",
                isCurrent = state.currentPlan == "premium",
                badge = "MOST POPULAR",
                features = listOf(
                    "Everything in Basic",
                    "4 voice clones (EN/KN/HI/TA)",
                    "Analytics dashboard",
                    "AI common questions analysis",
                    "Unlimited calls",
                    "Priority support"
                ),
                onClick = { viewModel.selectPlan("premium") }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Subscribe button
            Button(
                onClick = {
                    viewModel.createOrder(state.selectedPlan) { orderId, amount ->
                        activity?.let { act ->
                            val checkout = com.razorpay.Checkout()
                            checkout.setKeyID(BuildConfig.RAZORPAY_KEY_ID)
                            checkout.setImage(com.urvoice.app.R.mipmap.ic_launcher)
                            val options = org.json.JSONObject().apply {
                                put("name", "UrVoice")
                                put("description", if (state.selectedPlan == "basic") "Basic Plan - ₹999/month" else "Premium Plan - ₹2499/month")
                                put("order_id", orderId)
                                put("currency", "INR")
                                put("amount", amount)
                                put("theme", org.json.JSONObject().apply {
                                    put("color", "#7C3AED")
                                })
                            }
                            checkout.open(act, options)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !state.isLoading && state.selectedPlan != state.currentPlan,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.selectedPlan == "premium") BillingGold else BillingPurple,
                    contentColor = if (state.selectedPlan == "premium") Color.Black else Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        "Subscribe to ${if (state.selectedPlan == "basic") "Basic" else "Premium"}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (state.currentPlan != "free") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Current plan: ${state.currentPlan.replaceFirstChar { it.uppercase() }}",
                    color = BillingGreen,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Payments secured by Razorpay. Cancel anytime.",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    period: String,
    color: Color,
    isSelected: Boolean,
    isCurrent: Boolean,
    features: List<String>,
    badge: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) color else color.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) color.copy(alpha = 0.1f) else BillingCard
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        if (isCurrent) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BillingGreen.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    "CURRENT",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = BillingGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    if (badge != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = color.copy(alpha = 0.2f)
                        ) {
                            Text(
                                badge,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = color,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(price, color = color, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(period, color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = color.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            features.forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(feature, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                }
            }
        }
    }
}

package com.urvoice.app.ui.onboarding

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val BackgroundColor = Color(0xFF0A0A0A)
private val AccentColor = Color(0xFF6C63FF)
private val AccentLight = Color(0xFF9C94FF)
private val SurfaceColor = Color(0xFF1A1A1A)
private val BorderColor = Color(0xFF2E2E2E)
private val ErrorColor = Color(0xFFFF5252)

@Composable
fun OnboardingScreen(
    onNavigateToDashboard: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val resendCountdown by viewModel.resendCountdown.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? Activity

    var phoneNumber by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var currentStep by remember { mutableStateOf(1) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is OnboardingUiState.OtpSent -> {
                currentStep = 2
                otp = ""
            }
            is OnboardingUiState.Success -> onNavigateToDashboard()
            else -> {}
        }
    }

    val isLoading = uiState is OnboardingUiState.Loading
    val errorMessage = (uiState as? OnboardingUiState.Error)?.message

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .statusBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LogoSection()

            Spacer(modifier = Modifier.height(56.dp))

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "OnboardingStepTransition"
            ) { step ->
                when (step) {
                    1 -> PhoneInputStep(
                        phoneNumber = phoneNumber,
                        onPhoneNumberChange = { phoneNumber = it },
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        onSendOtp = {
                            activity?.let { act ->
                                viewModel.sendOtp(phoneNumber, act)
                            }
                        }
                    )
                    else -> OtpVerificationStep(
                        otp = otp,
                        onOtpChange = { otp = it },
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        resendCountdown = resendCountdown,
                        onVerify = { viewModel.verifyOtp(otp) },
                        onResendOtp = {
                            activity?.let { act ->
                                viewModel.resendOtp(act, "+91$phoneNumber")
                            }
                        },
                        onBack = {
                            viewModel.resetState()
                            currentStep = 1
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LogoSection() {
    val logoBrush = Brush.horizontalGradient(
        colors = listOf(AccentLight, AccentColor)
    )

    Text(
        text = "UrVoice",
        style = TextStyle(
            brush = logoBrush,
            fontSize = 52.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1).sp
        )
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Your AI Phone Assistant",
        color = Color(0xFF888888),
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun PhoneInputStep(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onSendOtp: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Enter your phone number",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "We'll send you a one-time verification code",
            color = Color(0xFF666666),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        PhoneInputField(
            value = phoneNumber,
            onValueChange = { input ->
                if (input.length <= 10 && input.all { it.isDigit() }) {
                    onPhoneNumberChange(input)
                }
            },
            hasError = errorMessage != null
        )

        AnimatedVisibility(visible = errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = ErrorColor,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        PrimaryButton(
            text = "Send OTP",
            isLoading = isLoading,
            enabled = phoneNumber.length == 10 && !isLoading,
            onClick = onSendOtp
        )
    }
}

@Composable
private fun PhoneInputField(
    value: String,
    onValueChange: (String) -> Unit,
    hasError: Boolean
) {
    val borderColor = when {
        hasError -> ErrorColor
        value.isNotEmpty() -> AccentColor
        else -> BorderColor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "+91",
            color = AccentLight,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(20.dp)
                .padding(horizontal = 8.dp)
                .background(BorderColor)
        )

        Spacer(modifier = Modifier.width(12.dp))

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            cursorBrush = SolidColor(AccentColor),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = "10-digit phone number",
                        color = Color(0xFF555555),
                        fontSize = 16.sp
                    )
                }
                inner()
            }
        )
    }
}

@Composable
private fun OtpVerificationStep(
    otp: String,
    onOtpChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    resendCountdown: Int,
    onVerify: () -> Unit,
    onResendOtp: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Verify your number",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Enter the 6-digit code sent to your phone",
            color = Color(0xFF666666),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(36.dp))

        OtpInputRow(
            otpValue = otp,
            onOtpChange = onOtpChange
        )

        AnimatedVisibility(visible = errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = ErrorColor,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        PrimaryButton(
            text = "Verify",
            isLoading = isLoading,
            enabled = otp.length == 6 && !isLoading,
            onClick = onVerify
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (resendCountdown > 0) {
                Text(
                    text = "Resend OTP in ",
                    color = Color(0xFF666666),
                    fontSize = 13.sp
                )
                Text(
                    text = "${resendCountdown}s",
                    color = AccentLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    text = "Didn't receive the code? ",
                    color = Color(0xFF666666),
                    fontSize = 13.sp
                )
                Text(
                    text = "Resend OTP",
                    color = AccentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onResendOtp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onBack) {
            Text(
                text = "← Change number",
                color = Color(0xFF555555),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun OtpInputRow(
    otpValue: String,
    onOtpChange: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    Box(contentAlignment = Alignment.Center) {
        // Hidden field captures keyboard input
        BasicTextField(
            value = otpValue,
            onValueChange = { input ->
                if (input.length <= 6 && input.all { it.isDigit() }) {
                    onOtpChange(input)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier
                .size(1.dp)
                .focusRequester(focusRequester),
            cursorBrush = SolidColor(Color.Transparent)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(6) { index ->
                val char = otpValue.getOrNull(index)?.toString() ?: ""
                val isCurrent = index == otpValue.length && otpValue.length < 6
                val isFilled = index < otpValue.length

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isFilled || isCurrent) Color(0xFF16162A) else SurfaceColor
                        )
                        .border(
                            width = if (isCurrent) 2.dp else 1.dp,
                            color = when {
                                isCurrent -> AccentColor
                                isFilled -> AccentColor.copy(alpha = 0.5f)
                                else -> BorderColor
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { focusRequester.requestFocus() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = char,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentColor,
            disabledContainerColor = AccentColor.copy(alpha = 0.35f),
            contentColor = Color.White,
            disabledContentColor = Color.White.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

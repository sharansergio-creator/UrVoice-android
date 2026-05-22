package com.urvoice.app.ui.onboarding

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * ViewModel for handling onboarding flow with Firebase Phone Authentication
 */
class OnboardingViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Idle)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _resendCountdown = MutableStateFlow(0)
    val resendCountdown: StateFlow<Int> = _resendCountdown.asStateFlow()

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    /**
     * Sends OTP to the provided phone number
     * @param phoneNumber Phone number in format +91XXXXXXXXXX
     * @param activity Activity instance required for phone auth
     */
    fun sendOtp(phoneNumber: String, activity: Activity) {
        // Validate phone number format
        if (!isValidIndianPhoneNumber(phoneNumber)) {
            _uiState.value = OnboardingUiState.Error("Please enter a valid 10-digit phone number")
            return
        }
        
        val formattedNumber = formatPhoneNumber(phoneNumber)
        _uiState.value = OnboardingUiState.Loading("Sending OTP...")

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto-verification happened (instant or via SMS auto-retrieval)
                viewModelScope.launch {
                    try {
                        signInWithCredential(credential)
                    } catch (e: Exception) {
                        _uiState.value = OnboardingUiState.Error(
                            e.message ?: "Auto-verification failed"
                        )
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _uiState.value = OnboardingUiState.Error(
                    e.message ?: "Verification failed. Please try again."
                )
            }

            override fun onCodeSent(
                vId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                verificationId = vId
                resendToken = token
                _uiState.value = OnboardingUiState.OtpSent(formattedNumber)
                startResendCountdown()
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    /**
     * Verifies the OTP entered by the user
     * @param otp The 6-digit OTP code
     */
    fun verifyOtp(otp: String) {
        if (otp.length != 6) {
            _uiState.value = OnboardingUiState.Error("Please enter a valid 6-digit OTP")
            return
        }

        val vId = verificationId
        if (vId == null) {
            _uiState.value = OnboardingUiState.Error("Verification ID not found. Please resend OTP.")
            return
        }

        _uiState.value = OnboardingUiState.Loading("Verifying OTP...")

        viewModelScope.launch {
            try {
                val credential = PhoneAuthProvider.getCredential(vId, otp)
                signInWithCredential(credential)
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(
                    e.message ?: "OTP verification failed"
                )
            }
        }
    }

    /**
     * Resends OTP to the same phone number
     */
    fun resendOtp(activity: Activity, phoneNumber: String) {
        val token = resendToken
        if (token == null) {
            sendOtp(phoneNumber, activity)
            return
        }

        _uiState.value = OnboardingUiState.Loading("Resending OTP...")

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                viewModelScope.launch {
                    try {
                        signInWithCredential(credential)
                    } catch (e: Exception) {
                        _uiState.value = OnboardingUiState.Error(
                            e.message ?: "Auto-verification failed"
                        )
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _uiState.value = OnboardingUiState.Error(
                    e.message ?: "Failed to resend OTP"
                )
            }

            override fun onCodeSent(
                vId: String,
                newToken: PhoneAuthProvider.ForceResendingToken
            ) {
                verificationId = vId
                resendToken = newToken
                _uiState.value = OnboardingUiState.OtpSent(phoneNumber)
                startResendCountdown()
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .setForceResendingToken(token)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    /**
     * Signs in with the phone credential
     */
    private suspend fun signInWithCredential(credential: PhoneAuthCredential) {
        try {
            val result = auth.signInWithCredential(credential).await()
            if (result.user != null) {
                _uiState.value = OnboardingUiState.Success(result.user!!.uid)
            } else {
                _uiState.value = OnboardingUiState.Error("Authentication failed")
            }
        } catch (e: Exception) {
            _uiState.value = OnboardingUiState.Error(
                e.message ?: "Sign in failed"
            )
        }
    }

    /**
     * Validates Indian phone number (10 digits)
     */
    private fun isValidIndianPhoneNumber(phoneNumber: String): Boolean {
        val cleaned = phoneNumber.replace("+91", "").replace(" ", "").replace("-", "")
        return cleaned.matches(Regex("^[6-9]\\d{9}$"))
    }

    /**
     * Formats phone number to +91XXXXXXXXXX
     */
    private fun formatPhoneNumber(phoneNumber: String): String {
        val cleaned = phoneNumber.replace("+91", "").replace(" ", "").replace("-", "")
        return "+91$cleaned"
    }

    /**
     * Starts a 60-second countdown for the resend OTP button
     */
    private fun startResendCountdown() {
        viewModelScope.launch {
            for (i in 60 downTo 0) {
                _resendCountdown.value = i
                if (i > 0) delay(1000L)
            }
        }
    }

    /**
     * Resets UI state to Idle
     */
    fun resetState() {
        _uiState.value = OnboardingUiState.Idle
        _resendCountdown.value = 0
    }
}

/**
 * UI State sealed class for onboarding flow
 */
sealed class OnboardingUiState {
    object Idle : OnboardingUiState()
    data class Loading(val message: String) : OnboardingUiState()
    data class OtpSent(val phoneNumber: String) : OnboardingUiState()
    data class Success(val userId: String) : OnboardingUiState()
    data class Error(val message: String) : OnboardingUiState()
}

# UrVoice Android 🎙️

> AI phone assistant for Indian small businesses — answers calls 24/7 in the owner's cloned voice

[![Android](https://img.shields.io/badge/Platform-Android-green)](https://github.com/sharansergio-creator/UrVoice-android)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue)](https://github.com/sharansergio-creator/UrVoice-android)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-orange)](https://github.com/sharansergio-creator/UrVoice-android)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

> 📡 Backend repo: [UrVoice (FastAPI)](https://github.com/sharansergio-creator/UrVoice)

## What is UrVoice?

UrVoice is an Android app that lets Indian small businesses — resorts, restaurants, clinics, shops — handle incoming calls 24/7 using an AI assistant that speaks in the owner's own cloned voice. Callers have a natural conversation with an AI that knows the business inside out.

**Live demo:** Call **+1 (620) 659-6566** to speak with the AI assistant for Vishmaa Resorts, Coorg.

---

## Screens

| Screen | Description |
|---|---|
| SplashScreen | Animated logo with radial glow and pulse effect |
| OnboardingScreen | Phone OTP auth — +91 validation, 6-digit OTP, 60s resend countdown |
| BusinessSetupScreen | Multi-section business form with auto-fill from Google Business Profile or website URL |
| DashboardScreen | 5-tab hub — Home, Analytics, Contacts, AI Voice, Settings; live call stats with session list |
| AiVoiceSetupScreen | 6-step voice cloning — language → script → environment check → record → process → confirm |
| ContactsScreen | VIP / Customer / Blocked tabs; device contacts integration; long-press type management |
| AnalyticsScreen | Premium: peak hours chart, 7-day trend, language breakdown, common questions |
| BillingScreen | Free / Basic ₹999 / Premium ₹2,499 plans with Razorpay payment flow |
| CallHandlingScreen | AI answering toggle, after-hours mode, call duration threshold, block list |
| SettingsScreen | Business profile, plan upgrade, call handling, voice clone, sign out |

---

## Key Features

- **Voice Cloning** — Owner records 35–60s audio. ElevenLabs clones their voice per language. Every caller hears the owner, not a robot.
- **Multilingual** — English, Kannada, Hindi, Tamil. Original and phonetic scripts per language for accurate pronunciation.
- **Real-time Call Sessions** — Firestore snapshot listeners stream live call data. Full transcript (caller + AI per exchange) stored per session. Categories: CUSTOMER, SPAM, BLOCKED, AFTER_HOURS.
- **Smart Contact Management** — Reads device contacts via ContentProvider. VIP/Customer/Blocked status synced to Firestore. Returning callers greeted by name.
- **Analytics Dashboard** — Premium feature: peak hours bar chart, 7-day call trend, language distribution, common questions via backend NLP.
- **Subscription Billing** — Razorpay order creation and signature verification. Firestore `subscriptions/{uid}` gates Premium features.
- **Business Auto-fill** — Backend scrapes Google Business Profile or website URL to pre-fill business context and 5 Q&A pairs.
- **FCM Push Notifications** — CALL_STARTED (high priority) and CALL_ENDED (auto-cancel) channels with token auto-refresh.
- **Firebase Phone Auth** — SMS OTP with session persistence and auto-login on restart.
- **Test Call FAB** — One-tap to dial the live Twilio number and verify the full AI pipeline end-to-end.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM with StateFlow |
| Auth | Firebase Phone Auth (OTP) |
| Database | Firestore (real-time snapshot listeners) |
| Push | Firebase Cloud Messaging |
| HTTP | Retrofit 2 + OkHttp (120s timeout for voice cloning) |
| Billing | Razorpay Checkout SDK |
| Min SDK | Android 8.0 (API 26) |
| Backend | Railway-hosted FastAPI — [UrVoice backend](https://github.com/sharansergio-creator/UrVoice) |

---

## Architecture

MVVM with reactive StateFlow. Every screen has a dedicated ViewModel exposing `StateFlow<UiState>` consumed via `collectAsState()`. Sealed UI state classes (`Idle / Loading / Success / Error`) enforce type-safe state transitions. Composables are intentionally thin — all business logic lives in ViewModels.

No local Room database. Firestore is the single source of truth with real-time snapshot listeners. Navigation is enum-based (`Screen` enum + `mutableStateOf`) handled in `MainActivity`.
MainActivity (enum router)
├── SplashScreen
├── OnboardingScreen → OnboardingViewModel
├── BusinessSetupScreen → BusinessSetupViewModel
└── DashboardScreen (5 tabs) → DashboardViewModel
├── AnalyticsScreen → AnalyticsViewModel
├── ContactsScreen → ContactsViewModel
├── AiVoiceSetupScreen → AiVoiceSetupViewModel
├── BillingScreen → BillingViewModel
└── CallHandlingScreen

---

## Firestore Schema
users/{uid}
fcmToken, twilioNumber
voiceClones: { en, kn, hi, ta }
business_context/{uid}
businessName, businessType, location, phone, email
about, services, pricing, accommodations
businessHours: [{day, enabled, openTime, closeTime}]
qaAnswers: { 5 custom Q&A pairs }
call_sessions/{sessionId}
userId, callerNumber, callerName, category
startTime, endTime, status, totalExchanges
exchanges: [{transcript, aiResponse, language, timestamp}]
contact_permissions/{uid}/contacts/{phoneNumber}
name, type (VIP/CUSTOMER/BLOCKED/UNKNOWN)
firstCall, lastCall, totalCalls
subscriptions/{uid}
plan (FREE/BASIC/PREMIUM), validUntil, razorpayOrderId

---

## Getting Started

```bash
git clone https://github.com/sharansergio-creator/UrVoice-android
# Open in Android Studio
# Add google-services.json from your Firebase Console
# Add to gradle.properties:
#   RAZORPAY_KEY_ID=your_key
#   GEMINI_API_KEY=your_key
# Run on device (min Android 8.0)
```

> Physical device recommended for FCM push notifications and audio recording.

---

## Design

Dark-only theme. Custom color scheme: `#6C63FF` purple primary, `#0A0A0A` background, `#FFD700` gold for Premium badge. All screens built with Jetpack Compose + Material 3.

---

## Built By

**Sharan S** — BCA (Data Science), Srinivas Institute of Technology, Mangalore

[GitHub](https://github.com/sharansergio-creator) · [LinkedIn](https://linkedin.com/in/sharansergio)

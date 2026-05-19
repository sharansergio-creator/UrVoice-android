package com.urvoice.app.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * User data model for UrVoice app
 * Represents a business user with their profile and settings
 */
data class User(
    @DocumentId
    val userId: String? = null,
    
    @PropertyName("phone_number")
    val phoneNumber: String? = null,
    
    @PropertyName("business_name")
    val businessName: String? = null,
    
    @PropertyName("business_type")
    val businessType: String? = null,
    
    @PropertyName("business_hours")
    val businessHours: String? = null,
    
    @PropertyName("website")
    val website: String? = null,
    
    @PropertyName("gbp_url")
    val gbpUrl: String? = null,
    
    @PropertyName("twilio_number")
    val twilioNumber: String? = null,
    
    @PropertyName("created_at")
    @ServerTimestamp
    val createdAt: Date? = null
) {
    // No-argument constructor required for Firestore deserialization
    constructor() : this(
        userId = null,
        phoneNumber = null,
        businessName = null,
        businessType = null,
        businessHours = null,
        website = null,
        gbpUrl = null,
        twilioNumber = null,
        createdAt = null
    )
}

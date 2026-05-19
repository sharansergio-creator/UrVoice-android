package com.urvoice.app.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Contact permission model for managing caller access levels
 * Allows categorization of contacts as personal, VIP, or unknown
 */
data class ContactPermission(
    @PropertyName("phone_number")
    val phoneNumber: String? = null,
    
    @PropertyName("name")
    val name: String? = null,
    
    @PropertyName("type")
    val type: ContactType? = ContactType.UNKNOWN
) {
    // No-argument constructor required for Firestore deserialization
    constructor() : this(
        phoneNumber = null,
        name = null,
        type = ContactType.UNKNOWN
    )
}

/**
 * Contact type enum for categorizing contacts
 */
enum class ContactType {
    @PropertyName("PERSONAL")
    PERSONAL,
    
    @PropertyName("VIP")
    VIP,
    
    @PropertyName("UNKNOWN")
    UNKNOWN
}

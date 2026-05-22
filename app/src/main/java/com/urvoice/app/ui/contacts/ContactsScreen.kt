package com.urvoice.app.ui.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ── Design tokens (matches Dashboard palette) ──────────────────────────────────
private val BgColor       = Color(0xFF0A0A0A)
private val AccentColor   = Color(0xFF6C63FF)
private val SurfaceColor  = Color(0xFF1A1A1A)
private val DividerColor  = Color(0xFF1E1E1E)
private val TextSecondary = Color(0xFF888888)
private val VipColor      = Color(0xFFFFD700)
private val CustomerColor = Color(0xFF4CAF50)
private val BlockedColor  = Color(0xFFFF5252)

private fun typeColor(type: String) = when (type.uppercase()) {
    "VIP"      -> VipColor
    "CUSTOMER" -> CustomerColor
    "BLOCKED"  -> BlockedColor
    else       -> TextSecondary
}

private fun typeAvatarBg(type: String) = when (type.uppercase()) {
    "VIP"      -> Color(0xFF2C2800)
    "CUSTOMER" -> Color(0xFF122012)
    "BLOCKED"  -> Color(0xFF2C1212)
    else       -> Color(0xFF222222)
}

private fun typeLabel(type: String) = when (type.uppercase()) {
    "VIP"      -> "VIP"
    "CUSTOMER" -> "Customer"
    "BLOCKED"  -> "Blocked"
    else       -> "Unknown"
}

private data class PendingContact(val name: String, val phoneNumber: String)

private data class ContactTab(val label: String, val icon: ImageVector, val color: Color)

private val contactTabs = listOf(
    ContactTab("All",       Icons.Default.List,   Color.White),
    ContactTab("VIP",       Icons.Default.Star,   VipColor),
    ContactTab("Customers", Icons.Default.Person, CustomerColor),
    ContactTab("Blocked",   Icons.Default.Block,  BlockedColor)
)

// ── Top bar (used by DashboardScreen) ─────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsTopBar() {
    TopAppBar(
        title = {
            Text(
                text       = "Contacts",
                color      = Color.White,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = BgColor)
    )
}

// ── Main contacts content ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsContent(
    paddingValues: PaddingValues,
    viewModel: ContactsViewModel = viewModel()
) {
    val uiState         by viewModel.uiState.collectAsState()
    var selectedTab     by remember { mutableIntStateOf(0) }
    var searchQuery     by remember { mutableStateOf("") }
    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    var pendingContact  by remember { mutableStateOf<PendingContact?>(null) }

    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val picked = withContext(Dispatchers.IO) { readContactFromUri(context, uri) }
            pendingContact = picked
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) contactPickerLauncher.launch(null)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(paddingValues)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Tab row ───────────────────────────────────────────────────────
            val allCount       = uiState.vip.size + uiState.customers.size + uiState.blocked.size
            val tabCounts      = listOf(allCount, uiState.vip.size, uiState.customers.size, uiState.blocked.size)
            val indicatorColor = contactTabs[selectedTab].color

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = SurfaceColor,
                contentColor     = Color.White,
                indicator        = { tabPositions ->
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height   = 2.dp,
                        color    = indicatorColor
                    )
                },
                divider = { HorizontalDivider(color = DividerColor) }
            ) {
                contactTabs.forEachIndexed { index, tab ->
                    val selected = index == selectedTab
                    Tab(
                        selected               = selected,
                        onClick                = { selectedTab = index; searchQuery = "" },
                        selectedContentColor   = tab.color,
                        unselectedContentColor = TextSecondary
                    ) {
                        Column(
                            modifier            = Modifier.padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(tab.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text       = tab.label,
                                    fontSize   = 12.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                                val count = tabCounts[index]
                                if (count > 0) {
                                    Spacer(Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                if (selected) tab.color.copy(alpha = 0.15f)
                                                else Color(0xFF2A2A2A)
                                            )
                                            .padding(horizontal = 5.dp, vertical = 1.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text       = "$count",
                                            fontSize   = 10.sp,
                                            color      = if (selected) tab.color else TextSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Search bar ────────────────────────────────────────────────────
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = { searchQuery = it },
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                placeholder   = { Text("Search contacts…", color = TextSecondary, fontSize = 14.sp) },
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor   = SurfaceColor,
                    unfocusedContainerColor = SurfaceColor,
                    focusedBorderColor      = AccentColor,
                    unfocusedBorderColor    = DividerColor,
                    focusedTextColor        = Color.White,
                    unfocusedTextColor      = Color.White,
                    cursorColor             = AccentColor
                ),
                leadingIcon   = {
                    Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                },
                trailingIcon  = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )

            // ── Contact list ──────────────────────────────────────────────────
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color       = AccentColor,
                            strokeWidth = 3.dp,
                            modifier    = Modifier.size(40.dp)
                        )
                    }
                }

                uiState.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(uiState.error!!, color = BlockedColor, modifier = Modifier.padding(32.dp))
                    }
                }

                else -> {
                    val allContacts = (uiState.vip + uiState.customers + uiState.blocked)
                        .sortedBy { it.name?.lowercase() ?: it.phoneNumber }
                    val baseList = when (selectedTab) {
                        1    -> uiState.vip
                        2    -> uiState.customers
                        3    -> uiState.blocked
                        else -> allContacts
                    }
                    val filtered = if (searchQuery.isBlank()) baseList else {
                        val q = searchQuery.lowercase()
                        baseList.filter {
                            it.phoneNumber.contains(q) || it.name?.lowercase()?.contains(q) == true
                        }
                    }
                    if (filtered.isEmpty()) {
                        ContactsEmptyState(
                            tabIndex   = selectedTab,
                            isFiltered = searchQuery.isNotBlank()
                        )
                    } else {
                        LazyColumn(
                            modifier       = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 88.dp)
                        ) {
                            items(filtered, key = { it.phoneNumber }) { c ->
                                ContactItem(contact = c, onLongPress = { selectedContact = c })
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                                    color    = DividerColor
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── FAB ───────────────────────────────────────────────────────────────
        FloatingActionButton(
            onClick = {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) contactPickerLauncher.launch(null)
                else permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            },
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            containerColor = AccentColor,
            contentColor   = Color.White,
            shape          = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add contact")
        }
    }

    // ── Bottom sheets ─────────────────────────────────────────────────────────
    selectedContact?.let { contact ->
        ContactOptionsSheet(
            contact          = contact,
            onDismiss        = { selectedContact = null },
            onMoveToVip      = { viewModel.updateContactType(contact.phoneNumber, "VIP");      selectedContact = null },
            onMoveToCustomer = { viewModel.updateContactType(contact.phoneNumber, "CUSTOMER"); selectedContact = null },
            onBlock          = { viewModel.updateContactType(contact.phoneNumber, "BLOCKED");  selectedContact = null },
            onDelete         = { viewModel.deleteContact(contact.phoneNumber);                 selectedContact = null }
        )
    }

    pendingContact?.let { pending ->
        AddConfirmSheet(
            pending   = pending,
            onDismiss = { pendingContact = null },
            onConfirm = { name, number, type ->
                viewModel.addContact(number, name, type)
                pendingContact = null
            }
        )
    }
}

// ── Empty state ────────────────────────────────────────────────────────────────
@Composable
private fun ContactsEmptyState(tabIndex: Int, isFiltered: Boolean) {
    if (isFiltered) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No results found", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Try a different search term.", color = TextSecondary, fontSize = 14.sp)
            }
        }
        return
    }

    val (icon, title, subtitle) = when (tabIndex) {
        1    -> Triple(Icons.Default.Star,   "No VIP contacts",    "Add important people who should always reach you directly.")
        2    -> Triple(Icons.Default.Person, "No customers yet",   "They'll appear here after their first call.")
        3    -> Triple(Icons.Default.Block,  "No blocked numbers", "")
        else -> Triple(Icons.Default.List,   "No contacts yet",    "Tap + to import from your phonebook.")
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(horizontal = 48.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(SurfaceColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, modifier = Modifier.size(36.dp), tint = TextSecondary)
            }
            Spacer(Modifier.height(16.dp))
            Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text      = subtitle,
                    color     = TextSecondary,
                    fontSize  = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Single contact row ─────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactItem(contact: Contact, onLongPress: () -> Unit) {
    val displayName  = if (!contact.name.isNullOrBlank()) contact.name!! else contact.phoneNumber
    val avatarLetter = displayName.first().uppercaseChar().toString()
    val color        = typeColor(contact.type)
    val avatarBg     = typeAvatarBg(contact.type)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier         = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(avatarBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = avatarLetter,
                color      = color,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(12.dp))

        // Name + subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = displayName,
                color      = Color.White,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            if (!contact.name.isNullOrBlank()) {
                Text(
                    text     = contact.phoneNumber,
                    color    = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
        }

        // Call count
        if (contact.totalCalls > 0) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text       = "${contact.totalCalls}",
                    color      = color,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text     = if (contact.totalCalls == 1) "call" else "calls",
                    color    = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ── Contact options bottom sheet ───────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ContactOptionsSheet(
    contact:          Contact,
    onDismiss:        () -> Unit,
    onMoveToVip:      () -> Unit,
    onMoveToCustomer: () -> Unit,
    onBlock:          () -> Unit,
    onDelete:         () -> Unit
) {
    val sheetState  = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val displayName = if (!contact.name.isNullOrBlank()) contact.name!! else contact.phoneNumber

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = SurfaceColor,
        tonalElevation   = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Contact header
            Row(
                modifier          = Modifier.padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier         = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(typeAvatarBg(contact.type)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = displayName.first().uppercaseChar().toString(),
                        color      = typeColor(contact.type),
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text       = displayName,
                        color      = Color.White,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(text = contact.phoneNumber, color = TextSecondary, fontSize = 13.sp)
                    Text(
                        text       = typeLabel(contact.type),
                        color      = typeColor(contact.type),
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            HorizontalDivider(color = DividerColor)
            Spacer(Modifier.height(8.dp))

            // Actions — only show options different from current type
            if (contact.type != "VIP") {
                SheetActionRow(icon = Icons.Default.Star, label = "Move to VIP", color = VipColor, onClick = onMoveToVip)
            }
            if (contact.type != "CUSTOMER") {
                SheetActionRow(icon = Icons.Default.Person, label = "Move to Customer", color = CustomerColor, onClick = onMoveToCustomer)
            }
            if (contact.type != "BLOCKED") {
                SheetActionRow(icon = Icons.Default.Block, label = "Block this number", color = BlockedColor, onClick = onBlock)
            }

            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 4.dp))

            SheetActionRow(icon = Icons.Default.Delete, label = "Delete contact", color = BlockedColor, onClick = onDelete)
        }
    }
}

@Composable
private fun SheetActionRow(
    icon:    ImageVector,
    label:   String,
    color:   Color,
    onClick: () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = color,
            modifier           = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text       = label,
            color      = color,
            fontSize   = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Confirm import bottom sheet ───────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddConfirmSheet(
    pending:   PendingContact,
    onDismiss: () -> Unit,
    onConfirm: (name: String, number: String, type: String) -> Unit
) {
    val sheetState   = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var type         by remember { mutableStateOf("CUSTOMER") }
    var typeExpanded by remember { mutableStateOf(false) }

    val typeOptions  = listOf("CUSTOMER", "VIP", "BLOCKED")
    val avatarLetter = (pending.name.firstOrNull() ?: pending.phoneNumber.firstOrNull() ?: '?')
        .uppercaseChar().toString()

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor   = Color(0xFF1E1E1E),
        unfocusedContainerColor = Color(0xFF1E1E1E),
        focusedBorderColor      = AccentColor,
        unfocusedBorderColor    = DividerColor,
        focusedTextColor        = Color.White,
        unfocusedTextColor      = Color.White,
        cursorColor             = AccentColor
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = SurfaceColor,
        tonalElevation   = 0.dp
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text       = "Add to Contacts",
                color      = Color.White,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Contact preview
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141414), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier         = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(typeAvatarBg(type)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = avatarLetter,
                        color      = typeColor(type),
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    if (pending.name.isNotBlank()) {
                        Text(
                            text       = pending.name,
                            color      = Color.White,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text     = pending.phoneNumber,
                        color    = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            // Category dropdown
            ExposedDropdownMenuBox(
                expanded         = typeExpanded,
                onExpandedChange = { typeExpanded = it }
            ) {
                OutlinedTextField(
                    value         = typeLabel(type),
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text("Category", color = TextSecondary) },
                    modifier      = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    shape         = RoundedCornerShape(10.dp),
                    colors        = fieldColors,
                    trailingIcon  = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                    }
                )
                ExposedDropdownMenu(
                    expanded         = typeExpanded,
                    onDismissRequest = { typeExpanded = false },
                    containerColor   = Color(0xFF252525)
                ) {
                    typeOptions.forEach { opt ->
                        DropdownMenuItem(
                            text    = {
                                Text(
                                    text       = typeLabel(opt),
                                    color      = typeColor(opt),
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            onClick = { type = opt; typeExpanded = false }
                        )
                    }
                }
            }

            // Action buttons
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onDismiss,
                    colors  = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = { onConfirm(pending.name, pending.phoneNumber, type) },
                    colors  = ButtonDefaults.textButtonColors(contentColor = AccentColor)
                ) {
                    Text("Add Contact", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Read name + first phone number from a contact URI ─────────────────────────
private fun readContactFromUri(context: Context, uri: Uri): PendingContact? {
    var contactId:   String? = null
    var displayName: String? = null
    context.contentResolver.query(
        uri,
        arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
        null, null, null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            contactId   = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
            displayName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY))
        }
    }
    if (contactId == null) return null

    var phone: String? = null
    context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
        arrayOf(contactId),
        "${ContactsContract.CommonDataKinds.Phone.IS_PRIMARY} DESC"
    )?.use { cursor ->
        if (cursor.moveToFirst()) phone = cursor.getString(0)
    }
    if (phone == null) return null

    return PendingContact(name = displayName.orEmpty(), phoneNumber = phone!!)
}

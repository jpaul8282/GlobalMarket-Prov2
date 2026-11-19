package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.PayoutWireEntity
import com.example.data.StripeConnectionHealth
import com.example.data.StripePayoutGateway
import com.example.data.StripePayoutItem
import com.example.ui.theme.CleanBorder
import com.example.ui.theme.CleanBorderSubtle
import com.example.ui.theme.CleanDanger
import com.example.ui.theme.CleanDangerContainer
import com.example.ui.theme.CleanOrange
import com.example.ui.theme.CleanOrangeContainer
import com.example.ui.theme.CleanOrangeText
import com.example.ui.theme.CleanPrimary
import com.example.ui.theme.CleanPrimaryContainer
import com.example.ui.theme.CleanSecondary
import com.example.ui.theme.CleanSuccess
import com.example.ui.theme.CleanSuccessContainer
import com.example.ui.theme.CleanSurface
import com.example.ui.theme.CleanSurfaceSecondary
import com.example.ui.theme.CleanSurfaceVariant
import com.example.ui.theme.CleanTertiary
import com.example.ui.theme.CleanTertiaryContainer
import com.example.ui.theme.CleanTextMuted
import com.example.ui.theme.CleanTextPrimary
import com.example.ui.theme.CleanTextSecondary
import com.example.ui.theme.StripeIndigo
import com.example.ui.theme.StripeIndigoContainer
import com.example.ui.theme.StripeIndigoDark
import com.example.ui.theme.StripeOnIndigo
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dedicated Stripe Connect & Live Payouts Hub screen.
 * Surfaces Stripe Treasury balances, API key diagnostics, external bank/card destinations,
 * live v1/payouts execution, and Room-persisted Stripe payout audit logs.
 */
@Composable
fun StripeDashboardScreen(
    viewModel: GlobalMarketViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val stripeState by viewModel.stripePayoutState.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val payoutWireHistory by viewModel.payoutWireHistory.collectAsStateWithLifecycle()
    val firestorePayoutStreamState by viewModel.firestorePayoutStreamState.collectAsStateWithLifecycle()
    val currencyFormatter = remember { CurrencyFormatter.DEFAULT }

    var payoutAmountInput by remember { mutableStateOf("250.00") }
    var payoutCurrency by remember { mutableStateOf(selectedCurrency) }
    var isCurrencyMenuExpanded by remember { mutableStateOf(false) }
    var payoutDescription by remember { mutableStateOf("GlobalMarket Pro Stripe Settlement") }
    val parsedAmount = payoutAmountInput.toDoubleOrNull() ?: 0.0

    val stripeRoomRecords = remember(payoutWireHistory) {
        payoutWireHistory.filter { it.method.equals("STRIPE_PAYOUT", ignoreCase = true) }
    }

    LaunchedEffect(stripeState.isKeyConfigured) {
        if (stripeState.isKeyConfigured &&
            stripeState.lastSyncedEpochMs == null &&
            !stripeState.isCheckingConnection
        ) {
            viewModel.triggerVerifyStripeConnection()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("stripe_dashboard_screen")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp)
    ) {
        // 1. TOP HEADER BAR
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CleanSurfaceVariant)
                            .border(BorderStroke(1.dp, CleanBorder), CircleShape)
                            .testTag("stripe_ui_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = StripeIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Stripe Connect & Payouts",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "Live v1/balance • v1/account • v1/payouts",
                            fontSize = 11.sp,
                            color = CleanTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.triggerVerifyStripeConnection() },
                    enabled = !stripeState.isCheckingConnection,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StripeIndigo.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("stripe_ui_sync_button")
                ) {
                    if (stripeState.isCheckingConnection) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = StripeIndigo
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Sync Stripe API",
                            tint = StripeIndigo,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (stripeState.isCheckingConnection) "Syncing" else "Sync API",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StripeIndigo
                    )
                }
            }
        }

        // 1B. REAL-TIME FIRESTORE PAYOUT SYNC STATUS
        item {
            FirestoreLiveSyncBadgeStrip(
                streamState = firestorePayoutStreamState,
                onRetry = { viewModel.retryFirestorePayoutListener() }
            )
        }

        // 2. HERO STRIPE TREASURY & ACCOUNT CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stripe_ui_hero_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(StripeIndigo, StripeIndigoDark)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(StripeOnIndigo.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = StripeOnIndigo,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "STRIPE TREASURY",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StripeOnIndigo.copy(alpha = 0.85f),
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = stripeState.accountBusinessName ?: "Merchant Treasury Account",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = StripeOnIndigo,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = StripeOnIndigo.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, StripeOnIndigo.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .testTag("stripe_ui_env_badge")
                                    .semantics(mergeDescendants = true) {}
                            ) {
                                Text(
                                    text = stripeState.environmentLabel,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StripeOnIndigo,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Primary Available & Pending Balance
                        val primaryAvail = stripeState.availableBalances.firstOrNull()
                        val primaryPending = stripeState.pendingBalances.firstOrNull()
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (primaryAvail != null) {
                                    currencyFormatter.format(primaryAvail.majorAmount, primaryAvail.currencyCode)
                                } else {
                                    "$0.00"
                                },
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = StripeOnIndigo,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = buildString {
                                    append("Health: ${stripeState.connectionHealth.label}")
                                    if (primaryPending != null) {
                                        append(" • Pending: ${currencyFormatter.format(primaryPending.majorAmount, primaryPending.currencyCode)}")
                                    }
                                    if (stripeState.lastLatencyMs != null) {
                                        append(" • ${stripeState.lastLatencyMs}ms")
                                    }
                                },
                                fontSize = 11.sp,
                                color = StripeOnIndigo.copy(alpha = 0.85f)
                            )
                        }

                        HorizontalDivider(color = StripeOnIndigo.copy(alpha = 0.2f))

                        // Account Metadata & Quick Shortcuts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.closeStripeUi()
                                    viewModel.openOwnerPayoutSection()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("stripe_ui_open_vault_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, StripeOnIndigo.copy(alpha = 0.55f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StripeOnIndigo)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Payout Vault", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.closeStripeUi()
                                    viewModel.openPayoutHistoryScreen()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("stripe_ui_open_ledger_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, StripeOnIndigo.copy(alpha = 0.55f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StripeOnIndigo)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ledger (${stripeRoomRecords.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3. API KEY & DIAGNOSTICS STATUS CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stripe_ui_credentials_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CleanSurface),
                border = BorderStroke(1.dp, CleanBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STRIPE CREDENTIALS & API TELEMETRY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextMuted,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "API v${StripePayoutGateway.STRIPE_API_VERSION}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = StripeIndigo,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    StripeKeyStatusRow(
                        label = "STRIPE_PUBLISHABLE_KEY",
                        maskedValue = stripeState.maskedPublishableKeyPreview,
                        isHealthy = stripeState.isPublishableKeyConfigured
                    )
                    StripeKeyStatusRow(
                        label = "STRIPE_SECRET_KEY",
                        maskedValue = stripeState.maskedKeyPreview,
                        isHealthy = stripeState.isKeyConfigured
                    )

                    if (!stripeState.connectedAccountId.isNullOrBlank() || !stripeState.accountId.isNullOrBlank()) {
                        val acct = stripeState.accountId ?: stripeState.connectedAccountId.orEmpty()
                        StripeKeyStatusRow(
                            label = "STRIPE_ACCOUNT",
                            maskedValue = acct,
                            isHealthy = true
                        )
                    }

                    if (!stripeState.isKeyConfigured) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CleanOrangeContainer.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, CleanOrange.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("stripe_ui_missing_key_callout")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = CleanOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (stripeState.isPublishableKeyMistakenForSecret) {
                                        "STRIPE_SECRET_KEY holds a Publishable Key (pk_...). Move pk_... to STRIPE_PUBLISHABLE_KEY and set your Secret Key (sk_live_... or sk_test_...) in the AI Studio Secrets panel."
                                    } else {
                                        "Configure STRIPE_PUBLISHABLE_KEY and STRIPE_SECRET_KEY in the AI Studio Secrets panel to enable live Stripe Treasury sync and payouts."
                                    },
                                    fontSize = 11.sp,
                                    color = CleanTextPrimary,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. INTERACTIVE LIVE STRIPE PAYOUT COMPOSER
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stripe_ui_payout_composer"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CleanSurface),
                border = BorderStroke(1.5.dp, StripeIndigo.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Send Live Stripe Payout",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = payoutAmountInput,
                            onValueChange = { payoutAmountInput = it },
                            label = { Text("Amount (USD)") },
                            prefix = { Text("$", fontWeight = FontWeight.Bold, color = StripeIndigo) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stripe_ui_amount_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StripeIndigo,
                                unfocusedBorderColor = CleanBorder
                            )
                        )

                        Box {
                            Surface(
                                onClick = { isCurrencyMenuExpanded = true },
                                shape = RoundedCornerShape(14.dp),
                                color = CleanSurfaceVariant,
                                border = BorderStroke(1.dp, CleanBorder),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(payoutCurrency.code, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Currency", modifier = Modifier.size(18.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = isCurrencyMenuExpanded,
                                onDismissRequest = { isCurrencyMenuExpanded = false }
                            ) {
                                viewModel.currencies.forEach { curr ->
                                    DropdownMenuItem(
                                        text = { Text("${curr.code} (${curr.symbol}) - ${curr.defaultCountry}") },
                                        onClick = {
                                            payoutCurrency = curr
                                            isCurrencyMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Preset Amount Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("50.00", "100.00", "250.00", "500.00", "1000.00").forEach { preset ->
                            val selected = payoutAmountInput == preset
                            Surface(
                                onClick = { payoutAmountInput = preset },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) StripeIndigoContainer else CleanSurfaceVariant,
                                border = BorderStroke(1.dp, if (selected) StripeIndigo else CleanBorderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$${preset.substringBefore(".")}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) StripeIndigo else CleanTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Speed Selector (Standard vs Instant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isStandard = stripeState.selectedSpeedMethod == "standard"
                        Surface(
                            onClick = { viewModel.setStripePayoutSpeed("standard") },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isStandard) StripeIndigoContainer else CleanSurfaceVariant,
                            border = BorderStroke(1.dp, if (isStandard) StripeIndigo else CleanBorderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("stripe_ui_speed_standard")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (isStandard) StripeIndigo else CleanTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text("Standard ($0)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isStandard) StripeIndigo else CleanTextPrimary)
                                    Text("1-2 business days", fontSize = 9.sp, color = CleanTextSecondary)
                                }
                            }
                        }

                        val isInstant = stripeState.selectedSpeedMethod == "instant"
                        Surface(
                            onClick = { viewModel.setStripePayoutSpeed("instant") },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isInstant) CleanOrangeContainer else CleanSurfaceVariant,
                            border = BorderStroke(1.dp, if (isInstant) CleanOrange else CleanBorderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("stripe_ui_speed_instant")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = if (isInstant) CleanOrange else CleanTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text("Instant (1%)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isInstant) CleanOrangeText else CleanTextPrimary)
                                    Text("Arrives in minutes", fontSize = 9.sp, color = CleanTextSecondary)
                                }
                            }
                        }
                    }

                    // External Bank / Card Destinations
                    if (stripeState.externalAccounts.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            stripeState.externalAccounts.forEach { ext ->
                                val selected = stripeState.selectedDestinationId == ext.id
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.selectStripeDestination(ext.id) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (ext.type.equals("card", ignoreCase = true)) {
                                                Icons.Default.CreditCard
                                            } else {
                                                Icons.Default.AccountBalance
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    label = {
                                        Text("${ext.label} ••••${ext.last4} (${ext.currencyCode})", fontSize = 10.sp)
                                    },
                                    modifier = Modifier.testTag("stripe_ui_dest_${ext.id}")
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = payoutDescription,
                        onValueChange = { payoutDescription = it },
                        label = { Text("Statement Descriptor / Memo") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stripe_ui_description_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StripeIndigo,
                            unfocusedBorderColor = CleanBorder
                        )
                    )

                    Button(
                        onClick = {
                            if (parsedAmount <= 0.0) {
                                Toast.makeText(context, "Enter a valid payout amount", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.executeLiveStripePayout(
                                    amountInUsd = parsedAmount,
                                    currencyCode = payoutCurrency.code,
                                    speedMethod = stripeState.selectedSpeedMethod,
                                    description = payoutDescription.ifBlank { null },
                                    onResult = { _, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        enabled = !stripeState.isSubmittingPayout && parsedAmount > 0.0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("stripe_ui_submit_payout_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StripeIndigo,
                            contentColor = StripeOnIndigo
                        )
                    ) {
                        if (stripeState.isSubmittingPayout) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = StripeOnIndigo
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Submitting to Stripe...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Execute Live Stripe Payout (${currencyFormatter.formatUsd(parsedAmount)} → ${payoutCurrency.code})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (!stripeState.statusMessage.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CleanSuccessContainer,
                            border = BorderStroke(1.dp, CleanSuccess.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("stripe_ui_status_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CleanSuccess, modifier = Modifier.size(16.dp))
                                Text(stripeState.statusMessage.orEmpty(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CleanSuccess)
                            }
                        }
                    }

                    if (!stripeState.errorMessage.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CleanDangerContainer,
                            border = BorderStroke(1.dp, CleanDanger.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("stripe_ui_error_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = CleanDanger, modifier = Modifier.size(16.dp))
                                Text(stripeState.errorMessage.orEmpty(), fontSize = 11.sp, color = CleanDanger)
                            }
                        }
                    }
                }
            }
        }

        // 5. LIVE STRIPE REMOTE PAYOUTS FEED
        item {
            Text(
                text = "STRIPE API PAYOUT ACTIVITY (${stripeState.remotePayouts.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CleanTextMuted,
                letterSpacing = 0.8.sp
            )
        }

        if (stripeState.remotePayouts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CleanSurface),
                    border = BorderStroke(1.dp, CleanBorderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = StripeIndigo,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "No Remote Stripe Payouts Loaded",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextPrimary
                        )
                        Text(
                            text = "Tap Sync API above after configuring Stripe keys in Secrets to fetch recent v1/payouts records.",
                            fontSize = 11.sp,
                            color = CleanTextSecondary
                        )
                    }
                }
            }
        } else {
            items(stripeState.remotePayouts, key = { it.id }) { item ->
                StripeDashboardRemotePayoutCard(
                    item = item,
                    isCanceling = stripeState.isCancelingPayoutId == item.id,
                    currencyFormatter = currencyFormatter,
                    onCopyId = {
                        clipboardManager.setText(AnnotatedString(item.id))
                        Toast.makeText(context, "Copied ${item.id}", Toast.LENGTH_SHORT).show()
                    },
                    onCancel = {
                        viewModel.cancelPendingStripePayout(item.id) { _, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        // 6. ROOM-PERSISTED STRIPE LEDGER SNAPSHOT
        if (stripeRoomRecords.isNotEmpty()) {
            item {
                Text(
                    text = "LOCAL ROOM STRIPE LEDGER (${stripeRoomRecords.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanTextMuted,
                    letterSpacing = 0.8.sp
                )
            }

            items(stripeRoomRecords.take(5), key = { "room_${it.id}" }) { record ->
                StripeRoomSnapshotRow(record = record, currencyFormatter = currencyFormatter)
            }
        }
    }
}

@Composable
private fun StripeKeyStatusRow(
    label: String,
    maskedValue: String,
    isHealthy: Boolean
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CleanSurfaceVariant,
        border = BorderStroke(1.dp, CleanBorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanTextMuted
                )
                Text(
                    text = maskedValue,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = CleanTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isHealthy) CleanSuccessContainer else CleanOrangeContainer
            ) {
                Text(
                    text = if (isHealthy) "READY" else "ACTION NEEDED",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isHealthy) CleanSuccess else CleanOrangeText,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun StripeDashboardRemotePayoutCard(
    item: StripePayoutItem,
    isCanceling: Boolean,
    currencyFormatter: CurrencyFormatter,
    onCopyId: () -> Unit,
    onCancel: () -> Unit
) {
    val arrivalLabel = remember(item.arrivalDateEpochMs) {
        if (item.arrivalDateEpochMs > 0L) {
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(item.arrivalDateEpochMs))
        } else {
            "Pending"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stripe_ui_remote_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CleanSurface),
        border = BorderStroke(1.dp, CleanBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.id,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = StripeIndigo,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(
                        onClick = onCopyId,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Stripe Payout ID",
                            tint = CleanTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = "${item.method.uppercase()} • Arrival $arrivalLabel • ${item.payoutType}",
                    fontSize = 10.sp,
                    color = CleanTextSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = currencyFormatter.format(item.majorAmount, item.currencyCode),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CleanTextPrimary
                    )
                    PayoutStatusPill(status = item.status)
                }

                if (item.status.equals("pending", ignoreCase = true)) {
                    IconButton(
                        onClick = onCancel,
                        enabled = !isCanceling,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(36.dp)
                            .testTag("stripe_ui_cancel_${item.id}")
                    ) {
                        if (isCanceling) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Cancel pending payout",
                                tint = CleanDanger,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StripeRoomSnapshotRow(
    record: PayoutWireEntity,
    currencyFormatter: CurrencyFormatter
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CleanSurfaceSecondary,
        border = BorderStroke(1.dp, CleanBorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.clearingRail,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${record.referenceCode} • ${record.status}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CleanTextSecondary
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PayoutStatusPill(status = record.status)
                Text(
                    text = currencyFormatter.formatUsd(record.netAmountUsd),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = StripeIndigo
                )
            }
        }
    }
}

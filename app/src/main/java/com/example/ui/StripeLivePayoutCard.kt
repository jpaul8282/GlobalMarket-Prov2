package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StripePayoutItem
import com.example.ui.theme.CleanBorder
import com.example.ui.theme.CleanBorderSubtle
import com.example.ui.theme.CleanDanger
import com.example.ui.theme.CleanDangerContainer
import com.example.ui.theme.CleanOnPrimary
import com.example.ui.theme.CleanOrange
import com.example.ui.theme.CleanOrangeContainer
import com.example.ui.theme.CleanOrangeText
import com.example.ui.theme.CleanPrimary
import com.example.ui.theme.CleanPrimaryContainer
import com.example.ui.theme.CleanSecondary
import com.example.ui.theme.CleanSecondaryContainer
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
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StripeLivePayoutCard(
    stripeState: StripePayoutUiState,
    amountInUsd: Double,
    currencyCode: String,
    currencyFormatter: CurrencyFormatter,
    onSelectSpeed: (String) -> Unit,
    onSyncStripe: () -> Unit,
    onExecuteStripePayout: () -> Unit,
    onCancelStripePayout: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stripe_live_payout_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CleanSurface),
        border = BorderStroke(
            1.5.dp,
            if (stripeState.isKeyConfigured) CleanSecondary.copy(alpha = 0.55f) else CleanBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header & Environment Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CleanSecondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Stripe Live Payouts",
                            tint = CleanSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Stripe Payouts Live API",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextPrimary
                        )
                        Text(
                            text = "api.stripe.com/v1/payouts • Key: ${stripeState.maskedKeyPreview}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CleanTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        stripeState.isLiveMode -> CleanSuccessContainer
                        stripeState.isKeyConfigured -> CleanOrangeContainer
                        else -> CleanTertiaryContainer
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            stripeState.isLiveMode -> CleanSuccess.copy(alpha = 0.45f)
                            stripeState.isKeyConfigured -> CleanOrange.copy(alpha = 0.45f)
                            else -> CleanTertiary.copy(alpha = 0.45f)
                        }
                    ),
                    modifier = Modifier
                        .testTag("stripe_env_badge")
                        .semantics(mergeDescendants = true) {}
                ) {
                    Text(
                        text = stripeState.environmentLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            stripeState.isLiveMode -> CleanSuccess
                            stripeState.isKeyConfigured -> CleanOrangeText
                            else -> CleanTertiary
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Optional Connect account info
            if (!stripeState.connectedAccountId.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CleanSurfaceVariant,
                    border = BorderStroke(1.dp, CleanBorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Stripe-Account Header",
                            fontSize = 11.sp,
                            color = CleanTextMuted
                        )
                        Text(
                            text = stripeState.connectedAccountId,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CleanPrimary
                        )
                    }
                }
            }

            // Missing Key Callout (directing to Secrets panel, never an interactive key input field)
            if (!stripeState.isKeyConfigured) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CleanOrangeContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, CleanOrange.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stripe_missing_key_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = CleanOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Configure STRIPE_SECRET_KEY in AI Studio Secrets",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextPrimary
                            )
                            Text(
                                text = "Add your Stripe test or live secret key (sk_test_... / sk_live_...) in the Secrets panel to query live Stripe balances and execute real payouts.",
                                fontSize = 11.sp,
                                color = CleanTextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            // 2. Balance Snapshot & Sync Action
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CleanSurfaceVariant,
                border = BorderStroke(1.dp, CleanBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "LIVE STRIPE TREASURY BALANCE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextMuted,
                                letterSpacing = 0.7.sp
                            )
                            val primaryAvail = stripeState.availableBalances.firstOrNull()
                            val primaryPending = stripeState.pendingBalances.firstOrNull()
                            Text(
                                text = if (primaryAvail != null) {
                                    "Available: ${currencyFormatter.format(primaryAvail.majorAmount, primaryAvail.currencyCode)}"
                                } else if (stripeState.isKeyConfigured) {
                                    "Tap Sync to load live Stripe balance"
                                } else {
                                    "Awaiting Stripe API key"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextPrimary,
                                modifier = Modifier.testTag("stripe_available_balance_text")
                            )
                            if (primaryPending != null) {
                                Text(
                                    text = "Pending: ${currencyFormatter.format(primaryPending.majorAmount, primaryPending.currencyCode)}",
                                    fontSize = 11.sp,
                                    color = CleanTextSecondary
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onSyncStripe,
                            enabled = !stripeState.isLoadingBalance,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CleanSecondary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("stripe_sync_button")
                        ) {
                            if (stripeState.isLoadingBalance) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = CleanSecondary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = "Sync Stripe Balance",
                                    tint = CleanSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (stripeState.isLoadingBalance) "Syncing" else "Sync API",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanSecondary
                            )
                        }
                    }

                    // Multi-currency balances pills if Stripe returned multiple currencies
                    if (stripeState.availableBalances.size > 1) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            stripeState.availableBalances.take(4).forEach { bal ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CleanSurface,
                                    border = BorderStroke(1.dp, CleanBorderSubtle)
                                ) {
                                    Text(
                                        text = "${bal.currencyCode}: ${currencyFormatter.format(bal.majorAmount, bal.currencyCode)}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CleanTextPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Speed Rail Selector (Standard vs Instant)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "STRIPE PAYOUT RAIL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanTextMuted,
                    letterSpacing = 0.7.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isStandard = stripeState.selectedSpeedMethod == "standard"
                    Surface(
                        onClick = { onSelectSpeed("standard") },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isStandard) CleanPrimaryContainer else CleanSurfaceVariant,
                        border = BorderStroke(1.dp, if (isStandard) CleanPrimary else CleanBorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("stripe_speed_standard")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (isStandard) CleanPrimary else CleanTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = "Standard ($0 Fee)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isStandard) CleanPrimary else CleanTextPrimary
                                )
                                Text(
                                    text = "1-2 business days",
                                    fontSize = 9.sp,
                                    color = CleanTextSecondary
                                )
                            }
                        }
                    }

                    val isInstant = stripeState.selectedSpeedMethod == "instant"
                    Surface(
                        onClick = { onSelectSpeed("instant") },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isInstant) CleanOrangeContainer else CleanSurfaceVariant,
                        border = BorderStroke(1.dp, if (isInstant) CleanOrange else CleanBorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("stripe_speed_instant")
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
                                Text(
                                    text = "Instant (1% Fee)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isInstant) CleanOrangeText else CleanTextPrimary
                                )
                                Text(
                                    text = "Minutes to debit/bank",
                                    fontSize = 9.sp,
                                    color = CleanTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 4. Execute Live Stripe Payout Button
            Button(
                onClick = {
                    if (amountInUsd <= 0.0) {
                        Toast.makeText(context, "Enter a valid payout amount above", Toast.LENGTH_SHORT).show()
                    } else {
                        onExecuteStripePayout()
                    }
                },
                enabled = !stripeState.isSubmittingPayout && amountInUsd > 0.0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("stripe_execute_payout_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CleanSecondary,
                    contentColor = CleanOnPrimary
                )
            ) {
                if (stripeState.isSubmittingPayout) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = CleanOnPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Calling Stripe API...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Send Live Stripe Payout (${currencyFormatter.formatUsd(amountInUsd)} → $currencyCode)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 5. API Status / Error Feedback
            if (!stripeState.statusMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CleanSuccessContainer,
                    border = BorderStroke(1.dp, CleanSuccess.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stripe_status_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = CleanSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stripeState.statusMessage,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CleanSuccess
                        )
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
                        .testTag("stripe_error_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = CleanDanger,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stripeState.errorMessage,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = CleanDanger
                        )
                    }
                }
            }

            // 6. Live Stripe API Payouts Feed
            if (stripeState.remotePayouts.isNotEmpty()) {
                HorizontalDivider(color = CleanBorderSubtle)
                Text(
                    text = "LIVE STRIPE API PAYOUTS (${stripeState.remotePayouts.size})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanTextMuted,
                    letterSpacing = 0.7.sp
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stripeState.remotePayouts.take(5).forEach { remote ->
                        StripeRemotePayoutRow(
                            item = remote,
                            isCanceling = stripeState.isCancelingPayoutId == remote.id,
                            currencyFormatter = currencyFormatter,
                            onCancel = { onCancelStripePayout(remote.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StripeRemotePayoutRow(
    item: StripePayoutItem,
    isCanceling: Boolean,
    currencyFormatter: CurrencyFormatter,
    onCancel: () -> Unit
) {
    val arrivalStr = if (item.arrivalDateEpochMs > 0L) {
        SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(item.arrivalDateEpochMs))
    } else {
        "Scheduled"
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CleanSurfaceSecondary,
        border = BorderStroke(1.dp, CleanBorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stripe_remote_item_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.id,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = CleanPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.method.uppercase()} • Arrival $arrivalStr • ${item.payoutType}",
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
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (item.status.lowercase()) {
                            "paid" -> CleanSuccessContainer
                            "canceled", "failed" -> CleanDangerContainer
                            else -> CleanTertiaryContainer
                        }
                    ) {
                        Text(
                            text = item.status.uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when (item.status.lowercase()) {
                                "paid" -> CleanSuccess
                                "canceled", "failed" -> CleanDanger
                                else -> CleanTertiary
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (item.status.equals("pending", ignoreCase = true)) {
                    TextButton(
                        onClick = onCancel,
                        enabled = !isCanceling,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("stripe_cancel_${item.id}")
                    ) {
                        if (isCanceling) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Cancel pending Stripe payout",
                                tint = CleanDanger,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

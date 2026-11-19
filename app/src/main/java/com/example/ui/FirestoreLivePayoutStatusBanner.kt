package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirestoreOwnerPayoutRecord
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
import com.example.ui.theme.CleanSurfaceVariant
import com.example.ui.theme.CleanTextMuted
import com.example.ui.theme.CleanTextPrimary
import com.example.ui.theme.CleanTextSecondary
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PayoutStatusColors(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color,
    val icon: ImageVector
)

fun resolvePayoutStatusColors(status: String): PayoutStatusColors {
    return when (status.uppercase(Locale.US)) {
        "SETTLED", "COMPLETED", "PAID" -> PayoutStatusColors(
            containerColor = CleanSuccessContainer,
            contentColor = CleanSuccess,
            borderColor = CleanSuccess.copy(alpha = 0.35f),
            icon = Icons.Default.CheckCircle
        )
        "PROCESSING", "IN_TRANSIT", "PENDING" -> PayoutStatusColors(
            containerColor = CleanOrangeContainer,
            contentColor = CleanOrangeText,
            borderColor = CleanOrange.copy(alpha = 0.4f),
            icon = Icons.Default.Schedule
        )
        "FAILED", "CANCELED" -> PayoutStatusColors(
            containerColor = CleanDangerContainer,
            contentColor = CleanDanger,
            borderColor = CleanDanger.copy(alpha = 0.4f),
            icon = Icons.Default.ErrorOutline
        )
        else -> PayoutStatusColors(
            containerColor = CleanPrimaryContainer,
            contentColor = CleanPrimary,
            borderColor = CleanPrimary.copy(alpha = 0.35f),
            icon = Icons.Default.Schedule
        )
    }
}

@Composable
fun PayoutStatusPill(
    status: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val normalized = status.uppercase(Locale.US).ifBlank { "PENDING" }
    val visuals = resolvePayoutStatusColors(normalized)
    val animatedContainer by animateColorAsState(
        targetValue = visuals.containerColor,
        animationSpec = tween(250),
        label = "status_container"
    )
    val animatedContent by animateColorAsState(
        targetValue = visuals.contentColor,
        animationSpec = tween(250),
        label = "status_content"
    )

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = animatedContainer,
        border = BorderStroke(1.dp, visuals.borderColor),
        modifier = if (onClick != null) {
            modifier.clickable(onClick = onClick)
        } else {
            modifier
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = visuals.icon,
                contentDescription = "Status $normalized",
                tint = animatedContent,
                modifier = Modifier.size(11.dp)
            )
            AnimatedContent(
                targetState = normalized,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                label = "status_label"
            ) { label ->
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = animatedContent
                )
            }
        }
    }
}

@Composable
fun FirestoreLivePayoutStatusPanel(
    streamState: FirestorePayoutStreamState,
    currencyFormatter: CurrencyFormatter,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectPayout: ((FirestoreOwnerPayoutRecord) -> Unit)? = null
) {
    val pulseTransition = rememberInfiniteTransition(label = "firestore_live_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "firestore_pulse_alpha"
    )

    val borderColor = when (streamState) {
        is FirestorePayoutStreamState.Live -> CleanSuccess.copy(alpha = 0.45f)
        is FirestorePayoutStreamState.Error -> CleanDanger.copy(alpha = 0.5f)
        is FirestorePayoutStreamState.Connecting -> CleanPrimary.copy(alpha = 0.4f)
        FirestorePayoutStreamState.Unavailable -> CleanBorder
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("firestore_live_payout_panel"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CleanSurface),
        border = BorderStroke(1.2.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                when (streamState) {
                                    is FirestorePayoutStreamState.Live -> CleanSuccessContainer
                                    is FirestorePayoutStreamState.Error -> CleanDangerContainer
                                    is FirestorePayoutStreamState.Connecting -> CleanPrimaryContainer
                                    FirestorePayoutStreamState.Unavailable -> CleanSurfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when (streamState) {
                            is FirestorePayoutStreamState.Connecting -> CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = CleanPrimary
                            )
                            is FirestorePayoutStreamState.Live -> Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Firestore Live Listener Active",
                                tint = CleanSuccess,
                                modifier = Modifier.size(20.dp)
                            )
                            is FirestorePayoutStreamState.Error -> Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "Firestore Listener Error",
                                tint = CleanDanger,
                                modifier = Modifier.size(20.dp)
                            )
                            FirestorePayoutStreamState.Unavailable -> Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Local Ledger Mode",
                                tint = CleanTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Real-Time Payment & Payout Status",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitle = when (streamState) {
                            is FirestorePayoutStreamState.Live -> {
                                val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                    .format(Date(streamState.lastEventEpochMs))
                                "Firestore snapshot listener active • Synced $time"
                            }
                            is FirestorePayoutStreamState.Connecting -> "Attaching Firestore snapshot listener..."
                            is FirestorePayoutStreamState.Error -> streamState.message
                            FirestorePayoutStreamState.Unavailable -> "Local Room persistence active (sign in for cloud stream)"
                        }
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = if (streamState is FirestorePayoutStreamState.Error) CleanDanger else CleanTextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Live status indicator badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when (streamState) {
                        is FirestorePayoutStreamState.Live -> CleanSuccessContainer
                        is FirestorePayoutStreamState.Error -> CleanDangerContainer
                        is FirestorePayoutStreamState.Connecting -> CleanPrimaryContainer
                        FirestorePayoutStreamState.Unavailable -> CleanSurfaceVariant
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (streamState is FirestorePayoutStreamState.Live) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(CleanSuccess.copy(alpha = pulseAlpha))
                            )
                        }
                        Text(
                            text = when (streamState) {
                                is FirestorePayoutStreamState.Live -> "LIVE"
                                is FirestorePayoutStreamState.Connecting -> "SYNCING"
                                is FirestorePayoutStreamState.Error -> "ERROR"
                                FirestorePayoutStreamState.Unavailable -> "LOCAL"
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when (streamState) {
                                is FirestorePayoutStreamState.Live -> CleanSuccess
                                is FirestorePayoutStreamState.Error -> CleanDanger
                                is FirestorePayoutStreamState.Connecting -> CleanPrimary
                                FirestorePayoutStreamState.Unavailable -> CleanTextSecondary
                            }
                        )
                    }
                }
            }

            when (streamState) {
                is FirestorePayoutStreamState.Live -> {
                    val payouts = streamState.payouts
                    val settledCount = remember(payouts) {
                        payouts.count {
                            it.status.equals("SETTLED", true) ||
                                it.status.equals("COMPLETED", true) ||
                                it.status.equals("PAID", true)
                        }
                    }
                    val inFlightCount = remember(payouts) {
                        payouts.count {
                            it.status.equals("PROCESSING", true) ||
                                it.status.equals("IN_TRANSIT", true) ||
                                it.status.equals("PENDING", true)
                        }
                    }
                    val attentionCount = remember(payouts) {
                        payouts.count {
                            it.status.equals("FAILED", true) ||
                                it.status.equals("CANCELED", true)
                        }
                    }

                    // Latest status transition callout
                    AnimatedVisibility(
                        visible = !streamState.latestStatusChange.isNullOrBlank(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CleanPrimaryContainer,
                            border = BorderStroke(1.dp, CleanPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("firestore_latest_transition_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Real-time update",
                                    tint = CleanPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Live status update: ${streamState.latestStatusChange}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Status breakdown counters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusCounterTile(
                            label = "Settled / Paid",
                            count = settledCount,
                            containerColor = CleanSuccessContainer,
                            accentColor = CleanSuccess,
                            modifier = Modifier.weight(1f)
                        )
                        StatusCounterTile(
                            label = "In Flight",
                            count = inFlightCount,
                            containerColor = CleanOrangeContainer,
                            accentColor = CleanOrangeText,
                            modifier = Modifier.weight(1f)
                        )
                        StatusCounterTile(
                            label = "Failed / Canc.",
                            count = attentionCount,
                            containerColor = if (attentionCount > 0) CleanDangerContainer else CleanSurfaceVariant,
                            accentColor = if (attentionCount > 0) CleanDanger else CleanTextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (payouts.isNotEmpty()) {
                        HorizontalDivider(color = CleanBorderSubtle)
                        Text(
                            text = "LIVE CLOUD PAYOUT FEED (${payouts.size})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextMuted,
                            letterSpacing = 0.6.sp
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            payouts.take(3).forEach { record ->
                                val timeMillis = (record.updatedAt ?: record.createdAt)?.toDate()?.time
                                val formattedTime = remember(timeMillis) {
                                    if (timeMillis != null) {
                                        SimpleDateFormat("MMM dd • HH:mm", Locale.getDefault()).format(Date(timeMillis))
                                    } else {
                                        "Pending server time"
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = CleanSurfaceVariant,
                                    border = BorderStroke(1.dp, CleanBorderSubtle),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (onSelectPayout != null) {
                                                Modifier.clickable { onSelectPayout(record) }
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .testTag("firestore_live_payout_row_${record.payoutId}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = "${record.method} • ${currencyFormatter.format(record.netAmount, record.currency)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CleanTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${record.payoutId} • $formattedTime",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = CleanTextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        PayoutStatusPill(status = record.status)
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Listening to your Firestore payouts collection. New payouts and status changes will appear here in real time.",
                            fontSize = 11.sp,
                            color = CleanTextSecondary
                        )
                    }
                }

                is FirestorePayoutStreamState.Error -> {
                    OutlinedButton(
                        onClick = onRetry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("firestore_payout_retry_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CleanDanger.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CleanDanger),
                        contentPadding = PaddingValues(horizontal = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Retry Real-Time Listener",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                is FirestorePayoutStreamState.Connecting,
                FirestorePayoutStreamState.Unavailable -> Unit
            }
        }
    }
}

@Composable
private fun StatusCounterTile(
    label: String,
    count: Int,
    containerColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "$count",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FirestoreLiveSyncBadgeStrip(
    streamState: FirestorePayoutStreamState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when (streamState) {
        is FirestorePayoutStreamState.Live -> CleanSuccessContainer
        is FirestorePayoutStreamState.Error -> CleanDangerContainer
        is FirestorePayoutStreamState.Connecting -> CleanPrimaryContainer
        FirestorePayoutStreamState.Unavailable -> CleanSurfaceVariant
    }
    val fgColor = when (streamState) {
        is FirestorePayoutStreamState.Live -> CleanSuccess
        is FirestorePayoutStreamState.Error -> CleanDanger
        is FirestorePayoutStreamState.Connecting -> CleanPrimary
        FirestorePayoutStreamState.Unavailable -> CleanTextSecondary
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.dp, fgColor.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("firestore_live_sync_strip")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when (streamState) {
                        is FirestorePayoutStreamState.Live -> Icons.Default.CloudDone
                        is FirestorePayoutStreamState.Error -> Icons.Default.CloudOff
                        else -> Icons.Default.CloudSync
                    },
                    contentDescription = "Firestore Real-Time Sync",
                    tint = fgColor,
                    modifier = Modifier.size(16.dp)
                )
                val label = when (streamState) {
                    is FirestorePayoutStreamState.Live -> {
                        val change = streamState.latestStatusChange
                        if (!change.isNullOrBlank()) {
                            "Firestore Live • Updated $change"
                        } else {
                            "Firestore Live • Monitoring ${streamState.payouts.size} cloud payout(s)"
                        }
                    }
                    is FirestorePayoutStreamState.Connecting -> "Firestore Live • Connecting listener..."
                    is FirestorePayoutStreamState.Error -> "Firestore Sync Error • ${streamState.message}"
                    FirestorePayoutStreamState.Unavailable -> "Room Offline Mode • Cloud sync paused"
                }
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = fgColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (streamState is FirestorePayoutStreamState.Error) {
                Surface(
                    onClick = onRetry,
                    shape = RoundedCornerShape(8.dp),
                    color = CleanSurface,
                    modifier = Modifier.testTag("firestore_strip_retry_button")
                ) {
                    Text(
                        text = "Retry",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanDanger,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else if (streamState is FirestorePayoutStreamState.Live) {
                Text(
                    text = "REAL-TIME",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CleanSuccess
                )
            }
        }
    }
}

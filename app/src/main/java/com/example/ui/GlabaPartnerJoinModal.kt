package com.example.ui

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun GlabaPartnerJoinModal(
    viewModel: GlobalMarketViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val partnerUrl by viewModel.partnerJoinUrl.collectAsStateWithLifecycle()
    val partnerCode by viewModel.partnerReferralCode.collectAsStateWithLifecycle()
    val dealCode by viewModel.partnerDealCode.collectAsStateWithLifecycle()
    val hostName by viewModel.partnerHostName.collectAsStateWithLifecycle()
    val hostEmail by viewModel.partnerHostEmail.collectAsStateWithLifecycle()
    val hostIban by viewModel.partnerHostIban.collectAsStateWithLifecycle()
    val hostBic by viewModel.partnerHostBic.collectAsStateWithLifecycle()
    val liveRates by viewModel.liveRateMap.collectAsStateWithLifecycle()

    var businessNameInput by remember { mutableStateOf("Westerveld Global Commerce BV") }
    var contactNameInput by remember { mutableStateOf(hostName) }
    var emailInput by remember { mutableStateOf(hostEmail) }
    var countryInput by remember { mutableStateOf("Netherlands") }
    var ibanInput by remember { mutableStateOf(hostIban) }
    var bicInput by remember { mutableStateOf(hostBic) }

    // Volume Slider & Calculator State
    var monthlyVolumeEur by remember { mutableFloatStateOf(50000f) }
    var selectedCalcCurrency by remember { mutableStateOf("EUR") }

    // Onboarding activation state
    var isSubmitting by remember { mutableStateOf(false) }
    var activationResult by remember { mutableStateOf<PartnerRegistrationEntity?>(null) }
    var copyToast by remember { mutableStateOf<String?>(null) }

    val rateMultiplier = liveRates[selectedCalcCurrency] ?: 1.0
    val convertedVolume = monthlyVolumeEur * rateMultiplier

    // Standard fee: 2.9% + 0.30 fixed (assume 500 orders per €50k volume = ~€100 avg order)
    val avgOrder = 100.0 * rateMultiplier
    val estimatedTransactions = if (avgOrder > 0) (convertedVolume / avgOrder) else 500.0
    val standardFeeRate = 0.029
    val standardFixedFee = 0.30 * rateMultiplier
    val standardTotalFees = (convertedVolume * standardFeeRate) + (estimatedTransactions * standardFixedFee)

    // Glaba Pro PARTNER25 fee: 25% discount on variable rate (2.17%) and fixed fee (0.22)
    val partnerFeeRate = 0.02175
    val partnerFixedFee = 0.225 * rateMultiplier
    val partnerTotalFees = (convertedVolume * partnerFeeRate) + (estimatedTransactions * partnerFixedFee)

    val monthlySavings = (standardTotalFees - partnerTotalFees).coerceAtLeast(0.0)
    val annualSavings = monthlySavings * 12.0
    val referralKickback = convertedVolume * 0.005 // 0.50% recurring affiliate commission

    val currencySymbol = when (selectedCalcCurrency) {
        "EUR" -> "€"
        "USD" -> "$"
        "GBP" -> "£"
        "JPY" -> "¥"
        "CHF" -> "CHF "
        else -> "$selectedCalcCurrency "
    }

    Dialog(
        onDismissRequest = {
            if (!isSubmitting) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isSubmitting,
            dismissOnClickOutside = !isSubmitting
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CleanSurface),
            border = BorderStroke(1.5.dp, CleanBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // -------------------------------------------------------------
                // BROWSER CHROME (SIMULATED SECURE BROWSER HEADER)
                // -------------------------------------------------------------
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CleanSurfaceVariant)
                ) {
                    // Top Browser Action Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Simulated Window Dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF10B981)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Glaba Partner Terminal",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CleanTextSecondary
                            )
                        }

                        // Close button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = CleanTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Omnibox / URL Bar
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = CleanSurface,
                        border = BorderStroke(1.dp, CleanBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "SSL Verified",
                                    tint = CleanSuccess,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = partnerUrl,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    color = CleanTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // Copy URL Button
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(partnerUrl))
                                        copyToast = "Partner invite link copied to clipboard!"
                                        coroutineScope.launch {
                                            delay(2500)
                                            copyToast = null
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy URL",
                                        tint = CleanPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                // Share URL
                                IconButton(
                                    onClick = {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "Join Glaba Pro with J.P. Westerveld's VIP invite! Use code PARTNER25 for 25% off fees: $partnerUrl"
                                            )
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Glaba Partner Link"))
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = CleanPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Copy notification toast
                    AnimatedVisibility(visible = copyToast != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            color = CleanSuccess.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, CleanSuccess.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = copyToast ?: "",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanSuccess,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                // -------------------------------------------------------------
                // PAGE BODY CONTENT (SCROLLABLE)
                // -------------------------------------------------------------
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (activationResult != null) {
                        // -----------------------------------------------------
                        // SUCCESS / ACTIVATED VIEW
                        // -----------------------------------------------------
                        PartnerActivatedSuccessView(
                            partner = activationResult!!,
                            onLaunchTestCheckout = {
                                onDismiss()
                                viewModel.openWebCheckout("https://glaba.pro/pay?m=${activationResult!!.merchantId}&c=EUR&code=PARTNER25")
                            },
                            onClose = onDismiss
                        )
                    } else {
                        // -----------------------------------------------------
                        // VIP PARTNER HERO INVITATION CARD
                        // -----------------------------------------------------
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = CleanPrimary,
                            shadowElevation = 4.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                CleanPrimary,
                                                Color(0xFF1E3A8A)
                                            )
                                        )
                                    )
                                    .padding(18.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White.copy(alpha = 0.2f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Verified,
                                                    contentDescription = null,
                                                    tint = CleanOrange,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "EXECUTIVE PARTNER PROGRAM",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = CleanOrange
                                        ) {
                                            Text(
                                                text = "DEAL: $dealCode",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    // Host Avatar & Title
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                                .border(2.dp, CleanOrange, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "JP",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Black,
                                                color = CleanPrimary
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Direct VIP Invite from $hostName",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "@$partnerCode • $hostEmail",
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.85f),
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "ING Bank N.V. Verified Account: $hostIban",
                                                fontSize = 10.sp,
                                                color = CleanOrange,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    // Host Quote Box
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.Black.copy(alpha = 0.25f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                                    ) {
                                        Text(
                                            text = "“Welcome to Glaba Pro! Through this direct partner agreement, your business receives our premier 25% fee discount tier (code: $dealCode), zero-spread live FX settlement, and direct instant bank payout rails.”",
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.95f),
                                            lineHeight = 16.sp,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // -----------------------------------------------------
                        // DEAL CODE HIGHLIGHT STRIP
                        // -----------------------------------------------------
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = CleanSuccess.copy(alpha = 0.08f),
                            border = BorderStroke(1.5.dp, CleanSuccess.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = CleanSuccess,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Promo Deal $dealCode Applied",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CleanTextPrimary
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = CleanSuccess
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DealPerkPill(modifier = Modifier.weight(1f), icon = Icons.Default.Percent, title = "-25% Fees", subtitle = "Lifetime Rate")
                                    DealPerkPill(modifier = Modifier.weight(1f), icon = Icons.Default.CurrencyExchange, title = "0% FX Spread", subtitle = "12 Currencies")
                                    DealPerkPill(modifier = Modifier.weight(1f), icon = Icons.Default.AccountBalance, title = "Instant Payout", subtitle = "SEPA / ING Bank")
                                }
                            }
                        }

                        // -----------------------------------------------------
                        // INTERACTIVE REVENUE & FEE SAVINGS CALCULATOR
                        // -----------------------------------------------------
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = CleanSurfaceVariant,
                            border = BorderStroke(1.dp, CleanBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "PARTNER FEE & PROFIT CALCULATOR",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CleanPrimary,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = "Interactive savings based on monthly card & bank volume",
                                            fontSize = 11.sp,
                                            color = CleanTextSecondary
                                        )
                                    }

                                    // Currency Switcher
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("EUR", "USD", "GBP").forEach { curr ->
                                            val isSel = selectedCalcCurrency == curr
                                            Surface(
                                                onClick = { selectedCalcCurrency = curr },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSel) CleanPrimary else CleanSurface,
                                                border = BorderStroke(1.dp, if (isSel) CleanPrimary else CleanBorder)
                                            ) {
                                                Text(
                                                    text = curr,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSel) Color.White else CleanTextSecondary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Volume Slider
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Estimated Monthly Sales Volume:",
                                            fontSize = 12.sp,
                                            color = CleanTextPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "$currencySymbol${NumberFormat.getNumberInstance(Locale.US).format(convertedVolume.toInt())}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CleanPrimary
                                        )
                                    }

                                    Slider(
                                        value = monthlyVolumeEur,
                                        onValueChange = { monthlyVolumeEur = it },
                                        valueRange = 5000f..250000f,
                                        steps = 48,
                                        colors = SliderDefaults.colors(
                                            thumbColor = CleanPrimary,
                                            activeTrackColor = CleanPrimary,
                                            inactiveTrackColor = CleanBorder
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "${currencySymbol}5,000", fontSize = 10.sp, color = CleanTextMuted)
                                        Text(text = "${currencySymbol}100,000", fontSize = 10.sp, color = CleanTextMuted)
                                        Text(text = "${currencySymbol}250,000+", fontSize = 10.sp, color = CleanTextMuted)
                                    }
                                }

                                // Savings Comparison Cards
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Standard Gateway Cost
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        color = CleanSurface,
                                        border = BorderStroke(1.dp, CleanBorder)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(text = "Standard Gateway", fontSize = 10.sp, color = CleanTextMuted, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                text = "$currencySymbol${NumberFormat.getNumberInstance(Locale.US).format(standardTotalFees.toInt())}/mo",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFDC2626)
                                            )
                                            Text(text = "2.9% + 0.30/txn", fontSize = 9.sp, color = CleanTextMuted)
                                        }
                                    }

                                    // Glaba Pro PARTNER25 Cost
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        color = CleanSuccess.copy(alpha = 0.1f),
                                        border = BorderStroke(1.dp, CleanSuccess.copy(alpha = 0.4f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(text = "Glaba PARTNER25", fontSize = 10.sp, color = CleanSuccess, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = "$currencySymbol${NumberFormat.getNumberInstance(Locale.US).format(partnerTotalFees.toInt())}/mo",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CleanSuccess
                                            )
                                            Text(text = "2.17% + 0.22/txn", fontSize = 9.sp, color = CleanSuccess)
                                        }
                                    }
                                }

                                // Bottom Highlight: Total Cash Saved
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = CleanPrimaryContainer,
                                    border = BorderStroke(1.dp, CleanPrimary.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "YOUR ANNUAL CASH SAVINGS",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CleanPrimary
                                            )
                                            Text(
                                                text = "+$currencySymbol${NumberFormat.getNumberInstance(Locale.US).format(annualSavings.toInt())} / yr",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black,
                                                color = CleanPrimary
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Monthly Savings",
                                                fontSize = 10.sp,
                                                color = CleanTextSecondary
                                            )
                                            Text(
                                                text = "+$currencySymbol${NumberFormat.getNumberInstance(Locale.US).format(monthlySavings.toInt())}/mo",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CleanSuccess
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // -----------------------------------------------------
                        // LOCALIZED PAYMENT RAILS PILLARS
                        // -----------------------------------------------------
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "GLOBAL PAYMENT RAILS INCLUDED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextMuted,
                                letterSpacing = 0.8.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RailBadge(modifier = Modifier.weight(1f), flag = "🇳🇱", name = "iDEAL Instant", sub = "0.29 fee")
                                RailBadge(modifier = Modifier.weight(1f), flag = "🇪🇺", name = "SEPA Credit", sub = "ING Direct")
                                RailBadge(modifier = Modifier.weight(1f), flag = "🇺🇸", name = "FedNow & ACH", sub = "Zero FX")
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RailBadge(modifier = Modifier.weight(1f), flag = "🇧🇷", name = "Pix Brazil", sub = "Instant QR")
                                RailBadge(modifier = Modifier.weight(1f), flag = "🇬🇧", name = "Faster Pkts", sub = "GBP Rail")
                                RailBadge(modifier = Modifier.weight(1f), flag = "💳", name = "Apple/Google", sub = "1-Tap Pay")
                            }
                        }

                        // -----------------------------------------------------
                        // 1-TAP MERCHANT PARTNER ONBOARDING FORM
                        // -----------------------------------------------------
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = CleanSurface,
                            border = BorderStroke(1.5.dp, CleanPrimary.copy(alpha = 0.35f)),
                            shadowElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "ACTIVATE YOUR PARTNER TERMINAL",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CleanTextPrimary
                                        )
                                        Text(
                                            text = "Direct provisioning under deal code $dealCode",
                                            fontSize = 11.sp,
                                            color = CleanTextSecondary
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = CleanTertiaryContainer
                                    ) {
                                        Text(
                                            text = "HOST: @$partnerCode",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CleanTertiary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // Business / Store Name
                                OutlinedTextField(
                                    value = businessNameInput,
                                    onValueChange = { businessNameInput = it },
                                    label = { Text("Business / Store Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CleanPrimary,
                                        unfocusedBorderColor = CleanBorder
                                    )
                                )

                                // Contact Name & Email
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = contactNameInput,
                                        onValueChange = { contactNameInput = it },
                                        label = { Text("Contact Name") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CleanPrimary,
                                            unfocusedBorderColor = CleanBorder
                                        )
                                    )

                                    OutlinedTextField(
                                        value = emailInput,
                                        onValueChange = { emailInput = it },
                                        label = { Text("Business Email") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CleanPrimary,
                                            unfocusedBorderColor = CleanBorder
                                        )
                                    )
                                }

                                // Settlement Bank (ING Bank / IBAN)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = CleanSurfaceVariant,
                                    border = BorderStroke(1.dp, CleanBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AccountBalance,
                                                    contentDescription = null,
                                                    tint = CleanOrange,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Primary Settlement Bank: ING Bank N.V.",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CleanTextPrimary
                                                )
                                            }

                                            Text(
                                                text = "SEPA INSTANT",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CleanSuccess
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = ibanInput,
                                                onValueChange = { ibanInput = it },
                                                label = { Text("Owner Settlement IBAN") },
                                                modifier = Modifier.weight(1.4f),
                                                singleLine = true,
                                                shape = RoundedCornerShape(10.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = CleanPrimary,
                                                    unfocusedBorderColor = CleanBorder
                                                )
                                            )

                                            OutlinedTextField(
                                                value = bicInput,
                                                onValueChange = { bicInput = it },
                                                label = { Text("BIC") },
                                                modifier = Modifier.weight(0.7f),
                                                singleLine = true,
                                                shape = RoundedCornerShape(10.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = CleanPrimary,
                                                    unfocusedBorderColor = CleanBorder
                                                )
                                            )
                                        }
                                    }
                                }

                                // Agreement badge
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = CleanSuccess,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Pre-qualified for €250 initial processing fee credit upon activation.",
                                        fontSize = 11.sp,
                                        color = CleanTextSecondary
                                    )
                                }

                                // Main Activation Button
                                Button(
                                    onClick = {
                                        isSubmitting = true
                                        coroutineScope.launch {
                                            delay(1000)
                                            viewModel.registerPartnerMerchant(
                                                businessName = businessNameInput,
                                                contactName = contactNameInput,
                                                email = emailInput,
                                                country = countryInput,
                                                iban = ibanInput,
                                                bic = bicInput,
                                                currencyCode = selectedCalcCurrency,
                                                onSuccess = { partner ->
                                                    activationResult = partner
                                                    isSubmitting = false
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CleanPrimary,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    enabled = !isSubmitting && businessNameInput.isNotBlank()
                                ) {
                                    if (isSubmitting) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(text = "Provisioning Terminal...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    } else {
                                        Icon(imageVector = Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Activate Partner Account (Code: $dealCode)",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // -----------------------------------------------------
                        // BOTTOM TEST SIMULATION & SHARING FOOTER
                        // -----------------------------------------------------
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = CleanSurfaceVariant,
                            border = BorderStroke(1.dp, CleanBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "PARTNER INVITE & TEST ACTIONS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanTextMuted,
                                    letterSpacing = 0.8.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(partnerUrl))
                                            copyToast = "Link copied: $partnerUrl"
                                            coroutineScope.launch {
                                                delay(2500)
                                                copyToast = null
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(42.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CleanPrimary),
                                        border = BorderStroke(1.dp, CleanBorder)
                                    ) {
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Copy Link", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    Button(
                                        onClick = {
                                            onDismiss()
                                            viewModel.openWebCheckout("https://glaba.pro/pay?m=JPWESTERVELD&c=EUR&code=PARTNER25")
                                        },
                                        modifier = Modifier.weight(1.3f).height(42.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CleanOrange,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "Test Checkout ($dealCode)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DealPerkPill(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = CleanSurface,
        border = BorderStroke(1.dp, CleanBorder)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = CleanPrimary, modifier = Modifier.size(16.dp))
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CleanTextPrimary, textAlign = TextAlign.Center)
            Text(text = subtitle, fontSize = 9.sp, color = CleanTextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun RailBadge(
    modifier: Modifier = Modifier,
    flag: String,
    name: String,
    sub: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = CleanSurfaceVariant,
        border = BorderStroke(1.dp, CleanBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = flag, fontSize = 16.sp)
            Column {
                Text(text = name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CleanTextPrimary, maxLines = 1)
                Text(text = sub, fontSize = 8.sp, color = CleanTextMuted)
            }
        }
    }
}

@Composable
private fun PartnerActivatedSuccessView(
    partner: PartnerRegistrationEntity,
    onLaunchTestCheckout: () -> Unit,
    onClose: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var copiedApiKey by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CleanSurface,
        border = BorderStroke(1.5.dp, CleanSuccess),
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(CleanSuccessContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = CleanSuccess,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "Partner Terminal Provisioned!",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CleanTextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Your business is now fully onboarded under the Glaba Pro VIP Partner Program with 25% fee discount pre-applied.",
                fontSize = 12.sp,
                color = CleanTextSecondary,
                textAlign = TextAlign.Center
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = CleanSurfaceVariant,
                border = BorderStroke(1.dp, CleanBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailRow("Merchant ID", partner.merchantId)
                    DetailRow("Business Name", partner.businessName)
                    DetailRow("Contact", "${partner.contactName} (${partner.email})")
                    DetailRow("Settlement IBAN", partner.iban)
                    DetailRow("BIC / Swift", partner.bic)
                    DetailRow("Partner Deal", "${partner.dealCode} (-${partner.discountPercent}% Fees)")
                    DetailRow("Welcome Credit", "€250.00 (Active)")
                }
            }

            // API Secret Key Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = CleanPrimaryContainer,
                border = BorderStroke(1.dp, CleanPrimary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE API SECRET KEY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanPrimary
                        )

                        TextButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(partner.apiKey))
                                copiedApiKey = true
                                coroutineScope.launch {
                                    delay(2000)
                                    copiedApiKey = false
                                }
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = if (copiedApiKey) "Copied!" else "Copy Key",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanPrimary
                            )
                        }
                    }

                    Text(
                        text = partner.apiKey,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CleanTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action Buttons
            Button(
                onClick = onLaunchTestCheckout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CleanPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Launch Test Checkout with PARTNER25", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }

            OutlinedButton(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CleanTextPrimary),
                border = BorderStroke(1.dp, CleanBorder)
            ) {
                Text(text = "Done & Return to Terminal", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = CleanTextSecondary)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CleanTextPrimary, fontFamily = FontFamily.Monospace)
    }
}

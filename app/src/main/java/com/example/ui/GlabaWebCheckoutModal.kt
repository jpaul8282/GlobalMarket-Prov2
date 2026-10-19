package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun GlabaWebCheckoutModal(
    viewModel: GlobalMarketViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val currentUrl by viewModel.webCheckoutUrl.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val appliedPromo by viewModel.appliedPromoCode.collectAsStateWithLifecycle()
    val promoDiscount by viewModel.promoDiscountPercent.collectAsStateWithLifecycle()
    val merchantId by viewModel.activeMerchantId.collectAsStateWithLifecycle()
    val liveRates by viewModel.liveRateMap.collectAsStateWithLifecycle()
    val rateChangePercents by viewModel.rateChangePercents.collectAsStateWithLifecycle()
    val isRateRefreshing by viewModel.isRateRefreshing.collectAsStateWithLifecycle()
    val lastRefreshTime by viewModel.lastRateRefreshTimestamp.collectAsStateWithLifecycle()

    var promoInput by remember(appliedPromo) { mutableStateOf(appliedPromo ?: "GLOBAL15") }
    var customerName by remember { mutableStateOf("Elena Rostova") }
    var customerEmail by remember { mutableStateOf("elena.rostova@example.eu") }
    var selectedPaymentMethod by remember { mutableIntStateOf(0) } // 0: Apple/Google Pay, 1: Card, 2: SEPA
    var isPaying by remember { mutableStateOf(false) }
    var paymentSuccessResult by remember { mutableStateOf<String?>(null) }
    var urlCopiedToast by remember { mutableStateOf(false) }

    val baseAmountUsd = 49.99
    val discountAmountUsd = baseAmountUsd * (promoDiscount / 100.0)
    val netAmountUsd = (baseAmountUsd - discountAmountUsd).coerceAtLeast(0.0)

    val currentRate = liveRates[selectedCurrency.code] ?: selectedCurrency.exchangeRate
    val baseAmountTarget = baseAmountUsd * currentRate
    val discountAmountTarget = discountAmountUsd * currentRate
    val netAmountTarget = netAmountUsd * currentRate

    Dialog(
        onDismissRequest = {
            if (!isPaying) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isPaying,
            dismissOnClickOutside = !isPaying
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
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Top controls bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Window dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF10B981)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Glaba Web Checkout Viewer",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextMuted
                            )
                        }

                        IconButton(
                            onClick = { if (!isPaying) onDismiss() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Browser",
                                tint = CleanTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Address Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
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
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "SSL Secure",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentUrl,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CleanTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(currentUrl))
                                        urlCopiedToast = true
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

                                IconButton(
                                    onClick = {
                                        viewModel.openWebCheckout(currentUrl)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reload",
                                        tint = CleanSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = urlCopiedToast) {
                        LaunchedEffect(urlCopiedToast) {
                            delay(1500)
                            urlCopiedToast = false
                        }
                        Text(
                            text = "✓ Checkout link copied to clipboard",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // -------------------------------------------------------------
                // WEB STOREFRONT BODY
                // -------------------------------------------------------------
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(CleanBackground)
                ) {
                    if (paymentSuccessResult != null) {
                        // Success View
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFECFDF5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(44.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Payment Approved!",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextPrimary
                            )

                            Text(
                                text = "Glaba Pro Storefront Web Settlement Completed",
                                fontSize = 13.sp,
                                color = CleanTextSecondary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = CleanSurface,
                                border = BorderStroke(1.dp, CleanBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Paid Total", fontSize = 13.sp, color = CleanTextMuted)
                                        Text(
                                            "${selectedCurrency.symbol}${String.format(java.util.Locale.US, "%.2f", netAmountTarget)} ${selectedCurrency.code}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CleanTextPrimary
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Customer", fontSize = 13.sp, color = CleanTextMuted)
                                        Text(customerName, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = CleanTextPrimary)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Promo Applied", fontSize = 13.sp, color = CleanTextMuted)
                                        Text(
                                            "${appliedPromo ?: "GLOBAL15"} (-$promoDiscount%)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Transaction Ref", fontSize = 13.sp, color = CleanTextMuted)
                                        Text(
                                            paymentSuccessResult ?: "",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = CleanPrimary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.loadBarcodeIntoTerminal(netAmountUsd, selectedCurrency.code)
                                        onDismiss()
                                    },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, CleanBorder)
                                ) {
                                    Text("To Terminal", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = { onDismiss() },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CleanPrimary)
                                ) {
                                    Text("Done", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // Storefront Form View
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Merchant Trust Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(CleanPrimaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("GP", fontWeight = FontWeight.Black, color = CleanPrimary, fontSize = 16.sp)
                                    }
                                    Column {
                                        Text(
                                            text = "Glaba Pro Global Storefront",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CleanTextPrimary
                                        )
                                        Text(
                                            text = "Merchant: ${merchantId ?: "org_6RsNhnGbSQ3s7rFPAoYfQjA"}",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = CleanTextMuted
                                        )
                                    }
                                }

                                // Currency Badge
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = CleanSurfaceVariant,
                                    border = BorderStroke(1.dp, CleanBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            selectedCurrency.symbol,
                                            fontWeight = FontWeight.Bold,
                                            color = CleanPrimary,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            selectedCurrency.code,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = CleanTextPrimary
                                        )
                                    }
                                }
                            }

                            // Real-Time Currency Conversion Tool
                            RealTimeCurrencyConversionTool(
                                selectedCurrency = selectedCurrency,
                                currencies = viewModel.currencies,
                                amountUsd = netAmountUsd,
                                liveRateMap = liveRates,
                                rateChangePercents = rateChangePercents,
                                lastRefreshTimestamp = lastRefreshTime,
                                isRefreshing = isRateRefreshing,
                                onCurrencySelected = { viewModel.selectCurrency(it) },
                                onRefreshRates = { viewModel.refreshExchangeRates() },
                                compactMode = true
                            )

                            // Order Summary Card with Live Discount
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = CleanSurface),
                                border = BorderStroke(1.dp, CleanBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "ORDER SUMMARY",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CleanTextMuted,
                                        letterSpacing = 0.8.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Storefront Standard Plan", fontSize = 14.sp, color = CleanTextPrimary)
                                        Text(
                                            "${selectedCurrency.symbol}${String.format(java.util.Locale.US, "%.2f", baseAmountTarget)}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CleanTextPrimary
                                        )
                                    }

                                    if (promoDiscount > 0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.LocalOffer,
                                                    contentDescription = null,
                                                    tint = Color(0xFF10B981),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "Promo Code (${appliedPromo ?: "GLOBAL15"} -$promoDiscount%)",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF10B981)
                                                )
                                            }
                                            Text(
                                                "-${selectedCurrency.symbol}${String.format(java.util.Locale.US, "%.2f", discountAmountTarget)}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = CleanBorder)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Total Due",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CleanTextPrimary
                                        )
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                "${selectedCurrency.symbol}${String.format(java.util.Locale.US, "%.2f", netAmountTarget)}",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Black,
                                                color = CleanPrimary
                                            )
                                            Text(
                                                "≈ $${String.format(java.util.Locale.US, "%.2f", netAmountUsd)} USD",
                                                fontSize = 11.sp,
                                                color = CleanTextMuted
                                            )
                                        }
                                    }
                                }
                            }

                            // Promo Code Box
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = promoInput,
                                    onValueChange = { promoInput = it },
                                    label = { Text("Promo / Coupon Code", fontSize = 12.sp) },
                                    placeholder = { Text("e.g. GLOBAL15") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CleanPrimary,
                                        unfocusedBorderColor = CleanBorder
                                    )
                                )

                                Button(
                                    onClick = {
                                        viewModel.applyPromoCode(promoInput)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CleanPrimary),
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    Text("Apply", fontWeight = FontWeight.Bold)
                                }
                            }

                            // Payment Method Selector
                            Text(
                                text = "SELECT PAYMENT METHOD",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextMuted,
                                letterSpacing = 0.8.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val methods = listOf(
                                    "Google / Apple Pay" to Icons.Default.Smartphone,
                                    "Credit Card" to Icons.Default.CreditCard,
                                    "SEPA Instant" to Icons.Default.AccountBalance
                                )

                                methods.forEachIndexed { index, (label, icon) ->
                                    val isSelected = selectedPaymentMethod == index
                                    Surface(
                                        onClick = { selectedPaymentMethod = index },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) CleanPrimaryContainer else CleanSurface,
                                        border = BorderStroke(1.dp, if (isSelected) CleanPrimary else CleanBorder)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (isSelected) CleanPrimary else CleanTextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = label,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) CleanPrimary else CleanTextPrimary,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            // Customer Details
                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { customerName = it },
                                label = { Text("Customer Full Name", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CleanPrimary,
                                    unfocusedBorderColor = CleanBorder
                                )
                            )

                            OutlinedTextField(
                                value = customerEmail,
                                onValueChange = { customerEmail = it },
                                label = { Text("Customer Email Address", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CleanPrimary,
                                    unfocusedBorderColor = CleanBorder
                                )
                            )

                            // Action Buttons
                            Button(
                                onClick = {
                                    isPaying = true
                                    coroutineScope.launch {
                                        delay(900)
                                        viewModel.completeWebCheckoutTransaction(
                                            customerName = customerName,
                                            country = selectedCurrency.defaultCountry,
                                            netAmountUsd = netAmountUsd
                                        ) { savedEntity ->
                                            isPaying = false
                                            paymentSuccessResult = savedEntity.stripeSubscriptionId
                                        }
                                    }
                                },
                                enabled = !isPaying,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CleanPrimary)
                            ) {
                                if (isPaying) {
                                    CircularProgressIndicator(
                                        color = CleanOnPrimary,
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Authorizing Payment...", fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Pay ${selectedCurrency.symbol}${String.format(java.util.Locale.US, "%.2f", netAmountTarget)} (${appliedPromo ?: "GLOBAL15"})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            // Load into App Terminal Button
                            OutlinedButton(
                                onClick = {
                                    viewModel.loadBarcodeIntoTerminal(netAmountUsd, selectedCurrency.code)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, CleanBorder)
                            ) {
                                Icon(imageVector = Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync into App Terminal (${selectedCurrency.code} $${String.format(java.util.Locale.US, "%.2f", netAmountUsd)})")
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

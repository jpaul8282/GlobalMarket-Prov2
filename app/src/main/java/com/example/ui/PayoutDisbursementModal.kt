package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.StripeConnectionHealth
import com.example.ui.theme.CleanBorder
import com.example.ui.theme.CleanBorderLight
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
import com.example.ui.theme.CleanTextMuted
import com.example.ui.theme.CleanTextPrimary
import com.example.ui.theme.CleanTextSecondary
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun PayoutDisbursementModal(
    viewModel: GlobalMarketViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val payoutConfig by viewModel.payoutConfig.collectAsStateWithLifecycle()
    val stripePayoutState by viewModel.stripePayoutState.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val currencyFormatter = remember { CurrencyFormatter.DEFAULT }

    var activeMethod by remember { mutableStateOf(payoutConfig.selectedMethod) }

    // Bank wire inputs
    var bankName by remember { mutableStateOf(payoutConfig.bankWire.bankName) }
    var accountHolder by remember { mutableStateOf(payoutConfig.bankWire.accountHolderName) }
    var ibanOrAccount by remember { mutableStateOf(payoutConfig.bankWire.accountNumberOrIban) }
    var swiftBic by remember { mutableStateOf(payoutConfig.bankWire.swiftBic) }
    var routingNumber by remember { mutableStateOf(payoutConfig.bankWire.routingNumber) }
    var bankCountry by remember { mutableStateOf(payoutConfig.bankWire.country) }

    // PayPal inputs
    var paypalEmail by remember { mutableStateOf(payoutConfig.payPal.paypalEmailOrId) }
    var paypalRecipient by remember { mutableStateOf(payoutConfig.payPal.recipientName) }
    var paypalNote by remember { mutableStateOf("GLABA Marketing Merchant Settlement") }

    // Stripe Live input
    var stripeDescription by remember { mutableStateOf("GlobalMarket Pro Owner Stripe Payout") }
    var stripeInlineError by remember { mutableStateOf<String?>(null) }

    // Amount and currency
    var payoutAmountInput by remember { mutableStateOf("250.00") }
    var selectedPayoutCurrency by remember { mutableStateOf(selectedCurrency) }
    var isCurrencyDropdownOpen by remember { mutableStateOf(false) }

    // Disbursement states
    var isDisbursing by remember { mutableStateOf(false) }
    var completedReference by remember { mutableStateOf<String?>(null) }
    var completedAmount by remember { mutableStateOf<Double?>(null) }

    val parsedAmountUsd = payoutAmountInput.toDoubleOrNull() ?: 0.0
    val convertedAmount = parsedAmountUsd * selectedPayoutCurrency.exchangeRate
    val fee = viewModel.calculatePayoutFee(activeMethod, parsedAmountUsd)
    val netAmountUsd = (parsedAmountUsd - fee).coerceAtLeast(0.0)
    val netConvertedAmount = netAmountUsd * selectedPayoutCurrency.exchangeRate

    Dialog(
        onDismissRequest = { if (!isDisbursing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.96f)
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(28.dp))
                .testTag("payout_disbursement_modal"),
            color = CleanSurface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, CleanBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(CleanPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = "Payout",
                                tint = CleanPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Merchant Payout & Disbursement",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextPrimary
                            )
                            Text(
                                text = "Bank Wire, PayPal, or Live Stripe Payouts",
                                fontSize = 12.sp,
                                color = CleanTextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isDisbursing,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = CleanTextSecondary
                        )
                    }
                }

                HorizontalDivider(color = CleanBorderSubtle)

                if (completedReference != null) {
                    // Success View
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CleanSuccessContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = CleanSuccess,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Text(
                            text = "Payout Successfully Disbursed!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextPrimary
                        )

                        Text(
                            text = when (activeMethod) {
                                PayoutMethodType.BANK_WIRE -> "Funds dispatched via ${activeMethod.displayName} to $bankName."
                                PayoutMethodType.PAYPAL -> "Funds dispatched via ${activeMethod.displayName} to $paypalEmail."
                                PayoutMethodType.STRIPE_PAYOUT -> "Live payout dispatched via Stripe API (${stripePayoutState.selectedSpeedMethod.uppercase()})."
                            },
                            fontSize = 13.sp,
                            color = CleanTextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CleanSurfaceSecondary),
                            border = BorderStroke(1.dp, CleanBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Reference Code:", fontSize = 12.sp, color = CleanTextMuted)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = completedReference ?: "",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = CleanPrimary
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = CleanPrimary,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    completedReference?.let { ref ->
                                                        clipboardManager.setText(AnnotatedString(ref))
                                                        Toast.makeText(context, "Reference code copied!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Disbursed Amount:", fontSize = 12.sp, color = CleanTextMuted)
                                    Text(
                                        text = "$${String.format(Locale.US, "%.2f", completedAmount ?: 0.0)} USD",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CleanTextPrimary
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Status:", fontSize = 12.sp, color = CleanTextMuted)
                                    Text("SETTLED / QUEUED", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CleanSuccess)
                                }
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CleanPrimary)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                } else {
                    // Payout Method Selector Segmented Cards
                    Text(
                        text = "SELECT PAYOUT RAIL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextMuted,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Bank Wire Tab Option
                        val isWireSelected = activeMethod == PayoutMethodType.BANK_WIRE
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    stripeInlineError = null
                                    activeMethod = PayoutMethodType.BANK_WIRE
                                }
                                .testTag("payout_method_bank_wire"),
                            color = if (isWireSelected) CleanPrimaryContainer else CleanSurfaceSecondary,
                            border = BorderStroke(if (isWireSelected) 2.dp else 1.dp, if (isWireSelected) CleanPrimary else CleanBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = "Bank Wire",
                                    tint = if (isWireSelected) CleanPrimary else CleanTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Bank Wire",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isWireSelected) CleanPrimary else CleanTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$15 flat • 1-2d",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isWireSelected) CleanPrimary else CleanTextMuted,
                                    maxLines = 1
                                )
                            }
                        }

                        // PayPal Tab Option
                        val isPayPalSelected = activeMethod == PayoutMethodType.PAYPAL
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    stripeInlineError = null
                                    activeMethod = PayoutMethodType.PAYPAL
                                }
                                .testTag("payout_method_paypal"),
                            color = if (isPayPalSelected) CleanPrimaryContainer else CleanSurfaceSecondary,
                            border = BorderStroke(if (isPayPalSelected) 2.dp else 1.dp, if (isPayPalSelected) CleanPrimary else CleanBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = "PayPal",
                                    tint = if (isPayPalSelected) CleanPrimary else CleanTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "PayPal",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPayPalSelected) CleanPrimary else CleanTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "2.9%+$0.30 • Now",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isPayPalSelected) CleanPrimary else CleanTextMuted,
                                    maxLines = 1
                                )
                            }
                        }

                        // Stripe Live Tab Option
                        val isStripeSelected = activeMethod == PayoutMethodType.STRIPE_PAYOUT
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    stripeInlineError = null
                                    activeMethod = PayoutMethodType.STRIPE_PAYOUT
                                    if (stripePayoutState.lastSyncedEpochMs == null && !stripePayoutState.isCheckingConnection) {
                                        viewModel.triggerVerifyStripeConnection()
                                    }
                                }
                                .testTag("payout_method_stripe"),
                            color = if (isStripeSelected) CleanSecondaryContainer else CleanSurfaceSecondary,
                            border = BorderStroke(if (isStripeSelected) 2.dp else 1.dp, if (isStripeSelected) CleanSecondary else CleanBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Stripe Live",
                                    tint = if (isStripeSelected) CleanSecondary else CleanTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Stripe Live",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isStripeSelected) CleanSecondary else CleanTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$0 Std / 1% Inst",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isStripeSelected) CleanSecondary else CleanTextMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Dynamic Fields based on method
                    if (activeMethod == PayoutMethodType.BANK_WIRE) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = CleanSurfaceSecondary),
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
                                        text = "BANK WIRE DETAILS (ING BANK N.V.)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CleanPrimary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = CleanSuccessContainer,
                                        border = BorderStroke(1.dp, CleanSuccess.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "LIVE NL SEPA",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CleanSuccess,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = accountHolder,
                                    onValueChange = { accountHolder = it },
                                    label = { Text("Account Holder / Beneficiary Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CleanPrimary,
                                        unfocusedBorderColor = CleanBorder
                                    )
                                )

                                OutlinedTextField(
                                    value = bankName,
                                    onValueChange = { bankName = it },
                                    label = { Text("Bank Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CleanPrimary,
                                        unfocusedBorderColor = CleanBorder
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = ibanOrAccount,
                                        onValueChange = { ibanOrAccount = it },
                                        label = { Text("IBAN / Account Number") },
                                        modifier = Modifier.weight(1.3f),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CleanPrimary,
                                            unfocusedBorderColor = CleanBorder
                                        )
                                    )

                                    OutlinedTextField(
                                        value = swiftBic,
                                        onValueChange = { swiftBic = it.uppercase() },
                                        label = { Text("SWIFT / BIC") },
                                        modifier = Modifier.weight(0.9f),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CleanPrimary,
                                            unfocusedBorderColor = CleanBorder
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = routingNumber,
                                        onValueChange = { routingNumber = it },
                                        label = { Text("Routing / Sort Code (Optional)") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CleanPrimary,
                                            unfocusedBorderColor = CleanBorder
                                        )
                                    )

                                    OutlinedTextField(
                                        value = bankCountry,
                                        onValueChange = { bankCountry = it },
                                        label = { Text("Bank Country") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CleanPrimary,
                                            unfocusedBorderColor = CleanBorder
                                        )
                                    )
                                }
                            }
                        }
                    } else if (activeMethod == PayoutMethodType.PAYPAL) {
                        // PayPal Fields
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = CleanSurfaceSecondary),
                            border = BorderStroke(1.dp, CleanBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "PAYPAL RECIPIENT ACCOUNT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanPrimary
                                )

                                OutlinedTextField(
                                    value = paypalEmail,
                                    onValueChange = { paypalEmail = it },
                                    label = { Text("PayPal Email Address / PayPal ID") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CleanPrimary,
                                        unfocusedBorderColor = CleanBorder
                                    )
                                )

                                OutlinedTextField(
                                    value = paypalRecipient,
                                    onValueChange = { paypalRecipient = it },
                                    label = { Text("Recipient Name / Business Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CleanPrimary,
                                        unfocusedBorderColor = CleanBorder
                                    )
                                )

                                OutlinedTextField(
                                    value = paypalNote,
                                    onValueChange = { paypalNote = it },
                                    label = { Text("Payout Memo / Note") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CleanPrimary,
                                        unfocusedBorderColor = CleanBorder
                                    )
                                )
                            }
                        }
                    } else {
                        // Stripe Live Payouts API Panel
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("modal_stripe_details_card"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = CleanSurfaceSecondary),
                            border = BorderStroke(1.dp, CleanSecondary.copy(alpha = 0.5f))
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "STRIPE LIVE PAYOUTS (${stripePayoutState.environmentLabel})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CleanSecondary
                                        )
                                        Text(
                                            text = "Health: ${stripePayoutState.connectionHealth.label} • Secret: ${stripePayoutState.maskedKeyPreview}",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = CleanTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    TextButton(
                                        onClick = { viewModel.triggerVerifyStripeConnection() },
                                        enabled = !stripePayoutState.isCheckingConnection,
                                        modifier = Modifier.testTag("modal_stripe_verify_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudSync,
                                            contentDescription = "Verify Stripe",
                                            tint = CleanSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (stripePayoutState.isCheckingConnection) "Syncing" else "Sync API",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CleanSecondary
                                        )
                                    }
                                }

                                val modalAvail = stripePayoutState.availableBalances.firstOrNull()
                                if (modalAvail != null) {
                                    Text(
                                        text = "Live Stripe Available: ${currencyFormatter.format(modalAvail.majorAmount, modalAvail.currencyCode)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CleanSuccess
                                    )
                                }

                                if (!stripePayoutState.isKeyConfigured) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = CleanOrangeContainer.copy(alpha = 0.65f),
                                        border = BorderStroke(1.dp, CleanOrange.copy(alpha = 0.45f)),
                                        modifier = Modifier.fillMaxWidth()
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
                                                text = if (stripePayoutState.isPublishableKeyMistakenForSecret) {
                                                    "STRIPE_SECRET_KEY has a Publishable Key (pk_...). Set your Secret Key (sk_live_... or sk_test_...) in the AI Studio Secrets panel."
                                                } else {
                                                    "Configure STRIPE_SECRET_KEY and STRIPE_PUBLISHABLE_KEY in the AI Studio Secrets panel to send live payouts."
                                                },
                                                fontSize = 11.sp,
                                                color = CleanTextPrimary,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }

                                // Speed Selector (Standard vs Instant)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isStd = stripePayoutState.selectedSpeedMethod == "standard"
                                    OutlinedButton(
                                        onClick = { viewModel.setStripePayoutSpeed("standard") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .testTag("modal_stripe_speed_standard"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isStd) CleanSecondaryContainer else CleanSurface,
                                            contentColor = if (isStd) CleanSecondary else CleanTextSecondary
                                        ),
                                        border = BorderStroke(1.dp, if (isStd) CleanSecondary else CleanBorder)
                                    ) {
                                        Text("Standard ($0)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    val isInst = stripePayoutState.selectedSpeedMethod == "instant"
                                    OutlinedButton(
                                        onClick = { viewModel.setStripePayoutSpeed("instant") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .testTag("modal_stripe_speed_instant"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isInst) CleanOrangeContainer else CleanSurface,
                                            contentColor = if (isInst) CleanOrangeText else CleanTextSecondary
                                        ),
                                        border = BorderStroke(1.dp, if (isInst) CleanOrange else CleanBorder)
                                    ) {
                                        Text("Instant (1%)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (stripePayoutState.externalAccounts.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        stripePayoutState.externalAccounts.forEach { ext ->
                                            val isDest = stripePayoutState.selectedDestinationId == ext.id
                                            FilterChip(
                                                selected = isDest,
                                                onClick = { viewModel.selectStripeDestination(ext.id) },
                                                label = {
                                                    Text(
                                                        text = "${ext.label} ••••${ext.last4}",
                                                        fontSize = 10.sp
                                                    )
                                                },
                                                modifier = Modifier.testTag("modal_stripe_dest_${ext.id}")
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = stripeDescription,
                                    onValueChange = { stripeDescription = it },
                                    label = { Text("Stripe Statement Description") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CleanSecondary,
                                        unfocusedBorderColor = CleanBorder
                                    )
                                )

                                val displayError = stripeInlineError ?: stripePayoutState.errorMessage
                                if (!displayError.isNullOrBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = CleanDangerContainer,
                                        border = BorderStroke(1.dp, CleanDanger.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = CleanDanger,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = displayError,
                                                fontSize = 11.sp,
                                                color = CleanDanger
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Payout Amount & Currency Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = CleanSurfaceVariant),
                        border = BorderStroke(1.dp, CleanBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "PAYOUT AMOUNT & CURRENCY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextMuted
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
                                    prefix = { Text("$", color = CleanTextPrimary, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CleanPrimary,
                                        unfocusedBorderColor = CleanBorder
                                    )
                                )

                                // Currency button
                                Surface(
                                    onClick = { isCurrencyDropdownOpen = !isCurrencyDropdownOpen },
                                    shape = RoundedCornerShape(14.dp),
                                    color = CleanSurface,
                                    border = BorderStroke(1.dp, CleanBorder),
                                    modifier = Modifier.height(56.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(selectedPayoutCurrency.flagEmoji, fontSize = 18.sp)
                                        Text(selectedPayoutCurrency.code, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CleanTextPrimary)
                                    }
                                }
                            }

                            // Quick Presets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("50.00", "100.00", "250.00", "500.00", "1000.00").forEach { preset ->
                                    Surface(
                                        onClick = { payoutAmountInput = preset },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (payoutAmountInput == preset) CleanPrimaryContainer else CleanSurface,
                                        border = BorderStroke(1.dp, if (payoutAmountInput == preset) CleanPrimary else CleanBorderSubtle),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "$${preset.substringBefore(".")}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (payoutAmountInput == preset) CleanPrimary else CleanTextSecondary,
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // Converted Rate Indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = null,
                                        tint = CleanPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "1 USD = ${String.format(Locale.US, "%.4f", selectedPayoutCurrency.exchangeRate)} ${selectedPayoutCurrency.code}",
                                        fontSize = 11.sp,
                                        color = CleanTextSecondary
                                    )
                                }

                                Text(
                                    text = "Converted: ${selectedPayoutCurrency.symbol}${String.format(Locale.US, "%.2f", convertedAmount)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanPrimary
                                )
                            }
                        }
                    }

                    // Fee & Net Settlement Breakdown Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CleanSurfaceSecondary),
                        border = BorderStroke(1.dp, CleanBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Gross Disbursement:", fontSize = 12.sp, color = CleanTextSecondary)
                                Text("$${String.format(Locale.US, "%.2f", parsedAmountUsd)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CleanTextPrimary)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Estimated ${activeMethod.displayName} Fee:", fontSize = 12.sp, color = CleanTextSecondary)
                                Text("-$${String.format(Locale.US, "%.2f", fee)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFDC2626))
                            }

                            HorizontalDivider(color = CleanBorderSubtle)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Net Disbursed to Account:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CleanTextPrimary)
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", netAmountUsd)} USD",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CleanSuccess
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Icon(
                                    imageVector = when (activeMethod) {
                                        PayoutMethodType.PAYPAL -> Icons.Default.FlashOn
                                        PayoutMethodType.STRIPE_PAYOUT -> Icons.Default.Bolt
                                        PayoutMethodType.BANK_WIRE -> Icons.Default.AccountBalance
                                    },
                                    contentDescription = null,
                                    tint = if (activeMethod == PayoutMethodType.STRIPE_PAYOUT) CleanSecondary else CleanPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = when (activeMethod) {
                                        PayoutMethodType.PAYPAL -> "Instant settlement to PayPal account"
                                        PayoutMethodType.STRIPE_PAYOUT -> if (stripePayoutState.selectedSpeedMethod == "instant") {
                                            "Stripe Instant Payout: arrives in minutes (1% fee)"
                                        } else {
                                            "Stripe Standard Payout: 1-2 business days ($0 fee)"
                                        }
                                        PayoutMethodType.BANK_WIRE -> "SWIFT interbank wire: 1-2 business days"
                                    },
                                    fontSize = 11.sp,
                                    color = if (activeMethod == PayoutMethodType.STRIPE_PAYOUT) CleanSecondary else CleanPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Security & Compliance Notice
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security",
                            tint = CleanSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "256-bit TLS encrypted • ISO 20022 / Stripe & PayPal Payouts API",
                            fontSize = 11.sp,
                            color = CleanTextSecondary
                        )
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                // Save as default configuration
                                if (activeMethod == PayoutMethodType.BANK_WIRE) {
                                    viewModel.updateBankWireDetails(
                                        BankWireDetails(
                                            bankName = bankName,
                                            accountHolderName = accountHolder,
                                            accountNumberOrIban = ibanOrAccount,
                                            swiftBic = swiftBic,
                                            routingNumber = routingNumber,
                                            country = bankCountry
                                        )
                                    )
                                } else if (activeMethod == PayoutMethodType.PAYPAL) {
                                    viewModel.updatePayPalDetails(
                                        PayPalDetails(
                                            paypalEmailOrId = paypalEmail,
                                            recipientName = paypalRecipient
                                        )
                                    )
                                }
                                viewModel.selectPayoutMethod(activeMethod)
                                Toast.makeText(context, "${activeMethod.displayName} saved as default payout method!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(0.9f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, CleanBorder)
                        ) {
                            Text("Save as Default", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CleanTextPrimary)
                        }

                        Button(
                            onClick = {
                                if (parsedAmountUsd <= 0.0) {
                                    Toast.makeText(context, "Please enter a valid payout amount", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (activeMethod == PayoutMethodType.STRIPE_PAYOUT) {
                                    stripeInlineError = null
                                    isDisbursing = true
                                    viewModel.executeLiveStripePayout(
                                        amountInUsd = parsedAmountUsd,
                                        currencyCode = selectedPayoutCurrency.code,
                                        speedMethod = stripePayoutState.selectedSpeedMethod,
                                        description = stripeDescription.ifBlank { null },
                                        onResult = { ok, msg ->
                                            isDisbursing = false
                                            if (ok) {
                                                completedReference = viewModel.lastDisbursedPayoutReference.value ?: "po_stripe_live"
                                                completedAmount = parsedAmountUsd
                                            } else {
                                                stripeInlineError = msg
                                            }
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                    return@Button
                                }
                                isDisbursing = true
                                scope.launch {
                                    delay(900) // Simulated processing animation
                                    val recipient = if (activeMethod == PayoutMethodType.BANK_WIRE) accountHolder else paypalRecipient
                                    val destination = if (activeMethod == PayoutMethodType.BANK_WIRE) ibanOrAccount else paypalEmail

                                    viewModel.disbursePayout(
                                        method = activeMethod,
                                        amountInUsd = parsedAmountUsd,
                                        currencyCode = selectedPayoutCurrency.code,
                                        recipientName = recipient,
                                        destinationIdentifier = destination,
                                        notes = if (activeMethod == PayoutMethodType.PAYPAL) paypalNote else "Wire to $bankName ($swiftBic)",
                                        onSuccess = { _ ->
                                            completedReference = viewModel.lastDisbursedPayoutReference.value
                                            completedAmount = parsedAmountUsd
                                            isDisbursing = false
                                        }
                                    )
                                }
                            },
                            enabled = !isDisbursing && parsedAmountUsd > 0.0,
                            modifier = Modifier
                                .weight(1.1f)
                                .height(50.dp)
                                .testTag("confirm_payout_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeMethod == PayoutMethodType.STRIPE_PAYOUT) CleanSecondary else CleanPrimary
                            )
                        ) {
                            if (isDisbursing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = CleanOnPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (activeMethod == PayoutMethodType.STRIPE_PAYOUT) "Calling Stripe..." else "Disbursing...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = "Disburse ${activeMethod.displayName}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

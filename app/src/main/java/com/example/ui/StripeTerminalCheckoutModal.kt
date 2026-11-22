package com.example.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.StripeCardValidator
import com.example.data.StripePayoutGateway
import com.example.data.StripeTerminalPaymentMode
import com.example.data.StripeTerminalPaymentRequest
import com.example.data.StripeTerminalRailMapper
import com.example.ui.theme.CleanBorder
import com.example.ui.theme.CleanBorderLight
import com.example.ui.theme.CleanDanger
import com.example.ui.theme.CleanDangerContainer
import com.example.ui.theme.CleanOnPrimary
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
import com.example.ui.theme.StripeIndigo
import com.example.ui.theme.StripeIndigoContainer
import com.example.ui.theme.StripeOnIndigo
import com.example.util.CurrencyFormatter
import com.example.data.StripePaymentSheetPreparedConfig
import com.stripe.android.paymentsheet.PaymentSheet
import java.util.Locale

private enum class CheckoutModalTab {
    CARD,
    LOCAL_RAIL,
    PAYMENT_SHEET
}

/**
 * Stripe SDK-powered Localized Sales Terminal Checkout Modal.
 *
 * Guides the terminal through:
 * 1. Localized currency & regional payment rail / Card selection
 * 2. PCI-safe in-memory Luhn + expiry + masked CVC validation & Stripe SDK PaymentMethodCreateParams tokenization
 * 3. Optional EMV 3DS2 / Strong Customer Authentication (SCA) / localized wallet challenge
 * 4. Tokenized receipt & automated Room ledger persistence
 */
@Composable
fun StripeTerminalCheckoutModal(
    viewModel: GlobalMarketViewModel,
    onDismiss: () -> Unit,
    onPresentLivePaymentSheet: ((StripePaymentSheetPreparedConfig, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val checkoutState by viewModel.stripeTerminalCheckoutState.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val amountUsd by viewModel.amountUsd.collectAsStateWithLifecycle()
    val liveRates by viewModel.liveRateMap.collectAsStateWithLifecycle()
    val rateChangePercents by viewModel.rateChangePercents.collectAsStateWithLifecycle()
    val isRateRefreshing by viewModel.isRateRefreshing.collectAsStateWithLifecycle()
    val lastRefreshTime by viewModel.lastRateRefreshTimestamp.collectAsStateWithLifecycle()
    val selectedPaymentRail by viewModel.selectedPaymentRail.collectAsStateWithLifecycle()

    val cardHolder by viewModel.cardHolderName.collectAsStateWithLifecycle()
    val cardNumber by viewModel.cardNumber.collectAsStateWithLifecycle()
    val expiry by viewModel.expiryDate.collectAsStateWithLifecycle()
    val cvv by viewModel.cvv.collectAsStateWithLifecycle()
    val country by viewModel.selectedCountry.collectAsStateWithLifecycle()

    val openToPaymentSheet by viewModel.openCheckoutToPaymentSheet.collectAsStateWithLifecycle()
    var activeTab by remember(openToPaymentSheet) {
        mutableStateOf(
            if (openToPaymentSheet) CheckoutModalTab.PAYMENT_SHEET else CheckoutModalTab.CARD
        )
    }
    var paymentMode by remember { mutableStateOf(StripeTerminalPaymentMode.CARD) }
    var force3dsChallenge by remember { mutableStateOf(false) }
    var idealBankCode by remember { mutableStateOf("ing") }
    var sepaIban by remember { mutableStateOf("NL43INGB0006372146") }
    var validationErrorText by remember { mutableStateOf<String?>(null) }

    val fallbackPaymentSheet = if (onPresentLivePaymentSheet == null) {
        PaymentSheet.Builder { result ->
            viewModel.onNativePaymentSheetResult(result)
        }.build()
    } else {
        null
    }

    LaunchedEffect(Unit) {
        viewModel.initializeStripeTerminalSdk(context)
    }

    LaunchedEffect(selectedCurrency.code) {
        if (country.isBlank() || country == "United States" || viewModel.currencies.any { it.defaultCountry == country }) {
            viewModel.selectedCountry.value = selectedCurrency.defaultCountry
        }
    }

    val currentAmountUsd = amountUsd.toDoubleOrNull()?.takeIf { it > 0.0 } ?: 49.99
    val currentRate = liveRates[selectedCurrency.code] ?: selectedCurrency.exchangeRate
    val convertedTotal = currentAmountUsd * currentRate
    val minorUnits = StripePayoutGateway.toMinorUnits(convertedTotal, selectedCurrency.code)
    val minorLabel = if (selectedCurrency.code.uppercase(Locale.US) == "JPY") "yen" else "minor units"
    val formattedLocalizedTotal = CurrencyFormatter.DEFAULT.format(convertedTotal, selectedCurrency.code)

    val availableRails = remember(selectedCurrency.code) {
        viewModel.getPaymentRailsForCurrency(selectedCurrency.code)
    }
    val activeRail = selectedPaymentRail ?: availableRails.first()
    val activeRailSpec = remember(activeRail.id, selectedCurrency.code) {
        StripeTerminalRailMapper.resolveSpec(activeRail.id, selectedCurrency.code)
    }
    val detectedBrand = remember(cardNumber) { StripeCardValidator.detectBrand(cardNumber) }

    val isBusy = checkoutState is StripeTerminalCheckoutState.ValidatingInput ||
        checkoutState is StripeTerminalCheckoutState.TokenizingWithStripeSdk ||
        checkoutState is StripeTerminalCheckoutState.ConfirmingPaymentIntent

    val sdkEnvBadge = remember { viewModel.getStripeSdkEnvironmentBadge() }
    val keyHint = remember { viewModel.getStripePublishableKeyHint() }

    Dialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isBusy,
            dismissOnClickOutside = !isBusy
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 760.dp)
                .testTag("stripe_terminal_checkout_modal"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = CleanSurface),
            border = BorderStroke(1.5.dp, StripeIndigo.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
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
                                    Brush.linearGradient(
                                        listOf(StripeIndigo, CleanPrimary)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Stripe SDK Protected Terminal",
                                tint = StripeOnIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Stripe Terminal Checkout",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CleanTextPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = StripeIndigoContainer
                            ) {
                                Text(
                                    text = sdkEnvBadge,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StripeIndigo,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { if (!isBusy) onDismiss() },
                        enabled = !isBusy,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("stripe_terminal_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Stripe Terminal Checkout",
                            tint = CleanTextSecondary
                        )
                    }
                }

                when (val state = checkoutState) {
                    is StripeTerminalCheckoutState.Idle,
                    is StripeTerminalCheckoutState.Failed -> {
                        // Localized Amount & Stripe Minor-Unit Payload Card
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = CleanSurfaceVariant,
                            border = BorderStroke(1.dp, CleanBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "LOCALIZED TERMINAL TOTAL",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = CleanTextMuted,
                                            letterSpacing = 0.6.sp
                                        )
                                        Text(
                                            text = "${selectedCurrency.flagEmoji} $formattedLocalizedTotal",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = CleanTextPrimary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$${String.format(Locale.US, "%.2f", currentAmountUsd)} USD",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CleanPrimary
                                        )
                                        Text(
                                            text = "$minorUnits $minorLabel (${selectedCurrency.code.lowercase(Locale.US)})",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = CleanTextSecondary
                                        )
                                    }
                                }

                                // Currency Quick Selector
                                RealTimeCurrencyConversionTool(
                                    selectedCurrency = selectedCurrency,
                                    currencies = viewModel.currencies,
                                    amountUsd = currentAmountUsd,
                                    liveRateMap = liveRates,
                                    rateChangePercents = rateChangePercents,
                                    lastRefreshTimestamp = lastRefreshTime,
                                    isRefreshing = isRateRefreshing,
                                    onCurrencySelected = { viewModel.selectCurrency(it) },
                                    onRefreshRates = { viewModel.refreshExchangeRates() },
                                    compactMode = true
                                )
                            }
                        }

                        // Failure Banner if Failed
                        if (state is StripeTerminalCheckoutState.Failed) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = CleanDangerContainer,
                                border = BorderStroke(1.dp, CleanDanger.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("stripe_terminal_error_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = "Payment Error",
                                        tint = CleanDanger,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Stripe Error: ${state.errorCode}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CleanDanger
                                        )
                                        Text(
                                            text = state.message,
                                            fontSize = 11.sp,
                                            color = CleanTextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Inline Validation Banner
                        AnimatedVisibility(visible = validationErrorText != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CleanDangerContainer,
                                border = BorderStroke(1.dp, CleanDanger.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = validationErrorText ?: "",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CleanDanger,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        // Payment Method Mode Switcher: Card (Stripe SDK), Localized Rail, & PaymentSheet
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CleanSurfaceVariant)
                                .border(BorderStroke(1.dp, CleanBorder), RoundedCornerShape(14.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                onClick = {
                                    activeTab = CheckoutModalTab.CARD
                                    paymentMode = StripeTerminalPaymentMode.CARD
                                    validationErrorText = null
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (activeTab == CheckoutModalTab.CARD) StripeIndigo else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("stripe_terminal_mode_card")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CreditCard,
                                        contentDescription = null,
                                        tint = if (activeTab == CheckoutModalTab.CARD) StripeOnIndigo else CleanTextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Card",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (activeTab == CheckoutModalTab.CARD) StripeOnIndigo else CleanTextSecondary
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    activeTab = CheckoutModalTab.LOCAL_RAIL
                                    paymentMode = StripeTerminalPaymentMode.LOCAL_RAIL
                                    validationErrorText = null
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (activeTab == CheckoutModalTab.LOCAL_RAIL) CleanPrimary else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("stripe_terminal_mode_local_rail")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = if (activeTab == CheckoutModalTab.LOCAL_RAIL) CleanOnPrimary else CleanTextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = activeRail.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (activeTab == CheckoutModalTab.LOCAL_RAIL) CleanOnPrimary else CleanTextSecondary
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    activeTab = CheckoutModalTab.PAYMENT_SHEET
                                    validationErrorText = null
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (activeTab == CheckoutModalTab.PAYMENT_SHEET) StripeIndigo else Color.Transparent,
                                modifier = Modifier
                                    .weight(1.1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("stripe_terminal_mode_payment_sheet")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingBag,
                                        contentDescription = null,
                                        tint = if (activeTab == CheckoutModalTab.PAYMENT_SHEET) StripeOnIndigo else CleanTextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "PaymentSheet",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (activeTab == CheckoutModalTab.PAYMENT_SHEET) StripeOnIndigo else CleanTextSecondary
                                    )
                                }
                            }
                        }

                        if (activeTab == CheckoutModalTab.PAYMENT_SHEET) {
                            StripePaymentSheetSection(
                                viewModel = viewModel,
                                onPresentLivePaymentSheet = { prepared, secret ->
                                    if (onPresentLivePaymentSheet != null) {
                                        onPresentLivePaymentSheet(prepared, secret)
                                    } else {
                                        fallbackPaymentSheet?.presentWithPaymentIntent(
                                            paymentIntentClientSecret = secret,
                                            configuration = prepared.configuration
                                        )
                                    }
                                }
                            )
                        } else if (activeTab == CheckoutModalTab.CARD) {
                            // Stripe Official Test Card Presets
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Stripe Sandbox Test Card Presets",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanTextSecondary
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StripeTestCardChip(
                                        label = "Visa 4242 (Instant)",
                                        tag = "stripe_terminal_preset_visa",
                                        onClick = {
                                            viewModel.cardNumber.value = "4242424242424242"
                                            viewModel.expiryDate.value = "12/28"
                                            viewModel.cvv.value = "424"
                                            if (cardHolder.isBlank()) viewModel.cardHolderName.value = "Elena Rostova"
                                            force3dsChallenge = false
                                            validationErrorText = null
                                        }
                                    )
                                    StripeTestCardChip(
                                        label = "3DS2 Challenge 3220",
                                        tag = "stripe_terminal_preset_3ds",
                                        onClick = {
                                            viewModel.cardNumber.value = "4000000000003220"
                                            viewModel.expiryDate.value = "12/28"
                                            viewModel.cvv.value = "424"
                                            if (cardHolder.isBlank()) viewModel.cardHolderName.value = "Elena Rostova"
                                            force3dsChallenge = true
                                            validationErrorText = null
                                        }
                                    )
                                    StripeTestCardChip(
                                        label = "Decline 0002",
                                        tag = "stripe_terminal_preset_decline",
                                        onClick = {
                                            viewModel.cardNumber.value = "4000000000000002"
                                            viewModel.expiryDate.value = "12/28"
                                            viewModel.cvv.value = "424"
                                            if (cardHolder.isBlank()) viewModel.cardHolderName.value = "Elena Rostova"
                                            force3dsChallenge = false
                                            validationErrorText = null
                                        }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = cardNumber,
                                onValueChange = { raw ->
                                    val digits = StripeCardValidator.digitsOnly(raw).take(19)
                                    viewModel.cardNumber.value = digits
                                    validationErrorText = null
                                },
                                label = { Text("Card Number") },
                                placeholder = { Text("4242 4242 4242 4242") },
                                trailingIcon = {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = StripeIndigoContainer
                                    ) {
                                        Text(
                                            text = detectedBrand.displayName.uppercase(Locale.US),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = StripeIndigo,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("stripe_terminal_card_number_input"),
                                shape = RoundedCornerShape(14.dp),
                                colors = terminalFieldColors()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = expiry,
                                    onValueChange = { raw ->
                                        val cleaned = StripeCardValidator.digitsOnly(raw).take(4)
                                        viewModel.expiryDate.value = if (cleaned.length > 2) {
                                            "${cleaned.take(2)}/${cleaned.drop(2)}"
                                        } else {
                                            cleaned
                                        }
                                        validationErrorText = null
                                    },
                                    label = { Text("Expiry (MM/YY)") },
                                    placeholder = { Text("12/28") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("stripe_terminal_expiry_input"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = terminalFieldColors()
                                )
                                OutlinedTextField(
                                    value = cvv,
                                    onValueChange = { raw ->
                                        viewModel.cvv.value = StripeCardValidator.digitsOnly(raw).take(4)
                                        validationErrorText = null
                                    },
                                    label = { Text("CVC") },
                                    placeholder = { Text("424") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("stripe_terminal_cvc_input"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = terminalFieldColors()
                                )
                            }
                        } else {
                            // LOCALIZED REGIONAL RAIL PANEL (iDEAL / SEPA / Bancontact / Konbini / Alipay / Pix / etc.)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Localized Rail (${selectedCurrency.code})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanTextSecondary
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    availableRails.forEach { rail ->
                                        val isRailSelected = activeRail.id == rail.id
                                        Surface(
                                            onClick = {
                                                viewModel.selectPaymentRail(rail)
                                                validationErrorText = null
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isRailSelected) CleanSecondaryContainer else CleanSurfaceVariant,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isRailSelected) CleanSecondary else CleanBorderLight
                                            ),
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = rail.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CleanTextPrimary
                                                )
                                                Text(
                                                    text = "${rail.feeText} • ${rail.settlementTime}",
                                                    fontSize = 10.sp,
                                                    color = CleanTextSecondary
                                                )
                                            }
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = StripeIndigoContainer.copy(alpha = 0.65f),
                                    border = BorderStroke(1.dp, StripeIndigo.copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Stripe PaymentMethod Type: ${activeRailSpec.stripeMethodCode}",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = StripeIndigo
                                        )
                                        Text(
                                            text = activeRailSpec.localizedPrompt,
                                            fontSize = 11.sp,
                                            color = CleanTextSecondary
                                        )
                                    }
                                }

                                if (activeRailSpec.stripeMethodCode == "ideal") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("ing" to "ING Bank", "rabobank" to "Rabobank", "abn_amro" to "ABN AMRO").forEach { (code, label) ->
                                            Surface(
                                                onClick = { idealBankCode = code },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (idealBankCode == code) CleanPrimaryContainer else CleanSurfaceVariant,
                                                border = BorderStroke(1.dp, if (idealBankCode == code) CleanPrimary else CleanBorder),
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (idealBankCode == code) CleanPrimary else CleanTextSecondary,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                                )
                                            }
                                        }
                                    }
                                } else if (activeRailSpec.stripeMethodCode == "sepa_debit") {
                                    OutlinedTextField(
                                        value = sepaIban,
                                        onValueChange = {
                                            sepaIban = it.uppercase(Locale.US)
                                            validationErrorText = null
                                        },
                                        label = { Text("SEPA IBAN") },
                                        placeholder = { Text("NL43INGB0006372146") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("stripe_terminal_iban_input"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = terminalFieldColors()
                                    )
                                }
                            }
                        }

                        if (activeTab != CheckoutModalTab.PAYMENT_SHEET) {
                            // Customer Name & Settlement Country
                            OutlinedTextField(
                                value = cardHolder,
                                onValueChange = {
                                    viewModel.cardHolderName.value = it
                                    validationErrorText = null
                                },
                                label = { Text("Customer / Cardholder Name") },
                                placeholder = { Text("Elena Rostova") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("stripe_terminal_holder_input"),
                                shape = RoundedCornerShape(14.dp),
                                colors = terminalFieldColors()
                            )

                            OutlinedTextField(
                                value = country,
                                onValueChange = {
                                    viewModel.selectedCountry.value = it
                                    validationErrorText = null
                                },
                                label = { Text("Billing Country / Market") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("stripe_terminal_country_input"),
                                shape = RoundedCornerShape(14.dp),
                                colors = terminalFieldColors()
                            )

                            // Optional 3DS2 SCA Challenge Toggle
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = CleanSurfaceSecondary,
                                border = BorderStroke(1.dp, CleanBorderLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Enforce 3DS2 / SCA Challenge",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CleanTextPrimary
                                        )
                                        Text(
                                            text = "Simulate European Strong Customer Authentication modal",
                                            fontSize = 10.sp,
                                            color = CleanTextSecondary
                                        )
                                    }
                                    Switch(
                                        checked = force3dsChallenge,
                                        onCheckedChange = { force3dsChallenge = it },
                                        colors = SwitchDefaults.colors(checkedTrackColor = StripeIndigo),
                                        modifier = Modifier.testTag("stripe_terminal_3ds_toggle")
                                    )
                                }
                            }

                            // Tokenize & Pay CTA
                            Button(
                                onClick = {
                                    val request = StripeTerminalPaymentRequest(
                                        amountUsd = currentAmountUsd,
                                        targetCurrencyCode = selectedCurrency.code,
                                        exchangeRate = currentRate,
                                        localizedAmountMajor = convertedTotal,
                                        paymentMode = paymentMode,
                                        railId = activeRail.id,
                                        railName = if (paymentMode == StripeTerminalPaymentMode.CARD) {
                                            "Stripe SDK Card"
                                        } else {
                                            activeRail.name
                                        },
                                        customerName = cardHolder.ifBlank { "Terminal Customer" },
                                        countryName = country.ifBlank { selectedCurrency.defaultCountry },
                                        cardNumber = cardNumber,
                                        expiryMmYy = expiry,
                                        cvc = cvv,
                                        idealBankCode = idealBankCode,
                                        sepaIban = sepaIban,
                                        force3dsChallenge = force3dsChallenge
                                    )
                                    val check = StripeCardValidator.validateRequest(request)
                                    if (!check.isValid) {
                                        validationErrorText = check.cardNumberError
                                            ?: check.expiryError
                                            ?: check.cvcError
                                            ?: check.ibanError
                                            ?: check.holderNameError
                                    } else {
                                        validationErrorText = null
                                        viewModel.startStripeTerminalPayment(context, request)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("checkout_pay_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StripeIndigo,
                                    contentColor = StripeOnIndigo
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tokenize & Pay $formattedLocalizedTotal",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // PCI Security & Publishable Key Footer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = StripeIndigo,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PCI-safe • Key: $keyHint • Raw PAN/CVC never stored",
                                fontSize = 10.sp,
                                color = CleanTextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    is StripeTerminalCheckoutState.ValidatingInput,
                    is StripeTerminalCheckoutState.TokenizingWithStripeSdk,
                    is StripeTerminalCheckoutState.ConfirmingPaymentIntent -> {
                        val stepTitle = when (state) {
                            is StripeTerminalCheckoutState.ValidatingInput -> "Validating PCI-Safe Parameters..."
                            is StripeTerminalCheckoutState.TokenizingWithStripeSdk ->
                                "Tokenizing ${state.railLabel} (${state.currencyCode}) via Stripe SDK..."
                            is StripeTerminalCheckoutState.ConfirmingPaymentIntent ->
                                "Settling PaymentIntent ${state.paymentIntentId}..."
                            else -> "Processing..."
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(
                                color = StripeIndigo,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = stepTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "PaymentConfiguration & PaymentMethodCreateParams active",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = StripeIndigo,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    is StripeTerminalCheckoutState.Requires3dsAction -> {
                        val ch = state.challenge
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("stripe_terminal_3ds_challenge_panel"),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = StripeIndigoContainer,
                                border = BorderStroke(1.dp, StripeIndigo.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VerifiedUser,
                                            contentDescription = "3DS2 Verification",
                                            tint = StripeIndigo,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = "3DS2 / SCA Authentication Required",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = StripeIndigo
                                        )
                                    }
                                    Text(
                                        text = ch.challengeReason,
                                        fontSize = 12.sp,
                                        color = CleanTextPrimary
                                    )
                                    HorizontalDivider(color = StripeIndigo.copy(alpha = 0.2f))
                                    ReceiptKeyValueRow("Amount", "${ch.localizedAmountFormatted} (${ch.currencyCode})")
                                    ReceiptKeyValueRow("Instrument", "${ch.cardBrand} •••• ${ch.last4}")
                                    ReceiptKeyValueRow("PaymentIntent", ch.paymentIntentId)
                                    ReceiptKeyValueRow("PaymentMethod", ch.paymentMethodId)
                                    ReceiptKeyValueRow("Minor Units", "${ch.minorUnits} (${ch.currencyCode.lowercase(Locale.US)})")
                                }
                            }

                            Button(
                                onClick = { viewModel.completeSimulated3dsChallenge() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("stripe_terminal_3ds_approve_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CleanSuccess,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Approve & Settle ${ch.localizedAmountFormatted}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.cancelOrFailSimulated3dsChallenge(cancelledByUser = true) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("stripe_terminal_3ds_cancel_button"),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, CleanDanger),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CleanDanger)
                            ) {
                                Text(
                                    text = "Cancel / Fail Authentication",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    is StripeTerminalCheckoutState.Succeeded -> {
                        val r = state.receipt
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("stripe_terminal_success_panel"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .background(CleanTertiaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Stripe Transaction Settled",
                                    tint = CleanTertiary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }

                            Text(
                                text = "Localized Transaction Settled",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CleanTextPrimary,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "${r.localizedAmountFormatted} (${r.currencyCode}) • $${String.format(Locale.US, "%.2f", r.amountUsd)} USD",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanSuccess
                            )

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = CleanSurfaceVariant,
                                border = BorderStroke(1.dp, CleanBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ReceiptKeyValueRow("Reference", r.referenceCode)
                                    ReceiptKeyValueRow("PaymentIntent", r.paymentIntentId)
                                    ReceiptKeyValueRow("PaymentMethod", r.paymentMethodId)
                                    ReceiptKeyValueRow("Instrument", "${r.cardBrand} •••• ${r.last4}")
                                    ReceiptKeyValueRow("Stripe Rail", "${r.railName} (${r.stripeMethodCode})")
                                    ReceiptKeyValueRow("SCA Status", r.scaStatus)
                                    ReceiptKeyValueRow("Minor Payload", "${r.minorUnits} ${r.currencyCode.lowercase(Locale.US)}")
                                    ReceiptKeyValueRow("Ledger", "Saved to Room SQLite (PCI-safe)")
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.resetStripeTerminalCheckout() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("stripe_terminal_new_tx_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, CleanPrimary),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CleanPrimary)
                                ) {
                                    Text("New Sale", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Button(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("stripe_terminal_done_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = StripeIndigo,
                                        contentColor = StripeOnIndigo
                                    )
                                ) {
                                    Text("Done", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
private fun StripeTestCardChip(
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = StripeIndigoContainer,
        border = BorderStroke(1.dp, StripeIndigo.copy(alpha = 0.35f)),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag(tag)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = StripeIndigo,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun ReceiptKeyValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = CleanTextMuted
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = CleanTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun terminalFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = CleanTextPrimary,
    unfocusedTextColor = CleanTextPrimary,
    focusedBorderColor = StripeIndigo,
    unfocusedBorderColor = CleanBorder,
    focusedContainerColor = CleanSurfaceVariant,
    unfocusedContainerColor = CleanSurfaceVariant
)

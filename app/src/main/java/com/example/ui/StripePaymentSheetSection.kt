package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CreditScore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.SimulatedPaymentSheetOutcome
import com.example.data.StripePaymentSheetConfigFactory
import com.example.data.StripePaymentSheetPreparedConfig
import com.example.ui.theme.CleanBorder
import com.example.ui.theme.CleanBorderLight
import com.example.ui.theme.CleanPrimary
import com.example.ui.theme.CleanPrimaryContainer
import com.example.ui.theme.CleanSecondary
import com.example.ui.theme.CleanSecondaryContainer
import com.example.ui.theme.CleanSuccess
import com.example.ui.theme.CleanSurfaceSecondary
import com.example.ui.theme.CleanSurfaceVariant
import com.example.ui.theme.CleanTextMuted
import com.example.ui.theme.CleanTextPrimary
import com.example.ui.theme.CleanTextSecondary
import com.example.ui.theme.StripeIndigo
import com.example.ui.theme.StripeIndigoContainer
import com.example.ui.theme.StripeOnIndigo
import com.stripe.android.paymentsheet.PaymentSheet
import java.util.Locale

/**
 * Interactive Stripe [PaymentSheet.Configuration] builder & simulated customer checkout panel.
 *
 * Configures merchant display identity, [PaymentSheet.CustomerConfiguration],
 * [PaymentSheet.BillingDetails], [PaymentSheet.Appearance], [PaymentSheet.GooglePayConfiguration],
 * [PaymentSheet.BillingDetailsCollectionConfiguration], delayed payment rails, and
 * [PaymentSheet.IntentConfiguration] for simulated customer checkout runs.
 */
@Composable
fun StripePaymentSheetSection(
    viewModel: GlobalMarketViewModel,
    onPresentLivePaymentSheet: (StripePaymentSheetPreparedConfig, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val amountUsd by viewModel.amountUsd.collectAsStateWithLifecycle()
    val liveRates by viewModel.liveRateMap.collectAsStateWithLifecycle()
    val cardHolder by viewModel.cardHolderName.collectAsStateWithLifecycle()
    val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()

    val selectedCustomer by viewModel.selectedSimulatedCustomer.collectAsStateWithLifecycle()
    val allowsDelayed by viewModel.paymentSheetAllowsDelayedMethods.collectAsStateWithLifecycle()
    val googlePayEnabled by viewModel.paymentSheetGooglePayEnabled.collectAsStateWithLifecycle()
    val collectFullAddress by viewModel.paymentSheetCollectFullAddress.collectAsStateWithLifecycle()
    val layoutMode by viewModel.paymentSheetLayout.collectAsStateWithLifecycle()
    val simulatedOutcome by viewModel.paymentSheetSimulatedOutcome.collectAsStateWithLifecycle()
    val clientSecret by viewModel.paymentSheetLiveClientSecret.collectAsStateWithLifecycle()
    val preparedConfigState by viewModel.preparedPaymentSheetConfig.collectAsStateWithLifecycle()

    var showAdvancedLiveIntent by remember { mutableStateOf(false) }

    // Keep official Stripe PaymentSheet.Configuration synchronized whenever parameters change
    LaunchedEffect(
        selectedCurrency.code,
        amountUsd,
        liveRates,
        cardHolder,
        selectedCountry,
        selectedCustomer,
        allowsDelayed,
        googlePayEnabled,
        collectFullAddress,
        layoutMode,
        clientSecret
    ) {
        viewModel.preparePaymentSheetConfiguration(context)
    }

    val prepared = preparedConfigState ?: remember(
        selectedCurrency.code,
        amountUsd,
        selectedCustomer,
        allowsDelayed,
        googlePayEnabled,
        collectFullAddress,
        layoutMode,
        clientSecret
    ) {
        viewModel.preparePaymentSheetConfiguration(context)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("paymentsheet_config_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Banner
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = StripeIndigoContainer.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, StripeIndigo.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SettingsSuggest,
                    contentDescription = "Stripe PaymentSheet Configuration",
                    tint = StripeIndigo,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.paymentsheet_config_title),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = StripeIndigo
                    )
                    Text(
                        text = "Official PaymentSheet.Configuration • Deferred Intent & Customer Sheet Ready",
                        fontSize = 10.sp,
                        color = CleanTextSecondary
                    )
                }
            }
        }

        // 1. Simulated Customer Preset Selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.paymentsheet_customer_presets_label),
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
                viewModel.simulatedCheckoutCustomers.forEach { customer ->
                    val isSelected = selectedCustomer.id == customer.id
                    Surface(
                        onClick = { viewModel.selectSimulatedCustomer(customer) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) StripeIndigoContainer else CleanSurfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) StripeIndigo else CleanBorderLight
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("paymentsheet_customer_chip_${customer.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = if (isSelected) StripeIndigo else CleanTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = customer.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) StripeIndigo else CleanTextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (customer.isGuest) {
                                        "Guest • Collects Billing"
                                    } else {
                                        "${customer.countryIso2} • ${customer.defaultCardBrand} ${customer.defaultCardLast4}"
                                    },
                                    fontSize = 9.sp,
                                    color = CleanTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. PaymentSheet Layout Selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "PaymentMethodLayout",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CleanTextSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    PaymentSheet.PaymentMethodLayout.Automatic to "Auto",
                    PaymentSheet.PaymentMethodLayout.Horizontal to "Horizontal",
                    PaymentSheet.PaymentMethodLayout.Vertical to "Vertical"
                ).forEach { (layout, label) ->
                    val active = layoutMode == layout
                    Surface(
                        onClick = { viewModel.setPaymentSheetLayout(layout) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) CleanPrimaryContainer else CleanSurfaceVariant,
                        border = BorderStroke(1.dp, if (active) CleanPrimary else CleanBorderLight),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("paymentsheet_layout_${layout.name.lowercase(Locale.US)}")
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) CleanPrimary else CleanTextSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 3. PaymentSheet Configuration Switches
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = CleanSurfaceSecondary,
            border = BorderStroke(1.dp, CleanBorderLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PaymentSheetToggleRow(
                    title = "Allow Delayed Payment Methods",
                    subtitle = "Enable SEPA Debit, BACS, ACH & Konbini in PaymentSheet",
                    checked = allowsDelayed,
                    onCheckedChange = { viewModel.setPaymentSheetAllowsDelayedMethods(it) },
                    testTag = "paymentsheet_delayed_methods_switch"
                )
                HorizontalDivider(color = CleanBorderLight)
                PaymentSheetToggleRow(
                    title = "Google Pay (Test Environment)",
                    subtitle = "Configure PaymentSheet.GooglePayConfiguration (${prepared.countryIso2}/${prepared.currencyCode})",
                    checked = googlePayEnabled,
                    onCheckedChange = { viewModel.setPaymentSheetGooglePayEnabled(it) },
                    testTag = "paymentsheet_google_pay_switch"
                )
                HorizontalDivider(color = CleanBorderLight)
                PaymentSheetToggleRow(
                    title = "Full Billing Address Collection",
                    subtitle = "Switch AddressCollectionMode between Automatic & Full",
                    checked = collectFullAddress,
                    onCheckedChange = { viewModel.setPaymentSheetCollectFullAddress(it) },
                    testTag = "paymentsheet_full_address_switch"
                )
            }
        }

        // 4. Active PaymentSheet.Configuration Summary Inspector
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CleanSurfaceVariant,
            border = BorderStroke(1.dp, CleanBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("paymentsheet_config_summary_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE CONFIGURATION SNAPSHOT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = StripeIndigo,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "#${prepared.configDigest}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CleanSuccess
                    )
                }
                ConfigRow("Merchant", prepared.merchantDisplayName)
                ConfigRow("CustomerConfig", prepared.customerId ?: "Guest (No Customer ID)")
                ConfigRow(
                    "BillingDetails",
                    "${prepared.defaultBillingDetails.name ?: "Guest"} • ${prepared.countryIso2}"
                )
                ConfigRow(
                    "IntentMode",
                    "Payment(${prepared.minorUnits} ${prepared.currencyCode.lowercase(Locale.US)})"
                )
                ConfigRow(
                    "MethodOrder",
                    prepared.paymentMethodOrder.joinToString(", ")
                )
                ConfigRow(
                    "GooglePay",
                    if (prepared.googlePayEnabled) "TEST (${prepared.countryIso2}/${prepared.currencyCode})" else "Disabled"
                )
            }
        }

        // 5. Simulated Customer Checkout Outcome Selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.paymentsheet_simulated_outcome_label),
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
                SimulatedPaymentSheetOutcome.entries.forEach { outcome ->
                    val selected = simulatedOutcome == outcome
                    Surface(
                        onClick = { viewModel.setPaymentSheetSimulatedOutcome(outcome) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) CleanSecondaryContainer else CleanSurfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (selected) CleanSecondary else CleanBorderLight
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("paymentsheet_outcome_${outcome.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = outcome.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) CleanSecondary else CleanTextPrimary
                            )
                            Text(
                                text = outcome.subtitle,
                                fontSize = 9.sp,
                                color = CleanTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 6. Optional Live PaymentIntent Client Secret Toggle for Native Sheet Launch
        Surface(
            onClick = { showAdvancedLiveIntent = !showAdvancedLiveIntent },
            shape = RoundedCornerShape(10.dp),
            color = CleanSurfaceSecondary,
            border = BorderStroke(1.dp, CleanBorderLight),
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("paymentsheet_toggle_live_secret")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showAdvancedLiveIntent) {
                        "Hide Live PaymentIntent Client Secret"
                    } else {
                        "Optional: Attach Backend PaymentIntent client_secret"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CleanTextSecondary
                )
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = StripeIndigo,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        AnimatedVisibility(visible = showAdvancedLiveIntent) {
            OutlinedTextField(
                value = clientSecret,
                onValueChange = { viewModel.setPaymentSheetLiveClientSecret(it) },
                label = { Text("PaymentIntent Client Secret (pi_..._secret_...)") },
                placeholder = { Text("pi_3Q..._secret_...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("paymentsheet_client_secret_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = CleanTextPrimary,
                    unfocusedTextColor = CleanTextPrimary,
                    focusedBorderColor = StripeIndigo,
                    unfocusedBorderColor = CleanBorder,
                    focusedContainerColor = CleanSurfaceVariant,
                    unfocusedContainerColor = CleanSurfaceVariant
                )
            )
        }

        // 7. Primary Simulated Customer PaymentSheet Checkout CTA
        Button(
            onClick = { viewModel.runSimulatedPaymentSheetCheckout(context) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("paymentsheet_simulate_checkout_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = StripeIndigo,
                contentColor = StripeOnIndigo
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CreditScore,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${stringResource(R.string.paymentsheet_launch_sim_cta)} • ${prepared.localizedAmountFormatted}",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Secondary Native Stripe Sheet Launcher (shown only when a backend client_secret is attached)
        val trimmedSecret = clientSecret.trim()
        if (StripePaymentSheetConfigFactory.isLikelyLivePaymentIntentClientSecret(trimmedSecret)) {
            OutlinedButton(
                onClick = { onPresentLivePaymentSheet(prepared, trimmedSecret) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("paymentsheet_launch_native_button"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CleanPrimary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CleanPrimary)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Launch,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.paymentsheet_launch_native_cta),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun PaymentSheetToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CleanTextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = CleanTextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = StripeIndigo),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun ConfigRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = CleanTextMuted
        )
        Text(
            text = value,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = CleanTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StripeCurrencyCatalog
import com.example.data.StripeCurrencyCoin
import com.example.data.StripeExternalAccount
import com.example.ui.theme.CleanBorder
import com.example.ui.theme.CleanBorderSubtle
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
import com.example.ui.theme.StripeIndigo
import com.example.ui.theme.StripeIndigoContainer
import java.util.Locale

private data class CoinFilterCategory(
    val key: String,
    val label: String
)

private val COIN_FILTER_CATEGORIES = listOf(
    CoinFilterCategory("ALL", "All (${StripeCurrencyCatalog.allStripeCurrencies.size})"),
    CoinFilterCategory("POPULAR", "Popular"),
    CoinFilterCategory("ZERO_DECIMAL", "0-Decimal"),
    CoinFilterCategory("THREE_DECIMAL", "3-Decimal"),
    CoinFilterCategory("INSTANT", "Instant Card"),
    CoinFilterCategory("AMERICAS", "Americas"),
    CoinFilterCategory("EUROPE", "Europe"),
    CoinFilterCategory("ASIA_PACIFIC", "Asia-Pacific"),
    CoinFilterCategory("MIDDLE_EAST", "Middle East"),
    CoinFilterCategory("AFRICA", "Africa")
)

/**
 * Tactile metallic coin badge for any Stripe currency code.
 */
@Composable
fun CurrencyCoinEmblem(
    currencyCode: String,
    modifier: Modifier = Modifier,
    coin: StripeCurrencyCoin? = StripeCurrencyCatalog.findByCode(currencyCode),
    size: Dp = 36.dp,
    selected: Boolean = false
) {
    val cleanCode = (coin?.code ?: currencyCode).trim().uppercase(Locale.US)
    val symbol = (coin?.symbol ?: cleanCode).take(4)
    val baseColor = Color(coin?.coinTintHex ?: 0xFF635BFF)
    val highlightColor = Color(
        red = (baseColor.red + 0.25f).coerceAtMost(1f),
        green = (baseColor.green + 0.25f).coerceAtMost(1f),
        blue = (baseColor.blue + 0.25f).coerceAtMost(1f),
        alpha = 1f
    )
    val shadowColor = Color(
        red = (baseColor.red * 0.72f).coerceIn(0f, 1f),
        green = (baseColor.green * 0.72f).coerceIn(0f, 1f),
        blue = (baseColor.blue * 0.72f).coerceIn(0f, 1f),
        alpha = 1f
    )
    val rimColor = if (selected) StripeIndigo else Color(0xFFFFD166)
    val symbolFontSize = when {
        symbol.length >= 4 -> (size.value * 0.25f).sp
        symbol.length == 3 -> (size.value * 0.29f).sp
        symbol.length == 2 -> (size.value * 0.34f).sp
        else -> (size.value * 0.42f).sp
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(highlightColor, baseColor, shadowColor)
                )
            )
            .border(
                width = if (selected) 2.dp else 1.5.dp,
                color = rimColor,
                shape = CircleShape
            )
            .semantics {
                contentDescription = "$cleanCode currency coin"
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size((size.value - 5f).dp)
                .clip(CircleShape)
                .border(0.75.dp, Color.White.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                fontSize = symbolFontSize,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * External Accounts + Complete Stripe Currencies & Coins selector (136+ ISO-4217 currencies).
 * Supports linked bank/card destination selection with coin emblems and live search/filtering
 * across all Stripe-supported settlement & payout currencies.
 */
@Composable
fun ExternalAccountsCurrencyCoinsSection(
    externalAccounts: List<StripeExternalAccount>,
    selectedDestinationId: String?,
    selectedCurrencyCode: String,
    onSelectDestination: (String?) -> Unit,
    onSelectCurrencyCoin: (String) -> Unit,
    modifier: Modifier = Modifier,
    scopeTag: String = "stripe_ext",
    destinationTagPrefix: String = "stripe_dest_",
    includeAutoDestinationOption: Boolean = true,
    compact: Boolean = false
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("ALL") }

    val activeCoin = remember(selectedCurrencyCode) {
        StripeCurrencyCatalog.findByCode(selectedCurrencyCode)
            ?: StripeCurrencyCatalog.allStripeCurrencies.first()
    }

    val filteredCoins = remember(searchQuery, activeFilter) {
        StripeCurrencyCatalog.filterCoins(
            query = searchQuery,
            categoryFilter = activeFilter
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("${scopeTag}_external_accounts_coins_section"),
        shape = RoundedCornerShape(16.dp),
        color = CleanSurfaceVariant,
        border = BorderStroke(1.dp, CleanBorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 12.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header with total Stripe coins counter & active coin summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    CurrencyCoinEmblem(
                        currencyCode = activeCoin.code,
                        coin = activeCoin,
                        size = if (compact) 32.dp else 38.dp,
                        selected = true
                    )
                    Column {
                        Text(
                            text = "EXTERNAL ACCOUNTS & STRIPE COINS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextMuted,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = "${activeCoin.flagEmoji} ${activeCoin.code} • ${activeCoin.name}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = StripeIndigoContainer,
                    border = BorderStroke(1.dp, StripeIndigo.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "${StripeCurrencyCatalog.allStripeCurrencies.size} Coins",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StripeIndigo,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("${scopeTag}_coin_count_badge")
                    )
                }
            }

            // Active coin capability strip (decimals, instant rail, FX preview)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CleanSurface,
                    border = BorderStroke(1.dp, CleanBorderSubtle)
                ) {
                    val precisionLabel = when (activeCoin.decimalPlaces) {
                        0 -> "0-Decimal (Zero)"
                        3 -> "3-Decimal (Fils/Dinar)"
                        else -> "2-Decimal (Standard)"
                    }
                    Text(
                        text = precisionLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CleanTextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (activeCoin.supportsInstantPayout) CleanSuccessContainer else CleanSurfaceSecondary
                ) {
                    Text(
                        text = if (activeCoin.supportsInstantPayout) "Instant Card + Bank" else if (activeCoin.supportsBankExternalAccount) "Bank External Account" else "Checkout / Presentment",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (activeCoin.supportsInstantPayout) CleanSuccess else CleanTextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CleanPrimaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "1 USD ≈ ${formatCoinRate(activeCoin.approximateUsdRate)} ${activeCoin.code}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Linked Stripe External Accounts (with Currency Coins)
            if (externalAccounts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "LINKED EXTERNAL BANK & CARD ACCOUNTS (${externalAccounts.size})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (includeAutoDestinationOption) {
                            val isAutoSelected = selectedDestinationId == null
                            Surface(
                                onClick = { onSelectDestination(null) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isAutoSelected) CleanPrimaryContainer else CleanSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isAutoSelected) CleanPrimary else CleanBorderSubtle
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .height(48.dp)
                                    .testTag("${destinationTagPrefix}auto")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CurrencyCoinEmblem(
                                        currencyCode = activeCoin.code,
                                        coin = activeCoin,
                                        size = 24.dp,
                                        selected = isAutoSelected
                                    )
                                    Text(
                                        text = "Auto Default",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAutoSelected) CleanPrimary else CleanTextSecondary
                                    )
                                }
                            }
                        }

                        externalAccounts.forEach { ext ->
                            val isSelected = selectedDestinationId == ext.id
                            val extCoin = StripeCurrencyCatalog.findByCode(ext.currencyCode)
                            Surface(
                                onClick = { onSelectDestination(ext.id) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) CleanSecondaryContainer else CleanSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) CleanSecondary else CleanBorderSubtle
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .height(48.dp)
                                    .testTag("${destinationTagPrefix}${ext.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CurrencyCoinEmblem(
                                        currencyCode = ext.currencyCode,
                                        coin = extCoin,
                                        size = 28.dp,
                                        selected = isSelected
                                    )
                                    Icon(
                                        imageVector = if (ext.type.equals("card", ignoreCase = true)) {
                                            Icons.Default.CreditCard
                                        } else {
                                            Icons.Default.AccountBalance
                                        },
                                        contentDescription = ext.type,
                                        tint = if (isSelected) CleanSecondary else CleanTextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "${ext.label} ••••${ext.last4}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) CleanSecondary else CleanTextPrimary,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${extCoin?.flagEmoji ?: "🌐"} ${ext.currencyCode}${if (ext.isDefaultForCurrency) " • Default" else ""}",
                                            fontSize = 9.sp,
                                            color = CleanTextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val selectedDest = externalAccounts.firstOrNull { it.id == selectedDestinationId }
                    if (selectedDest != null && !selectedDest.currencyCode.equals(activeCoin.code, ignoreCase = true)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CleanOrangeContainer.copy(alpha = 0.65f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Note: ${selectedDest.label} ••••${selectedDest.last4} settles in ${selectedDest.currencyCode}. Tap ${selectedDest.currencyCode} below or choose Auto Default for ${activeCoin.code}.",
                                fontSize = 10.sp,
                                color = CleanOrangeText,
                                lineHeight = 14.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Search Input for all 136+ Stripe Currency Coins
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search all ${StripeCurrencyCatalog.allStripeCurrencies.size} Stripe currency coins (USD, EUR, KWD, JPY...)",
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Stripe currency coins",
                        tint = StripeIndigo,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("${scopeTag}_coin_search_clear")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear currency search",
                                tint = CleanTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${scopeTag}_coin_search"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StripeIndigo,
                    unfocusedBorderColor = CleanBorder,
                    focusedContainerColor = CleanSurface,
                    unfocusedContainerColor = CleanSurface
                )
            )

            // Region & Decimal Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(COIN_FILTER_CATEGORIES, key = { it.key }) { category ->
                    val isSelected = activeFilter == category.key
                    FilterChip(
                        selected = isSelected,
                        onClick = { activeFilter = category.key },
                        label = {
                            Text(
                                text = category.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StripeIndigoContainer,
                            selectedLabelColor = StripeIndigo
                        ),
                        modifier = Modifier.testTag("${scopeTag}_coin_filter_${category.key}")
                    )
                }
            }

            // Horizontal Scrollable Currency Coins Rail (all matching Stripe currencies)
            if (filteredCoins.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CleanSurface,
                    border = BorderStroke(1.dp, CleanBorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No Stripe currency coin matched \"$searchQuery\" in $activeFilter.",
                        fontSize = 11.sp,
                        color = CleanTextSecondary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${scopeTag}_coin_list")
                ) {
                    items(filteredCoins, key = { it.code }) { coin ->
                        val isSelected = coin.code.equals(activeCoin.code, ignoreCase = true)
                        StripeCurrencyCoinChipCard(
                            coin = coin,
                            isSelected = isSelected,
                            compact = compact,
                            onClick = { onSelectCurrencyCoin(coin.code) },
                            modifier = Modifier.testTag("${scopeTag}_coin_${coin.code}")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StripeCurrencyCoinChipCard(
    coin: StripeCurrencyCoin,
    isSelected: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) StripeIndigoContainer else CleanSurface,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) StripeIndigo else CleanBorderSubtle
        ),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .width(if (compact) 128.dp else 142.dp)
            .semantics {
                contentDescription = "${coin.code} ${coin.name} currency coin"
            }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurrencyCoinEmblem(
                    currencyCode = coin.code,
                    coin = coin,
                    size = if (compact) 30.dp else 34.dp,
                    selected = isSelected
                )

                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(coin.flagEmoji, fontSize = 12.sp)
                        Text(
                            text = coin.code,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) StripeIndigo else CleanTextPrimary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (coin.decimalPlaces == 0) {
                            CleanOrangeContainer
                        } else if (coin.decimalPlaces == 3) {
                            CleanSecondaryContainer
                        } else {
                            CleanSurfaceSecondary
                        }
                    ) {
                        Text(
                            text = "${coin.decimalPlaces}d",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (coin.decimalPlaces == 0) {
                                CleanOrangeText
                            } else if (coin.decimalPlaces == 3) {
                                CleanSecondary
                            } else {
                                CleanTextMuted
                            },
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = coin.name,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = CleanTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected coin",
                        tint = StripeIndigo,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

private fun formatCoinRate(rate: Double): String {
    return when {
        rate >= 100.0 -> String.format(Locale.US, "%.1f", rate)
        rate >= 10.0 -> String.format(Locale.US, "%.2f", rate)
        else -> String.format(Locale.US, "%.3f", rate)
    }
}

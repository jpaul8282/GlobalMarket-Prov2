package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.FirestoreRepository
import com.example.data.TransactionEntity
import com.example.data.TransactionRepository
import com.example.ui.AppLanguage
import com.example.ui.AuthScreen
import com.example.ui.signOutUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import androidx.credentials.CredentialManager
import com.example.ui.BarcodeScannerModal
import com.example.ui.CurrencyInfo
import com.example.ui.GlabaWebCheckoutModal
import com.example.ui.GlobalMarketViewModel
import com.example.ui.GlobalMarketViewModelFactory
import com.example.ui.LocalizedPaymentSimulator
import com.example.ui.MarketingCampaign
import com.example.ui.OwnerPayoutSectionView
import com.example.ui.PayoutDisbursementModal
import com.example.ui.PayoutMethodType
import com.example.ui.QrPaymentDialog
import com.example.ui.RealTimeCurrencyConversionTool
import com.example.ui.TransactionSummaryScreen
import com.example.ui.UserProfile
import com.example.ui.theme.*
import com.example.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private var attachedViewModel: GlobalMarketViewModel? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let { uri ->
            attachedViewModel?.handleDeepLink(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = TransactionRepository(
            database.transactionDao(),
            database.transactionLogDao(),
            database.scannedProductDao(),
            database.transactionHistoryDao(),
            database.salesTerminalDao(),
            database.inventoryProductDao(),
            database.salesTransactionDao()
        )

        val firestoreDb = try {
            FirebaseFirestore.getInstance(getString(R.string.firestore_database_id))
        } catch (e: Exception) {
            null
        }
        val firestoreRepository = firestoreDb?.let { FirestoreRepository(it) }

        setContent {
            var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

            DisposableEffect(Unit) {
                val listener = FirebaseAuth.AuthStateListener { auth ->
                    currentUser = auth.currentUser
                }
                Firebase.auth.addAuthStateListener(listener)
                onDispose {
                    Firebase.auth.removeAuthStateListener(listener)
                }
            }

            MyApplicationTheme {
                if (currentUser == null) {
                    AuthScreen(
                        onAuthSuccess = { currentUser = Firebase.auth.currentUser }
                    )
                } else {
                    val viewModel: GlobalMarketViewModel = viewModel(
                        factory = GlobalMarketViewModelFactory(repository, firestoreRepository)
                    )
                    attachedViewModel = viewModel

                    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
                    val isShareModalOpen by viewModel.isShareModalOpen.collectAsStateWithLifecycle()
                    val selectedCampaign by viewModel.selectedCampaign.collectAsStateWithLifecycle()
                    val isBarcodeScannerOpen by viewModel.isBarcodeScannerOpen.collectAsStateWithLifecycle()
                    val isWebCheckoutOpen by viewModel.isWebCheckoutOpen.collectAsStateWithLifecycle()
                    val isPayoutModalOpen by viewModel.isPayoutModalOpen.collectAsStateWithLifecycle()
                    val isOwnerPayoutScreenOpen by viewModel.isOwnerPayoutScreenOpen.collectAsStateWithLifecycle()
                    val deepLinkBanner by viewModel.deepLinkBanner.collectAsStateWithLifecycle()

                    // Handle incoming deep link when launched from external URL or intent
                    LaunchedEffect(Unit) {
                        intent?.data?.let { uri ->
                            viewModel.handleDeepLink(uri)
                        }
                    }

                    Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        CleanBottomNavigation(
                            selectedTab = selectedTab,
                            onTabSelected = { viewModel.selectTab(it) }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        MainContentSwitcher(
                            selectedTab = selectedTab,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Floating Deep Link Notification Banner
                        AnimatedVisibility(
                            visible = deepLinkBanner != null,
                            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(16.dp)
                        ) {
                            deepLinkBanner?.let { bannerText ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = CleanPrimaryContainer,
                                    border = BorderStroke(1.dp, CleanPrimary),
                                    shadowElevation = 6.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Link,
                                                contentDescription = null,
                                                tint = CleanPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = bannerText,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = CleanTextPrimary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            TextButton(
                                                onClick = { viewModel.openWebCheckout() },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text("View Checkout", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CleanPrimary)
                                            }
                                            IconButton(
                                                onClick = { viewModel.clearDeepLinkBanner() },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Dismiss",
                                                    tint = CleanTextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isShareModalOpen) {
                        MarketingShareHubModal(
                            viewModel = viewModel,
                            selectedCampaign = selectedCampaign,
                            onDismiss = { viewModel.closeShareModal() }
                        )
                    }

                    if (isBarcodeScannerOpen) {
                        BarcodeScannerModal(
                            viewModel = viewModel,
                            onDismiss = { viewModel.closeBarcodeScanner() }
                        )
                    }

                    if (isWebCheckoutOpen) {
                        GlabaWebCheckoutModal(
                            viewModel = viewModel,
                            onDismiss = { viewModel.closeWebCheckout() }
                        )
                    }

                    if (isPayoutModalOpen) {
                        PayoutDisbursementModal(
                            viewModel = viewModel,
                            onDismiss = { viewModel.closePayoutModal() }
                        )
                    }

                    if (isOwnerPayoutScreenOpen) {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            OwnerPayoutSectionView(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize(),
                                onBack = { viewModel.closeOwnerPayoutSection() }
                            )
                        }
                    }
                }
            }
        }
    }
}
}

// -------------------------------------------------------------
// NAVIGATION CONTENT SWITCHER
// -------------------------------------------------------------
@Composable
fun MainContentSwitcher(
    selectedTab: Int,
    viewModel: GlobalMarketViewModel,
    modifier: Modifier = Modifier
) {
    Crossfade(
        targetState = selectedTab,
        label = "TabContentCrossfade",
        modifier = modifier
    ) { tab ->
        when (tab) {
            0 -> GlobalMarketDashboard(viewModel = viewModel)
            1 -> TransactionSummaryScreen(viewModel = viewModel)
            2 -> AnalyticsView(viewModel = viewModel)
            3 -> AudienceView(viewModel = viewModel)
            4 -> EnhancedAccountSettingsView(viewModel = viewModel)
            else -> GlobalMarketDashboard(viewModel = viewModel)
        }
    }
}

// -------------------------------------------------------------
// NAVIGATION BAR
// -------------------------------------------------------------
@Composable
fun CleanBottomNavigation(
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CleanSurfaceSecondary,
        border = BorderStroke(1.dp, CleanBorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val navItems = listOf(
                Triple("Home", Icons.Filled.GridView, Icons.Outlined.GridView),
                Triple("Sales", Icons.AutoMirrored.Outlined.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
                Triple("Analytics", Icons.Filled.Insights, Icons.Outlined.Insights),
                Triple("Audience", Icons.Filled.Group, Icons.Outlined.Group),
                Triple("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
            )

            navItems.forEachIndexed { index, (label, activeIcon, inactiveIcon) ->
                val isSelected = index == selectedTab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onTabSelected(index) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(CleanPrimaryContainer)
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = activeIcon,
                                contentDescription = label,
                                tint = CleanOnPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = inactiveIcon,
                            contentDescription = label,
                            tint = CleanTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) CleanOnPrimaryContainer else CleanTextSecondary
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 0: HOME DASHBOARD
// -------------------------------------------------------------
@Composable
fun GlobalMarketDashboard(
    viewModel: GlobalMarketViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val amountUsd by viewModel.amountUsd.collectAsStateWithLifecycle()
    val isCheckoutVisible by viewModel.isCheckoutVisible.collectAsStateWithLifecycle()
    val isSubscribed by viewModel.isSubscribed.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val defaultCampaign = viewModel.marketingCampaigns.first()

    var isLanguageDropdownExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        // 1. CLEAN HEADER
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CleanPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Globe",
                            tint = CleanOnPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Glaba Pro",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = CleanTextPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Global Marketing & Payments",
                            fontSize = 11.sp,
                            color = CleanTextSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quick Header Marketing Share Button
                    Surface(
                        onClick = { viewModel.openShareModal() },
                        shape = CircleShape,
                        color = CleanPrimaryContainer,
                        border = BorderStroke(1.dp, CleanBorderSubtle)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Marketing Campaign",
                                tint = CleanOnPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Custom Language Selection dropdown button
                    Box {
                        Surface(
                            onClick = { isLanguageDropdownExpanded = true },
                            shape = RoundedCornerShape(20.dp),
                            color = CleanSurfaceVariant,
                            border = BorderStroke(1.dp, CleanBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = when (selectedLanguage) {
                                        AppLanguage.ENGLISH -> "🇺🇸 EN"
                                        AppLanguage.ESPANOL -> "🇪🇸 ES"
                                        AppLanguage.FRANCAIS -> "🇫🇷 FR"
                                        AppLanguage.DEUTSCH -> "🇩🇪 DE"
                                        AppLanguage.CHINESE -> "🇨🇳 中文"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CleanTextPrimary
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Expand",
                                    tint = CleanTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isLanguageDropdownExpanded,
                            onDismissRequest = { isLanguageDropdownExpanded = false },
                            modifier = Modifier.background(CleanSurface)
                        ) {
                            AppLanguage.values().forEach { language ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = when (language) {
                                                AppLanguage.ENGLISH -> "🇺🇸 English"
                                                AppLanguage.ESPANOL -> "🇪🇸 Español"
                                                AppLanguage.FRANCAIS -> "🇫🇷 Français"
                                                AppLanguage.DEUTSCH -> "🇩🇪 Deutsch"
                                                AppLanguage.CHINESE -> "🇨🇳 中文"
                                            },
                                            color = CleanTextPrimary,
                                            fontWeight = if (language == selectedLanguage) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.selectLanguage(language)
                                        isLanguageDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // User Profile Avatar "JP"
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0))
                            .border(BorderStroke(2.dp, CleanBorder), CircleShape)
                            .clickable { viewModel.selectTab(4) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "JP",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CleanTextMuted
                        )
                    }
                }
            }
        }

        // 2. HERO TOTAL REVENUE CARD
        item {
            val totalTransactionsAmount = transactions.sumOf { it.amountInUsd }
            val currentTotalRevenue = 42850.00 + totalTransactionsAmount

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 170.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = CleanPrimaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(22.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL REVENUE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CleanOnPrimaryContainer.copy(alpha = 0.7f),
                                letterSpacing = 1.2.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.5f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isSubscribed) "PRO ACTIVE" else "LIVE TERMINAL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSubscribed) CleanTertiary else CleanPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$${String.format("%,.2f", currentTotalRevenue)}",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanOnPrimaryContainer,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.55f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = "Trend",
                                tint = CleanOnPrimaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+12.4%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanOnPrimaryContainer
                            )
                        }
                        Text(
                            text = "vs last month",
                            fontSize = 11.sp,
                            color = CleanOnPrimaryContainer.copy(alpha = 0.65f)
                        )
                    }
                }
            }
        }

        // 3. PROACTIVE MARKETING & GROWTH SHARE HERO BANNER
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CleanSurface),
                border = BorderStroke(1.5.dp, CleanPrimary.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
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
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(CleanPrimaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = CleanPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "MARKETING CAMPAIGN LINKS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanPrimary,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "Share links with custom promos & content",
                                    fontSize = 12.sp,
                                    color = CleanTextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CleanTertiaryContainer
                        ) {
                            Text(
                                text = "15% OFF ACTIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTertiary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Content preview pill
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CleanSurfaceVariant,
                        border = BorderStroke(1.dp, CleanBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = defaultCampaign.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanTextPrimary
                                )
                                Text(
                                    text = defaultCampaign.shortUrl,
                                    fontSize = 11.sp,
                                    color = CleanSecondary,
                                    fontFamily = FontFamily.Monospace,
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
                                        viewModel.openWebCheckout(defaultCampaign.shortUrl)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInBrowser,
                                        contentDescription = "Preview Web Checkout",
                                        tint = CleanPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        copyToClipboard(
                                            context = context,
                                            label = "Campaign Link",
                                            text = defaultCampaign.shortUrl,
                                            toastMsg = viewModel.translate("link_copied_toast")
                                        )
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = CleanPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Action Buttons Row: Share Button + Quick Send
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.openShareModal(defaultCampaign) },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CleanPrimary,
                                contentColor = CleanOnPrimary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Share Campaign",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                shareMarketingDirect(
                                    context = context,
                                    title = defaultCampaign.title,
                                    message = defaultCampaign.shareMessage
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = CleanPrimary
                            ),
                            border = BorderStroke(1.dp, CleanPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Quick Send",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 4. TWO-COLUMN QUICK STATS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CleanSurface),
                    border = BorderStroke(1.dp, CleanBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "CONVERSION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextMuted,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "4.82%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextPrimary
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CleanSurface),
                    border = BorderStroke(1.dp, CleanBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "CTR",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextMuted,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1.25%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextPrimary
                        )
                    }
                }
            }
        }

        // 5. LOCALIZED PAYMENT SIMULATOR & SALES TERMINAL
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Quick Actions: ML Kit Scanner + Merchant Payout Disbursement
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick ML Kit Barcode / QR Scanner Access Banner
                    Surface(
                        onClick = { viewModel.openBarcodeScanner() },
                        shape = RoundedCornerShape(18.dp),
                        color = CleanSurface,
                        border = BorderStroke(1.dp, CleanBorder),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("terminal_scan_barcode_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(CleanPrimaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan Product Barcode or Payout QR",
                                    tint = CleanPrimary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Scan Code",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanTextPrimary
                                )
                                Text(
                                    text = "ML Kit scanner",
                                    fontSize = 10.sp,
                                    color = CleanTextSecondary
                                )
                            }
                        }
                    }

                    // Quick Payout Disbursement Banner (Bank Wire / PayPal)
                    Surface(
                        onClick = { viewModel.openPayoutModal() },
                        shape = RoundedCornerShape(18.dp),
                        color = CleanSurface,
                        border = BorderStroke(1.dp, CleanBorder),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("terminal_payout_disbursement_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(CleanSecondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = "Payout Bank Wire or PayPal",
                                    tint = CleanSecondary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Payout Funds",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanTextPrimary
                                )
                                Text(
                                    text = "Wire or PayPal",
                                    fontSize = 10.sp,
                                    color = CleanTextSecondary
                                )
                            }
                        }
                    }
                }

                // Dedicated Localized Payment Simulator Component
                LocalizedPaymentSimulator(
                    viewModel = viewModel,
                    onOpenTerminalCheckout = { viewModel.openCheckout() }
                )
            }
        }

        // 6. ACTIVE SUBSCRIPTION VIEW (IF SUBSCRIBED)
        if (isSubscribed) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CleanSurface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, CleanBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = CleanTertiary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = viewModel.translate("active_sub"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextPrimary
                            )
                        }

                        HorizontalDivider(color = CleanBorderSubtle)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Plan Cost",
                                fontSize = 13.sp,
                                color = CleanTextSecondary
                            )
                            Text(
                                text = "$49.99 / mo",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextPrimary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = viewModel.translate("renew_date"),
                                fontSize = 13.sp,
                                color = CleanTextSecondary
                            )
                            Text(
                                text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                                    Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)
                                ),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextPrimary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = viewModel.translate("org_id"),
                                fontSize = 13.sp,
                                color = CleanTextSecondary
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CleanSurfaceVariant,
                                border = BorderStroke(1.dp, CleanBorder)
                            ) {
                                Text(
                                    text = "org_6RsNhnGbSQ3s7rFPAoYfQjA",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CleanPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. ANALYTICS - SALES GRAPH PREVIEW
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CleanSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, CleanBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewModel.translate("analytics_title"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextPrimary
                        )

                        TextButton(onClick = { viewModel.selectTab(2) }) {
                            Text("Full Analytics →", fontSize = 12.sp, color = CleanPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    val countMap = mutableMapOf("USD" to 0, "EUR" to 0, "GBP" to 0, "JPY" to 0, "CNY" to 0)
                    transactions.forEach {
                        countMap[it.targetCurrency] = (countMap[it.targetCurrency] ?: 0) + 1
                    }

                    val total = countMap.values.sum() + 10
                    val eurPct = ((countMap["EUR"] ?: 0) + 4).toFloat() / total
                    val usdPct = ((countMap["USD"] ?: 0) + 3).toFloat() / total
                    val gbpPct = ((countMap["GBP"] ?: 0) + 2).toFloat() / total
                    val cnyPct = ((countMap["CNY"] ?: 0) + 1).toFloat() / total

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CleanChartBar("🇪🇺 EUR - Europe", eurPct, CleanPrimary)
                        CleanChartBar("🇺🇸 USD - United States", usdPct, CleanSecondary)
                        CleanChartBar("🇬🇧 GBP - United Kingdom", gbpPct, CleanTertiary)
                        CleanChartBar("🇨🇳 CNY - China", cnyPct, CleanOrange)
                    }
                }
            }
        }

        // 8. TRANSACTION LOGS
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CleanSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, CleanBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewModel.translate("history_title"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(
                                onClick = { viewModel.selectTab(1) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "All Sales (${transactions.size}) →",
                                    fontSize = 12.sp,
                                    color = CleanPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (transactions.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.clearHistory() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "Clear logs",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (transactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                    contentDescription = null,
                                    tint = CleanTextMuted.copy(alpha = 0.6f),
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = viewModel.translate("no_transactions"),
                                    fontSize = 13.sp,
                                    color = CleanTextMuted
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = { viewModel.seedSampleTransactions() },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "+ Seed Sample Sales",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CleanPrimary
                                    )
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            transactions.take(5).forEach { transaction ->
                                CleanTransactionRow(
                                    transaction = transaction,
                                    onClick = {
                                        viewModel.selectTransaction(transaction)
                                        viewModel.selectTab(1)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isCheckoutVisible) {
        CleanCheckoutModal(viewModel = viewModel)
    }
}

// -------------------------------------------------------------
// MARKETING SHARE HUB MODAL (BOTTOM SHEET / DIALOG)
// -------------------------------------------------------------
@Composable
fun MarketingShareHubModal(
    viewModel: GlobalMarketViewModel,
    selectedCampaign: MarketingCampaign,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {},
        containerColor = CleanSurface,
        shape = RoundedCornerShape(28.dp),
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Close
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CleanPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = CleanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Marketing Share Hub",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextPrimary
                            )
                            Text(
                                text = "Distribute links & promotional content",
                                fontSize = 11.sp,
                                color = CleanTextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = CleanTextSecondary)
                    }
                }

                HorizontalDivider(color = CleanBorderSubtle)

                // Campaign Selector Pills
                Text(
                    text = "SELECT CAMPAIGN TO SHARE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanTextMuted,
                    letterSpacing = 0.8.sp
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(viewModel.marketingCampaigns) { campaign ->
                        val isCurrent = campaign.id == selectedCampaign.id
                        Surface(
                            onClick = { viewModel.selectCampaign(campaign) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) CleanPrimary else CleanSurfaceVariant,
                            border = BorderStroke(1.dp, if (isCurrent) CleanPrimary else CleanBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = campaign.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCurrent) CleanOnPrimary else CleanTextPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isCurrent) Color.White.copy(alpha = 0.25f) else CleanPrimaryContainer)
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${campaign.discountPercent}% OFF",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) CleanOnPrimary else CleanPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Campaign Live Link Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CleanSurfaceVariant,
                    border = BorderStroke(1.dp, CleanBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CAMPAIGN URL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextMuted
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CleanTertiaryContainer
                            ) {
                                Text(
                                    text = "PROMO: ${selectedCampaign.promoCode}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanTertiary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = selectedCampaign.shortUrl,
                            fontSize = 12.sp,
                            color = CleanPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Marketing Pitch Preview Box
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CleanSurfaceVariant,
                    border = BorderStroke(1.dp, CleanBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "PITCH & SOCIAL CONTENT PREVIEW",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextMuted
                        )
                        Text(
                            text = selectedCampaign.shareMessage,
                            fontSize = 12.sp,
                            color = CleanTextPrimary,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Main Action Buttons
                Button(
                    onClick = {
                        shareMarketingDirect(
                            context = context,
                            title = selectedCampaign.title,
                            message = selectedCampaign.shareMessage
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CleanPrimary,
                        contentColor = CleanOnPrimary
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share via System Share Sheet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Two Quick Copy Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            copyToClipboard(
                                context = context,
                                label = "Campaign Link",
                                text = selectedCampaign.shortUrl,
                                toastMsg = viewModel.translate("link_copied_toast")
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CleanPrimary),
                        border = BorderStroke(1.dp, CleanBorder)
                    ) {
                        Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Copy Link", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            copyToClipboard(
                                context = context,
                                label = "Marketing Content",
                                text = selectedCampaign.shareMessage,
                                toastMsg = viewModel.translate("content_copied_toast")
                            )
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CleanPrimary),
                        border = BorderStroke(1.dp, CleanBorder)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Copy Full Pitch", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Direct Channel Quick Dispatch Grid
                Text(
                    text = "DIRECT CHANNELS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanTextMuted,
                    letterSpacing = 0.8.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickChannelButton(
                        icon = Icons.AutoMirrored.Filled.Message,
                        label = "SMS",
                        color = Color(0xFF2563EB)
                    ) {
                        shareViaSms(context, selectedCampaign.shareMessage)
                    }

                    QuickChannelButton(
                        icon = Icons.Default.Mail,
                        label = "Email",
                        color = Color(0xFFDC2626)
                    ) {
                        shareViaEmail(context, selectedCampaign.title, selectedCampaign.shareMessage)
                    }

                    QuickChannelButton(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        label = "Chat",
                        color = Color(0xFF16A34A)
                    ) {
                        shareMarketingDirect(context, selectedCampaign.title, selectedCampaign.shareMessage)
                    }

                    QuickChannelButton(
                        icon = Icons.Default.OpenInBrowser,
                        label = "Preview",
                        color = CleanPrimary
                    ) {
                        viewModel.openWebCheckout(selectedCampaign.shortUrl)
                    }
                }
            }
        }
    )
}

@Composable
fun QuickChannelButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = CleanTextPrimary
        )
    }
}

// -------------------------------------------------------------
// HELPER SHARING & INTENT FUNCTIONS
// -------------------------------------------------------------
fun copyToClipboard(context: Context, label: String, text: String, toastMsg: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
}

fun shareMarketingDirect(context: Context, title: String, message: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, message)
        putExtra(Intent.EXTRA_TITLE, title)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Marketing Campaign")
    context.startActivity(shareIntent)
}

fun shareViaSms(context: Context, message: String) {
    val uri = Uri.parse("smsto:")
    val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
        putExtra("sms_body", message)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        shareMarketingDirect(context, "Marketing Campaign", message)
    }
}

fun shareViaEmail(context: Context, subject: String, body: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        shareMarketingDirect(context, subject, body)
    }
}

fun openUrlInBrowser(context: Context, url: String) {
    try {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(browserIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Opening link: $url", Toast.LENGTH_SHORT).show()
    }
}

// -------------------------------------------------------------
// TAB 1: ANALYTICS VIEW
// -------------------------------------------------------------
@Composable
fun AnalyticsView(
    viewModel: GlobalMarketViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        item {
            Text(
                text = "Performance & Metrics",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CleanTextPrimary
            )
            Text(
                text = "Multi-currency sales velocity and settlement health",
                fontSize = 12.sp,
                color = CleanTextSecondary
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CleanSurface),
                border = BorderStroke(1.dp, CleanBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Global Conversion Rate by Channel",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextPrimary
                    )

                    CleanChartBar("Storefront Direct Checkout", 0.68f, CleanPrimary)
                    CleanChartBar("Partner Affiliate Referrals", 0.44f, CleanSecondary)
                    CleanChartBar("Email & WhatsApp Campaigns", 0.32f, CleanTertiary)
                    CleanChartBar("Social Flash Deals", 0.22f, CleanOrange)
                }
            }
        }

        item {
            Surface(
                onClick = { viewModel.openShareModal() },
                shape = RoundedCornerShape(20.dp),
                color = CleanPrimaryContainer,
                border = BorderStroke(1.dp, CleanBorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = CleanPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Boost Campaign Conversions",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = CleanOnPrimaryContainer
                            )
                            Text(
                                text = "Share customized checkout links & discount codes",
                                fontSize = 11.sp,
                                color = CleanOnPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = CleanOnPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: AUDIENCE VIEW
// -------------------------------------------------------------
@Composable
fun AudienceView(
    viewModel: GlobalMarketViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        item {
            Text(
                text = viewModel.translate("audience_title"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CleanTextPrimary
            )
            Text(
                text = viewModel.translate("audience_desc"),
                fontSize = 12.sp,
                color = CleanTextSecondary
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CleanSurface),
                border = BorderStroke(1.dp, CleanBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Top Active Regional Markets",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextPrimary
                    )

                    AudienceRegionRow(flag = "🇪🇺", country = "European Union (Eurozone)", buyers = "14,820 buyers", growth = "+18%")
                    AudienceRegionRow(flag = "🇺🇸", country = "United States & Canada", buyers = "11,430 buyers", growth = "+12%")
                    AudienceRegionRow(flag = "🇬🇧", country = "United Kingdom", buyers = "6,190 buyers", growth = "+9%")
                    AudienceRegionRow(flag = "🇯🇵", country = "Japan & East Asia", buyers = "4,210 buyers", growth = "+24%")
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CleanSurface),
                border = BorderStroke(1.dp, CleanBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Affiliate & Influencer Invitations",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextPrimary
                    )
                    Text(
                        text = "Invite international partners to sell in EUR, GBP, USD, JPY and earn 25% recurring commissions.",
                        fontSize = 12.sp,
                        color = CleanTextSecondary
                    )

                    Button(
                        onClick = {
                            viewModel.openShareModal(viewModel.marketingCampaigns.firstOrNull { it.id == "partner_referral" })
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CleanPrimary,
                            contentColor = CleanOnPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Partner Referral Link", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AudienceRegionRow(flag: String, country: String, buyers: String, growth: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CleanSurfaceVariant)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = flag, fontSize = 20.sp)
            Column {
                Text(text = country, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CleanTextPrimary)
                Text(text = buyers, fontSize = 11.sp, color = CleanTextSecondary)
            }
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = CleanTertiaryContainer
        ) {
            Text(
                text = growth,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CleanTertiary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// TAB 3: ENHANCED ACCOUNT SETTINGS
// -------------------------------------------------------------
@Composable
fun EnhancedAccountSettingsView(
    viewModel: GlobalMarketViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isSubscribed by viewModel.isSubscribed.collectAsStateWithLifecycle()

    var isEditProfileDialogVisible by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(profile.name) }
    var editEmail by remember { mutableStateOf(profile.email) }
    var editOrg by remember { mutableStateOf(profile.organization) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        item {
            Text(
                text = "Account & Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CleanTextPrimary
            )
            Text(
                text = "Organization credentials, marketing tools, security & billing",
                fontSize = 12.sp,
                color = CleanTextSecondary
            )
        }

        // 1. Profile Information Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CleanSurface),
                border = BorderStroke(1.dp, CleanBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(CleanPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = profile.initials,
                                    color = CleanOnPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                )
                            }
                            Column {
                                Text(
                                    text = profile.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanTextPrimary
                                )
                                Text(
                                    text = profile.email,
                                    fontSize = 12.sp,
                                    color = CleanTextSecondary
                                )
                                Text(
                                    text = profile.role,
                                    fontSize = 11.sp,
                                    color = CleanPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                editName = profile.name
                                editEmail = profile.email
                                editOrg = profile.organization
                                isEditProfileDialogVisible = true
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Profile", tint = CleanPrimary)
                        }
                    }

                    HorizontalDivider(color = CleanBorderSubtle)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "ORGANIZATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CleanTextMuted)
                            Text(text = profile.organization, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CleanTextPrimary)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CleanSurfaceVariant,
                            border = BorderStroke(1.dp, CleanBorder)
                        ) {
                            Text(
                                text = profile.orgId,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CleanPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Marketing Hub Quick Access
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CleanSurface),
                border = BorderStroke(1.dp, CleanBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Marketing & Campaign Distribution",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextPrimary
                    )

                    OutlinedButton(
                        onClick = { viewModel.openShareModal() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CleanPrimary),
                        border = BorderStroke(1.dp, CleanPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Open Marketing Share Hub", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // 3. Security & Automation Toggles
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CleanSurface),
                border = BorderStroke(1.dp, CleanBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Security & Settlements",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextPrimary
                    )

                    SettingsToggleRow(
                        title = "Two-Factor Authentication",
                        subtitle = "Hardware 2FA active for overseas payouts",
                        checked = profile.isTwoFactorEnabled,
                        onCheckedChange = { viewModel.toggleTwoFactor(it) }
                    )

                    HorizontalDivider(color = CleanBorderSubtle)

                    SettingsToggleRow(
                        title = "Automated FX Conversion",
                        subtitle = "Auto-settle multi-currency revenues daily",
                        checked = profile.autoConvertPayouts,
                        onCheckedChange = { viewModel.toggleAutoConvert(it) }
                    )

                    HorizontalDivider(color = CleanBorderSubtle)

                    SettingsToggleRow(
                        title = "Email Transaction Receipts",
                        subtitle = "Notify westerveldjp@gmail.com on charges",
                        checked = profile.emailNotifications,
                        onCheckedChange = { viewModel.toggleNotifications(it) }
                    )
                }
            }
        }

        // 3.5. Merchant Payout Methods (Bank Wire & PayPal)
        item {
            val payoutConfig by viewModel.payoutConfig.collectAsStateWithLifecycle()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CleanSurface),
                border = BorderStroke(1.dp, CleanBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Payout Destination & Rail",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextPrimary
                            )
                            Text(
                                text = "Settlement accounts for global revenue disbursements",
                                fontSize = 11.sp,
                                color = CleanTextSecondary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = CleanPrimaryContainer,
                            border = BorderStroke(1.dp, CleanPrimary)
                        ) {
                            Text(
                                text = payoutConfig.selectedMethod.displayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Active account detail snippet
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = CleanSurfaceSecondary,
                        border = BorderStroke(1.dp, CleanBorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(CleanSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (payoutConfig.selectedMethod == PayoutMethodType.BANK_WIRE) Icons.Default.AccountBalance else Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = CleanPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (payoutConfig.selectedMethod == PayoutMethodType.BANK_WIRE) {
                                        "${payoutConfig.bankWire.bankName} • ${payoutConfig.bankWire.accountHolderName}"
                                    } else {
                                        "PayPal • ${payoutConfig.payPal.paypalEmailOrId}"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CleanTextPrimary
                                )
                                Text(
                                    text = if (payoutConfig.selectedMethod == PayoutMethodType.BANK_WIRE) {
                                        "IBAN: ••••${payoutConfig.bankWire.accountNumberOrIban.takeLast(6)} • SWIFT: ${payoutConfig.bankWire.swiftBic}"
                                    } else {
                                        "PayPal ID: ${payoutConfig.payPal.phoneOrPaypalMe} • Instant Transfer"
                                    },
                                    fontSize = 11.sp,
                                    color = CleanTextSecondary
                                )
                            }
                        }
                    }

                    // Action buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.openOwnerPayoutSection() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CleanPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Open Owner Payout Section", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.openPayoutModal() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, CleanBorder)
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Configure Rails", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }

                            FilledTonalButton(
                                onClick = { viewModel.openPayoutModal() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = CleanPrimaryContainer,
                                    contentColor = CleanPrimary
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Disburse Quick", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // 4. Subscription Management & Database
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CleanSurface),
                border = BorderStroke(1.dp, CleanBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Billing & Local Cache",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Subscription Status",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CleanTextPrimary
                            )
                            Text(
                                text = if (isSubscribed) "Pro Tier ($49.99/mo)" else "Free / Developer Tier",
                                fontSize = 11.sp,
                                color = if (isSubscribed) CleanTertiary else CleanTextSecondary
                            )
                        }

                        Button(
                            onClick = { viewModel.toggleSubscriptionStatus() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSubscribed) Color(0xFFDC2626) else CleanPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isSubscribed) "Deactivate" else "Activate Pro", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = CleanBorderSubtle)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Room Local Database",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CleanTextPrimary
                            )
                            Text(
                                text = "Clear stored local transaction history",
                                fontSize = 11.sp,
                                color = CleanTextSecondary
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.clearHistory()
                                Toast.makeText(context, "Local database cleared", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFDC2626)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                        ) {
                            Text("Clear", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (isEditProfileDialogVisible) {
        AlertDialog(
            onDismissRequest = { isEditProfileDialogVisible = false },
            title = { Text("Edit Merchant Profile", fontWeight = FontWeight.Bold, color = CleanTextPrimary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfile(editName, editEmail, editOrg)
                        isEditProfileDialogVisible = false
                        Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CleanPrimary)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditProfileDialogVisible = false }) {
                    Text("Cancel")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = editOrg,
                        onValueChange = { editOrg = it },
                        label = { Text("Organization Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            containerColor = CleanSurface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = CleanTextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = CleanTextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CleanOnPrimary,
                checkedTrackColor = CleanPrimary,
                uncheckedThumbColor = CleanTextSecondary,
                uncheckedTrackColor = CleanSurfaceVariant
            )
        )
    }
}

// -------------------------------------------------------------
// HELPER CHART BAR
// -------------------------------------------------------------
@Composable
fun CleanChartBar(
    label: String,
    percentage: Float,
    barColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 12.sp, color = CleanTextSecondary, fontWeight = FontWeight.Medium)
            Text(
                text = "${(percentage * 100).toInt()}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CleanTextPrimary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(CleanSurfaceVariant)
                .border(BorderStroke(0.5.dp, CleanBorderSubtle), CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percentage)
                    .clip(CircleShape)
                    .background(barColor)
            )
        }
    }
}

// -------------------------------------------------------------
// HELPER TRANSACTION ROW
// -------------------------------------------------------------
@Composable
fun CleanTransactionRow(
    transaction: TransactionEntity,
    onClick: (() -> Unit)? = null
) {
    val dateString = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(transaction.timestamp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CleanSurfaceVariant)
            .border(BorderStroke(1.dp, CleanBorder), RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CleanPrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (transaction.targetCurrency) {
                        "USD" -> "🇺🇸"
                        "EUR" -> "🇪🇺"
                        "GBP" -> "🇬🇧"
                        "JPY" -> "🇯🇵"
                        "CNY" -> "🇨🇳"
                        else -> "💳"
                    },
                    fontSize = 18.sp
                )
            }

            Column {
                Text(
                    text = transaction.customerName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$dateString • ${transaction.country}",
                    fontSize = 11.sp,
                    color = CleanTextMuted
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = CurrencyUtils.formatAmount(transaction.convertedAmount, transaction.targetCurrency),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CleanPrimary
            )
            Text(
                text = "Base: " + CurrencyUtils.formatUsd(transaction.amountInUsd),
                fontSize = 11.sp,
                color = CleanTextMuted
            )
        }
    }
}

// -------------------------------------------------------------
// CHECKOUT MODAL DIALOG
// -------------------------------------------------------------
@Composable
fun CleanCheckoutModal(viewModel: GlobalMarketViewModel) {
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val amountUsd by viewModel.amountUsd.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val paymentSuccess by viewModel.paymentSuccess.collectAsStateWithLifecycle()
    val liveRates by viewModel.liveRateMap.collectAsStateWithLifecycle()
    val rateChangePercents by viewModel.rateChangePercents.collectAsStateWithLifecycle()
    val isRateRefreshing by viewModel.isRateRefreshing.collectAsStateWithLifecycle()
    val lastRefreshTime by viewModel.lastRateRefreshTimestamp.collectAsStateWithLifecycle()

    val cardHolder by viewModel.cardHolderName.collectAsStateWithLifecycle()
    val cardNumber by viewModel.cardNumber.collectAsStateWithLifecycle()
    val expiry by viewModel.expiryDate.collectAsStateWithLifecycle()
    val cvv by viewModel.cvv.collectAsStateWithLifecycle()
    val country by viewModel.selectedCountry.collectAsStateWithLifecycle()

    val currentAmountUsd = amountUsd.toDoubleOrNull() ?: 49.99
    val currentRate = liveRates[selectedCurrency.code] ?: selectedCurrency.exchangeRate
    val totalAmount = currentAmountUsd * currentRate
    val formattedTotal = "${selectedCurrency.symbol}${String.format(Locale.US, "%.2f", totalAmount)} ${selectedCurrency.code}"

    AlertDialog(
        onDismissRequest = { if (!isProcessing) viewModel.closeCheckout() },
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = true,
            dismissOnBackPress = !isProcessing,
            dismissOnClickOutside = !isProcessing
        ),
        confirmButton = {},
        dismissButton = {},
        containerColor = CleanSurface,
        shape = RoundedCornerShape(28.dp),
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!isProcessing && !paymentSuccess) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewModel.translate("checkout_title"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextPrimary
                        )
                        IconButton(onClick = { viewModel.closeCheckout() }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = CleanTextSecondary)
                        }
                    }

                    // Integrated Real-Time Currency Conversion Tool for the Payment Interface
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

                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { viewModel.cardNumber.value = it },
                        label = { Text(viewModel.translate("card_number")) },
                        placeholder = { Text("4111 2222 3333 4444") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CleanTextPrimary,
                            unfocusedTextColor = CleanTextPrimary,
                            focusedBorderColor = CleanPrimary,
                            unfocusedBorderColor = CleanBorder,
                            focusedContainerColor = CleanSurfaceVariant,
                            unfocusedContainerColor = CleanSurfaceVariant
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = expiry,
                            onValueChange = { viewModel.expiryDate.value = it },
                            label = { Text(viewModel.translate("expiry")) },
                            placeholder = { Text("MM/YY") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = CleanTextPrimary,
                                unfocusedTextColor = CleanTextPrimary,
                                focusedBorderColor = CleanPrimary,
                                unfocusedBorderColor = CleanBorder,
                                focusedContainerColor = CleanSurfaceVariant,
                                unfocusedContainerColor = CleanSurfaceVariant
                            )
                        )
                        OutlinedTextField(
                            value = cvv,
                            onValueChange = { viewModel.cvv.value = it },
                            label = { Text(viewModel.translate("cvv")) },
                            placeholder = { Text("123") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = CleanTextPrimary,
                                unfocusedTextColor = CleanTextPrimary,
                                focusedBorderColor = CleanPrimary,
                                unfocusedBorderColor = CleanBorder,
                                focusedContainerColor = CleanSurfaceVariant,
                                unfocusedContainerColor = CleanSurfaceVariant
                            )
                        )
                    }

                    OutlinedTextField(
                        value = cardHolder,
                        onValueChange = { viewModel.cardHolderName.value = it },
                        label = { Text(viewModel.translate("card_holder")) },
                        placeholder = { Text("John Smith") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CleanTextPrimary,
                            unfocusedTextColor = CleanTextPrimary,
                            focusedBorderColor = CleanPrimary,
                            unfocusedBorderColor = CleanBorder,
                            focusedContainerColor = CleanSurfaceVariant,
                            unfocusedContainerColor = CleanSurfaceVariant
                        )
                    )

                    OutlinedTextField(
                        value = country,
                        onValueChange = { viewModel.selectedCountry.value = it },
                        label = { Text(viewModel.translate("country")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CleanTextPrimary,
                            unfocusedTextColor = CleanTextPrimary,
                            focusedBorderColor = CleanPrimary,
                            unfocusedBorderColor = CleanBorder,
                            focusedContainerColor = CleanSurfaceVariant,
                            unfocusedContainerColor = CleanSurfaceVariant
                        )
                    )

                    Button(
                        onClick = { viewModel.performSimulatedPayment() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("checkout_pay_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CleanPrimary,
                            contentColor = CleanOnPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "${viewModel.translate("pay_button")} $formattedTotal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock",
                            tint = CleanPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = viewModel.translate("secure_payment"),
                            fontSize = 11.sp,
                            color = CleanTextMuted
                        )
                    }
                } else if (isProcessing) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = CleanPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = viewModel.translate("processing"),
                            fontWeight = FontWeight.Bold,
                            color = CleanTextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Stripe Gateway Active",
                            fontSize = 11.sp,
                            color = CleanSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else if (paymentSuccess) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(CleanTertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Succeed",
                                tint = CleanTertiary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = viewModel.translate("sub_success"),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = CleanTertiary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = viewModel.translate("sub_created"),
                            fontSize = 13.sp,
                            color = CleanTextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CleanSurfaceVariant,
                            border = BorderStroke(1.dp, CleanBorder)
                        ) {
                            Text(
                                text = "org_6RsNhnGbSQ3s7rFPAoYfQjA",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CleanPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    )
}

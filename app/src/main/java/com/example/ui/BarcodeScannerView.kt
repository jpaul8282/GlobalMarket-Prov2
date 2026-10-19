package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ScannedProductEntity
import com.example.data.TransactionEntity
import com.example.ui.theme.*
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

// -------------------------------------------------------------
// DATA MODEL FOR SCANNED BARCODE
// -------------------------------------------------------------
data class ScannedBarcodeItem(
    val rawValue: String,
    val formatName: String,
    val title: String,
    val amountUsd: Double,
    val currencyCode: String,
    val customerOrRecipient: String,
    val country: String,
    val isPayout: Boolean = false
)

enum class ScannerEntryMode {
    TRANSACTION,
    PAYOUT
}

// -------------------------------------------------------------
// PRESETS FOR INSTANT VERIFICATION & TESTING
// -------------------------------------------------------------
val SAMPLE_BARCODE_PRESETS = listOf(
    ScannedBarcodeItem(
        rawValue = "https://glaba.pro/pay?m=org_6RsNhnGbSQ3s7rFPAoYfQjA&c=EUR&code=GLOBAL15",
        formatName = "QR Code",
        title = "Glaba Pro Storefront (EUR • GLOBAL15 -15%)",
        amountUsd = 42.49,
        currencyCode = "EUR",
        customerOrRecipient = "Elena Rostova",
        country = "Germany",
        isPayout = false
    ),
    ScannedBarcodeItem(
        rawValue = "012345678905",
        formatName = "UPC-A",
        title = "Glaba Pro POS Terminal Terminal-X",
        amountUsd = 249.00,
        currencyCode = "EUR",
        customerOrRecipient = "Retail Merchant Solutions",
        country = "France",
        isPayout = false
    ),
    ScannedBarcodeItem(
        rawValue = "SKU-GLB-PRO-99",
        formatName = "Code 128",
        title = "Enterprise Global SaaS Annual Plan",
        amountUsd = 199.99,
        currencyCode = "USD",
        customerOrRecipient = "Acme Corp International",
        country = "United States",
        isPayout = false
    ),
    ScannedBarcodeItem(
        rawValue = "PAYOUT-EUR-850",
        formatName = "QR Code",
        title = "European Merchant Settlement Batch #850",
        amountUsd = 925.00,
        currencyCode = "EUR",
        customerOrRecipient = "Elena Rostova",
        country = "Germany",
        isPayout = true
    ),
    ScannedBarcodeItem(
        rawValue = "PAYOUT-JPY-38800",
        formatName = "QR Code",
        title = "Asia-Pacific Regional Settlement #38800",
        amountUsd = 250.00,
        currencyCode = "JPY",
        customerOrRecipient = "Kenji Takahashi",
        country = "Japan",
        isPayout = true
    ),
    ScannedBarcodeItem(
        rawValue = "5901234123457",
        formatName = "EAN-13",
        title = "Global Multi-Currency NFC Reader",
        amountUsd = 89.50,
        currencyCode = "GBP",
        customerOrRecipient = "FinTech Devices Ltd",
        country = "United Kingdom",
        isPayout = false
    )
)

// -------------------------------------------------------------
// SMART BARCODE PARSER
// -------------------------------------------------------------
fun parseBarcodeToItem(
    barcodeText: String,
    format: Int,
    forcedMode: ScannerEntryMode? = null
): ScannedBarcodeItem {
    val clean = barcodeText.trim()
    val formatLabel = when (format) {
        Barcode.FORMAT_QR_CODE -> "QR Code"
        Barcode.FORMAT_UPC_A -> "UPC-A"
        Barcode.FORMAT_UPC_E -> "UPC-E"
        Barcode.FORMAT_EAN_13 -> "EAN-13"
        Barcode.FORMAT_EAN_8 -> "EAN-8"
        Barcode.FORMAT_CODE_128 -> "Code 128"
        Barcode.FORMAT_CODE_39 -> "Code 39"
        Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
        else -> "Barcode"
    }

    // Check exact catalog match first
    val matched = SAMPLE_BARCODE_PRESETS.find { it.rawValue.equals(clean, ignoreCase = true) }
    if (matched != null) {
        return if (forcedMode != null) {
            matched.copy(isPayout = forcedMode == ScannerEntryMode.PAYOUT)
        } else {
            matched
        }
    }

    // Check if it is a Glaba Pro Checkout QR Code / URL
    if (clean.contains("glaba.pro", ignoreCase = true) || clean.startsWith("glaba://", ignoreCase = true)) {
        val uri = try { android.net.Uri.parse(clean) } catch (_: Exception) { null }
        val currencyParam = uri?.getQueryParameter("c")?.uppercase() ?: if (clean.contains("EUR", ignoreCase = true)) "EUR" else "USD"
        val promoCode = uri?.getQueryParameter("code")?.uppercase() ?: if (clean.contains("GLOBAL15", ignoreCase = true)) "GLOBAL15" else "PROMO"
        val merchant = uri?.getQueryParameter("m") ?: "org_6RsNhnGbSQ3s7rFPAoYfQjA"
        val isPayout = clean.contains("payout", ignoreCase = true) || forcedMode == ScannerEntryMode.PAYOUT
        
        val discountPercent = when (promoCode) {
            "GLOBAL15" -> 15
            "PARTNER25" -> 25
            "SUMMER2026" -> 20
            else -> 10
        }
        val baseUsd = 49.99
        val discountedUsd = (baseUsd * (1.0 - discountPercent / 100.0)).coerceAtLeast(5.0)

        val countryName = when (currencyParam) {
            "EUR" -> "Germany"
            "GBP" -> "United Kingdom"
            "JPY" -> "Japan"
            "CNY" -> "China"
            else -> "United States"
        }

        return ScannedBarcodeItem(
            rawValue = clean,
            formatName = "Glaba QR Link",
            title = "Glaba Storefront ($currencyParam • $promoCode)",
            amountUsd = Math.round(discountedUsd * 100.0) / 100.0,
            currencyCode = currencyParam,
            customerOrRecipient = if (isPayout) "Payout to Merchant $merchant" else "Elena Rostova ($merchant)",
            country = countryName,
            isPayout = isPayout
        )
    }

    val isPayoutDetected = clean.startsWith("PAYOUT", ignoreCase = true) ||
            clean.contains("payout", ignoreCase = true) ||
            clean.contains("disburse", ignoreCase = true) ||
            forcedMode == ScannerEntryMode.PAYOUT

    // Determine target currency
    val detectedCurrency = when {
        clean.contains("EUR", ignoreCase = true) -> "EUR"
        clean.contains("GBP", ignoreCase = true) -> "GBP"
        clean.contains("JPY", ignoreCase = true) -> "JPY"
        clean.contains("CNY", ignoreCase = true) -> "CNY"
        else -> "USD"
    }

    val country = when (detectedCurrency) {
        "EUR" -> "Germany"
        "GBP" -> "United Kingdom"
        "JPY" -> "Japan"
        "CNY" -> "China"
        else -> "United States"
    }

    // Parse amount if embedded (e.g., amount=49.99 or numeric sequence)
    val amountRegex = Regex("(?:amount|price|payout|usd)?[:=]?\\s*\\$?([0-9]+(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)
    val match = amountRegex.find(clean)
    val extractedAmount = match?.groupValues?.getOrNull(1)?.toDoubleOrNull()

    val finalAmount = if (extractedAmount != null && extractedAmount > 0.0) {
        extractedAmount
    } else {
        // Deterministic price based on string hash so arbitrary barcodes always produce a valid amount
        val hash = kotlin.math.abs(clean.hashCode())
        val base = 15.0 + (hash % 185)
        val cents = (hash % 100) / 100.0
        kotlin.math.round((base + cents) * 100) / 100.0
    }

    val title = if (isPayoutDetected) {
        "Merchant Payout (${clean.take(14)})"
    } else {
        "Scanned Product SKU (${clean.take(14)})"
    }

    val customerName = if (isPayoutDetected) {
        "Merchant Settlement #$clean"
    } else {
        "Customer ($country)"
    }

    return ScannedBarcodeItem(
        rawValue = clean,
        formatName = formatLabel,
        title = title,
        amountUsd = finalAmount,
        currencyCode = detectedCurrency,
        customerOrRecipient = customerName,
        country = country,
        isPayout = isPayoutDetected
    )
}

// -------------------------------------------------------------
// CAMERA BARCODE SCANNER COMPOSABLE
// -------------------------------------------------------------
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun CameraBarcodeScannerView(
    onBarcodeDetected: (ScannedBarcodeItem) -> Unit,
    currentMode: ScannerEntryMode,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var isTorchEnabled by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var cameraControlInstance by remember { mutableStateOf<Camera?>(null) }
    var lastScannedTimestamp by remember { mutableLongStateOf(0L) }

    // Laser animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser_sweep")
    val laserOffsetFraction by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_offset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(380.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    val executor = Executors.newSingleThreadExecutor()

                    // Configure ML Kit barcode scanner for all standard formats
                    val barcodeOptions = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .build()
                    val scanner: BarcodeScanner = BarcodeScanning.getClient(barcodeOptions)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                scanner.process(inputImage)
                                    .addOnSuccessListener { barcodes ->
                                        val firstBarcode = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                                        if (firstBarcode != null) {
                                            val now = System.currentTimeMillis()
                                            if (now - lastScannedTimestamp > 1400) {
                                                lastScannedTimestamp = now
                                                val parsed = parseBarcodeToItem(
                                                    barcodeText = firstBarcode.rawValue ?: "",
                                                    format = firstBarcode.format,
                                                    forcedMode = currentMode
                                                )
                                                onBarcodeDetected(parsed)
                                            }
                                        }
                                    }
                                    .addOnFailureListener {
                                        // Frame dropped gracefully
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        try {
                            cameraProvider.unbindAll()
                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(lensFacing)
                                .build()
                            val cam = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                            cameraControlInstance = cam
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Scanning Reticle & Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.80f)
                        .height(230.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.Transparent)
                        .border(
                            BorderStroke(2.dp, Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7)))),
                            RoundedCornerShape(22.dp)
                        )
                ) {
                    // Corner targeting accents
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .align(Alignment.TopStart)
                            .border(BorderStroke(3.dp, Color(0xFF38BDF8)), RoundedCornerShape(topStart = 20.dp))
                    )
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .align(Alignment.TopEnd)
                            .border(BorderStroke(3.dp, Color(0xFF38BDF8)), RoundedCornerShape(topEnd = 20.dp))
                    )
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .align(Alignment.BottomStart)
                            .border(BorderStroke(3.dp, Color(0xFF38BDF8)), RoundedCornerShape(bottomStart = 20.dp))
                    )
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .align(Alignment.BottomEnd)
                            .border(BorderStroke(3.dp, Color(0xFF38BDF8)), RoundedCornerShape(bottomEnd = 20.dp))
                    )

                    // Animated glowing laser scan line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(laserOffsetFraction)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0xFF38BDF8),
                                            Color(0xFF0284C7),
                                            Color(0xFF38BDF8),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }
            }

            // Top Status Bar in Camera View
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E))
                        )
                        Text(
                            text = "ML Kit Barcode AI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Flashlight Toggle
                    Surface(
                        onClick = {
                            isTorchEnabled = !isTorchEnabled
                            cameraControlInstance?.cameraControl?.enableTorch(isTorchEnabled)
                        },
                        shape = CircleShape,
                        color = if (isTorchEnabled) Color(0xFFF59E0B) else Color.Black.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = if (isTorchEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            contentDescription = "Torch",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(18.dp)
                        )
                    }

                    // Flip Camera
                    Surface(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "Flip Camera",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(18.dp)
                        )
                    }
                }
            }

            // Bottom Subtitle Instruction Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.70f)
                ) {
                    Text(
                        text = if (currentMode == ScannerEntryMode.TRANSACTION)
                            "Center product barcode or UPC/EAN in frame"
                        else
                            "Scan settlement QR code or payout slip",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        } else {
            // Camera Permission Needed Placeholder
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(CleanPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = CleanPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Camera Access Required",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enable camera to scan product barcodes and payout QR codes in real-time with Google ML Kit.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = CleanPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Grant Camera Permission", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// MAIN BARCODE SCANNER MODAL DIALOG
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerModal(
    viewModel: GlobalMarketViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val savedProducts by viewModel.scannedProducts.collectAsStateWithLifecycle()
    val targetConverterCurrency by viewModel.converterTargetCurrency.collectAsStateWithLifecycle()
    var modalTab by remember { mutableStateOf(0) } // 0 = Scanner & FX Converter, 1 = Room DB Products
    var searchQuery by remember { mutableStateOf("") }
    var entryMode by remember { mutableStateOf(ScannerEntryMode.TRANSACTION) }
    var scannedItem by remember { mutableStateOf<ScannedBarcodeItem?>(null) }
    var customAmountText by remember { mutableStateOf("") }
    var showSuccessBanner by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .testTag("barcode_scanner_modal"),
            containerColor = CleanBackground,
            topBar = {
                Surface(
                    color = CleanSurface,
                    border = BorderStroke(1.dp, CleanBorderSubtle)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = CleanPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = viewModel.translate("barcode_scanner_title"),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CleanTextPrimary
                                    )
                                    Text(
                                        text = "Google ML Kit • Room DB • FX Converter",
                                        fontSize = 11.sp,
                                        color = CleanTextSecondary
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CleanSurfaceVariant)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = CleanTextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Top Segmented Tab Switcher (Scanner & Converter vs Room DB Products)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(CleanSurfaceVariant)
                                .border(BorderStroke(1.dp, CleanBorder), RoundedCornerShape(14.dp))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                onClick = { modalTab = 0 },
                                shape = RoundedCornerShape(11.dp),
                                color = if (modalTab == 0) CleanSurface else Color.Transparent,
                                border = if (modalTab == 0) BorderStroke(1.dp, CleanBorder) else null,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = if (modalTab == 0) CleanPrimary else CleanTextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Scanner & FX",
                                        fontSize = 12.sp,
                                        fontWeight = if (modalTab == 0) FontWeight.Bold else FontWeight.Medium,
                                        color = if (modalTab == 0) CleanTextPrimary else CleanTextSecondary
                                    )
                                }
                            }

                            Surface(
                                onClick = { modalTab = 1 },
                                shape = RoundedCornerShape(11.dp),
                                color = if (modalTab == 1) CleanSurface else Color.Transparent,
                                border = if (modalTab == 1) BorderStroke(1.dp, CleanBorder) else null,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = if (modalTab == 1) CleanPrimary else CleanTextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Room Products (${savedProducts.size})",
                                        fontSize = 12.sp,
                                        fontWeight = if (modalTab == 1) FontWeight.Bold else FontWeight.Medium,
                                        color = if (modalTab == 1) CleanTextPrimary else CleanTextSecondary
                                    )
                                }
                            }
                        }

                        // Scanner Entry Mode Switcher (only when in Scanner tab)
                        if (modalTab == 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CleanSurfaceVariant.copy(alpha = 0.5f))
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    onClick = {
                                        entryMode = ScannerEntryMode.TRANSACTION
                                        scannedItem = scannedItem?.copy(isPayout = false)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (entryMode == ScannerEntryMode.TRANSACTION) CleanPrimary else Color.Transparent,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ShoppingCart,
                                            contentDescription = null,
                                            tint = if (entryMode == ScannerEntryMode.TRANSACTION) CleanOnPrimary else CleanTextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = viewModel.translate("scan_mode_tx"),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (entryMode == ScannerEntryMode.TRANSACTION) CleanOnPrimary else CleanTextSecondary
                                        )
                                    }
                                }

                                Surface(
                                    onClick = {
                                        entryMode = ScannerEntryMode.PAYOUT
                                        scannedItem = scannedItem?.copy(isPayout = true)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (entryMode == ScannerEntryMode.PAYOUT) CleanOrange else Color.Transparent,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = if (entryMode == ScannerEntryMode.PAYOUT) Color.White else CleanTextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = viewModel.translate("scan_mode_payout"),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (entryMode == ScannerEntryMode.PAYOUT) Color.White else CleanTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Success feedback banner
                AnimatedVisibility(visible = showSuccessBanner) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CleanSuccessContainer),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CleanSuccess.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = CleanSuccess,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Action Recorded Successfully",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = CleanTextPrimary
                                )
                                Text(
                                    text = successMessage,
                                    fontSize = 12.sp,
                                    color = CleanTextSecondary
                                )
                            }
                            IconButton(onClick = { showSuccessBanner = false }) {
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

                if (modalTab == 0) {
                    // -------------------------------------------------------------
                    // TAB 0: SCANNER & CURRENCY CONVERTER
                    // -------------------------------------------------------------
                    // 1. LIVE CAMERA VIEWFINDER COMPOSABLE
                    CameraBarcodeScannerView(
                        onBarcodeDetected = { scanned ->
                            scannedItem = scanned
                            customAmountText = String.format(java.util.Locale.US, "%.2f", scanned.amountUsd)
                        },
                        currentMode = entryMode
                    )

                    // 2. SCANNED BARCODE RESULT CARD (IF DETECTED)
                    val currentItem = scannedItem
                    if (currentItem != null) {
                        val amountVal = customAmountText.toDoubleOrNull() ?: currentItem.amountUsd
                        val effectiveExchangeRate = viewModel.getEffectiveExchangeRate(currentItem.currencyCode, targetConverterCurrency.code)
                        val convertedTargetPrice = viewModel.convertCurrency(amountVal, currentItem.currencyCode, targetConverterCurrency.code)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("scanned_item_card"),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = CleanSurface),
                            border = BorderStroke(1.5.dp, if (entryMode == ScannerEntryMode.TRANSACTION) CleanPrimary else CleanOrange)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Header: Format & Raw value badges
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (entryMode == ScannerEntryMode.TRANSACTION) CleanPrimaryContainer else CleanOrangeContainer
                                        ) {
                                            Text(
                                                text = currentItem.formatName,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (entryMode == ScannerEntryMode.TRANSACTION) CleanPrimary else CleanOrangeText,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = CleanSurfaceVariant
                                        ) {
                                            Text(
                                                text = currentItem.rawValue,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.SemiBold,
                                                color = CleanTextPrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = if (entryMode == ScannerEntryMode.TRANSACTION) CleanTertiaryContainer else CleanOrangeContainer
                                    ) {
                                        Text(
                                            text = if (entryMode == ScannerEntryMode.TRANSACTION) "SALE INFLOW" else "PAYOUT OUTFLOW",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (entryMode == ScannerEntryMode.TRANSACTION) CleanTertiary else CleanOrangeText,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                // Title & Description
                                Column {
                                    Text(
                                        text = currentItem.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CleanTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (entryMode == ScannerEntryMode.TRANSACTION)
                                            "Customer: ${currentItem.customerOrRecipient} • Origin: ${currentItem.country}"
                                        else
                                            "Recipient: ${currentItem.customerOrRecipient} • Destination: ${currentItem.country}",
                                        fontSize = 12.sp,
                                        color = CleanTextSecondary
                                    )
                                }

                                // -------------------------------------------------------------
                                // CURRENCY CONVERTER FEATURE: Target Currency Selector & Live Rate
                                // -------------------------------------------------------------
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = CleanSurfaceVariant,
                                    border = BorderStroke(1.dp, CleanBorder)
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
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CurrencyExchange,
                                                    contentDescription = null,
                                                    tint = CleanPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "CURRENCY CONVERTER",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CleanPrimary,
                                                    letterSpacing = 0.6.sp
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = CleanPrimaryContainer
                                            ) {
                                                Text(
                                                    text = "1 ${currentItem.currencyCode} = ${String.format(java.util.Locale.US, "%.4f", effectiveExchangeRate)} ${targetConverterCurrency.code}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CleanPrimary,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "Select Target Conversion Currency:",
                                            fontSize = 11.sp,
                                            color = CleanTextSecondary
                                        )

                                        // Currency Selection Chips
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(viewModel.currencies) { curr ->
                                                val isCurrSelected = curr.code.equals(targetConverterCurrency.code, ignoreCase = true)
                                                Surface(
                                                    onClick = { viewModel.setConverterTargetCurrency(curr) },
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (isCurrSelected) CleanPrimary else CleanSurface,
                                                    border = BorderStroke(1.dp, if (isCurrSelected) CleanPrimary else CleanBorder)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(text = curr.flagEmoji, fontSize = 12.sp)
                                                        Text(
                                                            text = curr.code,
                                                            fontSize = 11.sp,
                                                            fontWeight = if (isCurrSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isCurrSelected) CleanOnPrimary else CleanTextPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Converted Price Comparison Box
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = CleanSurface,
                                            border = BorderStroke(1.dp, CleanBorderSubtle)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "ORIGINAL PRICE",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CleanTextMuted
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "$${String.format(java.util.Locale.US, "%,.2f", amountVal)} ${currentItem.currencyCode}",
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CleanTextPrimary
                                                    )
                                                }

                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = "Converted to",
                                                    tint = CleanPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )

                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "CONVERTED PRICE (${targetConverterCurrency.code})",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CleanPrimary
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "${targetConverterCurrency.symbol}${String.format(java.util.Locale.US, "%,.2f", convertedTargetPrice)}",
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = CleanPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // -------------------------------------------------------------
                                // ACTION BUTTONS: Room DB Save, Terminal Load & Entry Trigger
                                // -------------------------------------------------------------
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Save to Room DB button
                                    Button(
                                        onClick = {
                                            viewModel.saveScannedProductToDb(
                                                name = currentItem.title,
                                                price = amountVal,
                                                currency = currentItem.currencyCode,
                                                barcode = currentItem.rawValue,
                                                barcodeFormat = currentItem.formatName,
                                                targetCurrency = targetConverterCurrency.code,
                                                convertedPrice = convertedTargetPrice,
                                                notes = "Scanned item via ML Kit",
                                                onSuccess = {
                                                    successMessage = "Saved '${currentItem.title}' (${targetConverterCurrency.symbol}${String.format(java.util.Locale.US, "%.2f", convertedTargetPrice)}) to Room Database!"
                                                    showSuccessBanner = true
                                                    Toast.makeText(context, "Saved to Room Database!", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .height(46.dp)
                                            .testTag("save_to_room_db_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CleanSecondary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Storage,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Save to Room DB",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Load to Terminal button
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.loadBarcodeIntoTerminal(
                                                amountInUsd = amountVal,
                                                currencyCode = currentItem.currencyCode
                                            )
                                            Toast.makeText(context, "Loaded into Terminal!", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                            .testTag("load_to_terminal_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, CleanBorder)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Input,
                                            contentDescription = null,
                                            tint = CleanPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "To Terminal",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CleanPrimary
                                        )
                                    }
                                }

                                // Trigger Transaction / Payout Entry button
                                Button(
                                    onClick = {
                                        if (entryMode == ScannerEntryMode.TRANSACTION) {
                                            viewModel.triggerBarcodeTransactionEntry(
                                                amountInUsd = amountVal,
                                                currencyCode = currentItem.currencyCode,
                                                customerName = currentItem.customerOrRecipient,
                                                country = currentItem.country,
                                                barcodeValue = currentItem.rawValue,
                                                onSuccess = { created ->
                                                    successMessage = "Recorded transaction for $${String.format(java.util.Locale.US, "%.2f", created.amountInUsd)} (${created.convertedAmount} ${created.targetCurrency})"
                                                    showSuccessBanner = true
                                                    Toast.makeText(context, "Transaction successfully added to ledger!", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        } else {
                                            viewModel.triggerBarcodePayoutEntry(
                                                payoutAmountInUsd = amountVal,
                                                currencyCode = currentItem.currencyCode,
                                                recipientName = currentItem.customerOrRecipient,
                                                country = currentItem.country,
                                                barcodeValue = currentItem.rawValue,
                                                onSuccess = { created ->
                                                    successMessage = "Disbursed payout entry for ${created.customerName} (${created.convertedAmount} ${created.targetCurrency})"
                                                    showSuccessBanner = true
                                                    Toast.makeText(context, "Payout successfully logged in ledger!", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("trigger_barcode_entry_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (entryMode == ScannerEntryMode.TRANSACTION) CleanPrimary else CleanOrange
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (entryMode == ScannerEntryMode.TRANSACTION) Icons.Default.CheckCircle else Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (entryMode == ScannerEntryMode.TRANSACTION)
                                            "Trigger Transaction Entry"
                                        else
                                            "Trigger Payout Entry",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (entryMode == ScannerEntryMode.PAYOUT) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.disbursePayout(
                                                    method = PayoutMethodType.BANK_WIRE,
                                                    amountInUsd = amountVal,
                                                    currencyCode = currentItem.currencyCode,
                                                    recipientName = currentItem.customerOrRecipient,
                                                    onSuccess = { created ->
                                                        successMessage = "Disbursed Bank Wire to ${created.customerName} ($${String.format(java.util.Locale.US, "%.2f", created.amountInUsd)})"
                                                        showSuccessBanner = true
                                                        Toast.makeText(context, "Bank Wire payout dispatched!", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .testTag("scanner_payout_bank_wire_button"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalance,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Bank Wire", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                viewModel.disbursePayout(
                                                    method = PayoutMethodType.PAYPAL,
                                                    amountInUsd = amountVal,
                                                    currencyCode = currentItem.currencyCode,
                                                    recipientName = currentItem.customerOrRecipient,
                                                    onSuccess = { created ->
                                                        successMessage = "Disbursed PayPal to ${created.customerName} ($${String.format(java.util.Locale.US, "%.2f", created.amountInUsd)})"
                                                        showSuccessBanner = true
                                                        Toast.makeText(context, "PayPal payout dispatched!", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .testTag("scanner_payout_paypal_button"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Payments,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("PayPal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. SAMPLE PRESETS / SIMULATION BARCODES FOR INSTANT TESTING
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = null,
                                        tint = CleanSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Quick Sample Barcodes",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CleanTextPrimary
                                    )
                                }
                                Text(
                                    text = "Tap to test scan",
                                    fontSize = 11.sp,
                                    color = CleanTextMuted
                                )
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(SAMPLE_BARCODE_PRESETS) { sample ->
                                    Surface(
                                        onClick = {
                                            scannedItem = sample.copy(isPayout = entryMode == ScannerEntryMode.PAYOUT)
                                            customAmountText = String.format(java.util.Locale.US, "%.2f", sample.amountUsd)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = CleanSurfaceVariant,
                                        border = BorderStroke(1.dp, CleanBorder)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = sample.formatName,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CleanPrimary
                                                )
                                                Text(
                                                    text = "•",
                                                    fontSize = 9.sp,
                                                    color = CleanTextMuted
                                                )
                                                Text(
                                                    text = "$${sample.amountUsd}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CleanTextPrimary
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = sample.title,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = CleanTextPrimary,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // -------------------------------------------------------------
                    // TAB 1: ROOM DATABASE STORED PRODUCTS
                    // -------------------------------------------------------------
                    // Target Currency Filter Chips for Room DB Products
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
                                        imageVector = Icons.Default.CurrencyExchange,
                                        contentDescription = null,
                                        tint = CleanPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "CONVERT ALL STORED PRICES TO:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CleanTextPrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CleanPrimaryContainer
                                ) {
                                    Text(
                                        text = targetConverterCurrency.code,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CleanPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(viewModel.currencies) { curr ->
                                    val isCurrSelected = curr.code.equals(targetConverterCurrency.code, ignoreCase = true)
                                    Surface(
                                        onClick = { viewModel.setConverterTargetCurrency(curr) },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isCurrSelected) CleanPrimary else CleanSurfaceVariant,
                                        border = BorderStroke(1.dp, if (isCurrSelected) CleanPrimary else CleanBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(text = curr.flagEmoji, fontSize = 12.sp)
                                            Text(
                                                text = curr.code,
                                                fontSize = 11.sp,
                                                fontWeight = if (isCurrSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isCurrSelected) CleanOnPrimary else CleanTextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Search and Action Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by name, barcode...", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = CleanTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CleanSurface,
                                unfocusedContainerColor = CleanSurface,
                                focusedBorderColor = CleanPrimary,
                                unfocusedBorderColor = CleanBorder
                            ),
                            singleLine = true
                        )

                        if (savedProducts.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    viewModel.clearAllScannedProducts()
                                    Toast.makeText(context, "Cleared all stored products", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = CleanDanger)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Filtered Room DB Products List
                    val filteredProducts = remember(savedProducts, searchQuery) {
                        if (searchQuery.isBlank()) savedProducts
                        else savedProducts.filter {
                            it.name.contains(searchQuery, ignoreCase = true) ||
                            it.barcode.contains(searchQuery, ignoreCase = true) ||
                            it.category.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    if (filteredProducts.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CleanSurface),
                            border = BorderStroke(1.dp, CleanBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(CleanPrimaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = CleanPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = if (searchQuery.isBlank()) "No Stored Products Yet" else "No matching products",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanTextPrimary
                                )
                                Text(
                                    text = if (searchQuery.isBlank())
                                        "Scan products using the camera or select sample presets to save prices and currency conversions in the Room database."
                                    else
                                        "Try searching with a different product name or barcode digits.",
                                    fontSize = 12.sp,
                                    color = CleanTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                if (searchQuery.isBlank()) {
                                    Button(
                                        onClick = { modalTab = 0 },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CleanPrimary)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open Scanner", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            filteredProducts.forEach { product ->
                                val converted = viewModel.convertCurrency(product.price, product.currency, targetConverterCurrency.code)
                                val rate = viewModel.getEffectiveExchangeRate(product.currency, targetConverterCurrency.code)

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = CleanSurface),
                                    border = BorderStroke(1.dp, CleanBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Header: Name + Category badge
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = product.name,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CleanTextPrimary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = CleanSurfaceVariant
                                                    ) {
                                                        Text(
                                                            text = "${product.barcodeFormat}: ${product.barcode}",
                                                            fontSize = 10.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = CleanTextSecondary,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = CleanPrimaryContainer
                                                    ) {
                                                        Text(
                                                            text = product.category,
                                                            fontSize = 10.sp,
                                                            color = CleanPrimary,
                                                            fontWeight = FontWeight.SemiBold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteScannedProduct(product)
                                                    Toast.makeText(context, "Deleted from Room DB", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete product",
                                                    tint = CleanDanger,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        // Price Breakdown Box
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = CleanSurfaceVariant,
                                            border = BorderStroke(1.dp, CleanBorderSubtle)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "STORED PRICE",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CleanTextMuted
                                                    )
                                                    Text(
                                                        text = "$${String.format(java.util.Locale.US, "%,.2f", product.price)} ${product.currency}",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CleanTextPrimary
                                                    )
                                                }

                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = null,
                                                    tint = CleanPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )

                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "CONVERTED (${targetConverterCurrency.code})",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CleanPrimary
                                                    )
                                                    Text(
                                                        text = "${targetConverterCurrency.symbol}${String.format(java.util.Locale.US, "%,.2f", converted)}",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = CleanPrimary
                                                    )
                                                    Text(
                                                        text = "Rate: 1 ${product.currency} = ${String.format(java.util.Locale.US, "%.4f", rate)}",
                                                        fontSize = 9.sp,
                                                        color = CleanTextMuted
                                                    )
                                                }
                                            }
                                        }

                                        // Action Row: Send to Terminal or View in Scanner
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.loadBarcodeIntoTerminal(
                                                        amountInUsd = product.price,
                                                        currencyCode = product.currency
                                                    )
                                                    Toast.makeText(context, "Loaded into Terminal!", Toast.LENGTH_SHORT).show()
                                                    onDismiss()
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(38.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, CleanBorder)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Input,
                                                    contentDescription = null,
                                                    tint = CleanPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "To Terminal",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = CleanPrimary
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    scannedItem = ScannedBarcodeItem(
                                                        rawValue = product.barcode,
                                                        formatName = product.barcodeFormat,
                                                        title = product.name,
                                                        amountUsd = product.price,
                                                        currencyCode = product.currency,
                                                        customerOrRecipient = "Room DB Item",
                                                        country = "Global"
                                                    )
                                                    customAmountText = String.format(java.util.Locale.US, "%.2f", product.price)
                                                    modalTab = 0
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(38.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = CleanPrimary)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CurrencyExchange,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Convert FX",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
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
    }
}

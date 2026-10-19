package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.util.QrCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dialog displaying a dynamically generated QR code representing the transaction total,
 * allowing customers to scan and pay via their mobile wallet.
 */
@Composable
fun QrPaymentDialog(
    amount: Double,
    currencyCode: String,
    currencySymbol: String,
    amountUsd: Double,
    merchantId: String = "org_6RsNhnGbSQ3s7rFPAoYfQjA",
    referenceCode: String = remember { "QR-PAY-${(10000..99999).random()}" },
    onDismiss: () -> Unit,
    onPaymentConfirmed: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // Build the wallet payment URI
    val paymentUri = remember(amount, currencyCode, merchantId, referenceCode) {
        QrCodeGenerator.buildPaymentUri(
            merchantId = merchantId,
            currencyCode = currencyCode,
            amount = amount,
            reference = referenceCode
        )
    }

    // Generate QR Bitmap asynchronously
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGenerating by remember { mutableStateOf(true) }

    LaunchedEffect(paymentUri) {
        isGenerating = true
        val bitmap = withContext(Dispatchers.Default) {
            QrCodeGenerator.generateQrBitmap(
                content = paymentUri,
                size = 512,
                foregroundColor = android.graphics.Color.parseColor("#0F172A"),
                backgroundColor = android.graphics.Color.WHITE
            )
        }
        qrBitmap = bitmap
        isGenerating = false
    }

    val formattedTotal = remember(amount, currencySymbol) {
        "$currencySymbol${String.format(java.util.Locale.US, "%.2f", amount)}"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp)
                .testTag("qr_payment_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = CleanSurface,
            border = BorderStroke(1.dp, CleanBorder),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with title and close button
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CleanPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = CleanPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Scan & Pay QR Code",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextPrimary
                            )
                            Text(
                                text = "Customer mobile wallet checkout",
                                fontSize = 11.sp,
                                color = CleanTextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CleanSurfaceVariant)
                            .testTag("qr_dialog_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = CleanTextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(color = CleanBorderSubtle)

                // Total Amount Banner
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = CleanPrimaryContainer,
                    border = BorderStroke(1.dp, CleanPrimary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "TOTAL TO PAY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanPrimary,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = formattedTotal,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CleanOnPrimaryContainer,
                            modifier = Modifier.testTag("qr_payment_total_amount")
                        )
                        Text(
                            text = "≈ USD $${String.format(java.util.Locale.US, "%.2f", amountUsd)} • 0% Merchant FX Markup",
                            fontSize = 11.sp,
                            color = CleanPrimary.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // QR Code Display Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(2.dp, CleanBorder),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(240.dp)
                        .testTag("qr_code_image_container")
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isGenerating) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = CleanPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "Generating QR Code...",
                                    fontSize = 12.sp,
                                    color = CleanTextSecondary
                                )
                            }
                        } else if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap!!.asImageBitmap(),
                                contentDescription = "Transaction total QR code: $formattedTotal",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp)
                                    .testTag("qr_code_bitmap")
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "Failed to render QR",
                                    fontSize = 12.sp,
                                    color = Color(0xFFDC2626),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Supported Wallets Indicator Pills
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "SUPPORTED MOBILE WALLETS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextMuted,
                        letterSpacing = 0.6.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WalletPill(label = "Apple Pay", icon = "🍎")
                        WalletPill(label = "Google Pay", icon = "💳")
                        WalletPill(label = "SEPA/EPC", icon = "🇪🇺")
                        WalletPill(label = "Alipay/WeChat", icon = "🇨🇳")
                    }
                }

                // Payload Deep Link Preview & Copy
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CleanSurfaceVariant,
                    border = BorderStroke(1.dp, CleanBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PAYMENT REF: $referenceCode",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextSecondary
                            )
                            Text(
                                text = paymentUri,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CleanPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Payment Link", paymentUri))
                                Toast.makeText(context, "Payment link copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Link",
                                tint = CleanPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Action Buttons: Share QR Link & Simulated Mobile Wallet Confirmation
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Share Payment Link Button
                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Scan or open this link to pay $formattedTotal: $paymentUri"
                                )
                                putExtra(Intent.EXTRA_TITLE, "Mobile Wallet Payment ($formattedTotal)")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Payment QR"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("share_qr_payment_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CleanPrimary),
                        border = BorderStroke(1.dp, CleanPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Share Payment Link / QR",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Simulate Customer Scanned & Paid confirmation button
                    if (onPaymentConfirmed != null) {
                        Button(
                            onClick = {
                                onPaymentConfirmed()
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("confirm_qr_payment_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Simulate Customer Scan & Pay",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletPill(label: String, icon: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = CleanSurfaceVariant,
        border = BorderStroke(1.dp, CleanBorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = icon, fontSize = 11.sp)
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = CleanTextPrimary
            )
        }
    }
}

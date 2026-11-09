package com.example.ui

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SecurityThreatDefenseModal(
    viewModel: GlobalMarketViewModel,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CleanSurface),
            border = BorderStroke(1.5.dp, CleanBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            SecurityThreatDefenseContent(
                viewModel = viewModel,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
fun SecurityThreatDefenseContent(
    viewModel: GlobalMarketViewModel,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val threats by viewModel.threatList.collectAsStateWithLifecycle()
    val auditLogs by viewModel.securityAuditLogs.collectAsStateWithLifecycle()
    val firewallRules by viewModel.firewallRules.collectAsStateWithLifecycle()
    val isAutonomousActive by viewModel.isAutonomousContainmentActive.collectAsStateWithLifecycle()
    val isScanning by viewModel.isSecurityScanningActive.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Live Threats, 1: AI Risk, 2: Containment, 3: Approval Queue, 4: Audit Logs
    var selectedThreatForDetail by remember { mutableStateOf<SecurityThreat?>(null) }
    var actionNotification by remember { mutableStateOf<String?>(null) }

    val pendingCount = threats.count { it.status == ContainmentStatus.PENDING_APPROVAL }
    val criticalCount = threats.count { it.severity == ThreatSeverity.CRITICAL }
    val containedCount = threats.count { it.status == ContainmentStatus.CONTAINED_AUTO || it.status == ContainmentStatus.RESOLVED_APPROVED }

    Column(modifier = modifier.fillMaxSize()) {
        // -----------------------------------------------------------------
        // HEADER: SOC DEFENSE STATUS BAR
        // -----------------------------------------------------------------
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF0F172A),
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (criticalCount > 0) Color(0xFFDC2626) else CleanPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "GLABA SHIELD • SOC DEFENSE",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isScanning) Color(0xFF22C55E) else Color(0xFFEAB308))
                                )
                            }
                            Text(
                                text = "Live AI Threat Detection & Autonomous Containment",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Scan trigger
                        IconButton(
                            onClick = {
                                viewModel.triggerLiveThreatScan()
                                actionNotification = "Live WAF & packet scan completed"
                                coroutineScope.launch {
                                    delay(2500)
                                    actionNotification = null
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Scan Now",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (onDismiss != null) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // KPI Counter Ribbon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KpiStatChip(
                        modifier = Modifier.weight(1f),
                        label = "Active Threats",
                        value = "${threats.count { it.status == ContainmentStatus.ACTIVE_THREAT }}",
                        color = Color(0xFFEF4444)
                    )
                    KpiStatChip(
                        modifier = Modifier.weight(1f),
                        label = "Auto-Contained",
                        value = "$containedCount",
                        color = Color(0xFF10B981)
                    )
                    KpiStatChip(
                        modifier = Modifier.weight(1f),
                        label = "Approvals",
                        value = "$pendingCount",
                        color = Color(0xFFF59E0B)
                    )
                    KpiStatChip(
                        modifier = Modifier.weight(1f),
                        label = "Avg Risk",
                        value = if (threats.isNotEmpty()) "${threats.map { it.aiRiskScore }.average().toInt()}/100" else "0/100",
                        color = Color(0xFF38BDF8)
                    )
                }
            }
        }

        // Notification Banner
        AnimatedVisibility(visible = actionNotification != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CleanSuccess.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, CleanSuccess.copy(alpha = 0.3f))
            ) {
                Text(
                    text = actionNotification ?: "",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanSuccess,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        // -----------------------------------------------------------------
        // NAVIGATION TABS
        // -----------------------------------------------------------------
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = CleanSurfaceVariant,
            contentColor = CleanPrimary,
            edgePadding = 12.dp,
            divider = {}
        ) {
            val tabs = listOf(
                "Live Threats (${threats.size})",
                "AI Risk Matrix",
                "Auto-Containment",
                "Human Approval ($pendingCount)",
                "Audit Logs (${auditLogs.size})"
            )
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }

        // -----------------------------------------------------------------
        // MAIN BODY CONTENT
        // -----------------------------------------------------------------
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                0 -> LiveThreatStreamTab(
                    threats = threats,
                    firewallRules = firewallRules,
                    onInspectThreat = { selectedThreatForDetail = it },
                    onToggleRule = { ruleId, enabled -> viewModel.toggleFirewallRule(ruleId, enabled) }
                )
                1 -> AiRiskAssessmentTab(
                    threats = threats,
                    selectedThreat = selectedThreatForDetail ?: threats.firstOrNull(),
                    onSelectThreat = { selectedThreatForDetail = it }
                )
                2 -> AutomatedContainmentTab(
                    isAutonomousActive = isAutonomousActive,
                    threats = threats,
                    onToggleAutonomous = { viewModel.toggleAutonomousContainment(it) },
                    onTriggerEmergencyLockdown = {
                        viewModel.triggerEmergencyLockdown()
                        actionNotification = "Emergency Lockdown executed! Payout rails paused."
                        coroutineScope.launch {
                            delay(3000)
                            actionNotification = null
                        }
                    }
                )
                3 -> HumanApprovalQueueTab(
                    threats = threats.filter { it.status == ContainmentStatus.PENDING_APPROVAL || it.status == ContainmentStatus.ACTIVE_THREAT },
                    adminName = "J.P. Westerveld",
                    adminEmail = "westerveldjp@gmail.com",
                    onApprove = { threat ->
                        viewModel.approveThreatContainment(threat.id, "J.P. Westerveld")
                        actionNotification = "Threat ${threat.id} approved & contained by J.P. Westerveld"
                        coroutineScope.launch {
                            delay(2500)
                            actionNotification = null
                        }
                    },
                    onReject = { threat ->
                        viewModel.rejectThreat(threat.id, "J.P. Westerveld")
                        actionNotification = "Threat ${threat.id} marked as false positive"
                        coroutineScope.launch {
                            delay(2500)
                            actionNotification = null
                        }
                    }
                )
                4 -> SecurityAuditLogTab(
                    auditLogs = auditLogs,
                    onExportLogs = {
                        val csv = viewModel.generateSecurityAuditCsv()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, csv)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Export Security Audit Log"))
                    }
                )
            }
        }
    }

    // Threat Forensic Detail Dialog
    if (selectedThreatForDetail != null) {
        ThreatForensicDialog(
            threat = selectedThreatForDetail!!,
            onDismiss = { selectedThreatForDetail = null },
            onQuickApprove = {
                viewModel.approveThreatContainment(selectedThreatForDetail!!.id, "J.P. Westerveld")
                selectedThreatForDetail = null
            }
        )
    }
}

// =========================================================================
// TAB 0: LIVE THREATS & FIREWALL BREACHES
// =========================================================================
@Composable
private fun LiveThreatStreamTab(
    threats: List<SecurityThreat>,
    firewallRules: List<FirewallRuleStatus>,
    onInspectThreat: (SecurityThreat) -> Unit,
    onToggleRule: (String, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Live Stream Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "REAL-TIME THREAT DETECTIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanPrimary,
                    letterSpacing = 0.6.sp
                )
                Text(
                    text = "Edge WAF Engine: Active",
                    fontSize = 11.sp,
                    color = CleanSuccess,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        items(threats) { threat ->
            ThreatCardItem(
                threat = threat,
                onClick = { onInspectThreat(threat) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "ACTIVE WAF FIREWALL POLICIES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CleanTextMuted,
                letterSpacing = 0.6.sp
            )
        }

        items(firewallRules) { rule ->
            FirewallRuleCard(
                rule = rule,
                onToggle = { onToggleRule(rule.ruleId, it) }
            )
        }
    }
}

@Composable
private fun ThreatCardItem(
    threat: SecurityThreat,
    onClick: () -> Unit
) {
    val severityColor = when (threat.severity) {
        ThreatSeverity.CRITICAL -> Color(0xFFDC2626)
        ThreatSeverity.HIGH -> Color(0xFFEA580C)
        ThreatSeverity.MEDIUM -> Color(0xFFD97706)
        ThreatSeverity.LOW -> Color(0xFF2563EB)
        ThreatSeverity.INFO -> Color(0xFF64748B)
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CleanSurface,
        border = BorderStroke(1.2.dp, severityColor.copy(alpha = 0.4f)),
        shadowElevation = 2.dp
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
                    Text(text = threat.countryFlag, fontSize = 16.sp)
                    Text(
                        text = threat.sourceIp,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextPrimary
                    )
                    Text(
                        text = "• ${threat.sourceCountry}",
                        fontSize = 11.sp,
                        color = CleanTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = severityColor
                ) {
                    Text(
                        text = threat.severity.label,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = threat.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = CleanTextPrimary
            )

            // Target & Payload Excerpt
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CleanSurfaceVariant
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Target: ${threat.targetEndpoint}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CleanTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Risk: ${threat.aiRiskScore}/100",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = severityColor
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Status: ${threat.status.displayName}",
                    fontSize = 10.sp,
                    color = CleanTextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Tap to Inspect Forensics →",
                    fontSize = 11.sp,
                    color = CleanPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FirewallRuleCard(
    rule: FirewallRuleStatus,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CleanSurfaceVariant,
        border = BorderStroke(1.dp, CleanBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.ruleName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanTextPrimary
                )
                Text(
                    text = rule.description,
                    fontSize = 10.sp,
                    color = CleanTextSecondary
                )
                Text(
                    text = "${rule.packetsInspected} packets inspected • ${rule.threatsBlocked} threats dropped",
                    fontSize = 9.sp,
                    color = CleanPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Switch(
                checked = rule.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = CleanPrimary
                )
            )
        }
    }
}

// =========================================================================
// TAB 1: AI RISK ASSESSMENT MATRIX
// =========================================================================
@Composable
private fun AiRiskAssessmentTab(
    threats: List<SecurityThreat>,
    selectedThreat: SecurityThreat?,
    onSelectThreat: (SecurityThreat) -> Unit
) {
    val current = selectedThreat ?: threats.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Neural Engine Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = CleanPrimary,
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF1E1B4B), CleanPrimary)
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = CleanOrange, modifier = Modifier.size(20.dp))
                            Text(
                                text = "GLABASENTINEL-V4 NEURAL ENGINE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "Real-time AI behavioral heuristic analysis & deep blast radius estimation",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // Threat Selector Pills
        item {
            Text(
                text = "SELECT THREAT INCIDENT FOR AI DECOMPOSITION",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CleanTextMuted,
                letterSpacing = 0.6.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(threats) { t ->
                    val isSel = t.id == current?.id
                    Surface(
                        onClick = { onSelectThreat(t) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) CleanPrimary else CleanSurfaceVariant,
                        border = BorderStroke(1.dp, if (isSel) CleanPrimary else CleanBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = t.countryFlag, fontSize = 13.sp)
                            Text(
                                text = t.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else CleanTextPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        if (current != null) {
            item {
                // AI Risk Score & Confidence Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CleanSurface,
                    border = BorderStroke(1.5.dp, CleanBorder),
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
                                    text = "EVALUATION INCIDENT: ${current.id}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CleanTextMuted,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = current.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanTextPrimary
                                )
                            }

                            // Score Circle
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (current.aiRiskScore >= 80) Color(0xFFDC2626)
                                        else if (current.aiRiskScore >= 50) Color(0xFFEA580C)
                                        else Color(0xFF10B981)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${current.aiRiskScore}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "/100",
                                        fontSize = 8.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        // Confidence and Blast Radius
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = CleanSurfaceVariant
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(text = "AI Certainty", fontSize = 9.sp, color = CleanTextMuted)
                                    Text(text = "${current.confidencePercent}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CleanPrimary)
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(10.dp),
                                color = CleanSurfaceVariant
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(text = "Est. Financial Exposure", fontSize = 9.sp, color = CleanTextMuted)
                                    Text(text = current.blastRadiusExposure, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                }
                            }
                        }

                        // AI Assessment Summary
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CleanPrimaryContainer,
                            border = BorderStroke(1.dp, CleanPrimary.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "AI REASONING & RISK DECOMPOSITION:",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = current.aiAssessmentSummary,
                                    fontSize = 11.sp,
                                    color = CleanTextPrimary,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        // Action Recommendation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Gavel, contentDescription = null, tint = CleanOrange, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Action: ${current.containmentAction}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CleanTextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 2: AUTOMATED CONTAINMENT MATRIX
// =========================================================================
@Composable
private fun AutomatedContainmentTab(
    isAutonomousActive: Boolean,
    threats: List<SecurityThreat>,
    onToggleAutonomous: (Boolean) -> Unit,
    onTriggerEmergencyLockdown: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Master Autonomous Defense Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = if (isAutonomousActive) Color(0xFF065F46) else Color(0xFF7F1D1D),
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "AUTONOMOUS CONTAINMENT MATRIX",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = if (isAutonomousActive) "Active: Instant edge null-routing enabled" else "Standby: Manual security approval required",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Switch(
                            checked = isAutonomousActive,
                            onCheckedChange = onToggleAutonomous,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981)
                            )
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "CONTAINMENT RAILS & CIRCUIT BREAKERS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CleanTextMuted,
                letterSpacing = 0.6.sp
            )
        }

        item {
            ContainmentRailCard(
                title = "Edge WAF Global Null-Route",
                description = "Blacklists offending subnets across all Cloudflare/Edge POPs within <12ms.",
                status = "ACTIVE • 14,290 IPs Dropped",
                icon = Icons.Default.Block,
                color = Color(0xFF10B981)
            )
        }

        item {
            ContainmentRailCard(
                title = "Honeypot Sandbox Rerouting",
                description = "Silently captures botnet payload calls and supplies synthetic response tokens.",
                status = "ACTIVE • 88 Botnets Quarantined",
                icon = Icons.Default.FilterTiltShift,
                color = Color(0xFF38BDF8)
            )
        }

        item {
            ContainmentRailCard(
                title = "ING Bank Payout Circuit Breaker",
                description = "Halts automatic disbursements to NL43INGB0006372146 if risk exceeds 80/100.",
                status = "STANDBY • Payouts Protected",
                icon = Icons.Default.AccountBalance,
                color = CleanOrange
            )
        }

        item {
            ContainmentRailCard(
                title = "Credential Revocation & Token Freezing",
                description = "Instantly voids compromised partner or merchant API session keys.",
                status = "ARMED • Zero-Trust Enforced",
                icon = Icons.Default.KeyOff,
                color = Color(0xFF8B5CF6)
            )
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onTriggerEmergencyLockdown,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "TRIGGER EMERGENCY TERMINAL LOCKDOWN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ContainmentRailCard(
    title: String,
    description: String,
    status: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CleanSurfaceVariant,
        border = BorderStroke(1.dp, CleanBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CleanTextPrimary)
                Text(text = description, fontSize = 10.sp, color = CleanTextSecondary)
                Text(text = status, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}

// =========================================================================
// TAB 3: HUMAN APPROVAL QUEUE
// =========================================================================
@Composable
private fun HumanApprovalQueueTab(
    threats: List<SecurityThreat>,
    adminName: String,
    adminEmail: String,
    onApprove: (SecurityThreat) -> Unit,
    onReject: (SecurityThreat) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = CleanPrimaryContainer,
                border = BorderStroke(1.dp, CleanPrimary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, tint = CleanPrimary)
                    Column {
                        Text(
                            text = "SECURITY ADMIN APPROVAL REQUIRED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanPrimary
                        )
                        Text(
                            text = "Authorized Reviewer: $adminName ($adminEmail)",
                            fontSize = 11.sp,
                            color = CleanTextPrimary
                        )
                    }
                }
            }
        }

        if (threats.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = CleanSurfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = CleanSuccess, modifier = Modifier.size(36.dp))
                        Text(text = "Approval Queue Clean", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CleanTextPrimary)
                        Text(text = "No pending quarantine decisions awaiting authorization.", fontSize = 11.sp, color = CleanTextSecondary)
                    }
                }
            }
        } else {
            items(threats) { threat ->
                HumanApprovalCard(
                    threat = threat,
                    onApprove = { onApprove(threat) },
                    onReject = { onReject(threat) }
                )
            }
        }
    }
}

@Composable
private fun HumanApprovalCard(
    threat: SecurityThreat,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CleanSurface,
        border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = threat.countryFlag, fontSize = 16.sp)
                    Text(text = threat.sourceIp, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }

                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFF59E0B)) {
                    Text(text = "AWAITING APPROVAL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            Text(text = threat.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CleanTextPrimary)
            Text(text = "Recommended Action: ${threat.containmentAction}", fontSize = 11.sp, color = CleanTextSecondary)

            Surface(shape = RoundedCornerShape(8.dp), color = CleanSurfaceVariant) {
                Text(
                    text = "Payload: ${threat.payloadExcerpt}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CleanTextPrimary,
                    modifier = Modifier.padding(8.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1.2f).height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CleanPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Approve Containment", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(0.8f).height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CleanBorder)
                ) {
                    Text(text = "Whitelist", fontSize = 11.sp, color = CleanTextSecondary)
                }
            }
        }
    }
}

// =========================================================================
// TAB 4: SECURITY AUDIT LOGS & COMPLIANCE
// =========================================================================
@Composable
private fun SecurityAuditLogTab(
    auditLogs: List<SecurityAuditLogEntry>,
    onExportLogs: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "IMMUTABLE SECURITY AUDIT TRAIL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanPrimary,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = "Cryptographically signed • PCI-DSS v4.0 & GDPR Art 32 Compliant",
                        fontSize = 10.sp,
                        color = CleanTextSecondary
                    )
                }

                Button(
                    onClick = onExportLogs,
                    colors = ButtonDefaults.buttonColors(containerColor = CleanPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Export", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(auditLogs) { log ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = CleanSurface,
                border = BorderStroke(1.dp, CleanBorder)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = log.eventType,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextPrimary
                        )
                        Text(
                            text = dateFormat.format(Date(log.timestamp)),
                            fontSize = 9.sp,
                            color = CleanTextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(text = log.description, fontSize = 11.sp, color = CleanTextSecondary)

                    // Actor and Hash
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Actor: ${log.actor}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = CleanPrimary)
                        Text(
                            text = "SHA: ${log.cryptographicHash.take(12)}...",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CleanTextMuted
                        )
                    }

                    // Compliance tags
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        log.complianceTags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CleanSurfaceVariant
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 8.sp,
                                    color = CleanTextSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// FORENSIC DETAILS MODAL
// =========================================================================
@Composable
private fun ThreatForensicDialog(
    threat: SecurityThreat,
    onDismiss: () -> Unit,
    onQuickApprove: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CleanSurface),
            border = BorderStroke(1.5.dp, CleanBorder)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Forensic Threat Analysis", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CleanTextPrimary)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                HorizontalDivider(color = CleanBorder)

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { DetailForensicRow("Threat ID", threat.id) }
                    item { DetailForensicRow("Classification", threat.category.displayName) }
                    item { DetailForensicRow("Severity", threat.severity.label) }
                    item { DetailForensicRow("Source IP & Location", "${threat.countryFlag} ${threat.sourceIp} (${threat.sourceCountry})") }
                    item { DetailForensicRow("Targeted API Endpoint", threat.targetEndpoint) }
                    item { DetailForensicRow("AI Risk Score", "${threat.aiRiskScore}/100 (${threat.confidencePercent}% confidence)") }
                    item { DetailForensicRow("Financial Blast Exposure", threat.blastRadiusExposure) }
                    item { DetailForensicRow("Containment Action", threat.containmentAction) }

                    item {
                        Text(text = "RAW PAYLOAD EXCERPT:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CleanTextMuted)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A)
                        ) {
                            Text(
                                text = threat.payloadExcerpt,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    item {
                        Text(text = "AI ASSESSMENT SUMMARY:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CleanTextMuted)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CleanSurfaceVariant
                        ) {
                            Text(
                                text = threat.aiAssessmentSummary,
                                fontSize = 11.sp,
                                color = CleanTextPrimary,
                                modifier = Modifier.padding(10.dp),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Button(
                    onClick = onQuickApprove,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CleanPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Confirm & Enforce Containment", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun DetailForensicRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = CleanTextSecondary)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CleanTextPrimary)
    }
}

@Composable
private fun KpiStatChip(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E293B)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 8.sp, color = Color(0xFF94A3B8), maxLines = 1)
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

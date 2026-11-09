package com.example.data

enum class ThreatSeverity(val label: String, val level: Int) {
    CRITICAL("CRITICAL", 4),
    HIGH("HIGH", 3),
    MEDIUM("MEDIUM", 2),
    LOW("LOW", 1),
    INFO("INFO", 0)
}

enum class ThreatCategory(val displayName: String, val iconName: String) {
    DDOS_FLOOD("DDoS Traffic Spike", "NetworkCheck"),
    FIREWALL_BREACH("WAF Firewall Bypass", "Security"),
    CREDENTIAL_STUFFING("Credential Stuffing Attack", "VpnKey"),
    CARDING_BOTNET("Automated Carding Botnet", "CreditCardOff"),
    WEBHOOK_TAMPER("Spoofed Webhook Signature", "SyncProblem"),
    PAYOUT_HIJACK("Unauthorized Payout Divert", "AccountBalance"),
    SQLI_WAF_BYPASS("SQL Injection Probe", "Code"),
    ZERO_DAY_PROBE("Zero-Day API Scanning", "BugReport")
}

enum class ContainmentStatus(val displayName: String) {
    ACTIVE_THREAT("Active Threat"),
    CONTAINED_AUTO("Auto-Contained"),
    PENDING_APPROVAL("Pending Approval"),
    RESOLVED_APPROVED("Human Approved & Blocked"),
    FALSE_POSITIVE("Whitelisted")
}

data class SecurityThreat(
    val id: String,
    val title: String,
    val category: ThreatCategory,
    val severity: ThreatSeverity,
    val aiRiskScore: Int, // 0 - 100
    val confidencePercent: Float, // e.g. 99.4f
    val sourceIp: String,
    val sourceCountry: String,
    val countryFlag: String,
    val targetEndpoint: String,
    val payloadExcerpt: String,
    val status: ContainmentStatus,
    val detectedTimestamp: Long = System.currentTimeMillis(),
    val containmentAction: String,
    val aiAssessmentSummary: String,
    val blastRadiusExposure: String,
    val humanApprovedBy: String? = null,
    val humanApprovalTimestamp: Long? = null
)

data class SecurityAuditLogEntry(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,
    val severity: ThreatSeverity,
    val actor: String,
    val description: String,
    val cryptographicHash: String,
    val complianceTags: List<String>
)

data class FirewallRuleStatus(
    val ruleId: String,
    val ruleName: String,
    val description: String,
    val scope: String,
    val packetsInspected: Long,
    val threatsBlocked: Int,
    val isEnabled: Boolean = true
)

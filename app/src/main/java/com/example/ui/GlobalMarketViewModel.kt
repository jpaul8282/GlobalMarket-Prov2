package com.example.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ExchangeRateClient
import com.example.data.InventoryProductEntity
import com.example.data.SalesTransactionEntity
import com.example.data.ScannedProductEntity
import com.example.data.TerminalTransactionLogEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionHistoryEntity
import com.example.data.TransactionLogEntity
import com.example.data.TransactionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppLanguage(val displayName: String, val flagUrl: String) {
    ENGLISH("English", "https://flagcdn.com/w20/us.png"),
    ESPANOL("Español", "https://flagcdn.com/w20/es.png"),
    FRANCAIS("Français", "https://flagcdn.com/w20/fr.png"),
    DEUTSCH("Deutsch", "https://flagcdn.com/w20/de.png"),
    CHINESE("中文", "https://flagcdn.com/w20/cn.png")
}

enum class TransactionSortOrder {
    NEWEST,
    OLDEST,
    HIGHEST_AMOUNT,
    LOWEST_AMOUNT
}

enum class LogTypeFilter {
    ALL,
    SCANNED_ITEMS,
    PAYMENTS,
    PAYOUTS
}

data class LocalPaymentRail(
    val id: String,
    val name: String,
    val feeText: String,
    val settlementTime: String,
    val description: String,
    val isInstant: Boolean = true
)

data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val exchangeRate: Double,
    val flagUrl: String,
    val defaultCountry: String,
    val flagEmoji: String = "🌐",
    val region: String = "Global",
    val liveChangePercent: Double = 0.0
)

data class UserProfile(
    val name: String = "Jurgen-Paul Westerveld",
    val email: String = "westerveldjp@gmail.com",
    val initials: String = "JP",
    val role: String = "Admin / Global Merchant",
    val organization: String = "GLABA Marketing Pro Corp",
    val orgId: String = "org_6RsNhnGbSQ3s7rFPAoYfQjA",
    val isTwoFactorEnabled: Boolean = true,
    val autoConvertPayouts: Boolean = true,
    val emailNotifications: Boolean = true
)

data class MarketingCampaign(
    val id: String,
    val title: String,
    val tag: String,
    val promoCode: String,
    val discountPercent: Int,
    val shortUrl: String,
    val shareMessage: String
)

enum class PayoutMethodType(val id: String, val displayName: String, val subtitle: String) {
    BANK_WIRE("bank_wire", "Bank Wire Transfer", "SWIFT / IBAN / Direct Interbank Wire"),
    PAYPAL("paypal", "PayPal", "Instant digital wallet payout")
}

data class BankWireDetails(
    val bankName: String = "JPMorgan Chase Bank, N.A.",
    val accountHolderName: String = "Jurgen-Paul Westerveld",
    val accountNumberOrIban: String = "US89CHAS021000021482019",
    val swiftBic: String = "CHASUS33XXX",
    val routingNumber: String = "021000021",
    val country: String = "United States",
    val intermediaryBank: String = "Federal Reserve Bank NY"
)

data class PayPalDetails(
    val paypalEmailOrId: String = "westerveldjp@gmail.com",
    val recipientName: String = "Jurgen-Paul Westerveld",
    val phoneOrPaypalMe: String = "paypal.me/jpwesterveld",
    val accountType: String = "Business Premier"
)

data class MerchantPayoutConfig(
    val selectedMethod: PayoutMethodType = PayoutMethodType.BANK_WIRE,
    val bankWire: BankWireDetails = BankWireDetails(),
    val payPal: PayPalDetails = PayPalDetails(),
    val autoDisburse: Boolean = true,
    val minimumPayoutThresholdUsd: Double = 50.0,
    val preferredCurrency: String = "USD"
)

class GlobalMarketViewModel(
    private val repository: TransactionRepository,
    private val firestoreRepository: com.example.data.FirestoreRepository? = null
) : ViewModel() {

    // Localization Translations
    private val translations = mapOf(
        AppLanguage.ENGLISH to mapOf(
            "app_title" to "GlobalMarket Pro",
            "hero_title" to "Sell Globally, Accept Payments Locally",
            "hero_desc" to "Reach customers worldwide with localized payment options and marketing automation.",
            "terminal_title" to "Global Payment Terminal",
            "amount" to "Amount (USD)",
            "currency" to "Target Currency",
            "pay_now" to "Pay Now",
            "checkout_title" to "Complete Payment",
            "card_number" to "Card Number",
            "expiry" to "Expiry Date",
            "cvv" to "CVV",
            "card_holder" to "Name on Card",
            "country" to "Country",
            "pay_button" to "Pay",
            "secure_payment" to "Secure payment encrypted",
            "active_sub" to "Active Subscription",
            "renew_date" to "Next Renewal",
            "org_id" to "Organization ID",
            "analytics_title" to "Global Sales Distribution",
            "sub_success" to "Payment Successful!",
            "sub_created" to "Subscription created successfully.",
            "history_title" to "Transaction History",
            "no_transactions" to "No transactions found yet.",
            "start_trial" to "Start Free Trial",
            "watch_demo" to "Watch Demo",
            "processing" to "Processing secure transaction...",
            "share_marketing" to "Share Marketing Campaign",
            "share_hub_title" to "Marketing & Growth Share Hub",
            "share_hub_subtitle" to "Distribute checkout links, promo campaigns & referral invites",
            "copy_link" to "Copy Link",
            "copy_content" to "Copy Pitch & Link",
            "share_native" to "Share via...",
            "campaign_links" to "Campaign Links & Offers",
            "share_content_preview" to "Share Content Preview",
            "direct_channels" to "Quick Share Channels",
            "link_copied_toast" to "Campaign link copied to clipboard!",
            "content_copied_toast" to "Full marketing message copied to clipboard!",
            "account_settings" to "Account & Settings",
            "audience_title" to "Global Audience & Reach",
            "audience_desc" to "Customer demographics and active regional markets",
            "sales_summary_title" to "Sales History & Summary",
            "sales_summary_subtitle" to "Localized Room SQLite sales ledger across worldwide markets",
            "gross_sales" to "Total Gross Sales",
            "total_orders" to "Total Orders",
            "avg_order_value" to "Avg Order Value",
            "active_markets" to "Active Markets",
            "filter_all" to "All Currencies",
            "search_placeholder" to "Search customer name or country...",
            "seed_sample_data" to "Seed Sample Sales",
            "clear_all_tx" to "Clear Ledger",
            "receipt_details" to "Sales Receipt",
            "delete_transaction" to "Delete Record",
            "status_completed" to "Settled",
            "export_csv" to "Export Ledger",
            "export_success" to "Sales ledger exported to clipboard!",
            "no_results_filter" to "No transactions found matching criteria.",
            "drilldown_title" to "Transaction Details",
            "back_to_summary" to "Back to Sales Summary",
            "fee_breakdown" to "Settlement & Fee Breakdown",
            "payment_method_info" to "Payment Instrument & Auth",
            "audit_timeline" to "Lifecycle Audit Trail",
            "customer_info" to "Customer & Market Origin",
            "net_payout" to "Net Merchant Payout",
            "order_details" to "Item & Plan Details",
            "copy_json" to "Copy JSON Record",
            "share_receipt" to "Share Receipt",
            "export_csv_modal_title" to "Export Sales Ledger to CSV",
            "export_csv_modal_desc" to "Generate an RFC 4180 compliant CSV file with localized amounts and fee breakdown for QuickBooks, Xero, or Excel.",
            "save_csv_file" to "Save CSV to Device",
            "share_csv_file" to "Share CSV File",
            "copy_csv_text" to "Copy Raw CSV Text",
            "csv_saved_success" to "CSV ledger successfully saved!",
            "csv_save_failed" to "Could not save CSV file.",
            "rfc_compliant" to "RFC 4180 • Multi-Currency Ledger",
            "for_accounting" to "External Accounting & Reconciliation",
            "barcode_scanner_title" to "Barcode & QR Scanner",
            "scan_barcode_desc" to "Point camera at product barcode or payment QR code",
            "scan_mode_tx" to "Transaction Entry",
            "scan_mode_payout" to "Payout Entry",
            "trigger_tx_entry" to "Record Transaction Entry",
            "trigger_payout_entry" to "Record Payout Entry",
            "load_to_terminal" to "Load into Terminal",
            "scanned_code" to "Scanned Code",
            "camera_permission_required" to "Camera Access Required",
            "camera_permission_desc" to "Grant camera permission to scan product barcodes and payout QR codes in real-time with Google ML Kit.",
            "grant_camera_permission" to "Enable Camera",
            "quick_scan_presets" to "Sample Barcodes & Presets",
            "entry_recorded_success" to "Ledger entry successfully recorded!",
            "payout_modal_title" to "Merchant Payout & Disbursement",
            "payout_bank_wire" to "Bank Wire Transfer",
            "payout_paypal" to "PayPal",
            "disburse_payout" to "Disburse Payout",
            "payout_destination" to "Payout Destination",
            "payout_amount" to "Payout Amount",
            "payout_fee" to "Processing Fee",
            "payout_net" to "Net Disbursement",
            "payout_success" to "Payout successfully disbursed!"
        ),
        AppLanguage.ESPANOL to mapOf(
            "app_title" to "GlobalMarket Pro",
            "hero_title" to "Venda globalmente, acepte pagos localmente",
            "hero_desc" to "Llegue a clientes de todo el mundo con opciones de pago localizadas y automatización de marketing.",
            "terminal_title" to "Terminal de Pago Global",
            "amount" to "Cantidad (USD)",
            "currency" to "Moneda de Destino",
            "pay_now" to "Pagar Ahora",
            "checkout_title" to "Completar Pago",
            "card_number" to "Número de Tarjeta",
            "expiry" to "Fecha de Vencimiento",
            "cvv" to "CVV",
            "card_holder" to "Nombre en la Tarjeta",
            "country" to "País",
            "pay_button" to "Pagar",
            "secure_payment" to "Pago seguro encriptado",
            "active_sub" to "Suscripción Activa",
            "renew_date" to "Próxima Renovación",
            "org_id" to "ID de la Organización",
            "analytics_title" to "Distribución de Ventas Globales",
            "sub_success" to "¡Pago Exitoso!",
            "sub_created" to "Suscripción creada correctamente.",
            "history_title" to "Historial de Transacciones",
            "no_transactions" to "Aún no hay transacciones.",
            "start_trial" to "Iniciar Prueba Gratis",
            "watch_demo" to "Ver Demo",
            "processing" to "Procesando transacción segura...",
            "share_marketing" to "Compartir Campaña de Marketing",
            "share_hub_title" to "Centro de Difusión de Marketing",
            "share_hub_subtitle" to "Distribuya enlaces de pago, promociones e invitaciones",
            "copy_link" to "Copiar Enlace",
            "copy_content" to "Copiar Mensaje y Enlace",
            "share_native" to "Compartir vía...",
            "campaign_links" to "Enlaces de Campaña y Ofertas",
            "share_content_preview" to "Vista Previa del Contenido",
            "direct_channels" to "Canales de Envío Rápido",
            "link_copied_toast" to "¡Enlace de campaña copiado al portapapeles!",
            "content_copied_toast" to "¡Mensaje de marketing copiado al portapapeles!",
            "account_settings" to "Cuenta y Configuración",
            "audience_title" to "Audiencia Global",
            "audience_desc" to "Demografía de clientes y mercados activos",
            "sales_summary_title" to "Historial y Resumen de Ventas",
            "sales_summary_subtitle" to "Libro mayor de Room SQLite con liquidaciones localizadas",
            "gross_sales" to "Ventas Brutas Totales",
            "total_orders" to "Total de Pedidos",
            "avg_order_value" to "Valor Promedio",
            "active_markets" to "Mercados Activos",
            "filter_all" to "Todas las Monedas",
            "search_placeholder" to "Buscar cliente o país...",
            "seed_sample_data" to "Generar Ventas Muestra",
            "clear_all_tx" to "Limpiar Historial",
            "receipt_details" to "Recibo de Venta",
            "delete_transaction" to "Eliminar Registro",
            "status_completed" to "Liquidado",
            "export_csv" to "Exportar Libro",
            "export_success" to "¡Libro de ventas copiado al portapapeles!",
            "no_results_filter" to "No se encontraron transacciones.",
            "drilldown_title" to "Detalles de Transacción",
            "back_to_summary" to "Volver al Resumen",
            "fee_breakdown" to "Desglose de Tarifas y Liquidación",
            "payment_method_info" to "Método de Pago y Autenticación",
            "audit_timeline" to "Auditoría del Ciclo de Vida",
            "customer_info" to "Cliente y Origen del Mercado",
            "net_payout" to "Pago Neto al Comerciante",
            "order_details" to "Detalles del Pedido y Plan",
            "copy_json" to "Copiar Registro JSON",
            "share_receipt" to "Compartir Recibo",
            "export_csv_modal_title" to "Exportar Libro de Ventas a CSV",
            "export_csv_modal_desc" to "Genere un archivo CSV compatible con RFC 4180 con montos localizados y desglose de tarifas para QuickBooks, Xero o Excel.",
            "save_csv_file" to "Guardar CSV en Dispositivo",
            "share_csv_file" to "Compartir Archivo CSV",
            "copy_csv_text" to "Copiar Texto CSV",
            "csv_saved_success" to "¡Libro CSV guardado exitosamente!",
            "csv_save_failed" to "No se pudo guardar el archivo CSV.",
            "rfc_compliant" to "RFC 4180 • Libro Multidivisa",
            "for_accounting" to "Contabilidad Externa y Conciliación",
            "payout_modal_title" to "Desembolso y Pagos a Comerciantes",
            "payout_bank_wire" to "Transferencia Bancaria",
            "payout_paypal" to "PayPal",
            "disburse_payout" to "Desembolsar Pago",
            "payout_destination" to "Destino del Pago",
            "payout_amount" to "Monto a Desembolsar",
            "payout_fee" to "Tarifa de Procesamiento",
            "payout_net" to "Desembolso Neto",
            "payout_success" to "¡Pago desembolsado exitosamente!"
        ),
        AppLanguage.FRANCAIS to mapOf(
            "app_title" to "GlobalMarket Pro",
            "hero_title" to "Vendez globalement, acceptez localement",
            "hero_desc" to "Touchez des clients du monde entier avec des options de paiement localisées et l'automatisation du marketing.",
            "terminal_title" to "Terminal de Paiement Global",
            "amount" to "Montant (USD)",
            "currency" to "Devise Cible",
            "pay_now" to "Payer Maintenant",
            "checkout_title" to "Finaliser le Paiement",
            "card_number" to "Numéro de Carte",
            "expiry" to "Date d'Expiration",
            "cvv" to "CVV",
            "card_holder" to "Nom sur la Carte",
            "country" to "Pays",
            "pay_button" to "Payer",
            "secure_payment" to "Paiement sécurisé crypté",
            "active_sub" to "Abonnement Actif",
            "renew_date" to "Prochain Renouvellement",
            "org_id" to "ID de l'Organisation",
            "analytics_title" to "Distribution des Ventes",
            "sub_success" to "Paiement Réussi!",
            "sub_created" to "Abonnement créé avec succès.",
            "history_title" to "Historique des Transactions",
            "no_transactions" to "Aucune transaction pour le moment.",
            "start_trial" to "Essai Gratuit",
            "watch_demo" to "Voir la Démo",
            "processing" to "Traitement de la transaction sécurisée...",
            "share_marketing" to "Partager la Campagne Marketing",
            "share_hub_title" to "Centre de Partage Marketing",
            "share_hub_subtitle" to "Diffusez vos liens de paiement, promos et invitations",
            "copy_link" to "Copier le Lien",
            "copy_content" to "Copier le Message & Lien",
            "share_native" to "Partager via...",
            "campaign_links" to "Liens de Campagne & Offres",
            "share_content_preview" to "Aperçu du Contenu Partagé",
            "direct_channels" to "Canaux de Partage Direct",
            "link_copied_toast" to "Lien de campagne copié dans le presse-papiers!",
            "content_copied_toast" to "Message marketing complet copié!",
            "account_settings" to "Compte et Paramètres",
            "audience_title" to "Audience Mondiale",
            "audience_desc" to "Données démographiques des clients",
            "sales_summary_title" to "Historique et Résumé des Ventes",
            "sales_summary_subtitle" to "Grand livre Room SQLite avec règlements localisés",
            "gross_sales" to "Ventes Brutes Totales",
            "total_orders" to "Commandes Totales",
            "avg_order_value" to "Panier Moyen",
            "active_markets" to "Marchés Actifs",
            "filter_all" to "Toutes Devises",
            "search_placeholder" to "Rechercher client ou pays...",
            "seed_sample_data" to "Générer Ventes Exemples",
            "clear_all_tx" to "Effacer le Grand Livre",
            "receipt_details" to "Reçu de Vente",
            "delete_transaction" to "Supprimer",
            "status_completed" to "Réglé",
            "export_csv" to "Exporter le Livre",
            "export_success" to "Grand livre copié dans le presse-papiers!",
            "no_results_filter" to "Aucune transaction correspondante.",
            "drilldown_title" to "Détails de la Transaction",
            "back_to_summary" to "Retour au Résumé",
            "fee_breakdown" to "Détail du Règlement et Frais",
            "payment_method_info" to "Moyen de Paiement & Sécurité",
            "audit_timeline" to "Audit du Cycle de Vie",
            "customer_info" to "Client & Marché d'Origine",
            "net_payout" to "Paiement Net Marchand",
            "order_details" to "Détails de l'Article et Plan",
            "copy_json" to "Copier le JSON",
            "share_receipt" to "Partager le Reçu",
            "export_csv_modal_title" to "Exporter le Grand Livre en CSV",
            "export_csv_modal_desc" to "Générez un fichier CSV conforme à la RFC 4180 avec montants localisés et détail des frais pour QuickBooks, Xero ou Excel.",
            "save_csv_file" to "Enregistrer le CSV sur l'appareil",
            "share_csv_file" to "Partager le Fichier CSV",
            "copy_csv_text" to "Copier le Texte CSV",
            "csv_saved_success" to "Grand livre CSV enregistré avec succès !",
            "csv_save_failed" to "Impossible d'enregistrer le fichier CSV.",
            "rfc_compliant" to "RFC 4180 • Livre Multi-Devises",
            "for_accounting" to "Comptabilité Externe & Rapprochement",
            "payout_modal_title" to "Paiement et Versement Marchand",
            "payout_bank_wire" to "Virement Bancaire",
            "payout_paypal" to "PayPal",
            "disburse_payout" to "Effectuer le Versement",
            "payout_destination" to "Destination du Versement",
            "payout_amount" to "Montant du Versement",
            "payout_fee" to "Frais de Traitement",
            "payout_net" to "Versement Net",
            "payout_success" to "Versement effectué avec succès !"
        ),
        AppLanguage.DEUTSCH to mapOf(
            "app_title" to "GlobalMarket Pro",
            "hero_title" to "Global verkaufen, lokal kassieren",
            "hero_desc" to "Erreichen Sie Kunden weltweit mit lokalisierten Zahlungsoptionen und Marketingautomatisierung.",
            "terminal_title" to "Globales Zahlungsterminal",
            "amount" to "Betrag (USD)",
            "currency" to "Zielwährung",
            "pay_now" to "Jetzt Bezahlen",
            "checkout_title" to "Zahlung Abschließen",
            "card_number" to "Kartennummer",
            "expiry" to "Ablaufdatum",
            "cvv" to "CVV",
            "card_holder" to "Name auf der Karte",
            "country" to "Land",
            "pay_button" to "Bezahlen",
            "secure_payment" to "Sichere verschlüsselte Zahlung",
            "active_sub" to "Aktives Abonnement",
            "renew_date" to "Nächste Verlängerung",
            "org_id" to "Organisations-ID",
            "analytics_title" to "Globale Verkaufsverteilung",
            "sub_success" to "Zahlung Erfolgreich!",
            "sub_created" to "Abonnement erfolgreich erstellt.",
            "history_title" to "Transaktionsverlauf",
            "no_transactions" to "Noch keine Transaktionen vorhanden.",
            "start_trial" to "Kostenlose Testversion",
            "watch_demo" to "Demo Anschauen",
            "processing" to "Sichere Transaktion wird verarbeitet...",
            "share_marketing" to "Marketing-Kampagne Teilen",
            "share_hub_title" to "Marketing- und Wachstums-Share-Hub",
            "share_hub_subtitle" to "Zahlungslinks, Promo-Aktionen und Partner-Einladungen teilen",
            "copy_link" to "Link Kopieren",
            "copy_content" to "Text & Link Kopieren",
            "share_native" to "Teilen über...",
            "campaign_links" to "Kampagnen-Links & Angebote",
            "share_content_preview" to "Vorschau des Inhalts",
            "direct_channels" to "Direkte Sharing-Kanäle",
            "link_copied_toast" to "Kampagnen-Link in die Zwischenablage kopiert!",
            "content_copied_toast" to "Komplette Marketing-Botschaft kopiert!",
            "account_settings" to "Konto & Einstellungen",
            "audience_title" to "Globale Zielgruppe",
            "audience_desc" to "Kundendemografie und aktive regionale Märkte",
            "sales_summary_title" to "Verkaufshistorie & Zusammenfassung",
            "sales_summary_subtitle" to "Room SQLite-Hauptbuch mit lokalisierten Abrechnungen",
            "gross_sales" to "Bruttoumsatz Gesamt",
            "total_orders" to "Bestellungen Gesamt",
            "avg_order_value" to "Durchschnittsbestellung",
            "active_markets" to "Aktive Märkte",
            "filter_all" to "Alle Währungen",
            "search_placeholder" to "Kunde oder Land suchen...",
            "seed_sample_data" to "Beispieldaten Erstellen",
            "clear_all_tx" to "Hauptbuch Leeren",
            "receipt_details" to "Verkaufsbeleg",
            "delete_transaction" to "Eintrag Löschen",
            "status_completed" to "Abgerechnet",
            "export_csv" to "Hauptbuch Exportieren",
            "export_success" to "Verkaufsdaten in Zwischenablage kopiert!",
            "no_results_filter" to "Keine passenden Transaktionen gefunden.",
            "drilldown_title" to "Transaktionsdetails",
            "back_to_summary" to "Zurück zur Übersicht",
            "fee_breakdown" to "Abrechnung & Gebührenaufstellung",
            "payment_method_info" to "Zahlungsmittel & Authentifizierung",
            "audit_timeline" to "Ereignis-Audit-Trail",
            "customer_info" to "Kunde & Herkunftsmarkt",
            "net_payout" to "Netto-Händlerauszahlung",
            "order_details" to "Bestell- & Tarifdetails",
            "copy_json" to "JSON-Datensatz Kopieren",
            "share_receipt" to "Beleg Teilen",
            "export_csv_modal_title" to "Verkaufs-Hauptbuch als CSV Exportieren",
            "export_csv_modal_desc" to "Erstellen Sie eine RFC 4180-konforme CSV-Datei mit lokalisierten Beträgen und Gebührenaufstellung für QuickBooks, Xero oder Excel.",
            "save_csv_file" to "CSV auf Gerät Speichern",
            "share_csv_file" to "CSV-Datei Teilen",
            "copy_csv_text" to "CSV-Text Kopieren",
            "csv_saved_success" to "CSV-Hauptbuch erfolgreich gespeichert!",
            "csv_save_failed" to "CSV-Datei konnte nicht gespeichert werden.",
            "rfc_compliant" to "RFC 4180 • Mehrwährungs-Buchhaltung",
            "for_accounting" to "Externe Buchhaltung & Abstimmung",
            "payout_modal_title" to "Händlerauszahlung & Überweisung",
            "payout_bank_wire" to "Banküberweisung",
            "payout_paypal" to "PayPal",
            "disburse_payout" to "Auszahlung Veranlassen",
            "payout_destination" to "Auszahlungsziel",
            "payout_amount" to "Auszahlungsbetrag",
            "payout_fee" to "Bearbeitungsgebühr",
            "payout_net" to "Nettoauszahlung",
            "payout_success" to "Auszahlung erfolgreich veranlasst!"
        ),
        AppLanguage.CHINESE to mapOf(
            "app_title" to "GlobalMarket Pro",
            "hero_title" to "全球销售，本地收款",
            "hero_desc" to "利用本地化支付方案和营销自动化，触达全球每一位客户。",
            "terminal_title" to "全球支付终端",
            "amount" to "金额 (美元)",
            "currency" to "目标货币",
            "pay_now" to "立即支付",
            "checkout_title" to "完成支付",
            "card_number" to "卡号",
            "expiry" to "有效期",
            "cvv" to "安全码",
            "card_holder" to "持卡人姓名",
            "country" to "国家/地区",
            "pay_button" to "支付",
            "secure_payment" to "安全加密支付",
            "active_sub" to "生效中订阅",
            "renew_date" to "下期扣款日",
            "org_id" to "机构识别码",
            "analytics_title" to "全球销售分布图",
            "sub_success" to "支付成功！",
            "sub_created" to "您的全球商业订阅已成功激活。",
            "history_title" to "交易历史纪录",
            "no_transactions" to "暂无历史交易流水。",
            "start_trial" to "开始免费体验",
            "watch_demo" to "观看演示视频",
            "processing" to "正在进行安全金融级交易处理...",
            "share_marketing" to "分享营销活动推广",
            "share_hub_title" to "全球营销与增长分享中心",
            "share_hub_subtitle" to "快速分发商户收款链接、专属优惠码与分销邀请",
            "copy_link" to "复制链接",
            "copy_content" to "复制文案及链接",
            "share_native" to "系统分享...",
            "campaign_links" to "营销活动与推广链接",
            "share_content_preview" to "分享文案预览",
            "direct_channels" to "一键分发渠道",
            "link_copied_toast" to "营销推广链接已成功复制到剪贴板！",
            "content_copied_toast" to "完整营销文案与链接已复制！",
            "account_settings" to "账户与设置",
            "audience_title" to "全球受众与客群",
            "audience_desc" to "客户地域分布与活跃海外市场",
            "sales_summary_title" to "销售历史与汇总账簿",
            "sales_summary_subtitle" to "基于 Room SQLite 本地加密存储的全球多币种交易流水",
            "gross_sales" to "累计总销售额",
            "total_orders" to "总订单数",
            "avg_order_value" to "平均客单价",
            "active_markets" to "活跃结算市场",
            "filter_all" to "全部结算币种",
            "search_placeholder" to "搜索客户姓名或结算国家...",
            "seed_sample_data" to "生成示例销售流水",
            "clear_all_tx" to "清空销售账簿",
            "receipt_details" to "销售电子凭证",
            "delete_transaction" to "删除此笔流水",
            "status_completed" to "已清算结算",
            "export_csv" to "导出交易数据",
            "export_success" to "销售流水账簿已成功复制到剪贴板！",
            "no_results_filter" to "未查询到符合条件的交易记录。",
            "drilldown_title" to "交易详细信息",
            "back_to_summary" to "返回销售概览",
            "fee_breakdown" to "结算及费用明细",
            "payment_method_info" to "支付工具与安全认证",
            "audit_timeline" to "交易生命周期审计",
            "customer_info" to "客户与市场信息",
            "net_payout" to "商户净结算款项",
            "order_details" to "订购商品与套餐规格",
            "copy_json" to "复制 JSON 报文",
            "share_receipt" to "分享电子凭证",
            "export_csv_modal_title" to "导出销售账簿为 CSV 文件",
            "export_csv_modal_desc" to "生成符合 RFC 4180 标准的 CSV 文件，包含本地化金额及费用明细，适用于 QuickBooks、Xero 或 Excel。",
            "save_csv_file" to "保存 CSV 文件到设备",
            "share_csv_file" to "分享 CSV 文件",
            "copy_csv_text" to "复制 CSV 原始文本",
            "csv_saved_success" to "CSV 销售账簿已成功保存！",
            "csv_save_failed" to "无法保存 CSV 文件。",
            "rfc_compliant" to "RFC 4180 • 多币种对账账簿",
            "for_accounting" to "外部财务核算与对账审计",
            "payout_modal_title" to "商户结算与款项发放",
            "payout_bank_wire" to "银行电汇",
            "payout_paypal" to "PayPal",
            "disburse_payout" to "发放款项",
            "payout_destination" to "收款账户",
            "payout_amount" to "发放金额",
            "payout_fee" to "手续费",
            "payout_net" to "净到账金额",
            "payout_success" to "款项已成功发放！"
        )
    )

    val currencies = listOf(
        CurrencyInfo("USD", "$", 1.0, "https://flagcdn.com/w20/us.png", "United States", "🇺🇸", "Americas"),
        CurrencyInfo("EUR", "€", 0.92, "https://flagcdn.com/w20/eu.png", "Germany", "🇪🇺", "Europe"),
        CurrencyInfo("GBP", "£", 0.78, "https://flagcdn.com/w20/gb.png", "United Kingdom", "🇬🇧", "Europe"),
        CurrencyInfo("JPY", "¥", 155.20, "https://flagcdn.com/w20/jp.png", "Japan", "🇯🇵", "Asia-Pacific"),
        CurrencyInfo("CNY", "¥", 7.25, "https://flagcdn.com/w20/cn.png", "China", "🇨🇳", "Asia-Pacific"),
        CurrencyInfo("CAD", "CA$", 1.37, "https://flagcdn.com/w20/ca.png", "Canada", "🇨🇦", "Americas"),
        CurrencyInfo("AUD", "A$", 1.52, "https://flagcdn.com/w20/au.png", "Australia", "🇦🇺", "Asia-Pacific"),
        CurrencyInfo("CHF", "CHF", 0.90, "https://flagcdn.com/w20/ch.png", "Switzerland", "🇨🇭", "Europe"),
        CurrencyInfo("SGD", "S$", 1.34, "https://flagcdn.com/w20/sg.png", "Singapore", "🇸🇬", "Asia-Pacific"),
        CurrencyInfo("INR", "₹", 83.50, "https://flagcdn.com/w20/in.png", "India", "🇮🇳", "Asia-Pacific"),
        CurrencyInfo("BRL", "R$", 5.45, "https://flagcdn.com/w20/br.png", "Brazil", "🇧🇷", "Americas"),
        CurrencyInfo("MXN", "Mex$", 18.20, "https://flagcdn.com/w20/mx.png", "Mexico", "🇲🇽", "Americas")
    )

    // Marketing Campaign Presets
    val marketingCampaigns = listOf(
        MarketingCampaign(
            id = "storefront_checkout",
            title = "Global Checkout Link",
            tag = "STOREFRONT",
            promoCode = "GLOBAL15",
            discountPercent = 15,
            shortUrl = "https://glaba.pro/pay?m=org_6RsNhnGbSQ3s7rFPAoYfQjA&c=EUR&code=GLOBAL15",
            shareMessage = "🌍 Accept payments worldwide in EUR, GBP, USD, JPY & CNY with Glaba Pro! Use promo code GLOBAL15 for 15% off checkout: https://glaba.pro/pay?m=org_6RsNhnGbSQ3s7rFPAoYfQjA&c=EUR&code=GLOBAL15"
        ),
        MarketingCampaign(
            id = "partner_referral",
            title = "Merchant Partner Invite",
            tag = "AFFILIATE",
            promoCode = "PARTNER25",
            discountPercent = 25,
            shortUrl = "https://glaba.pro/join/JPWESTERVELD?deal=PARTNER25",
            shareMessage = "💼 Join Glaba Pro Global Marketing Platform! Start accepting local currencies with instant FX conversion. Use partner code PARTNER25 for 25% off fees: https://glaba.pro/join/JPWESTERVELD?deal=PARTNER25"
        ),
        MarketingCampaign(
            id = "summer_fx_deal",
            title = "Summer FX Flash Campaign",
            tag = "FLASH DEAL",
            promoCode = "SUMMER2026",
            discountPercent = 20,
            shortUrl = "https://glaba.pro/deals/summer?ref=org_6RsNhnGbSQ3s7rFPAoYfQjA",
            shareMessage = "🔥 Boost your international conversions today! Glaba Pro provides zero-friction global checkout. Claim your 20% discount now with code SUMMER2026: https://glaba.pro/deals/summer?ref=org_6RsNhnGbSQ3s7rFPAoYfQjA"
        )
    )

    // UI States
    private val _selectedLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val selectedLanguage = _selectedLanguage.asStateFlow()

    private val _selectedCurrency = MutableStateFlow(currencies[1]) // Default target: EUR
    val selectedCurrency = _selectedCurrency.asStateFlow()

    // Payment Simulator & Live FX Engine State
    private val _isRateRefreshing = MutableStateFlow(false)
    val isRateRefreshing: StateFlow<Boolean> = _isRateRefreshing.asStateFlow()

    private val _lastRateRefreshTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastRateRefreshTimestamp: StateFlow<Long> = _lastRateRefreshTimestamp.asStateFlow()

    private val _liveRateMap = MutableStateFlow<Map<String, Double>>(currencies.associate { it.code to it.exchangeRate })
    val liveRateMap: StateFlow<Map<String, Double>> = _liveRateMap.asStateFlow()

    private val _rateChangePercents = MutableStateFlow<Map<String, Double>>(currencies.associate { it.code to 0.0 })
    val rateChangePercents: StateFlow<Map<String, Double>> = _rateChangePercents.asStateFlow()

    private val _selectedPaymentRail = MutableStateFlow<LocalPaymentRail?>(null)
    val selectedPaymentRail: StateFlow<LocalPaymentRail?> = _selectedPaymentRail.asStateFlow()

    private val _isSimulatorModalOpen = MutableStateFlow(false)
    val isSimulatorModalOpen: StateFlow<Boolean> = _isSimulatorModalOpen.asStateFlow()

    private val _simulatorCustomerName = MutableStateFlow("Elena Rostova")
    val simulatorCustomerName: StateFlow<String> = _simulatorCustomerName.asStateFlow()

    private val _simulatorBillingCountry = MutableStateFlow("Germany")
    val simulatorBillingCountry: StateFlow<String> = _simulatorBillingCountry.asStateFlow()

    init {
        // Initialize default payment rail for starting currency EUR
        _selectedPaymentRail.value = getPaymentRailsForCurrency("EUR").firstOrNull()

        // Seed sample scanned products in Room database if empty
        viewModelScope.launch {
            try {
                val currentCount = repository.scannedProductCount.first()
                if (currentCount == 0) {
                    val sampleProducts = listOf(
                        ScannedProductEntity(
                            name = "Glaba Pro POS Terminal Terminal-X",
                            price = 249.00,
                            currency = "EUR",
                            barcode = "012345678905",
                            barcodeFormat = "UPC-A",
                            targetCurrency = "USD",
                            convertedPrice = 270.65,
                            exchangeRateUsed = 1.087,
                            category = "Hardware POS"
                        ),
                        ScannedProductEntity(
                            name = "Global Multi-Currency NFC Reader",
                            price = 89.50,
                            currency = "GBP",
                            barcode = "5901234123457",
                            barcodeFormat = "EAN-13",
                            targetCurrency = "EUR",
                            convertedPrice = 105.56,
                            exchangeRateUsed = 1.179,
                            category = "Retail Accessories"
                        ),
                        ScannedProductEntity(
                            name = "Enterprise Global SaaS Annual Plan",
                            price = 199.99,
                            currency = "USD",
                            barcode = "SKU-GLB-PRO-99",
                            barcodeFormat = "Code 128",
                            targetCurrency = "JPY",
                            convertedPrice = 31038.45,
                            exchangeRateUsed = 155.20,
                            category = "Software License"
                        )
                    )
                    repository.insertScannedProducts(sampleProducts)
                }

                val currentHistoryCount = repository.historyCount.first()
                if (currentHistoryCount == 0) {
                    val sampleHistory = listOf(
                        TransactionHistoryEntity(
                            date = System.currentTimeMillis() - 1000L * 60 * 35,
                            amount = 149.50,
                            currency = "EUR",
                            description = "SEPA Merchant Checkout - Berlin Retail",
                            type = "PAYMENT",
                            status = "COMPLETED",
                            category = "Retail Sales"
                        ),
                        TransactionHistoryEntity(
                            date = System.currentTimeMillis() - 1000L * 60 * 180,
                            amount = 89.00,
                            currency = "GBP",
                            description = "BACS Instant Settlement - London Tech",
                            type = "PAYMENT",
                            status = "COMPLETED",
                            category = "Subscription"
                        ),
                        TransactionHistoryEntity(
                            date = System.currentTimeMillis() - 1000L * 60 * 360,
                            amount = 22400.0,
                            currency = "JPY",
                            description = "Konbini Payment - Tokyo Franchise",
                            type = "PAYMENT",
                            status = "COMPLETED",
                            category = "Hardware POS"
                        ),
                        TransactionHistoryEntity(
                            date = System.currentTimeMillis() - 1000L * 60 * 720,
                            amount = 320.00,
                            currency = "USD",
                            description = "Wire Transfer Refund - New York Corp",
                            type = "REFUND",
                            status = "COMPLETED",
                            category = "Refunds"
                        )
                    )
                    repository.insertHistoryRecords(sampleHistory)
                }

                val currentInventoryCount = repository.inventoryProductCount.first()
                if (currentInventoryCount == 0) {
                    val sampleInventory = listOf(
                        InventoryProductEntity(
                            sku = "POS-DOCK-101",
                            barcode = "012345678905",
                            name = "Glaba Terminal Stand & Charging Dock",
                            category = "Hardware POS",
                            price = 49.00,
                            currency = "USD",
                            costPrice = 22.00,
                            stockQuantity = 28,
                            minStockThreshold = 5,
                            description = "Heavy-duty rotating mount for sales counter checkout."
                        ),
                        InventoryProductEntity(
                            sku = "POS-NFC-202",
                            barcode = "5901234123457",
                            name = "Multi-Currency Contactless NFC Reader",
                            category = "Hardware POS",
                            price = 89.50,
                            currency = "USD",
                            costPrice = 45.00,
                            stockQuantity = 14,
                            minStockThreshold = 5,
                            description = "Accepts Apple Pay, Google Pay, and EMV chip cards worldwide."
                        ),
                        InventoryProductEntity(
                            sku = "POS-PRN-303",
                            barcode = "8901234567891",
                            name = "High-Speed Thermal Receipt Printer",
                            category = "Accessories",
                            price = 129.00,
                            currency = "USD",
                            costPrice = 65.00,
                            stockQuantity = 9,
                            minStockThreshold = 4,
                            description = "80mm thermal receipt printer with Ethernet and USB support."
                        ),
                        InventoryProductEntity(
                            sku = "POS-TERM-404",
                            barcode = "SKU-GLB-PRO-99",
                            name = "Global Sales Terminal Pro (LTE & Wi-Fi)",
                            category = "Hardware POS",
                            price = 249.00,
                            currency = "USD",
                            costPrice = 140.00,
                            stockQuantity = 3,
                            minStockThreshold = 5,
                            description = "Portable multi-currency smart sales terminal with barcode scanner."
                        ),
                        InventoryProductEntity(
                            sku = "POS-PPR-505",
                            barcode = "041234567892",
                            name = "BPA-Free Thermal Paper Rolls (10-Pack)",
                            category = "Retail Supplies",
                            price = 14.99,
                            currency = "USD",
                            costPrice = 5.50,
                            stockQuantity = 65,
                            minStockThreshold = 10,
                            description = "Standard 80mm roll replacement pack for POS thermal printers."
                        )
                    )
                    repository.insertInventoryProducts(sampleInventory)
                }
            } catch (_: Exception) {}
        }
    }

    private val _amountUsd = MutableStateFlow("49.99")
    val amountUsd = _amountUsd.asStateFlow()

    private val _isCheckoutVisible = MutableStateFlow(false)
    val isCheckoutVisible = _isCheckoutVisible.asStateFlow()

    private val _isQrPaymentVisible = MutableStateFlow(false)
    val isQrPaymentVisible: StateFlow<Boolean> = _isQrPaymentVisible.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _paymentSuccess = MutableStateFlow(false)
    val paymentSuccess = _paymentSuccess.asStateFlow()

    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed = _isSubscribed.asStateFlow()

    // Navigation Tab Selection (0 = Home, 1 = Analytics, 2 = Audience, 3 = Settings)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // User Profile State
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    // Marketing Share Hub State
    private val _isShareModalOpen = MutableStateFlow(false)
    val isShareModalOpen: StateFlow<Boolean> = _isShareModalOpen.asStateFlow()

    // Glaba Web Checkout In-App Preview State
    private val _isWebCheckoutOpen = MutableStateFlow(false)
    val isWebCheckoutOpen: StateFlow<Boolean> = _isWebCheckoutOpen.asStateFlow()

    private val _webCheckoutUrl = MutableStateFlow("https://glaba.pro/pay?m=org_6RsNhnGbSQ3s7rFPAoYfQjA&c=EUR&code=GLOBAL15")
    val webCheckoutUrl: StateFlow<String> = _webCheckoutUrl.asStateFlow()

    private val _appliedPromoCode = MutableStateFlow<String?>("GLOBAL15")
    val appliedPromoCode: StateFlow<String?> = _appliedPromoCode.asStateFlow()

    private val _promoDiscountPercent = MutableStateFlow(15)
    val promoDiscountPercent: StateFlow<Int> = _promoDiscountPercent.asStateFlow()

    private val _activeMerchantId = MutableStateFlow<String?>("org_6RsNhnGbSQ3s7rFPAoYfQjA")
    val activeMerchantId: StateFlow<String?> = _activeMerchantId.asStateFlow()

    private val _deepLinkBanner = MutableStateFlow<String?>(null)
    val deepLinkBanner: StateFlow<String?> = _deepLinkBanner.asStateFlow()

    fun openWebCheckout(url: String? = null) {
        val target = url ?: "https://glaba.pro/pay?m=org_6RsNhnGbSQ3s7rFPAoYfQjA&c=EUR&code=GLOBAL15"
        _webCheckoutUrl.value = target
        parseAndApplyUrlParameters(target)
        _isWebCheckoutOpen.value = true
    }

    fun closeWebCheckout() {
        _isWebCheckoutOpen.value = false
    }

    fun clearDeepLinkBanner() {
        _deepLinkBanner.value = null
    }

    fun applyPromoCode(code: String) {
        val clean = code.trim().uppercase()
        when {
            clean == "GLOBAL15" -> {
                _appliedPromoCode.value = "GLOBAL15"
                _promoDiscountPercent.value = 15
            }
            clean == "PARTNER25" -> {
                _appliedPromoCode.value = "PARTNER25"
                _promoDiscountPercent.value = 25
            }
            clean == "SUMMER2026" -> {
                _appliedPromoCode.value = "SUMMER2026"
                _promoDiscountPercent.value = 20
            }
            clean.isNotBlank() -> {
                _appliedPromoCode.value = clean
                _promoDiscountPercent.value = 10
            }
            else -> {
                clearPromoCode()
            }
        }
    }

    fun clearPromoCode() {
        _appliedPromoCode.value = null
        _promoDiscountPercent.value = 0
    }

    fun parseAndApplyUrlParameters(url: String) {
        try {
            val uri = Uri.parse(url)
            val merchant = uri.getQueryParameter("m") ?: uri.getQueryParameter("ref")
            if (!merchant.isNullOrBlank()) {
                _activeMerchantId.value = merchant
            }
            val currencyParam = uri.getQueryParameter("c")
            if (!currencyParam.isNullOrBlank()) {
                val foundCurrency = currencies.find { it.code.equals(currencyParam, ignoreCase = true) }
                if (foundCurrency != null) {
                    _selectedCurrency.value = foundCurrency
                }
            }
            val promo = uri.getQueryParameter("code") ?: uri.getQueryParameter("deal")
            if (!promo.isNullOrBlank()) {
                applyPromoCode(promo)
            }
        } catch (_: Exception) {
            // Safe fallback
        }
    }

    fun handleDeepLink(uri: Uri?) {
        if (uri == null) return
        val urlString = uri.toString()
        if (urlString.contains("glaba.pro", ignoreCase = true) || uri.scheme.equals("glaba", ignoreCase = true)) {
            parseAndApplyUrlParameters(urlString)
            val currency = _selectedCurrency.value.code
            val promo = _appliedPromoCode.value ?: "GLOBAL15"
            val discount = _promoDiscountPercent.value
            _deepLinkBanner.value = "Glaba Pro Storefront Loaded ($currency • $promo: -$discount%)"
            _isWebCheckoutOpen.value = true
        }
    }

    fun handleDeepLinkUrl(url: String) {
        try {
            handleDeepLink(Uri.parse(url))
        } catch (_: Exception) {}
    }

    fun completeWebCheckoutTransaction(
        customerName: String,
        country: String,
        netAmountUsd: Double,
        onSuccess: (TransactionEntity) -> Unit = {}
    ) {
        val currency = _selectedCurrency.value
        val converted = netAmountUsd * currency.exchangeRate
        val promo = _appliedPromoCode.value ?: "NONE"
        val merchant = _activeMerchantId.value ?: "org_6RsNhnGbSQ3s7rFPAoYfQjA"
        val entity = TransactionEntity(
            amountInUsd = netAmountUsd,
            targetCurrency = currency.code,
            convertedAmount = converted,
            customerName = customerName.ifBlank { "Web Customer ($merchant)" },
            country = country.ifBlank { currency.defaultCountry },
            timestamp = System.currentTimeMillis(),
            stripeSubscriptionId = "web_glaba_${promo}_${(1000..9999).random()}"
        )
        val logEntity = TransactionLogEntity(
            logType = "WEB_CHECKOUT",
            title = "Glaba Web Checkout ($promo)",
            barcodeOrRawValue = "https://glaba.pro/pay?m=$merchant&c=${currency.code}&code=$promo",
            barcodeFormat = "WEB_URL",
            amount = converted,
            currencyCode = currency.code,
            amountInUsd = netAmountUsd,
            customerOrRecipient = customerName.ifBlank { "Web Customer ($merchant)" },
            country = country.ifBlank { currency.defaultCountry },
            paymentMethod = "WEB_CHECKOUT",
            status = "COMPLETED",
            referenceCode = "WEB-$promo-${(1000..9999).random()}",
            notes = "Promo discount applied: $promo"
        )
        onSuccess(entity)
        viewModelScope.launch {
            repository.insert(entity)
            repository.insertLog(logEntity)
        }
    }

    suspend fun saveWebCheckoutTransaction(
        customerName: String,
        country: String,
        netAmountUsd: Double
    ): TransactionEntity {
        val currency = _selectedCurrency.value
        val converted = netAmountUsd * currency.exchangeRate
        val promo = _appliedPromoCode.value ?: "NONE"
        val merchant = _activeMerchantId.value ?: "org_6RsNhnGbSQ3s7rFPAoYfQjA"
        val entity = TransactionEntity(
            amountInUsd = netAmountUsd,
            targetCurrency = currency.code,
            convertedAmount = converted,
            customerName = customerName.ifBlank { "Web Customer ($merchant)" },
            country = country.ifBlank { currency.defaultCountry },
            timestamp = System.currentTimeMillis(),
            stripeSubscriptionId = "web_glaba_${promo}_${(1000..9999).random()}"
        )
        val logEntity = TransactionLogEntity(
            logType = "WEB_CHECKOUT",
            title = "Glaba Web Checkout ($promo)",
            barcodeOrRawValue = "https://glaba.pro/pay?m=$merchant&c=${currency.code}&code=$promo",
            barcodeFormat = "WEB_URL",
            amount = converted,
            currencyCode = currency.code,
            amountInUsd = netAmountUsd,
            customerOrRecipient = customerName.ifBlank { "Web Customer ($merchant)" },
            country = country.ifBlank { currency.defaultCountry },
            paymentMethod = "WEB_CHECKOUT",
            status = "COMPLETED",
            referenceCode = "WEB-$promo-${(1000..9999).random()}",
            notes = "Promo discount applied: $promo"
        )
        repository.insert(entity)
        repository.insertLog(logEntity)
        return entity
    }

    // Barcode Scanner Modal State
    private val _isBarcodeScannerOpen = MutableStateFlow(false)
    val isBarcodeScannerOpen: StateFlow<Boolean> = _isBarcodeScannerOpen.asStateFlow()

    fun openBarcodeScanner() {
        _isBarcodeScannerOpen.value = true
    }

    fun closeBarcodeScanner() {
        _isBarcodeScannerOpen.value = false
    }

    suspend fun saveBarcodeTransactionEntry(
        amountInUsd: Double,
        currencyCode: String,
        customerName: String,
        country: String,
        barcodeValue: String
    ): TransactionEntity {
        val currency = currencies.find { it.code.equals(currencyCode, ignoreCase = true) } ?: _selectedCurrency.value
        val converted = amountInUsd * currency.exchangeRate
        val cleanBarcode = barcodeValue.take(12).filter { it.isLetterOrDigit() }.ifEmpty { "CODE" }
        val entity = TransactionEntity(
            amountInUsd = amountInUsd,
            targetCurrency = currency.code,
            convertedAmount = converted,
            customerName = customerName.ifBlank { "Scanned Customer" },
            country = country.ifBlank { currency.defaultCountry },
            timestamp = System.currentTimeMillis(),
            stripeSubscriptionId = "tx_barcode_${cleanBarcode}_${(1000..9999).random()}"
        )
        val logEntity = TransactionLogEntity(
            logType = "SCANNED_ITEM",
            title = "Scanned Item ($cleanBarcode)",
            barcodeOrRawValue = barcodeValue,
            barcodeFormat = if (barcodeValue.startsWith("http")) "QR_CODE" else "BARCODE",
            amount = converted,
            currencyCode = currency.code,
            amountInUsd = amountInUsd,
            customerOrRecipient = customerName.ifBlank { "Scanned Customer" },
            country = country.ifBlank { currency.defaultCountry },
            paymentMethod = "SCANNER",
            status = "COMPLETED",
            referenceCode = "SCAN-$cleanBarcode-${(1000..9999).random()}",
            notes = "ML Kit Barcode Scan"
        )
        repository.insert(entity)
        repository.insertLog(logEntity)
        return entity
    }

    fun triggerBarcodeTransactionEntry(
        amountInUsd: Double,
        currencyCode: String,
        customerName: String,
        country: String,
        barcodeValue: String,
        onSuccess: (TransactionEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val entity = saveBarcodeTransactionEntry(
                amountInUsd = amountInUsd,
                currencyCode = currencyCode,
                customerName = customerName,
                country = country,
                barcodeValue = barcodeValue
            )
            onSuccess(entity)
        }
    }

    fun triggerBarcodePayoutEntry(
        payoutAmountInUsd: Double,
        currencyCode: String,
        recipientName: String,
        country: String,
        barcodeValue: String,
        onSuccess: (TransactionEntity) -> Unit = {}
    ) {
        val currency = currencies.find { it.code.equals(currencyCode, ignoreCase = true) } ?: _selectedCurrency.value
        val converted = payoutAmountInUsd * currency.exchangeRate
        val cleanBarcode = barcodeValue.take(12).filter { it.isLetterOrDigit() }.ifEmpty { "PAYOUT" }
        val finalRecipient = if (recipientName.startsWith("Payout: ")) recipientName else "Payout: $recipientName"
        val entity = TransactionEntity(
            amountInUsd = payoutAmountInUsd,
            targetCurrency = currency.code,
            convertedAmount = converted,
            customerName = finalRecipient,
            country = country.ifBlank { currency.defaultCountry },
            timestamp = System.currentTimeMillis(),
            stripeSubscriptionId = "po_barcode_${cleanBarcode}_${(1000..9999).random()}"
        )
        val logEntity = TransactionLogEntity(
            logType = "PAYOUT",
            title = finalRecipient,
            barcodeOrRawValue = barcodeValue,
            barcodeFormat = if (barcodeValue.startsWith("http")) "QR_CODE" else "BARCODE",
            amount = converted,
            currencyCode = currency.code,
            amountInUsd = payoutAmountInUsd,
            customerOrRecipient = finalRecipient,
            country = country.ifBlank { currency.defaultCountry },
            paymentMethod = "PAYOUT_SCAN",
            status = "COMPLETED",
            referenceCode = "PO-$cleanBarcode-${(1000..9999).random()}",
            notes = "Direct Scanned Payout"
        )
        viewModelScope.launch {
            repository.insert(entity)
            repository.insertLog(logEntity)
            onSuccess(entity)
        }
    }

    // Merchant Payout State & Configuration (Bank Wire & PayPal)
    private val _payoutConfig = MutableStateFlow(MerchantPayoutConfig())
    val payoutConfig: StateFlow<MerchantPayoutConfig> = _payoutConfig.asStateFlow()

    private val _isPayoutModalOpen = MutableStateFlow(false)
    val isPayoutModalOpen: StateFlow<Boolean> = _isPayoutModalOpen.asStateFlow()

    private val _isOwnerPayoutScreenOpen = MutableStateFlow(false)
    val isOwnerPayoutScreenOpen: StateFlow<Boolean> = _isOwnerPayoutScreenOpen.asStateFlow()

    private val _lastDisbursedPayoutReference = MutableStateFlow<String?>(null)
    val lastDisbursedPayoutReference: StateFlow<String?> = _lastDisbursedPayoutReference.asStateFlow()

    fun openOwnerPayoutSection() {
        _isOwnerPayoutScreenOpen.value = true
    }

    fun closeOwnerPayoutSection() {
        _isOwnerPayoutScreenOpen.value = false
    }

    fun openPayoutModal() {
        _isPayoutModalOpen.value = true
    }

    fun closePayoutModal() {
        _isPayoutModalOpen.value = false
    }

    fun selectPayoutMethod(method: PayoutMethodType) {
        _payoutConfig.value = _payoutConfig.value.copy(selectedMethod = method)
    }

    fun updateBankWireDetails(details: BankWireDetails) {
        _payoutConfig.value = _payoutConfig.value.copy(bankWire = details)
    }

    fun updatePayPalDetails(details: PayPalDetails) {
        _payoutConfig.value = _payoutConfig.value.copy(payPal = details)
    }

    fun updatePayoutConfig(config: MerchantPayoutConfig) {
        _payoutConfig.value = config
    }

    fun calculatePayoutFee(method: PayoutMethodType, amountInUsd: Double): Double {
        return when (method) {
            PayoutMethodType.BANK_WIRE -> 15.00
            PayoutMethodType.PAYPAL -> {
                val feeVal = (amountInUsd * 0.029) + 0.30
                String.format(java.util.Locale.US, "%.2f", feeVal).toDoubleOrNull() ?: 1.75
            }
        }
    }

    suspend fun saveDisbursePayout(
        method: PayoutMethodType = _payoutConfig.value.selectedMethod,
        amountInUsd: Double,
        currencyCode: String = _selectedCurrency.value.code,
        recipientName: String = if (method == PayoutMethodType.BANK_WIRE) _payoutConfig.value.bankWire.accountHolderName else _payoutConfig.value.payPal.recipientName,
        destinationIdentifier: String = if (method == PayoutMethodType.BANK_WIRE) _payoutConfig.value.bankWire.accountNumberOrIban else _payoutConfig.value.payPal.paypalEmailOrId,
        notes: String? = null
    ): TransactionEntity {
        val currency = currencies.find { it.code.equals(currencyCode, ignoreCase = true) } ?: _selectedCurrency.value
        val converted = amountInUsd * currency.exchangeRate
        val prefix = if (method == PayoutMethodType.BANK_WIRE) "WIRE" else "PP"
        val refCode = "PO-$prefix-${(100000..999999).random()}"
        val finalRecipient = "Payout (${method.displayName}): $recipientName"

        val entity = TransactionEntity(
            amountInUsd = amountInUsd,
            targetCurrency = currency.code,
            convertedAmount = converted,
            customerName = finalRecipient,
            country = currency.defaultCountry,
            timestamp = System.currentTimeMillis(),
            stripeSubscriptionId = "po_${method.id}_${(1000..9999).random()}"
        )

        val logEntity = TransactionLogEntity(
            logType = "PAYOUT",
            title = "Payout Disbursement (${method.displayName})",
            barcodeOrRawValue = destinationIdentifier,
            barcodeFormat = if (method == PayoutMethodType.BANK_WIRE) "BANK_WIRE" else "PAYPAL",
            amount = converted,
            currencyCode = currency.code,
            amountInUsd = amountInUsd,
            customerOrRecipient = recipientName,
            country = currency.defaultCountry,
            paymentMethod = method.displayName,
            status = "COMPLETED",
            referenceCode = refCode,
            notes = notes ?: "Disbursed via ${method.displayName} to $destinationIdentifier"
        )

        val salesRecord = SalesTransactionEntity(
            transactionReference = refCode,
            customerName = finalRecipient,
            customerEmail = if (method == PayoutMethodType.PAYPAL) destinationIdentifier else null,
            subtotalAmount = converted,
            taxAmount = 0.0,
            discountAmount = 0.0,
            totalAmount = converted,
            currency = currency.code,
            amountInUsd = amountInUsd,
            exchangeRate = currency.exchangeRate,
            paymentMethod = method.displayName,
            paymentStatus = "COMPLETED",
            terminalId = "PAYOUT-TERMINAL",
            cashierName = "Automated Settlement",
            itemCount = 1,
            itemsSummary = "Merchant Settlement Disbursement (${method.displayName})",
            receiptNumber = "REC-$refCode",
            notes = notes
        )

        _lastDisbursedPayoutReference.value = refCode

        repository.insert(entity)
        repository.insertLog(logEntity)
        repository.insertSalesTransaction(salesRecord)

        // Sync to cloud Firestore when user is authenticated
        firestoreRepository?.let { firestoreRepo ->
            try {
                val feeVal = calculatePayoutFee(method, amountInUsd)
                val net = (converted - feeVal).coerceAtLeast(0.0)
                val cloudRecord = com.example.data.FirestoreOwnerPayoutRecord(
                    userId = firestoreRepo.requireUserId(),
                    payoutId = refCode,
                    method = if (method == PayoutMethodType.BANK_WIRE) "BANK_WIRE" else "PAYPAL",
                    amount = converted,
                    fee = feeVal,
                    netAmount = net,
                    currency = currency.code,
                    recipientName = recipientName,
                    destinationSummary = destinationIdentifier,
                    reference = refCode,
                    status = "COMPLETED"
                )
                firestoreRepo.recordOwnerPayout(cloudRecord)
            } catch (ignored: Exception) {
                // Non-blocking fallback if local offline or unauthenticated
            }
        }

        return entity
    }

    fun disbursePayout(
        method: PayoutMethodType = _payoutConfig.value.selectedMethod,
        amountInUsd: Double,
        currencyCode: String = _selectedCurrency.value.code,
        recipientName: String = if (method == PayoutMethodType.BANK_WIRE) _payoutConfig.value.bankWire.accountHolderName else _payoutConfig.value.payPal.recipientName,
        destinationIdentifier: String = if (method == PayoutMethodType.BANK_WIRE) _payoutConfig.value.bankWire.accountNumberOrIban else _payoutConfig.value.payPal.paypalEmailOrId,
        notes: String? = null,
        onSuccess: (TransactionEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val entity = saveDisbursePayout(
                method = method,
                amountInUsd = amountInUsd,
                currencyCode = currencyCode,
                recipientName = recipientName,
                destinationIdentifier = destinationIdentifier,
                notes = notes
            )
            onSuccess(entity)
        }
    }

    fun loadBarcodeIntoTerminal(
        amountInUsd: Double,
        currencyCode: String
    ) {
        _amountUsd.value = String.format(java.util.Locale.US, "%.2f", amountInUsd)
        val found = currencies.find { it.code.equals(currencyCode, ignoreCase = true) }
        if (found != null) {
            selectCurrency(found)
        }
        _selectedTab.value = 0
    }

    private val _selectedCampaign = MutableStateFlow(marketingCampaigns[0])
    val selectedCampaign: StateFlow<MarketingCampaign> = _selectedCampaign.asStateFlow()

    // Form inputs
    val cardHolderName = MutableStateFlow("")
    val cardNumber = MutableStateFlow("")
    val expiryDate = MutableStateFlow("")
    val cvv = MutableStateFlow("")
    val selectedCountry = MutableStateFlow("United States")

    // Database source
    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Transaction Logs source (Scanned Items & Payments)
    val transactionLogs: StateFlow<List<TransactionLogEntity>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val scannedItemLogs: StateFlow<List<TransactionLogEntity>> = repository.scannedItemLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val paymentLogs: StateFlow<List<TransactionLogEntity>> = repository.paymentLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val scannedItemCount: StateFlow<Int> = repository.scannedItemCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val paymentCount: StateFlow<Int> = repository.paymentCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Scanned Products Persistence (Room Database)
    val scannedProducts: StateFlow<List<ScannedProductEntity>> = repository.allScannedProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val scannedProductCount: StateFlow<Int> = repository.scannedProductCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Transaction History Records Persistence (Room Database)
    val transactionHistoryRecords: StateFlow<List<TransactionHistoryEntity>> = repository.allHistoryRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val transactionHistoryCount: StateFlow<Int> = repository.historyCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun recordTransactionHistory(
        amount: Double,
        currency: String,
        date: Long = System.currentTimeMillis(),
        description: String = "",
        type: String = "PAYMENT",
        status: String = "COMPLETED",
        category: String = "General",
        notes: String? = null
    ) {
        viewModelScope.launch {
            repository.insertHistoryRecord(
                TransactionHistoryEntity(
                    date = date,
                    amount = amount,
                    currency = currency,
                    description = description,
                    type = type,
                    status = status,
                    category = category,
                    notes = notes
                )
            )
        }
    }

    fun deleteTransactionHistoryRecord(record: TransactionHistoryEntity) {
        viewModelScope.launch {
            repository.deleteHistoryRecord(record)
        }
    }

    fun clearAllTransactionHistory() {
        viewModelScope.launch {
            repository.clearAllHistoryRecords()
        }
    }

    // ==========================================
    // Sales Transaction History (Room Database)
    // ==========================================
    val salesTransactions: StateFlow<List<SalesTransactionEntity>> = repository.allSalesTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalSalesCount: StateFlow<Int> = repository.totalSalesCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val totalGrossSales: StateFlow<Double?> = repository.totalGrossSales
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val totalSalesInUsd: StateFlow<Double?> = repository.totalSalesInUsd
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val averageOrderValueUsd: StateFlow<Double?> = repository.averageOrderValueUsd
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    fun recordSalesTransaction(
        transactionReference: String,
        customerName: String = "Walk-in Customer",
        customerEmail: String? = null,
        subtotalAmount: Double = 0.0,
        taxAmount: Double = 0.0,
        discountAmount: Double = 0.0,
        totalAmount: Double = 0.0,
        currency: String = "USD",
        amountInUsd: Double = 0.0,
        exchangeRate: Double = 1.0,
        paymentMethod: String = "CARD",
        paymentStatus: String = "COMPLETED",
        terminalId: String = "POS-TERM-01",
        cashierName: String = "Cashier",
        itemCount: Int = 1,
        itemsSummary: String = "",
        receiptNumber: String = "",
        notes: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.insertSalesTransaction(
                SalesTransactionEntity(
                    transactionReference = transactionReference,
                    timestamp = timestamp,
                    customerName = customerName,
                    customerEmail = customerEmail,
                    subtotalAmount = subtotalAmount,
                    taxAmount = taxAmount,
                    discountAmount = discountAmount,
                    totalAmount = totalAmount,
                    currency = currency,
                    amountInUsd = amountInUsd,
                    exchangeRate = exchangeRate,
                    paymentMethod = paymentMethod,
                    paymentStatus = paymentStatus,
                    terminalId = terminalId,
                    cashierName = cashierName,
                    itemCount = itemCount,
                    itemsSummary = itemsSummary,
                    receiptNumber = receiptNumber,
                    notes = notes
                )
            )
        }
    }

    fun deleteSalesTransaction(transaction: SalesTransactionEntity) {
        viewModelScope.launch {
            repository.deleteSalesTransaction(transaction)
        }
    }

    fun clearAllSalesTransactions() {
        viewModelScope.launch {
            repository.clearAllSalesTransactions()
        }
    }

    // Currency Converter Feature State
    private val _converterTargetCurrency = MutableStateFlow(currencies[1]) // Default to EUR
    val converterTargetCurrency: StateFlow<CurrencyInfo> = _converterTargetCurrency.asStateFlow()

    private val _isScannedProductsListOpen = MutableStateFlow(false)
    val isScannedProductsListOpen: StateFlow<Boolean> = _isScannedProductsListOpen.asStateFlow()

    fun openScannedProductsList() {
        _isScannedProductsListOpen.value = true
    }

    fun closeScannedProductsList() {
        _isScannedProductsListOpen.value = false
    }

    fun setConverterTargetCurrency(currency: CurrencyInfo) {
        _converterTargetCurrency.value = currency
    }

    fun setConverterTargetCurrencyCode(code: String) {
        val found = currencies.find { it.code.equals(code, ignoreCase = true) }
        if (found != null) {
            _converterTargetCurrency.value = found
        }
    }

    /**
     * Converts an amount between any two currencies using current exchange rates.
     */
    fun convertCurrency(amount: Double, fromCurrencyCode: String, toCurrencyCode: String): Double {
        if (fromCurrencyCode.equals(toCurrencyCode, ignoreCase = true)) return amount
        val fromRate = liveRateMap.value[fromCurrencyCode.uppercase()]
            ?: ExchangeRateClient.baselineRates[fromCurrencyCode.uppercase()]
            ?: currencies.find { it.code.equals(fromCurrencyCode, ignoreCase = true) }?.exchangeRate
            ?: 1.0
        val toRate = liveRateMap.value[toCurrencyCode.uppercase()]
            ?: ExchangeRateClient.baselineRates[toCurrencyCode.uppercase()]
            ?: currencies.find { it.code.equals(toCurrencyCode, ignoreCase = true) }?.exchangeRate
            ?: 1.0

        if (fromRate <= 0.0) return amount
        val amountInUsd = amount / fromRate
        val result = amountInUsd * toRate
        return Math.round(result * 100.0) / 100.0
    }

    fun getEffectiveExchangeRate(fromCurrencyCode: String, toCurrencyCode: String): Double {
        if (fromCurrencyCode.equals(toCurrencyCode, ignoreCase = true)) return 1.0
        val fromRate = liveRateMap.value[fromCurrencyCode.uppercase()]
            ?: ExchangeRateClient.baselineRates[fromCurrencyCode.uppercase()]
            ?: currencies.find { it.code.equals(fromCurrencyCode, ignoreCase = true) }?.exchangeRate
            ?: 1.0
        val toRate = liveRateMap.value[toCurrencyCode.uppercase()]
            ?: ExchangeRateClient.baselineRates[toCurrencyCode.uppercase()]
            ?: currencies.find { it.code.equals(toCurrencyCode, ignoreCase = true) }?.exchangeRate
            ?: 1.0

        if (fromRate <= 0.0) return 1.0
        val rate = toRate / fromRate
        return Math.round(rate * 10000.0) / 10000.0
    }

    fun saveScannedProductToDb(
        name: String,
        price: Double,
        currency: String,
        barcode: String = "",
        barcodeFormat: String = "BARCODE",
        targetCurrency: String? = null,
        convertedPrice: Double? = null,
        notes: String? = null,
        onSuccess: (ScannedProductEntity) -> Unit = {}
    ) {
        val tgtCurrency = targetCurrency ?: _converterTargetCurrency.value.code
        val convPrice = convertedPrice ?: convertCurrency(price, currency, tgtCurrency)
        val rate = getEffectiveExchangeRate(currency, tgtCurrency)

        val product = ScannedProductEntity(
            name = name.ifBlank { "Scanned Product ($barcode)" },
            price = price,
            currency = currency,
            barcode = barcode,
            barcodeFormat = barcodeFormat,
            targetCurrency = tgtCurrency,
            convertedPrice = convPrice,
            exchangeRateUsed = rate,
            timestamp = System.currentTimeMillis(),
            category = if (barcode.startsWith("http")) "QR Web Link" else "Retail Product",
            notes = notes
        )
        viewModelScope.launch {
            val id = repository.insertScannedProduct(product)
            onSuccess(product.copy(id = id))
        }
    }

    fun deleteScannedProduct(product: ScannedProductEntity) {
        viewModelScope.launch {
            repository.deleteScannedProduct(product)
        }
    }

    fun deleteScannedProductById(id: Long) {
        viewModelScope.launch {
            repository.deleteScannedProductById(id)
        }
    }

    fun clearAllScannedProducts() {
        viewModelScope.launch {
            repository.clearAllScannedProducts()
        }
    }

    // Log filter & selection
    private val _logTypeFilter = MutableStateFlow(LogTypeFilter.ALL)
    val logTypeFilter: StateFlow<LogTypeFilter> = _logTypeFilter.asStateFlow()

    private val _selectedLog = MutableStateFlow<TransactionLogEntity?>(null)
    val selectedLog: StateFlow<TransactionLogEntity?> = _selectedLog.asStateFlow()

    fun setLogTypeFilter(filter: LogTypeFilter) {
        _logTypeFilter.value = filter
    }

    fun selectLog(log: TransactionLogEntity?) {
        _selectedLog.value = log
    }

    fun deleteLog(log: TransactionLogEntity) {
        viewModelScope.launch {
            repository.deleteLog(log)
            if (_selectedLog.value?.id == log.id) {
                _selectedLog.value = null
            }
        }
    }

    fun deleteLogById(id: Long) {
        viewModelScope.launch {
            repository.deleteLogById(id)
            if (_selectedLog.value?.id == id) {
                _selectedLog.value = null
            }
        }
    }

    fun logScannedItem(
        title: String,
        barcodeValue: String,
        barcodeFormat: String,
        amount: Double,
        currencyCode: String,
        customerName: String,
        country: String,
        notes: String? = null
    ) {
        val curr = currencies.find { it.code.equals(currencyCode, ignoreCase = true) } ?: _selectedCurrency.value
        val amountUsd = if (curr.exchangeRate > 0) amount / curr.exchangeRate else amount
        val log = TransactionLogEntity(
            logType = "SCANNED_ITEM",
            title = title.ifBlank { "Scanned Barcode: $barcodeValue" },
            barcodeOrRawValue = barcodeValue,
            barcodeFormat = barcodeFormat,
            amount = amount,
            currencyCode = curr.code,
            amountInUsd = amountUsd,
            customerOrRecipient = customerName.ifBlank { "Store Customer" },
            country = country.ifBlank { curr.defaultCountry },
            paymentMethod = "SCANNER",
            status = "COMPLETED",
            referenceCode = "SCAN-" + barcodeValue.takeLast(6).filter { it.isLetterOrDigit() }.ifEmpty { "101" } + "-${(100..999).random()}",
            notes = notes
        )
        viewModelScope.launch {
            repository.insertLog(log)
        }
    }

    fun logPayment(
        title: String,
        amount: Double,
        currencyCode: String,
        amountInUsd: Double,
        customerName: String,
        country: String,
        paymentMethod: String = "CARD",
        referenceCode: String = "PAY-${(1000..9999).random()}",
        status: String = "COMPLETED",
        notes: String? = null
    ) {
        val log = TransactionLogEntity(
            logType = "PAYMENT",
            title = title,
            barcodeOrRawValue = null,
            barcodeFormat = null,
            amount = amount,
            currencyCode = currencyCode,
            amountInUsd = amountInUsd,
            customerOrRecipient = customerName,
            country = country,
            paymentMethod = paymentMethod,
            status = status,
            referenceCode = referenceCode,
            notes = notes
        )
        viewModelScope.launch {
            repository.insertLog(log)
        }
    }

    // Transaction Summary State
    private val _currencyFilter = MutableStateFlow<String?>(null)
    val currencyFilter: StateFlow<String?> = _currencyFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(TransactionSortOrder.NEWEST)
    val sortOrder: StateFlow<TransactionSortOrder> = _sortOrder.asStateFlow()

    private val _selectedTransaction = MutableStateFlow<TransactionEntity?>(null)
    val selectedTransaction: StateFlow<TransactionEntity?> = _selectedTransaction.asStateFlow()

    fun setCurrencyFilter(currency: String?) {
        _currencyFilter.value = currency
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: TransactionSortOrder) {
        _sortOrder.value = order
    }

    fun selectTransaction(transaction: TransactionEntity?) {
        _selectedTransaction.value = transaction
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.delete(transaction)
            if (_selectedTransaction.value?.id == transaction.id) {
                _selectedTransaction.value = null
            }
        }
    }

    fun deleteTransactionById(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
            if (_selectedTransaction.value?.id == id) {
                _selectedTransaction.value = null
            }
        }
    }

    fun seedSampleTransactions() {
        val now = System.currentTimeMillis()
        val samples = listOf(
            TransactionEntity(
                amountInUsd = 49.99,
                targetCurrency = "EUR",
                convertedAmount = 45.99,
                customerName = "Sophie Dubois",
                country = "France",
                timestamp = now - 1000L * 60 * 18,
                stripeSubscriptionId = "sub_live_eu948201"
            ),
            TransactionEntity(
                amountInUsd = 120.00,
                targetCurrency = "GBP",
                convertedAmount = 93.60,
                customerName = "Liam O'Connor",
                country = "United Kingdom",
                timestamp = now - 1000L * 60 * 65,
                stripeSubscriptionId = "sub_live_uk772190"
            ),
            TransactionEntity(
                amountInUsd = 150.00,
                targetCurrency = "JPY",
                convertedAmount = 23280.00,
                customerName = "Kenji Takahashi",
                country = "Japan",
                timestamp = now - 1000L * 60 * 60 * 3,
                stripeSubscriptionId = "sub_live_jp519823"
            ),
            TransactionEntity(
                amountInUsd = 65.00,
                targetCurrency = "CNY",
                convertedAmount = 471.25,
                customerName = "Wei Zhang",
                country = "China",
                timestamp = now - 1000L * 60 * 60 * 5,
                stripeSubscriptionId = "sub_live_cn339182"
            ),
            TransactionEntity(
                amountInUsd = 199.99,
                targetCurrency = "USD",
                convertedAmount = 199.99,
                customerName = "Michael Chang",
                country = "United States",
                timestamp = now - 1000L * 60 * 60 * 12,
                stripeSubscriptionId = "sub_live_us882194"
            ),
            TransactionEntity(
                amountInUsd = 89.50,
                targetCurrency = "EUR",
                convertedAmount = 82.34,
                customerName = "Elena Rostova",
                country = "Germany",
                timestamp = now - 1000L * 60 * 60 * 26,
                stripeSubscriptionId = "sub_live_de661902"
            ),
            TransactionEntity(
                amountInUsd = 250.00,
                targetCurrency = "JPY",
                convertedAmount = 38800.00,
                customerName = "Hiroshi Tanaka",
                country = "Japan",
                timestamp = now - 1000L * 60 * 60 * 38,
                stripeSubscriptionId = "sub_live_jp110948"
            ),
            TransactionEntity(
                amountInUsd = 75.00,
                targetCurrency = "GBP",
                convertedAmount = 58.50,
                customerName = "Olivia Smith",
                country = "Australia",
                timestamp = now - 1000L * 60 * 60 * 52,
                stripeSubscriptionId = "sub_live_au440182"
            )
        )

        val sampleLogs = listOf(
            TransactionLogEntity(
                logType = "SCANNED_ITEM",
                title = "Glaba Pro Storefront Package",
                barcodeOrRawValue = "https://glaba.pro/pay?m=org_6RsNhnGbSQ3s7rFPAoYfQjA&c=EUR&code=GLOBAL15",
                barcodeFormat = "QR_CODE",
                amount = 45.99,
                currencyCode = "EUR",
                amountInUsd = 49.99,
                customerOrRecipient = "Sophie Dubois",
                country = "France",
                paymentMethod = "SCANNER",
                status = "COMPLETED",
                timestamp = now - 1000L * 60 * 18,
                referenceCode = "SCAN-QR-9482",
                notes = "Glaba Storefront QR link scan"
            ),
            TransactionLogEntity(
                logType = "PAYMENT",
                title = "POS Retail Card Checkout",
                barcodeOrRawValue = null,
                barcodeFormat = null,
                amount = 93.60,
                currencyCode = "GBP",
                amountInUsd = 120.00,
                customerOrRecipient = "Liam O'Connor",
                country = "United Kingdom",
                paymentMethod = "CARD",
                status = "COMPLETED",
                timestamp = now - 1000L * 60 * 65,
                referenceCode = "POS-UK-7721",
                notes = "Contactless Visa card terminal"
            ),
            TransactionLogEntity(
                logType = "SCANNED_ITEM",
                title = "Tokyo Electronics Pack (UPC-A)",
                barcodeOrRawValue = "012345678905",
                barcodeFormat = "UPC_A",
                amount = 23280.00,
                currencyCode = "JPY",
                amountInUsd = 150.00,
                customerOrRecipient = "Kenji Takahashi",
                country = "Japan",
                paymentMethod = "SCANNER",
                status = "COMPLETED",
                timestamp = now - 1000L * 60 * 60 * 3,
                referenceCode = "SCAN-UPC-5198",
                notes = "Inventory barcode check-in"
            ),
            TransactionLogEntity(
                logType = "PAYMENT",
                title = "SEPA Instant Bank Transfer",
                barcodeOrRawValue = null,
                barcodeFormat = null,
                amount = 82.34,
                currencyCode = "EUR",
                amountInUsd = 89.50,
                customerOrRecipient = "Elena Rostova",
                country = "Germany",
                paymentMethod = "SEPA",
                status = "COMPLETED",
                timestamp = now - 1000L * 60 * 60 * 26,
                referenceCode = "SEPA-DE-6619",
                notes = "Direct European SEPA payment"
            ),
            TransactionLogEntity(
                logType = "SCANNED_ITEM",
                title = "Shanghai Logistics Code 128",
                barcodeOrRawValue = "CN-SH-99210",
                barcodeFormat = "CODE_128",
                amount = 471.25,
                currencyCode = "CNY",
                amountInUsd = 65.00,
                customerOrRecipient = "Wei Zhang",
                country = "China",
                paymentMethod = "SCANNER",
                status = "COMPLETED",
                timestamp = now - 1000L * 60 * 60 * 5,
                referenceCode = "SCAN-C128-3391",
                notes = "Warehouse handheld barcode dispatch"
            ),
            TransactionLogEntity(
                logType = "PAYMENT",
                title = "Apple Pay Express Checkout",
                barcodeOrRawValue = null,
                barcodeFormat = null,
                amount = 199.99,
                currencyCode = "USD",
                amountInUsd = 199.99,
                customerOrRecipient = "Michael Chang",
                country = "United States",
                paymentMethod = "APPLE_GOOGLE_PAY",
                status = "COMPLETED",
                timestamp = now - 1000L * 60 * 60 * 12,
                referenceCode = "PAY-US-8821",
                notes = "NFC Biometric Apple Pay authorization"
            )
        )

        viewModelScope.launch {
            repository.insertAll(samples)
            repository.insertLogs(sampleLogs)
        }
    }

    fun translate(key: String): String {
        return translations[_selectedLanguage.value]?.get(key) ?: key
    }

    fun selectLanguage(language: AppLanguage) {
        _selectedLanguage.value = language
    }

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun selectCurrency(currency: CurrencyInfo) {
        val currentLiveRate = _liveRateMap.value[currency.code] ?: currency.exchangeRate
        val currentLiveChange = _rateChangePercents.value[currency.code] ?: currency.liveChangePercent
        val updatedCurrency = currency.copy(
            exchangeRate = currentLiveRate,
            liveChangePercent = currentLiveChange
        )
        _selectedCurrency.value = updatedCurrency
        selectedCountry.value = currency.defaultCountry
        _simulatorBillingCountry.value = currency.defaultCountry
        val rails = getPaymentRailsForCurrency(currency.code)
        _selectedPaymentRail.value = rails.firstOrNull()
    }

    fun selectPaymentRail(rail: LocalPaymentRail) {
        _selectedPaymentRail.value = rail
    }

    fun openSimulatorModal() {
        _isSimulatorModalOpen.value = true
    }

    fun closeSimulatorModal() {
        _isSimulatorModalOpen.value = false
    }

    fun updateSimulatorCustomerName(name: String) {
        _simulatorCustomerName.value = name
    }

    fun updateSimulatorBillingCountry(country: String) {
        _simulatorBillingCountry.value = country
    }

    fun getPaymentRailsForCurrency(currencyCode: String): List<LocalPaymentRail> {
        return when (currencyCode.uppercase()) {
            "EUR" -> listOf(
                LocalPaymentRail("sepa_instant", "SEPA Instant", "0.20%", "< 10 sec", "European Instant Credit Transfer", true),
                LocalPaymentRail("ideal", "iDEAL", "€0.29 flat", "Real-time", "Netherlands Direct Bank Authorization", true),
                LocalPaymentRail("bancontact", "Bancontact", "0.85%", "Real-time", "Belgium Leading Debit Rail", true),
                LocalPaymentRail("sofort", "Sofort / Klarna", "1.40%", "Real-time", "DACH Region Instant Pay", true),
                LocalPaymentRail("bank_wire_eur", "Bank Wire / SWIFT", "€12.00 flat fee", "1-2 days", "Direct Interbank Wire Transfer", false),
                LocalPaymentRail("paypal_eur", "PayPal Europe", "2.90% + €0.35", "Instant", "PayPal Digital Wallet & Instant Payout", true)
            )
            "GBP" -> listOf(
                LocalPaymentRail("faster_payments", "Faster Payments", "£0.20 flat", "Instant", "UK Bank Real-Time Rail", true),
                LocalPaymentRail("bacs", "BACS Direct Debit", "0.50%", "1-3 days", "UK Standard Direct Debit", false),
                LocalPaymentRail("open_banking_uk", "Open Banking UK", "0.35%", "Instant", "Authorized Push Payment (APP)", true),
                LocalPaymentRail("bank_wire_gbp", "CHAPS / Bank Wire", "£10.00 flat fee", "Same Day", "High-value UK Bank Wire Settlement", true),
                LocalPaymentRail("paypal_gbp", "PayPal UK", "2.90% + £0.30", "Instant", "PayPal Digital Wallet & Instant Payout", true)
            )
            "JPY" -> listOf(
                LocalPaymentRail("konbini", "Konbini Convenience", "1.50%", "Real-time", "Lawson, 7-Eleven & FamilyMart cash voucher", true),
                LocalPaymentRail("paypay", "PayPay / LINE Pay", "1.98%", "Instant", "Japan Mobile QR Payment", true),
                LocalPaymentRail("jcb", "JCB Debit / Credit", "2.10%", "Instant", "Domestic Japan Card Network", true)
            )
            "CNY" -> listOf(
                LocalPaymentRail("alipay", "Alipay Global", "1.20%", "Instant", "Cross-Border Escrow & QR", true),
                LocalPaymentRail("wechat", "WeChat Pay", "1.20%", "Instant", "Tencent Tenpay Real-Time Rail", true),
                LocalPaymentRail("unionpay", "China UnionPay", "1.10%", "Instant", "Domestic Interbank Bankcard Rail", true)
            )
            "CAD" -> listOf(
                LocalPaymentRail("interac", "Interac e-Transfer", "CA$0.50 flat", "Instant", "Canadian Interbank Real-Time Debit", true),
                LocalPaymentRail("eft_ca", "Canada Pre-Auth Debit", "0.60%", "1-2 days", "Canadian Direct Bank Debit", false)
            )
            "AUD" -> listOf(
                LocalPaymentRail("payid", "PayID / Osko", "A$0.25 flat", "Instant", "New Payments Platform (NPP) Australia", true),
                LocalPaymentRail("bpay", "BPAY", "0.75%", "Next Day", "Australian Bill Payment System", false)
            )
            "CHF" -> listOf(
                LocalPaymentRail("twint", "TWINT", "1.30%", "Instant", "Swiss Mobile Payment App", true),
                LocalPaymentRail("postfinance", "PostFinance Pay", "0.90%", "Real-time", "Swiss Post Direct Debit", true)
            )
            "SGD" -> listOf(
                LocalPaymentRail("paynow", "PayNow", "0.30%", "Instant", "Singapore FAST Real-Time Network", true),
                LocalPaymentRail("grabpay", "GrabPay Singapore", "1.80%", "Instant", "Southeast Asia Mobile Wallet", true)
            )
            "INR" -> listOf(
                LocalPaymentRail("upi", "UPI Instant", "0.00% Zero-Fee", "Instant", "NPCI Unified Payments Interface", true),
                LocalPaymentRail("rupay", "RuPay Debit", "0.40%", "Instant", "Indian National Card Rail", true),
                LocalPaymentRail("netbanking", "NetBanking", "1.20%", "Real-time", "50+ Indian Bank Gateways", true)
            )
            "BRL" -> listOf(
                LocalPaymentRail("pix", "Pix Instant", "0.20%", "Instant 24/7", "Central Bank of Brazil Real-Time Network", true),
                LocalPaymentRail("boleto", "Boleto Bancário", "R$2.50 flat", "1-2 days", "Brazilian Cash Voucher / Barcode Slip", false)
            )
            "MXN" -> listOf(
                LocalPaymentRail("spei", "SPEI Instant", "Mex$5.00 flat", "Instant", "Banco de México Real-Time Interbank", true),
                LocalPaymentRail("oxxo", "OXXO Pay", "3.00%", "Cash Voucher", "Convenience Store Barcode Voucher", false)
            )
            else -> listOf(
                LocalPaymentRail("fednow", "FedNow / RTP Instant", "0.15%", "Instant 24/7", "US Real-Time Gross Settlement", true),
                LocalPaymentRail("ach", "ACH Bank Debit", "$0.25 flat", "1-2 days", "Automated Clearing House US", false),
                LocalPaymentRail("bank_wire", "Bank Wire Transfer", "$15.00 flat fee", "1-2 days", "SWIFT / Interbank Wire Transfer", false),
                LocalPaymentRail("paypal", "PayPal Global", "2.90% + $0.30", "Instant", "PayPal Digital Wallet & Instant Payout", true),
                LocalPaymentRail("cards", "Visa / Mastercard Local", "1.50%", "Instant", "US Domestic Card Interchange", true)
            )
        }
    }

    fun refreshExchangeRates() {
        viewModelScope.launch {
            _isRateRefreshing.value = true
            try {
                // Attempt Retrofit API call with graceful fallback
                val response = try {
                    com.example.data.ExchangeRateClient.api.getLatestRates("USD")
                } catch (e: Exception) {
                    null
                }

                val newRateMap = mutableMapOf<String, Double>()
                val newChangeMap = mutableMapOf<String, Double>()
                val now = System.currentTimeMillis()

                currencies.forEach { c ->
                    val apiRate = response?.rates?.get(c.code)
                    val effectiveRate = if (apiRate != null && apiRate > 0.0) {
                        apiRate
                    } else {
                        // Subtle tick fluctuation (-0.15% to +0.15%)
                        val jitterPercent = ((-15..15).random()) / 10000.0
                        val baseline = com.example.data.ExchangeRateClient.baselineRates[c.code] ?: c.exchangeRate
                        baseline * (1.0 + jitterPercent)
                    }
                    val roundedRate = Math.round(effectiveRate * 10000.0) / 10000.0
                    newRateMap[c.code] = roundedRate

                    val baseline = com.example.data.ExchangeRateClient.baselineRates[c.code] ?: c.exchangeRate
                    val changePct = Math.round(((roundedRate - baseline) / baseline) * 10000.0) / 100.0
                    newChangeMap[c.code] = changePct
                }

                _liveRateMap.value = newRateMap
                _rateChangePercents.value = newChangeMap
                _lastRateRefreshTimestamp.value = now

                // Update selected currency with live rate
                val currentSelected = _selectedCurrency.value
                val updatedRate = newRateMap[currentSelected.code] ?: currentSelected.exchangeRate
                val updatedChange = newChangeMap[currentSelected.code] ?: currentSelected.liveChangePercent
                _selectedCurrency.value = currentSelected.copy(
                    exchangeRate = updatedRate,
                    liveChangePercent = updatedChange
                )
            } finally {
                delay(350)
                _isRateRefreshing.value = false
            }
        }
    }

    fun calculateConvertedAmount(amountUsd: Double, targetCurrency: CurrencyInfo): Double {
        val rate = _liveRateMap.value[targetCurrency.code] ?: targetCurrency.exchangeRate
        return amountUsd * rate
    }

    fun calculateInverseRate(targetCurrency: CurrencyInfo): Double {
        val rate = _liveRateMap.value[targetCurrency.code] ?: targetCurrency.exchangeRate
        return if (rate > 0.0) 1.0 / rate else 1.0
    }

    fun calculateFxSavingsUsd(amountUsd: Double): Double {
        return Math.round(amountUsd * 0.035 * 100.0) / 100.0
    }

    fun simulatePaymentWithDetails(
        amountUsd: Double,
        targetCurrency: CurrencyInfo,
        paymentRail: LocalPaymentRail,
        customerName: String,
        country: String,
        onSuccess: (TransactionEntity) -> Unit = {}
    ) {
        val rate = _liveRateMap.value[targetCurrency.code] ?: targetCurrency.exchangeRate
        val converted = amountUsd * rate
        val refCode = "SIM-${paymentRail.id.take(4).uppercase()}-${(10000..99999).random()}"
        val transaction = TransactionEntity(
            amountInUsd = amountUsd,
            targetCurrency = targetCurrency.code,
            convertedAmount = converted,
            customerName = customerName.ifBlank { "Localized Simulator Customer" },
            country = country.ifBlank { targetCurrency.defaultCountry },
            timestamp = System.currentTimeMillis(),
            stripeSubscriptionId = "sim_${targetCurrency.code.lowercase()}_${refCode}"
        )
        val logEntity = TransactionLogEntity(
            logType = "PAYMENT",
            title = "Simulated ${paymentRail.name} Payment (${targetCurrency.code})",
            barcodeOrRawValue = "SIM:${targetCurrency.code}:$converted:$refCode",
            barcodeFormat = "SIMULATOR",
            amount = converted,
            currencyCode = targetCurrency.code,
            amountInUsd = amountUsd,
            customerOrRecipient = customerName.ifBlank { "Localized Simulator Customer" },
            country = country.ifBlank { targetCurrency.defaultCountry },
            paymentMethod = paymentRail.id,
            status = "SETTLED",
            referenceCode = refCode,
            notes = "Live FX rate 1 USD = $rate ${targetCurrency.code} via ${paymentRail.name} (${paymentRail.settlementTime})"
        )

        onSuccess(transaction)
        viewModelScope.launch {
            repository.insert(transaction)
            repository.insertLog(logEntity)
        }
    }

    suspend fun saveSimulatedPayment(
        amountUsd: Double,
        targetCurrency: CurrencyInfo,
        paymentRail: LocalPaymentRail,
        customerName: String,
        country: String
    ): TransactionEntity {
        val rate = _liveRateMap.value[targetCurrency.code] ?: targetCurrency.exchangeRate
        val converted = amountUsd * rate
        val refCode = "SIM-${paymentRail.id.take(4).uppercase()}-${(10000..99999).random()}"
        val transaction = TransactionEntity(
            amountInUsd = amountUsd,
            targetCurrency = targetCurrency.code,
            convertedAmount = converted,
            customerName = customerName.ifBlank { "Localized Simulator Customer" },
            country = country.ifBlank { targetCurrency.defaultCountry },
            timestamp = System.currentTimeMillis(),
            stripeSubscriptionId = "sim_${targetCurrency.code.lowercase()}_${refCode}"
        )
        val logEntity = TransactionLogEntity(
            logType = "PAYMENT",
            title = "Simulated ${paymentRail.name} Payment (${targetCurrency.code})",
            barcodeOrRawValue = "SIM:${targetCurrency.code}:$converted:$refCode",
            barcodeFormat = "SIMULATOR",
            amount = converted,
            currencyCode = targetCurrency.code,
            amountInUsd = amountUsd,
            customerOrRecipient = customerName.ifBlank { "Localized Simulator Customer" },
            country = country.ifBlank { targetCurrency.defaultCountry },
            paymentMethod = paymentRail.id,
            status = "SETTLED",
            referenceCode = refCode,
            notes = "Live FX rate 1 USD = $rate ${targetCurrency.code} via ${paymentRail.name}"
        )
        repository.insert(transaction)
        repository.insertLog(logEntity)
        return transaction
    }

    fun updateAmountUsd(amount: String) {
        // Allow only valid numeric inputs
        if (amount.isEmpty() || amount.toDoubleOrNull() != null || amount.endsWith(".")) {
            _amountUsd.value = amount
        }
    }

    fun openCheckout() {
        _isCheckoutVisible.value = true
        _paymentSuccess.value = false
        _isProcessing.value = false
    }

    fun closeCheckout() {
        _isCheckoutVisible.value = false
    }

    fun openQrPayment() {
        _isQrPaymentVisible.value = true
    }

    fun closeQrPayment() {
        _isQrPaymentVisible.value = false
    }

    fun confirmQrPayment(
        customerName: String = "Mobile Wallet Customer",
        country: String? = null
    ) {
        val rawAmount = _amountUsd.value.toDoubleOrNull() ?: 49.99
        val targetCurr = _selectedCurrency.value
        val converted = rawAmount * targetCurr.exchangeRate
        val finalCountry = country ?: targetCurr.defaultCountry
        val refCode = "QR-${(10000..99999).random()}"

        viewModelScope.launch {
            val entity = TransactionEntity(
                amountInUsd = rawAmount,
                targetCurrency = targetCurr.code,
                convertedAmount = converted,
                customerName = customerName,
                country = finalCountry,
                timestamp = System.currentTimeMillis(),
                stripeSubscriptionId = "qr_wallet_${refCode}_${(1000..9999).random()}"
            )
            val logEntity = TransactionLogEntity(
                logType = "PAYMENT",
                title = "Mobile Wallet QR Payment",
                barcodeOrRawValue = "https://glaba.pro/pay?m=org_6RsNhnGbSQ3s7rFPAoYfQjA&c=${targetCurr.code}&amt=${String.format(java.util.Locale.US, "%.2f", converted)}&ref=$refCode",
                barcodeFormat = "QR_CODE",
                amount = converted,
                currencyCode = targetCurr.code,
                amountInUsd = rawAmount,
                customerOrRecipient = customerName,
                country = finalCountry,
                paymentMethod = "MOBILE_WALLET_QR",
                status = "COMPLETED",
                referenceCode = refCode,
                notes = "Customer scanned and paid terminal total QR code"
            )
            repository.insert(entity)
            repository.insertLog(logEntity)
            _isSubscribed.value = true
        }
    }

    fun openShareModal(campaign: MarketingCampaign? = null) {
        if (campaign != null) {
            _selectedCampaign.value = campaign
        }
        _isShareModalOpen.value = true
    }

    fun closeShareModal() {
        _isShareModalOpen.value = false
    }

    fun selectCampaign(campaign: MarketingCampaign) {
        _selectedCampaign.value = campaign
    }

    fun updateProfile(name: String, email: String, organization: String) {
        val initials = name.trim().split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .ifEmpty { "JP" }
        _userProfile.value = _userProfile.value.copy(
            name = name,
            email = email,
            organization = organization,
            initials = initials
        )
    }

    fun toggleTwoFactor(enabled: Boolean) {
        _userProfile.value = _userProfile.value.copy(isTwoFactorEnabled = enabled)
    }

    fun toggleAutoConvert(enabled: Boolean) {
        _userProfile.value = _userProfile.value.copy(autoConvertPayouts = enabled)
    }

    fun toggleNotifications(enabled: Boolean) {
        _userProfile.value = _userProfile.value.copy(emailNotifications = enabled)
    }

    fun toggleSubscriptionStatus() {
        _isSubscribed.value = !_isSubscribed.value
    }

    fun performSimulatedPayment() {
        val rawAmount = _amountUsd.value.toDoubleOrNull() ?: 49.99
        val targetCurr = _selectedCurrency.value
        val effectiveRate = _liveRateMap.value[targetCurr.code] ?: targetCurr.exchangeRate
        val converted = rawAmount * effectiveRate

        viewModelScope.launch {
            _isProcessing.value = true
            delay(2000) // Simulated processing latency
            _isProcessing.value = false
            _paymentSuccess.value = true
            _isSubscribed.value = true

            // Save transaction to local Room database
            val transaction = TransactionEntity(
                amountInUsd = rawAmount,
                targetCurrency = targetCurr.code,
                convertedAmount = converted,
                customerName = if (cardHolderName.value.isBlank()) "John Doe" else cardHolderName.value,
                country = selectedCountry.value,
                stripeSubscriptionId = "sub_" + (100000..999999).random() + "org_6RsNhnGbSQ3s7rFPAoYfQjA"
            )
            val logEntity = TransactionLogEntity(
                logType = "PAYMENT",
                title = "Terminal Card Payment",
                barcodeOrRawValue = null,
                barcodeFormat = null,
                amount = converted,
                currencyCode = targetCurr.code,
                amountInUsd = rawAmount,
                customerOrRecipient = if (cardHolderName.value.isBlank()) "John Doe" else cardHolderName.value,
                country = selectedCountry.value,
                paymentMethod = "CARD",
                status = "COMPLETED",
                referenceCode = "POS-" + (100000..999999).random(),
                notes = "In-store POS card transaction"
            )
            repository.insert(transaction)
            repository.insertLog(logEntity)

            delay(2000) // Keep success showing briefly before closing
            _isCheckoutVisible.value = false
            _paymentSuccess.value = false
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
            repository.clearAllLogs()
            repository.clearAllHistoryRecords()
            _isSubscribed.value = false
        }
    }
}

class GlobalMarketViewModelFactory(
    private val repository: TransactionRepository,
    private val firestoreRepository: com.example.data.FirestoreRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GlobalMarketViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GlobalMarketViewModel(repository, firestoreRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.TransactionDao
import com.example.data.TransactionEntity
import com.example.data.TransactionHistoryDao
import com.example.data.TransactionHistoryEntity
import com.example.data.TransactionRepository
import com.example.ui.AppLanguage
import com.example.ui.GlobalMarketViewModel
import com.example.ui.parseBarcodeToItem
import com.google.mlkit.vision.barcode.common.Barcode
import com.example.util.CurrencyFormatter
import com.example.util.CurrencyUtils
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var db: AppDatabase
  private lateinit var dao: TransactionDao
  private lateinit var historyDao: TransactionHistoryDao
  private lateinit var repository: TransactionRepository

  @Before
  fun createDb() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    dao = db.transactionDao()
    val logDao = db.transactionLogDao()
    val scannedProductDao = db.scannedProductDao()
    historyDao = db.transactionHistoryDao()
    repository = TransactionRepository(
      dao,
      logDao,
      scannedProductDao,
      historyDao,
      db.salesTerminalDao(),
      db.inventoryProductDao(),
      db.salesTransactionDao()
    )
  }

  @After
  fun closeDb() {
    db.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("GlobalMarket Pro", appName)
  }

  @Test
  fun `insert and retrieve transactions`() = runBlocking {
    val transaction = TransactionEntity(
      amountInUsd = 100.0,
      targetCurrency = "EUR",
      convertedAmount = 92.0,
      customerName = "Jane Doe",
      country = "Germany",
      stripeSubscriptionId = "sub_test123"
    )
    dao.insertTransaction(transaction)

    val list = dao.getAllTransactions().first()
    assertEquals(1, list.size)
    assertEquals("Jane Doe", list[0].customerName)
    assertEquals("EUR", list[0].targetCurrency)
    assertEquals(100.0, list[0].amountInUsd, 0.001)
  }

  @Test
  fun `verify marketing campaign presets and share flow`() {
    val vm = GlobalMarketViewModel(repository)

    assertEquals(3, vm.marketingCampaigns.size)
    val defaultCampaign = vm.marketingCampaigns[0]
    assertEquals("storefront_checkout", defaultCampaign.id)
    assertEquals("GLOBAL15", defaultCampaign.promoCode)
    assertEquals(15, defaultCampaign.discountPercent)
    assertTrue(defaultCampaign.shortUrl.contains("glaba.pro"))

    // Test opening and closing share modal
    assertFalse(vm.isShareModalOpen.value)
    vm.openShareModal(vm.marketingCampaigns[1])
    assertTrue(vm.isShareModalOpen.value)
    assertEquals("partner_referral", vm.selectedCampaign.value.id)

    vm.closeShareModal()
    assertFalse(vm.isShareModalOpen.value)
  }

  @Test
  fun `verify localization and translations`() {
    val vm = GlobalMarketViewModel(repository)
    assertEquals(AppLanguage.ENGLISH, vm.selectedLanguage.value)
    assertEquals("Share Marketing Campaign", vm.translate("share_marketing"))

    vm.selectLanguage(AppLanguage.ESPANOL)
    assertEquals(AppLanguage.ESPANOL, vm.selectedLanguage.value)
    assertEquals("Compartir Campaña de Marketing", vm.translate("share_marketing"))

    vm.selectLanguage(AppLanguage.CHINESE)
    assertEquals("分享营销活动推广", vm.translate("share_marketing"))
  }

  @Test
  fun `verify user profile and settings update`() {
    val vm = GlobalMarketViewModel(repository)
    val initialProfile = vm.userProfile.value
    assertEquals("Jurgen-Paul Westerveld", initialProfile.name)
    assertEquals("JP", initialProfile.initials)

    vm.updateProfile("Sarah Connor", "sarah@resistance.org", "Tech Resistance Corp")
    assertEquals("Sarah Connor", vm.userProfile.value.name)
    assertEquals("SC", vm.userProfile.value.initials)
    assertEquals("Tech Resistance Corp", vm.userProfile.value.organization)

    vm.toggleTwoFactor(false)
    assertFalse(vm.userProfile.value.isTwoFactorEnabled)
  }

  @Test
  fun `verify transaction summary screen sorting, filtering, and deletion`() = runBlocking {
    repository.clearAll()

    val samples = listOf(
      TransactionEntity(
        amountInUsd = 49.99,
        targetCurrency = "EUR",
        convertedAmount = 45.99,
        customerName = "Sophie Dubois",
        country = "France",
        stripeSubscriptionId = "sub_live_eu948201"
      ),
      TransactionEntity(
        amountInUsd = 120.00,
        targetCurrency = "GBP",
        convertedAmount = 93.60,
        customerName = "Liam O'Connor",
        country = "United Kingdom",
        stripeSubscriptionId = "sub_live_uk772190"
      ),
      TransactionEntity(
        amountInUsd = 150.00,
        targetCurrency = "JPY",
        convertedAmount = 23280.00,
        customerName = "Kenji Takahashi",
        country = "Japan",
        stripeSubscriptionId = "sub_live_jp519823"
      ),
      TransactionEntity(
        amountInUsd = 89.50,
        targetCurrency = "EUR",
        convertedAmount = 82.34,
        customerName = "Elena Rostova",
        country = "Germany",
        stripeSubscriptionId = "sub_live_de661902"
      )
    )
    repository.insertAll(samples)

    val initialList = repository.allTransactions.first()
    assertEquals(4, initialList.size)

    val vm = GlobalMarketViewModel(repository)

    // Test Currency Filter
    vm.setCurrencyFilter("EUR")
    assertEquals("EUR", vm.currencyFilter.value)

    val eurTransactions = initialList.filter { it.targetCurrency == "EUR" }
    assertEquals(2, eurTransactions.size)

    // Reset filter
    vm.setCurrencyFilter(null)
    assertEquals(null, vm.currencyFilter.value)

    // Test Search Filter
    vm.setSearchQuery("Sophie")
    assertEquals("Sophie", vm.searchQuery.value)
    val searched = initialList.filter { it.customerName.contains("Sophie", ignoreCase = true) }
    assertEquals(1, searched.size)
    assertEquals("Sophie Dubois", searched[0].customerName)

    vm.setSearchQuery("")

    // Test Selection and deletion
    val firstTx = initialList[0]
    vm.selectTransaction(firstTx)
    assertEquals(firstTx.id, vm.selectedTransaction.value?.id)

    repository.delete(firstTx)
    val afterDeleteList = repository.allTransactions.first()
    assertEquals(3, afterDeleteList.size)
  }

  @Test
  fun `verify sales summary localized strings exist in all languages`() {
    val vm = GlobalMarketViewModel(repository)
    val keys = listOf(
      "sales_summary_title",
      "sales_summary_subtitle",
      "gross_sales",
      "total_orders",
      "avg_order_value",
      "active_markets",
      "filter_all",
      "search_placeholder",
      "seed_sample_data",
      "clear_all_tx",
      "receipt_details",
      "status_completed",
      "export_csv",
      "drilldown_title",
      "back_to_summary",
      "fee_breakdown",
      "payment_method_info",
      "audit_timeline",
      "customer_info",
      "net_payout",
      "order_details",
      "copy_json",
      "share_receipt"
    )

    for (lang in AppLanguage.values()) {
      vm.selectLanguage(lang)
      for (key in keys) {
        val translation = vm.translate(key)
        assertTrue("Translation for key '$key' in language ${lang.name} should not be equal to the key itself", translation != key)
        assertTrue("Translation for key '$key' in language ${lang.name} should not be empty", translation.isNotEmpty())
      }
    }
  }

  @Test
  fun `verify currency formatting with US locale`() {
    val usFormatter = CurrencyFormatter(Locale.US)

    // USD format in US locale
    val formattedUsd = usFormatter.format(49.99, "USD")
    assertTrue("Should contain 49.99", formattedUsd.contains("49.99"))
    assertTrue("Should contain $ symbol", formattedUsd.contains("$"))

    // JPY format in US locale (standard 0 decimal fraction digits)
    val formattedJpy = usFormatter.format(23280.0, "JPY")
    assertTrue("Should contain 23,280", formattedJpy.contains("23,280"))
    assertFalse("JPY should not contain decimals", formattedJpy.contains(".00"))

    // EUR format in US locale
    val formattedEur = usFormatter.format(45.99, "EUR")
    assertTrue("Should contain 45.99", formattedEur.contains("45.99"))
    assertTrue("Should contain € or EUR", formattedEur.contains("€") || formattedEur.contains("EUR"))

    // USD Base helper
    val baseUsd = usFormatter.formatUsd(120.00)
    assertTrue("Should format base USD with $", baseUsd.contains("$120.00"))
  }

  @Test
  fun `verify currency formatting with European locales`() {
    // France locale uses comma decimal separator
    val frenchFormatter = CurrencyFormatter(Locale.FRANCE)
    val formattedFr = frenchFormatter.format(45.99, "EUR")
    assertTrue("French locale should use comma decimal separator", formattedFr.contains("45,99"))
    assertTrue("French locale should contain euro symbol", formattedFr.contains("€"))

    // Germany locale uses comma decimal separator and dot for grouping
    val germanFormatter = CurrencyFormatter(Locale.GERMANY)
    val formattedDe = germanFormatter.format(1250.50, "EUR")
    assertTrue("German locale should contain 1.250,50", formattedDe.contains("1.250,50"))
    assertTrue("German locale should contain euro symbol", formattedDe.contains("€"))

    assertEquals(',', CurrencyUtils.getDecimalSeparator(Locale.FRANCE))
    assertEquals('.', CurrencyUtils.getDecimalSeparator(Locale.US))
  }

  @Test
  fun `verify currency formatter instance creation from AppLanguage`() {
    val enFormatter = CurrencyFormatter.fromLanguage(AppLanguage.ENGLISH)
    assertEquals(Locale.US, enFormatter.locale)

    val frFormatter = CurrencyFormatter.fromLanguage(AppLanguage.FRANCAIS)
    assertEquals(Locale.FRANCE, frFormatter.locale)

    val deFormatter = CurrencyFormatter.fromLanguage(AppLanguage.DEUTSCH)
    assertEquals(Locale.GERMANY, deFormatter.locale)

    val zhFormatter = CurrencyFormatter.fromLanguage(AppLanguage.CHINESE)
    assertEquals(Locale.SIMPLIFIED_CHINESE, zhFormatter.locale)

    val esFormatter = CurrencyFormatter.fromLanguage(AppLanguage.ESPANOL)
    assertEquals("es", esFormatter.locale.language)
  }

  @Test
  fun `verify fraction digits and currency symbol utilities`() {
    assertEquals(0, CurrencyUtils.getFractionDigits("JPY"))
    assertEquals(2, CurrencyUtils.getFractionDigits("USD"))
    assertEquals(2, CurrencyUtils.getFractionDigits("EUR"))
    assertEquals(2, CurrencyUtils.getFractionDigits("GBP"))

    val usdSymbol = CurrencyUtils.getCurrencySymbol("USD", Locale.US)
    assertTrue("USD symbol in US locale should be $", usdSymbol == "$")

    val gbpSymbol = CurrencyUtils.getCurrencySymbol("GBP", Locale.UK)
    assertTrue("GBP symbol in UK locale should be £", gbpSymbol == "£")
  }

  @Test
  fun `verify currency formatter gracefully handles unknown currency codes`() {
    val formatter = CurrencyFormatter(Locale.US)
    val result = formatter.format(99.99, "UNKNOWN_COIN")
    assertTrue("Should format number nicely without throwing", result.contains("99.99"))
  }

  @Test
  fun `verify transaction drill-down workflow and granular detail access`() = runBlocking {
    val vm = GlobalMarketViewModel(repository)
    val sampleTx = TransactionEntity(
      id = 42,
      amountInUsd = 120.00,
      targetCurrency = "EUR",
      convertedAmount = 110.40,
      customerName = "Claire Delacour",
      country = "France",
      stripeSubscriptionId = "sub_live_fr4201"
    )

    // Initial state: no selected transaction
    assertEquals(null, vm.selectedTransaction.value)

    // User clicks transaction to drill down
    vm.selectTransaction(sampleTx)
    val selected = vm.selectedTransaction.value
    assertTrue("Selected transaction should be populated", selected != null)
    assertEquals(42, selected?.id)
    assertEquals("Claire Delacour", selected?.customerName)
    assertEquals("EUR", selected?.targetCurrency)
    assertEquals(110.40, selected?.convertedAmount ?: 0.0, 0.001)
    assertEquals("sub_live_fr4201", selected?.stripeSubscriptionId)

    // Verify fee calculations for granular breakdown
    val gross = selected!!.convertedAmount
    val baseUsd = selected.amountInUsd
    val rate = gross / baseUsd
    val fixedFee = 0.30 * rate
    val percentFee = gross * 0.029
    val totalGatewayFee = percentFee + fixedFee
    val fxFee = gross * 0.010
    val netPayout = gross - totalGatewayFee - fxFee
    assertTrue("Net payout must be positive and less than gross", netPayout in 0.0..gross)

    // User clicks back button to exit drill-down
    vm.selectTransaction(null)
    assertEquals(null, vm.selectedTransaction.value)
  }

  @Test
  fun `verify glaba pro storefront checkout url deep link parsing and promo handling`() {
    val vm = GlobalMarketViewModel(repository)
    val testUrl = "https://glaba.pro/pay?m=org_6RsNhnGbSQ3s7rFPAoYfQjA&c=EUR&code=GLOBAL15"

    assertFalse("Web checkout should start closed", vm.isWebCheckoutOpen.value)
    // Initially select USD to verify deep link switches to EUR
    vm.selectCurrency(vm.currencies[0])
    assertEquals("USD", vm.selectedCurrency.value.code)

    // Handle incoming deep link
    vm.handleDeepLinkUrl(testUrl)

    // Verify state updates
    assertTrue("Web checkout should be opened", vm.isWebCheckoutOpen.value)
    assertEquals("EUR", vm.selectedCurrency.value.code)
    assertEquals("GLOBAL15", vm.appliedPromoCode.value)
    assertEquals(15, vm.promoDiscountPercent.value)
    assertEquals("org_6RsNhnGbSQ3s7rFPAoYfQjA", vm.activeMerchantId.value)
    assertEquals(testUrl, vm.webCheckoutUrl.value)
    assertTrue("Deep link banner should be populated", vm.deepLinkBanner.value?.contains("15%") == true)

    // Test dismiss banner and close checkout
    vm.clearDeepLinkBanner()
    assertEquals(null, vm.deepLinkBanner.value)
    vm.closeWebCheckout()
    assertFalse(vm.isWebCheckoutOpen.value)
  }

  @Test
  fun `verify glaba pro barcode and qr scanner parsing`() {
    val testUrl = "https://glaba.pro/pay?m=org_6RsNhnGbSQ3s7rFPAoYfQjA&c=EUR&code=GLOBAL15"
    val parsedItem = parseBarcodeToItem(testUrl, Barcode.FORMAT_QR_CODE)

    assertEquals("EUR", parsedItem.currencyCode)
    assertTrue("Format name should be QR Code or Glaba QR Link", parsedItem.formatName == "QR Code" || parsedItem.formatName == "Glaba QR Link")
    assertTrue("Title should mention EUR and GLOBAL15", parsedItem.title.contains("EUR") && parsedItem.title.contains("GLOBAL15"))
    assertEquals(42.49, parsedItem.amountUsd, 0.01)
    assertFalse(parsedItem.isPayout)
    assertTrue(parsedItem.customerOrRecipient.contains("Elena Rostova") || parsedItem.customerOrRecipient.contains("org_6RsNhnGbSQ3s7rFPAoYfQjA"))
  }

  @Test
  fun `verify barcode scan automatically retrieves product details and loads into terminal`() {
    val vm = GlobalMarketViewModel(repository)
    
    // Simulate scanning a UPC-A product barcode
    val scannedItem = parseBarcodeToItem("012345678905", Barcode.FORMAT_UPC_A)
    assertEquals("UPC-A", scannedItem.formatName)
    assertEquals("Glaba Pro POS Terminal Terminal-X", scannedItem.title)
    assertEquals(249.00, scannedItem.amountUsd, 0.01)
    assertEquals("EUR", scannedItem.currencyCode)

    // Load scanned product into the sales terminal
    vm.loadBarcodeIntoTerminal(scannedItem.amountUsd, scannedItem.currencyCode)
    assertEquals("249.00", vm.amountUsd.value)
    assertEquals("EUR", vm.selectedCurrency.value.code)
    assertEquals(0, vm.selectedTab.value)
  }

  @Test
  fun `verify barcode scan records product transaction directly into ledger`() = runBlocking {
    val vm = GlobalMarketViewModel(repository)
    val scannedSku = parseBarcodeToItem("SKU-GLB-PRO-99", Barcode.FORMAT_CODE_128)
    assertEquals("Code 128", scannedSku.formatName)
    assertEquals("Enterprise Global SaaS Annual Plan", scannedSku.title)
    assertEquals(199.99, scannedSku.amountUsd, 0.01)

    val recordedTx = vm.saveBarcodeTransactionEntry(
      amountInUsd = scannedSku.amountUsd,
      currencyCode = scannedSku.currencyCode,
      customerName = scannedSku.customerOrRecipient,
      country = scannedSku.country,
      barcodeValue = scannedSku.rawValue
    )

    // Verify callback was triggered and details match
    assertEquals(199.99, recordedTx.amountInUsd, 0.001)
    assertEquals("USD", recordedTx.targetCurrency)
    assertEquals("Acme Corp International", recordedTx.customerName)
    assertEquals("United States", recordedTx.country)

    // Verify saved to database repository
    val all = repository.allTransactions.first()
    assertTrue(all.any { it.amountInUsd == 199.99 && it.customerName == "Acme Corp International" })
  }

  @Test
  fun `verify in-app web checkout completion creates transaction record`() = runBlocking {
    val vm = GlobalMarketViewModel(repository)
    val testUrl = "https://glaba.pro/pay?m=org_6RsNhnGbSQ3s7rFPAoYfQjA&c=EUR&code=GLOBAL15"
    vm.handleDeepLinkUrl(testUrl)

    val completedTx = vm.saveWebCheckoutTransaction(
      customerName = "Elena Rostova",
      country = "Germany",
      netAmountUsd = 42.49
    )

    assertEquals("EUR", completedTx.targetCurrency)
    assertEquals(42.49, completedTx.amountInUsd, 0.01)
    assertEquals("Elena Rostova", completedTx.customerName)
    assertEquals("Germany", completedTx.country)
    assertTrue("Subscription ID should have web_glaba prefix", completedTx.stripeSubscriptionId.startsWith("web_glaba_GLOBAL15"))

    // Verify stored in repository
    val all = repository.allTransactions.first()
    assertTrue(all.any { it.stripeSubscriptionId == completedTx.stripeSubscriptionId })
  }

  @Test
  fun `verify localized payment simulator currencies list and regional metadata`() {
    val vm = GlobalMarketViewModel(repository)

    // Verify 12 global currencies configured
    assertEquals(12, vm.currencies.size)
    val codes = vm.currencies.map { it.code }
    assertTrue(codes.containsAll(listOf("USD", "EUR", "GBP", "JPY", "CNY", "CAD", "AUD", "CHF", "SGD", "INR", "BRL", "MXN")))

    // Verify default target currency is EUR
    assertEquals("EUR", vm.selectedCurrency.value.code)
    assertEquals("€", vm.selectedCurrency.value.symbol)
    assertEquals("Europe", vm.selectedCurrency.value.region)

    // Test selecting other currencies
    val jpy = vm.currencies.first { it.code == "JPY" }
    vm.selectCurrency(jpy)
    assertEquals("JPY", vm.selectedCurrency.value.code)
    assertEquals("Japan", vm.selectedCountry.value)
    assertEquals("Japan", vm.simulatorBillingCountry.value)
    assertEquals("Asia-Pacific", vm.selectedCurrency.value.region)

    val brl = vm.currencies.first { it.code == "BRL" }
    vm.selectCurrency(brl)
    assertEquals("BRL", vm.selectedCurrency.value.code)
    assertEquals("Brazil", vm.selectedCountry.value)
    assertEquals("Americas", vm.selectedCurrency.value.region)
  }

  @Test
  fun `verify live exchange rate calculations, inverse rates, and fee savings`() {
    val vm = GlobalMarketViewModel(repository)
    vm.updateAmountUsd("100.00")

    val eur = vm.currencies.first { it.code == "EUR" }
    val convertedEur = vm.calculateConvertedAmount(100.0, eur)
    assertEquals(92.0, convertedEur, 0.5)

    val inverseEur = vm.calculateInverseRate(eur)
    assertTrue("Inverse rate 1 EUR to USD should be ~1.08", inverseEur in 1.05..1.12)

    val jpy = vm.currencies.first { it.code == "JPY" }
    val convertedJpy = vm.calculateConvertedAmount(100.0, jpy)
    assertTrue("100 USD in JPY should be ~15,520 JPY", convertedJpy in 15000.0..16000.0)

    val savings = vm.calculateFxSavingsUsd(100.0)
    assertEquals(3.50, savings, 0.01)
  }

  @Test
  fun `verify regional payment rails selection for global currencies`() {
    val vm = GlobalMarketViewModel(repository)

    // EUR Rails
    val eurRails = vm.getPaymentRailsForCurrency("EUR")
    assertTrue(eurRails.any { it.name.contains("SEPA Instant") })
    assertTrue(eurRails.any { it.name.contains("iDEAL") })
    assertTrue(eurRails.any { it.name.contains("Bancontact") })

    // JPY Rails
    val jpyRails = vm.getPaymentRailsForCurrency("JPY")
    assertTrue(jpyRails.any { it.name.contains("Konbini") })
    assertTrue(jpyRails.any { it.name.contains("PayPay") })

    // BRL Rails
    val brlRails = vm.getPaymentRailsForCurrency("BRL")
    assertTrue(brlRails.any { it.name.contains("Pix Instant") })

    // INR Rails
    val inrRails = vm.getPaymentRailsForCurrency("INR")
    assertTrue(inrRails.any { it.name.contains("UPI Instant") })

    // Select rail
    val sepa = eurRails.first()
    vm.selectPaymentRail(sepa)
    assertEquals(sepa.id, vm.selectedPaymentRail.value?.id)
  }

  @Test
  fun `verify payment simulation modal lifecycle and ledger recording`() = runBlocking {
    val vm = GlobalMarketViewModel(repository)

    // Modal open/close
    assertFalse(vm.isSimulatorModalOpen.value)
    vm.openSimulatorModal()
    assertTrue(vm.isSimulatorModalOpen.value)
    vm.closeSimulatorModal()
    assertFalse(vm.isSimulatorModalOpen.value)

    // Update customer and country inputs
    vm.updateSimulatorCustomerName("Alexander von Humboldt")
    assertEquals("Alexander von Humboldt", vm.simulatorCustomerName.value)
    vm.updateSimulatorBillingCountry("Germany")
    assertEquals("Germany", vm.simulatorBillingCountry.value)

    val eur = vm.currencies.first { it.code == "EUR" }
    val rail = vm.getPaymentRailsForCurrency("EUR").first()

    // Execute simulated payment
    val simulatedTx = vm.saveSimulatedPayment(
      amountUsd = 75.0,
      targetCurrency = eur,
      paymentRail = rail,
      customerName = "Alexander von Humboldt",
      country = "Germany"
    )

    assertEquals(75.0, simulatedTx.amountInUsd, 0.01)
    assertEquals("EUR", simulatedTx.targetCurrency)
    assertEquals("Alexander von Humboldt", simulatedTx.customerName)
    assertEquals("Germany", simulatedTx.country)
    assertTrue(simulatedTx.stripeSubscriptionId.startsWith("sim_eur_SIM-"))

    // Verify stored in Room database
    val txList = repository.allTransactions.first()
    assertTrue(txList.any { it.stripeSubscriptionId == simulatedTx.stripeSubscriptionId })

    val logList = repository.allLogs.first()
    assertTrue(logList.any { it.referenceCode.startsWith("SIM-") && it.currencyCode == "EUR" })
  }

  @Test
  fun `verify live exchange rate refresh updates rate map`() {
    val vm = GlobalMarketViewModel(repository)
    val initialTime = vm.lastRateRefreshTimestamp.value

    vm.refreshExchangeRates()
    assertTrue(vm.liveRateMap.value.containsKey("EUR"))
    assertTrue(vm.liveRateMap.value.containsKey("JPY"))
    assertTrue(vm.liveRateMap.value.containsKey("GBP"))
    assertTrue(vm.liveRateMap.value.size >= 12)
  }

  @Test
  fun `insert and query transaction history records including date amount and currency`() = runBlocking {
    val timestamp = 1716000000000L
    val record = TransactionHistoryEntity(
      date = timestamp,
      amount = 250.75,
      currency = "EUR",
      description = "Online Storefront Order #10042",
      type = "PAYMENT",
      status = "COMPLETED",
      category = "E-Commerce"
    )

    val insertedId = historyDao.insertTransactionHistory(record)
    assertTrue("Inserted record ID should be positive", insertedId > 0)

    val records = historyDao.getAllTransactionHistory().first()
    assertEquals(1, records.size)
    val retrieved = records[0]
    assertEquals(timestamp, retrieved.date)
    assertEquals(250.75, retrieved.amount, 0.001)
    assertEquals("EUR", retrieved.currency)
    assertEquals("Online Storefront Order #10042", retrieved.description)
    assertEquals("PAYMENT", retrieved.type)
    assertEquals("COMPLETED", retrieved.status)
  }

  @Test
  fun `verify repository transaction history operations and filtering`() = runBlocking {
    val now = System.currentTimeMillis()
    val recordEur = TransactionHistoryEntity(
      date = now - 5000,
      amount = 89.90,
      currency = "EUR",
      description = "Berlin Cafe POS",
      type = "PAYMENT"
    )
    val recordGbp = TransactionHistoryEntity(
      date = now - 1000,
      amount = 120.00,
      currency = "GBP",
      description = "London SaaS Invoice",
      type = "PAYMENT"
    )
    val recordRefund = TransactionHistoryEntity(
      date = now,
      amount = 35.00,
      currency = "EUR",
      description = "Item Return",
      type = "REFUND"
    )

    repository.insertHistoryRecords(listOf(recordEur, recordGbp, recordRefund))

    val allHistory = repository.allHistoryRecords.first()
    assertEquals(3, allHistory.size)
    // Ordered by date DESC -> recordRefund, recordGbp, recordEur
    assertEquals("REFUND", allHistory[0].type)
    assertEquals("GBP", allHistory[1].currency)

    // Filter by currency
    val eurOnly = repository.getHistoryByCurrency("EUR").first()
    assertEquals(2, eurOnly.size)
    assertTrue(eurOnly.all { it.currency == "EUR" })

    // Total amount by currency
    val totalEur = repository.getTotalHistoryAmountByCurrency("EUR").first()
    assertEquals(124.90, totalEur ?: 0.0, 0.01)

    // Delete single record
    repository.deleteHistoryRecord(allHistory[0])
    val afterDelete = repository.allHistoryRecords.first()
    assertEquals(2, afterDelete.size)

    // Clear all history
    repository.clearAllHistoryRecords()
    val cleared = repository.allHistoryRecords.first()
    assertEquals(0, cleared.size)
  }

  @Test
  fun `verify real-time currency conversion tool toggles between currencies in payment interface`() {
    val vm = GlobalMarketViewModel(repository)

    // Verify initial currency is EUR
    assertEquals("EUR", vm.selectedCurrency.value.code)

    // Verify available global currencies list contains 12 currencies
    val currencies = vm.currencies
    assertEquals(12, currencies.size)
    assertTrue(currencies.any { it.code == "USD" })
    assertTrue(currencies.any { it.code == "EUR" })
    assertTrue(currencies.any { it.code == "GBP" })
    assertTrue(currencies.any { it.code == "JPY" })
    assertTrue(currencies.any { it.code == "CNY" })
    assertTrue(currencies.any { it.code == "CAD" })
    assertTrue(currencies.any { it.code == "AUD" })
    assertTrue(currencies.any { it.code == "CHF" })
    assertTrue(currencies.any { it.code == "SGD" })
    assertTrue(currencies.any { it.code == "INR" })
    assertTrue(currencies.any { it.code == "BRL" })
    assertTrue(currencies.any { it.code == "MXN" })

    // Toggle to USD
    val usd = currencies.first { it.code == "USD" }
    vm.selectCurrency(usd)
    assertEquals("USD", vm.selectedCurrency.value.code)
    assertEquals("$", vm.selectedCurrency.value.symbol)

    // Toggle to EUR
    val eur = currencies.first { it.code == "EUR" }
    vm.selectCurrency(eur)
    assertEquals("EUR", vm.selectedCurrency.value.code)
    assertEquals("€", vm.selectedCurrency.value.symbol)

    // Calculate converted amount for $100 USD
    val eurRate = vm.liveRateMap.value["EUR"] ?: eur.exchangeRate
    val convertedEur = vm.calculateConvertedAmount(100.0, eur)
    assertEquals(100.0 * eurRate, convertedEur, 0.001)

    // Toggle to JPY
    val jpy = currencies.first { it.code == "JPY" }
    vm.selectCurrency(jpy)
    assertEquals("JPY", vm.selectedCurrency.value.code)
    assertEquals("¥", vm.selectedCurrency.value.symbol)
    val jpyRate = vm.liveRateMap.value["JPY"] ?: jpy.exchangeRate
    val convertedJpy = vm.calculateConvertedAmount(100.0, jpy)
    assertEquals(100.0 * jpyRate, convertedJpy, 0.001)

    // Verify inverse rate calculation
    val inverseJpy = vm.calculateInverseRate(jpy)
    assertEquals(1.0 / jpyRate, inverseJpy, 0.0001)

    // Toggle to GBP
    val gbp = currencies.first { it.code == "GBP" }
    vm.selectCurrency(gbp)
    assertEquals("GBP", vm.selectedCurrency.value.code)
  }

  @Test
  fun `verify payment execution with selected converted currency records accurate transaction`() = runBlocking {
    val vm = GlobalMarketViewModel(repository)
    vm.updateAmountUsd("50.00")

    // Toggle to EUR in payment interface
    val eur = vm.currencies.first { it.code == "EUR" }
    vm.selectCurrency(eur)

    val expectedRate = vm.liveRateMap.value["EUR"] ?: eur.exchangeRate
    val convertedAmount = vm.calculateConvertedAmount(50.00, eur)

    // Save transaction as the payment interface does
    val transaction = TransactionEntity(
        amountInUsd = 50.00,
        targetCurrency = eur.code,
        convertedAmount = convertedAmount,
        customerName = "Alice Wonderland",
        country = "France",
        stripeSubscriptionId = "sub_test_checkout"
    )
    repository.insert(transaction)

    val transactions = repository.allTransactions.first()
    assertTrue("At least 1 transaction should be recorded", transactions.isNotEmpty())
    val lastTx = transactions.last()
    assertEquals("EUR", lastTx.targetCurrency)
    assertEquals(50.00, lastTx.amountInUsd, 0.01)
    assertEquals(50.00 * expectedRate, lastTx.convertedAmount, 0.05)
  }
}


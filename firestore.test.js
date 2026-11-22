const { test, before, after, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const { initializeTestEnvironment, assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
const fs = require('node:fs');

const PROJECT_ID = 'demo-no-project';

let testEnv;

before(async () => {
  const rules = fs.readFileSync('firestore.rules', 'utf8');
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      host: '127.0.0.1',
      port: 8085,
      rules: rules,
    },
  });
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

test('Unauthenticated user cannot read or write user profiles, payouts, or logs', async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthedDb.collection('users').doc('alice').get());
  await assertFails(unauthedDb.collection('users').doc('alice').collection('payouts').get());
  await assertFails(unauthedDb.collection('users').doc('alice').collection('transaction_logs').get());
  await assertFails(
    unauthedDb.collection('users').doc('alice').set({
      userId: 'alice',
      fullName: 'Alice Test',
      email: 'alice@test.com',
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test('Authenticated owner can create, read, and partially update their own profile while immutable fields remain protected', async () => {
  const aliceDb = testEnv.authenticatedContext('alice').firestore();
  const created = new Date(Date.now() - 5000);
  await assertSucceeds(
    aliceDb.collection('users').doc('alice').set({
      userId: 'alice',
      fullName: 'Alice Merchant',
      email: 'alice@example.com',
      role: 'OWNER',
      organization: 'Alice Global Store',
      orgId: 'ORG-100',
      country: 'USA',
      currency: 'USD',
      createdAt: created,
      updatedAt: created,
    })
  );

  const doc = await assertSucceeds(aliceDb.collection('users').doc('alice').get());
  assert.equal(doc.data().fullName, 'Alice Merchant');

  // Partial update of allowed profile fields succeeds
  await assertSucceeds(
    aliceDb.collection('users').doc('alice').update({
      fullName: 'Alice M. Updated',
      organization: 'Alice Enterprise',
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Listing /users collection is blocked to prevent enumeration
  await assertFails(aliceDb.collection('users').get());

  // Ghost fields / shadow updates are rejected
  await assertFails(
    aliceDb.collection('users').doc('alice').update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Mutating immutable createdAt or userId is rejected
  await assertFails(
    aliceDb.collection('users').doc('alice').update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Future timestamps are rejected
  await assertFails(
    aliceDb.collection('users').doc('alice').update({
      fullName: 'Future Alice',
      updatedAt: new Date(Date.now() + 3600000),
    })
  );
});

test('Authenticated user cannot read or write another user profile or subcollections', async () => {
  const aliceDb = testEnv.authenticatedContext('alice').firestore();
  const bobDb = testEnv.authenticatedContext('bob').firestore();
  const created = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection('users').doc('alice').set({
      userId: 'alice',
      fullName: 'Alice Merchant',
      email: 'alice@example.com',
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(bobDb.collection('users').doc('alice').get());
  await assertFails(bobDb.collection('users').doc('alice').collection('payouts').get());
  await assertFails(bobDb.collection('users').doc('alice').collection('transaction_logs').get());
  await assertFails(
    bobDb.collection('users').doc('alice').set({
      userId: 'alice',
      fullName: 'Malicious Bob',
      email: 'bob@example.com',
      createdAt: created,
      updatedAt: created,
    })
  );
});

test('Owner can create, update, and read payout settings and immutable audit payouts', async () => {
  const aliceDb = testEnv.authenticatedContext('alice').firestore();
  const created = new Date(Date.now() - 5000);

  // Create payout setting
  const settingRef = aliceDb.collection('users').doc('alice').collection('payout_settings').doc('active');
  await assertSucceeds(
    settingRef.set({
      userId: 'alice',
      selectedMethod: 'BANK_WIRE',
      bankName: 'JPMorgan Chase',
      accountHolderName: 'Alice Merchant',
      accountNumberOrIban: 'US1234567890',
      swiftBic: 'CHASUS33',
      routingNumber: '021000021',
      createdAt: created,
      updatedAt: created,
    })
  );

  // Update payout setting without overwriting createdAt
  await assertSucceeds(
    settingRef.update({
      selectedMethod: 'STRIPE_PAYOUT',
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Oversized bankName fails bounds check
  await assertFails(
    settingRef.update({
      bankName: 'B'.repeat(150),
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Record a payout disbursement
  await assertSucceeds(
    aliceDb.collection('users').doc('alice').collection('payouts').doc('payout_001').set({
      userId: 'alice',
      payoutId: 'payout_001',
      method: 'BANK_WIRE',
      amount: 1500.0,
      fee: 15.0,
      netAmount: 1485.0,
      currency: 'USD',
      recipientName: 'Alice Merchant',
      reference: 'WIRE-2026-001',
      status: 'SETTLED',
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Owner can list their own payouts
  await assertSucceeds(aliceDb.collection('users').doc('alice').collection('payouts').get());

  // Payout records cannot be deleted (audit trail invariant)
  await assertFails(
    aliceDb.collection('users').doc('alice').collection('payouts').doc('payout_001').delete()
  );
});

test('Owner can create Stripe payout and update status in real time while immutable fields remain protected', async () => {
  const aliceDb = testEnv.authenticatedContext('alice').firestore();
  const bobDb = testEnv.authenticatedContext('bob').firestore();
  const created = new Date(Date.now() - 5000);

  // Create STRIPE_PAYOUT record in PROCESSING state
  const payoutRef = aliceDb.collection('users').doc('alice').collection('payouts').doc('po_live_001');
  await assertSucceeds(
    payoutRef.set({
      userId: 'alice',
      payoutId: 'po_live_001',
      method: 'STRIPE_PAYOUT',
      amount: 420.0,
      fee: 4.2,
      netAmount: 415.8,
      currency: 'EUR',
      recipientName: 'Alice Merchant',
      destinationSummary: 'ba_1INGNL01',
      reference: 'po_live_001',
      status: 'PROCESSING',
      createdAt: created,
      updatedAt: created,
    })
  );

  // Owner can update status -> SETTLED / CANCELED / FAILED
  await assertSucceeds(
    payoutRef.update({
      status: 'SETTLED',
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Invalid status is rejected
  await assertFails(
    payoutRef.update({
      status: 'ARBITRARY_STATUS',
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Tampering with immutable createdAt or amount is rejected
  await assertFails(
    payoutRef.update({
      status: 'PAID',
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertFails(
    payoutRef.update({
      amount: 999999.0,
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Cross-user read and update are rejected
  await assertFails(
    bobDb.collection('users').doc('alice').collection('payouts').doc('po_live_001').get()
  );
  await assertFails(
    bobDb.collection('users').doc('alice').collection('payouts').doc('po_live_001').update({
      status: 'CANCELED',
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test('Owner can create, list, and delete their own transaction logs while cross-user and malformed payloads fail', async () => {
  const aliceDb = testEnv.authenticatedContext('alice').firestore();
  const bobDb = testEnv.authenticatedContext('bob').firestore();
  const created = new Date(Date.now() - 2000);

  const logRef = aliceDb.collection('users').doc('alice').collection('transaction_logs').doc('log_tx_1001');
  await assertSucceeds(
    logRef.set({
      userId: 'alice',
      logId: 'log_tx_1001',
      logType: 'PAYMENT',
      title: 'Stripe SDK Card Checkout (USD)',
      amount: 49.99,
      currencyCode: 'USD',
      referenceCode: 'STRP-1001',
      customerOrRecipient: 'Jane Customer',
      status: 'SETTLED',
      createdAt: created,
    })
  );

  await assertSucceeds(aliceDb.collection('users').doc('alice').collection('transaction_logs').get());
  await assertSucceeds(logRef.update({ status: 'REFUNDED' }));

  // Negative amount, mismatched logId, or cross-user read fail
  await assertFails(
    aliceDb.collection('users').doc('alice').collection('transaction_logs').doc('log_bad').set({
      userId: 'alice',
      logId: 'other_id',
      logType: 'PAYMENT',
      title: 'Bad Log',
      amount: -10.0,
      currencyCode: 'USD',
      referenceCode: 'BAD-1',
      createdAt: created,
    })
  );
  await assertFails(
    bobDb.collection('users').doc('alice').collection('transaction_logs').doc('log_tx_1001').get()
  );
  await assertSucceeds(logRef.delete());
});

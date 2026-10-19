import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import fs from 'node:fs';

const PROJECT_ID = 'demo-no-project';
const DATABASE_ID = 'ai-studio-android-globalma-23676576-cf60-473f-867b-83a68e2777b3';

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

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

test('Unauthenticated user cannot read or write user profiles', async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthedDb.collection('users').doc('alice').get());
  await assertFails(
    unauthedDb.collection('users').doc('alice').set({
      userId: 'alice',
      fullName: 'Alice Test',
      email: 'alice@test.com',
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test('Authenticated owner can read and write their own profile with valid schema', async () => {
  const aliceDb = testEnv.authenticatedContext('alice').firestore();
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
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  const doc = await assertSucceeds(aliceDb.collection('users').doc('alice').get());
  assert.equal(doc.data().fullName, 'Alice Merchant');
});

test('Authenticated user cannot read or write another user profile', async () => {
  const bobDb = testEnv.authenticatedContext('bob').firestore();
  await assertFails(bobDb.collection('users').doc('alice').get());
  await assertFails(
    bobDb.collection('users').doc('alice').set({
      userId: 'alice',
      fullName: 'Malicious Bob',
      email: 'bob@example.com',
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test('Owner can create and read payout settings and disbursements', async () => {
  const aliceDb = testEnv.authenticatedContext('alice').firestore();

  // Create payout setting
  await assertSucceeds(
    aliceDb.collection('users').doc('alice').collection('payout_settings').doc('active').set({
      userId: 'alice',
      selectedMethod: 'BANK_WIRE',
      bankName: 'JPMorgan Chase',
      accountHolderName: 'Alice Merchant',
      accountNumberOrIban: 'US1234567890',
      swiftBic: 'CHASUS33',
      routingNumber: '021000021',
      createdAt: new Date(),
      updatedAt: new Date(),
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
      createdAt: new Date(),
    })
  );

  // Payout records cannot be deleted (audit trail invariant)
  await assertFails(
    aliceDb.collection('users').doc('alice').collection('payouts').doc('payout_001').delete()
  );
});

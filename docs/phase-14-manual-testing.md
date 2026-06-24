# Phase 14 Manual API Testing Report

Run date: 2026-06-17

Branch: `feature/phase-14-final-testing`

Status: Completed

## Environment

- Backend started successfully with variables loaded from `.env`.
- MongoDB connection through `MONGODB_URI` worked.
- Health check returned `UP`.
- No secrets were written to this report.

## Passed Checks

- Login as OWNER succeeded.
- Created property.
- Created room.
- Created tenant.
- Created contract.
- Verified room status changed to `OCCUPIED`.
- Verified tenant `currentRoomId` was updated.
- Configured service prices.
- Created meter reading.
- Generated invoice.
- Verified invoice `totalAmount = 3035000`.
- Dashboard summary returned live data.
- Dashboard revenue returned live data.
- Dashboard room status returned live data.
- Created maintenance request.
- Verified dashboard pending maintenance count.

## Earlier Blocked Check

- PayOS payment link creation failed.

PayOS response:

```txt
PayOS create payment link failed: Cong thanh toan khong ton tai hoac da tam dung, vui long chon cong khac
```

Meaning: the PayOS payment gateway for the configured credential appears to be missing, inactive, or suspended.

Because no PayOS payment link was created, the verified webhook flow could not be completed through the normal API flow.

## Test Data Created

```txt
propertyId=6a327e13db5499101d363b15
roomId=6a327e13db5499101d363b16
tenantId=6a327e13db5499101d363b17
contractId=6a327e14db5499101d363b18
meterReadingId=6a327e15db5499101d363b1a
invoiceId=6a327e15db5499101d363b1b
maintenanceId=6a327e16db5499101d363b1c
```

## Earlier Next Required Action

Update or activate the PayOS gateway credentials in `.env`, then rerun Phase 14 from the PayOS payment link step.

Do not mark Phase 14 as complete until:

- `POST /api/invoices/{invoiceId}/payment-link` succeeds.
- `POST /api/webhooks/payos` with a verified signature marks the payment as `PAID`.
- The related invoice becomes `PAID`.

## Retest: 2026-06-17 18:13

After updating PayOS keys in `.env`, the backend was restarted and Phase 14 was run again with new test data.

Passed again:

- Health check.
- OWNER login.
- Property creation.
- Room creation.
- Tenant creation.
- Contract creation.
- Room `OCCUPIED` verification.
- Tenant `currentRoomId` verification.
- Service price configuration.
- Meter reading creation.
- Invoice generation.
- Invoice total verification.

Retest data:

```txt
propertyId=6a32813180f94c540bc3df30
roomId=6a32813180f94c540bc3df31
tenantId=6a32813180f94c540bc3df32
contractId=6a32813280f94c540bc3df33
meterReadingId=6a32813380f94c540bc3df35
invoiceId=6a32813380f94c540bc3df36
```

PayOS payment link creation still failed with the same gateway response:

```txt
PayOS create payment link failed: Cong thanh toan khong ton tai hoac da tam dung, vui long chon cong khac
```

Conclusion: `.env` is being loaded and the backend can reach PayOS, but the configured PayOS payment gateway/credential is still not usable for creating payment links.

## Final Retest: 2026-06-17 18:37

After the saved `.env` file was confirmed to contain real PayOS keys, the backend was restarted and Phase 14 was run again.

All required checks passed:

- Login as OWNER.
- Create property.
- Create room.
- Create tenant.
- Create contract.
- Verify room becomes `OCCUPIED`.
- Verify tenant `currentRoomId` is updated.
- Configure service prices.
- Create meter reading.
- Generate invoice.
- Verify invoice `totalAmount = 3035000`.
- Create PayOS payment link through backend API.
- Trigger verified PayOS webhook.
- Verify payment becomes `PAID`.
- Verify invoice becomes `PAID`.
- Verify dashboard summary, revenue, and room status values.
- Create maintenance request.
- Verify dashboard pending maintenance count.

Final test data:

```txt
propertyId=6a32870059c2560b63524aa9
roomId=6a32870059c2560b63524aaa
tenantId=6a32870059c2560b63524aab
contractId=6a32870059c2560b63524aac
meterReadingId=6a32870159c2560b63524aae
invoiceId=6a32870159c2560b63524aaf
paymentId=6a32870259c2560b63524ab0
orderCode=1781696257913170
maintenanceId=6a32870459c2560b63524ab1
```

Final dashboard check:

```txt
totalRooms=3
unpaidInvoices=2
expectedRevenue=9105000
paidRevenue=3035000
occupiedRooms=3
pendingMaintenanceRequests=2
```

Conclusion: Phase 14 manual API testing is complete.

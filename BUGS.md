# Bug Audit Report — EU Auction Platform

Identified: 2026-02-24
Fixed: 2026-02-24

**Status: 20/21 bugs fixed. BUG-10 was a false positive (code is correct).**

---

## CRITICAL — Will cause incorrect behavior in production

### BUG-01: NATS Event Type / Subject Mismatches (inter-service messaging broken)

Three event types don't match their NATS subject constants. Events published by one service will never reach consumers in other services because the subject used for publishing differs from what consumers subscribe to.

**Files to fix:**
- `shared/nats-events/src/main/kotlin/eu/auctionplatform/events/auction/ProxyBidTriggeredEvent.kt` line 25 — `eventType = "auction.bid.proxy_triggered"` but `NatsSubjects.AUCTION_BID_PROXY = "auction.bid.proxy"`
- `shared/nats-events/src/main/kotlin/eu/auctionplatform/events/auction/AuctionClosedEvent.kt` line 26 — `eventType = "auction.closed"` but `NatsSubjects.AUCTION_LOT_CLOSED = "auction.lot.closed"`
- `shared/nats-events/src/main/kotlin/eu/auctionplatform/events/auction/AuctionExtendedEvent.kt` line 24 — `eventType = "auction.extended"` but `NatsSubjects.AUCTION_LOT_EXTENDED = "auction.lot.extended"`

**Reference:** `shared/kotlin-commons/src/main/kotlin/eu/auctionplatform/commons/messaging/NatsSubjects.kt` lines 14-17

**Fix:** Align each event's `eventType` string with its corresponding `NatsSubjects` constant. The NatsSubjects constants follow the `<domain>.<entity>.<verb>` convention, so the events should adopt those values:
- `ProxyBidTriggeredEvent.eventType` → `"auction.bid.proxy"`
- `AuctionClosedEvent.eventType` → `"auction.lot.closed"`
- `AuctionExtendedEvent.eventType` → `"auction.lot.extended"`

---

### BUG-02: Settlement uses auctionId as sellerId (wrong seller gets paid)

**File:** `services/payment-service/src/main/kotlin/eu/auctionplatform/payment/application/service/SettlementService.kt` lines 198-202

```kotlin
private fun resolveSellerId(payment: Payment): UUID {
    return payment.auctionId  // BUG: auctionId != sellerId
}
```

**Fix:** The Payment model should already carry a `sellerId`. Check the Payment entity and its DB table. If `sellerId` exists, use `payment.sellerId`. If not, add it to the Payment model, the DB migration, and populate it during checkout from lot data.

---

### BUG-03: Notification FAILED status never written (stuck PENDING forever)

**File:** `services/notification-service/src/main/kotlin/eu/auctionplatform/notification/application/service/NotificationService.kt` lines 161-168

The comment on line 166 says "update to FAILED" but there is no code to do it.

**Fix:** Add the missing status update inside the catch block. Handle the case where notification might not have been inserted yet (if the insert itself failed).

---

### BUG-04: GDPR erasure marked COMPLETED before downstream services process it

**File:** `services/compliance-service/src/main/kotlin/eu/auctionplatform/compliance/application/service/GdprService.kt` lines 148-151

The request transitions PENDING → IN_PROGRESS → COMPLETED in a single synchronous call.

**Fix:** Remove the immediate COMPLETED update on line 151. Leave status as IN_PROGRESS after publishing. Add a comment explaining that completion should be tracked via downstream acknowledgment events or a timeout-based job.

---

### BUG-05: CO2 Resource overwrites categoryId with random UUID on update

**File:** `services/co2-service/src/main/kotlin/eu/auctionplatform/co2/api/v1/resource/Co2Resource.kt` line 174

```kotlin
categoryId = UUID.randomUUID(), // categoryId is not updatable; preserved by the repository
```

**Fix:** Fetch the existing emission factor first and use its categoryId.

---

### BUG-06: Payment completion not transactional (invoice/settlement can fail independently)

**File:** `services/payment-service/src/main/kotlin/eu/auctionplatform/payment/application/service/CheckoutService.kt` lines 223-239

Three independent operations with no error handling between them.

**Fix:** Add try-catch around invoice generation and settlement creation. If either fails, log the error but don't leave the system in an inconsistent state. At minimum, catch exceptions on steps 2 and 3 so a failure in one doesn't prevent the other from executing.

---

## HIGH — Significant logic errors

### BUG-07: co2_lot_seller_mapping table missing `lot_id` column

**File:** `services/co2-service/src/main/resources/db/migration/V001__create_co2_tables.sql` lines 47-51

The table has no `lot_id` column. The UNIQUE constraint on `id` is redundant with PRIMARY KEY.

**Fix:** Create a new migration `V002__fix_lot_seller_mapping.sql` that adds `lot_id UUID NOT NULL`, drops the redundant constraint, and adds `UNIQUE (lot_id, seller_id)`.

---

### BUG-08: Deposit.isActive() returns true when refund is pending

**File:** `services/user-service/src/main/kotlin/eu/auctionplatform/user/domain/model/Deposit.kt` line 36

```kotlin
fun isActive(): Boolean = paidAt != null && refundedAt == null
```

**Fix:** `fun isActive(): Boolean = paidAt != null && refundRequestedAt == null && refundedAt == null`

---

### BUG-09: Catalog LotService returns page size as total count for country-filtered queries

**File:** `services/catalog-service/src/main/kotlin/eu/auctionplatform/catalog/application/service/LotService.kt` lines 313-315

`lots.size.toLong()` returns page size, not total count.

**Fix:** Add `countByCountry()` to the repository. Same issue exists for `findBySellerId()` and `findByAuctionId()` — they load ALL results without pagination.

---

### BUG-10: Auction proxy bid increment calculated from wrong base amount

**File:** `services/auction-engine/src/main/kotlin/eu/auctionplatform/auction/domain/model/Auction.kt` line 571

Increment is based on `secondHighest.maxAmount` instead of the current high bid amount.

**Fix:** Use the current high bid amount as the base for increment calculation, not the second auto-bidder's ceiling.

---

### BUG-11: Missing await on async auto-bid cancellation (frontend)

**File:** `frontend/buyer-web/components/auction/BidPanel.vue` lines 329-334

`toggleAutoBid()` is not async and doesn't await `handleCancelAutoBid()`.

**Fix:** Make `toggleAutoBid` async and await the cancellation call.

---

### BUG-12: Missing payment status guard in webhook handler

**File:** `services/payment-service/src/main/kotlin/eu/auctionplatform/payment/application/service/CheckoutService.kt` lines 223-231

No check that `payment.status == PROCESSING` before marking COMPLETED.

**Fix:** Add a status guard at the top of `handleAuthorisation` to ignore duplicate/replayed webhooks.

---

## MEDIUM — Data integrity and UX issues

### BUG-13: Outbox `created_at` allows NULL

**File:** `services/auction-engine/src/main/resources/db/migration/V002__create_outbox.sql` line 18

**Fix:** Create new migration to add NOT NULL constraint.

---

### BUG-14: Media images `original_url` and `processed_url` are nullable

**File:** `services/media-service/src/main/resources/db/migration/V001__create_images.sql` lines 18-19

**Fix:** `original_url` should be NOT NULL. `processed_url` can stay nullable if async processing happens.

---

### BUG-15: AML screening sorted NULLS FIRST (pending before completed)

**File:** `services/compliance-service/src/main/kotlin/eu/auctionplatform/compliance/infrastructure/persistence/repository/AmlScreeningRepository.kt` line 42

**Fix:** Change to `NULLS LAST`.

---

### BUG-16: Content reports sorted inconsistently

**File:** `services/compliance-service/src/main/kotlin/eu/auctionplatform/compliance/infrastructure/persistence/repository/ContentReportRepository.kt`
- Line 42: `SELECT_BY_STATUS_PAGED` sorts `created_at ASC`
- Line 52: `SELECT_ALL_PAGED` sorts `created_at DESC`

**Fix:** Use `DESC` consistently.

---

### BUG-17: Seller analytics defaults missing `created_at` to `Instant.now()`

**File:** `services/seller-service/src/main/kotlin/eu/auctionplatform/seller/application/service/SellerService.kt` line 229

**Fix:** Log a warning instead of silently substituting. Or throw if created_at should never be null.

---

### BUG-18: Broker bulk lot intake silently accepts invalid leadIds

**File:** `services/broker-service/src/main/kotlin/eu/auctionplatform/broker/application/service/BrokerService.kt` lines 138-145

**Fix:** Validate all leadIds exist before processing. Throw a `ValidationException` listing any invalid IDs.

---

### BUG-19: Profile page form populated before async user data loads

**File:** `frontend/buyer-web/pages/profile/index.vue` lines 279-286

**Fix:** Use a `watch` on `user` to populate the form when data arrives, with `{ immediate: true }`.

---

### BUG-20: Search pagination ellipsis incorrectly placed

**File:** `frontend/buyer-web/pages/search.vue` lines 221-226

**Fix:** Only show "..." when there's a gap of more than 1 page.

---

### BUG-21: Missing NATS subject constants for 4 event types

**File:** `shared/kotlin-commons/src/main/kotlin/eu/auctionplatform/commons/messaging/NatsSubjects.kt`

**Fix:** Add:
```kotlin
const val AUCTION_BID_REJECTED: String     = "auction.bid.rejected"
const val AUCTION_DEPOSIT_REQUIRED: String = "auction.deposit.required"
const val AUCTION_RESERVE_MET: String      = "auction.reserve.met"
const val CATALOG_LOT_STATUS_CHANGED: String = "catalog.lot.status_changed"
```

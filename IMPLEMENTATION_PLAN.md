# Implementation Plan: Fix All 18 Integration Gaps

## Context

The EU auction platform has 13 microservices and 3 frontends. Happy-path testing passes (31/33 PASS), but under the hood, a **critical NATS routing bug** silently drops all auction events, breaking the entire event-driven architecture. This cascades into 17 additional gaps: empty user bid/purchase history, broken seller-service sync, dead payment lifecycle, missing email templates, and stubbed analytics.

**Root cause:** `NatsSubjects.withBrand()` prefixes subjects with brand (e.g. `troostwijk.auction.bid.placed`), but streams only capture unprefixed patterns (`auction.>`). ALL events from auction-engine and catalog-service are silently lost.

### Decisions

1. **NATS subjects** -- Remove brand prefix from subjects; put brand in NATS message headers instead
2. **Payment PSP** -- Realistic simulation (no Adyen SDK); resolve real lot/auction data, compute real VAT/premiums, simulate PSP response, trigger full NATS event chain
3. **User history** -- NATS event projections; add consumers to user-service that build local bid/purchase tables from auction events
4. **Scope** -- Full implementation of all 18 gaps

### Summary

| Phase | Gaps | New Files | Modified Files | Est. LOC |
|-------|------|-----------|----------------|----------|
| 1 -- NATS Foundation | 1, 17 | 2 | 5 | ~250 |
| 2 -- Core Consumers | 2, 3, 5, 7 | 6 | 5 | ~800 |
| 3 -- Payment Flow | 4, 13, 14 | 4 | 4 | ~600 |
| 4 -- Notifications | 6, 15, 12 | 13 | 4 | ~700 |
| 5 -- Remaining | 8, 9, 10, 11, 16, 18 | 1 | 8 | ~400 |
| **Total** | **18** | **26** | **26** | **~2,750** |

---

## Phase 1: NATS Routing Foundation

*Must be done first -- everything depends on events reaching consumers.*

### Gap 1: Fix NATS Brand-Prefix Routing

**Problem:** Auction-engine publishes to `{brand}.auction.bid.placed` via `NatsSubjects.withBrand()`. The AUCTION stream only captures `auction.>`. ALL auction events are lost. Similarly, catalog-service appends brand as 4th segment: `catalog.lot.created.troostwijk`.

**Fix:**

#### auction-engine -- `BidService.kt` (line 290)
```kotlin
// BEFORE:
return NatsSubjects.withBrand(baseSubject, event.brand)
// AFTER:
return baseSubject
```
Brand header is already added by `OutboxPublisher` at line 110: `headers.add("brand", jsonNode.path("brand").asText(""))`.

**File:** `services/auction-engine/src/main/kotlin/eu/auctionplatform/auction/application/service/BidService.kt`

#### auction-engine -- `AuctionLifecycleService.kt` (line 338)
```kotlin
// BEFORE:
return NatsSubjects.withBrand(baseSubject, event.brand)
// AFTER:
return baseSubject
```

**File:** `services/auction-engine/src/main/kotlin/eu/auctionplatform/auction/application/service/AuctionLifecycleService.kt`

#### catalog-service -- `LotService.kt` (line 64)
```kotlin
// BEFORE:
val subject = "catalog.lot.$eventType.${lot.brand}"
// AFTER:
val subject = "catalog.lot.$eventType"
```
Also add brand header to NATS message (line 82 currently publishes bare bytes without headers):
```kotlin
val headers = io.nats.client.impl.NatsMessage.builder()
    .subject(subject)
    .data(json.toByteArray(Charsets.UTF_8))
    .headers(Headers().add("brand", lot.brand))
    .build()
natsConnection.jetStream().publish(headers)
```

**File:** `services/catalog-service/src/main/kotlin/eu/auctionplatform/catalog/application/service/LotService.kt`

#### shared -- `NatsSubjects.kt`
Deprecate `withBrand()`:
```kotlin
@Deprecated("Brand should be sent as NATS header, not as subject prefix", level = DeprecationLevel.WARNING)
fun withBrand(subject: String, brand: String): String = "$brand.$subject"
```

**File:** `shared/kotlin-commons/src/main/kotlin/eu/auctionplatform/commons/messaging/NatsSubjects.kt`

#### notification-service -- `AuctionEventNotificationConsumer.kt` (lines 46-48)
After removing brand prefix, bare subjects like `auction.bid.placed` won't match `auction.bid.placed.>`. Fix filters:
```kotlin
// BEFORE:
private const val BID_PLACED_FILTER = "auction.bid.placed.>"
private const val BID_PROXY_FILTER = "auction.bid.proxy.>"
private const val LOT_CLOSED_FILTER = "auction.lot.closed.>"
// AFTER:
private const val BID_PLACED_FILTER = "auction.bid.placed"
private const val BID_PROXY_FILTER = "auction.bid.proxy"
private const val LOT_CLOSED_FILTER = "auction.lot.closed"
```

**File:** `services/notification-service/src/main/kotlin/eu/auctionplatform/notification/infrastructure/nats/AuctionEventNotificationConsumer.kt`

---

### Gap 17: User-Service Event Publishing

**Problem:** User-service never publishes `UserRegisteredEvent` or `KycVerifiedEvent` to NATS. Analytics user-growth counters, notification welcome emails, and compliance consumers never fire.

#### New: `V003__add_outbox.sql`
```sql
CREATE TABLE IF NOT EXISTS app.outbox (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type TEXT NOT NULL,
    payload TEXT NOT NULL,
    nats_subject TEXT NOT NULL,
    published BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    published_at TIMESTAMPTZ,
    retry_count INT DEFAULT 0,
    dead_letter BOOLEAN DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_outbox_pending
    ON app.outbox (created_at) WHERE published = FALSE AND dead_letter = FALSE;
```

**File:** `services/user-service/src/main/resources/db/migration/V003__add_outbox.sql`

#### New: `UserOutboxPoller.kt`
Extend `OutboxPoller` from `shared/kotlin-commons/src/main/kotlin/eu/auctionplatform/commons/outbox/OutboxPoller.kt`:
- `tableName = "app.outbox"`, `batchSize = 50`
- Implement `mapRow()` and `publishEntry()` (build `NatsMessage` with headers, publish via JetStream)
- `@Scheduled(every = "5s")` on `poll()` override

**File:** `services/user-service/src/main/kotlin/eu/auctionplatform/user/infrastructure/nats/UserOutboxPoller.kt`

#### Modified: `UserService.kt`
After `registerUser()` and `getOrCreateUser()` persist calls, write outbox entry:
```kotlin
dataSource.connection.use { conn ->
    conn.prepareStatement("""
        INSERT INTO app.outbox (aggregate_id, event_type, payload, nats_subject)
        VALUES (?, 'UserRegisteredEvent', ?::text, 'user.registered')
    """).use { stmt ->
        stmt.setObject(1, user.id)
        stmt.setString(2, JsonMapper.toJson(mapOf(
            "userId" to user.id.toString(),
            "email" to user.email,
            "firstName" to user.firstName,
            "lastName" to user.lastName,
            "accountType" to user.accountType.name
        )))
        stmt.executeUpdate()
    }
}
```

**File:** `services/user-service/src/main/kotlin/eu/auctionplatform/user/application/service/UserService.kt`

---

## Phase 2: Core Event Consumers

*With routing fixed, wire up the consumers that build read models from events.*

### Gap 2: Analytics Service -- Rewrite Consumer

**Problem:** `AnalyticsEventConsumer` subscribes to stream `"ANALYTICS"` which doesn't exist in `NatsStreamInitializer` (only 8 streams: AUCTION, CATALOG, PAYMENT, USER, MEDIA, NOTIFY, COMPLIANCE, CO2). Consumer crashes with `IllegalStateException` after 10 retry attempts.

#### Rewrite: `AnalyticsEventConsumer.kt`
Create 3 inner `NatsConsumer` instances instead of subscribing to non-existent ANALYTICS stream:

| Inner Consumer | Stream | Filter | Durable Name | Handles |
|----------------|--------|--------|-------------|---------|
| auctionConsumer | `AUCTION` | `auction.>` | `analytics-auction-consumer` | bid.placed, lot.extended, lot.closed |
| paymentConsumer | `PAYMENT` | `payment.>` | `analytics-payment-consumer` | checkout.completed |
| userConsumer | `USER` | `user.>` | `analytics-user-consumer` | user.registered |

Keep existing handler methods but fix:
- `handleUserRegistered()` -- `accountType` is `BUSINESS`/`PRIVATE` (not `BUYER`/`SELLER`). Fix: always increment `new_registrations` regardless of type
- Inject CDI `Connection` instead of constructor parameter

**File:** `services/analytics-service/src/main/kotlin/eu/auctionplatform/analytics/infrastructure/nats/AnalyticsEventConsumer.kt`

#### Fix: `AnalyticsRepository.kt`
`UPSERT_USER_GROWTH` overwrites `new_registrations` on conflict instead of accumulating:
```sql
-- BEFORE:
ON CONFLICT (report_date) DO UPDATE SET
    new_registrations = EXCLUDED.new_registrations, ...
-- AFTER:
ON CONFLICT (report_date) DO UPDATE SET
    new_registrations = app.user_growth.new_registrations + EXCLUDED.new_registrations, ...
```

**File:** `services/analytics-service/src/main/kotlin/eu/auctionplatform/analytics/infrastructure/persistence/repository/AnalyticsRepository.kt`

#### Add: Platform Metrics Scheduler
Add `@Scheduled(every = "5m")` method in `AnalyticsService.kt` to compute and insert `platform_metrics` snapshots (currently the overview endpoint returns all zeros because no row is ever inserted).

**File:** `services/analytics-service/src/main/kotlin/eu/auctionplatform/analytics/application/service/AnalyticsService.kt`

#### New Migration: `V002__add_analytics_tables.sql`
Add `category_metrics` and `bid_volume` tables to populate the stub endpoints.

**File:** `services/analytics-service/src/main/resources/db/migration/V002__add_analytics_tables.sql`

#### Populate Stub Endpoints
`GET /categories` and `GET /bids/daily` currently return hardcoded empty lists. Populate from NATS events via the new tables.

**File:** `services/analytics-service/src/main/kotlin/eu/auctionplatform/analytics/api/v1/resource/AnalyticsResource.kt`

---

### Gap 3: Seller-Service NATS Consumer -- Complete Rewrite

**Problem:** `AuctionEventSellerConsumer.kt` has 6 confirmed bugs:
1. All subscriptions use `STREAM_NAME = "AUCTION"`, but `payment.settlement.ready` and `catalog.lot.created` are on PAYMENT and CATALOG streams
2. Brand-prefixed subjects don't match (fixed by Gap 1)
3. Catalog events published as `catalog.lot.created.{brand}` not `catalog.lot.created` (fixed by Gap 1)
4. Auction events (`BidPlacedEvent`, `AuctionClosedEvent`, `LotAwardedEvent`) lack `sellerId` field -- `extractSellerId()` always returns null
5. `handleSettlementReady()` looks for `data["amount"]` but `SettlementReadyEvent` has `netAmount`
6. Creates own NATS connection via `Nats.connect()` instead of using CDI bean

#### Rewrite as 3 CDI-managed consumers:

| Consumer Class | Stream | Filter | Durable Name | Purpose |
|----------------|--------|--------|-------------|---------|
| `CatalogEventSellerConsumer` | `CATALOG` | `catalog.lot.>` | `seller-catalog-consumer` | Populate `seller_lots` table |
| `AuctionEventSellerConsumer` | `AUCTION` | `auction.>` | `seller-auction-consumer` | Update seller metrics |
| `PaymentEventSellerConsumer` | `PAYMENT` | `payment.>` | `seller-payment-consumer` | Populate `seller_settlements` |

**sellerId resolution pattern:** Auction events don't have `sellerId`. Solution: look up `seller_lots` table by `lotId` from the event:
```kotlin
private fun findSellerIdByLotId(lotId: UUID): UUID? {
    dataSource.connection.use { conn ->
        conn.prepareStatement("SELECT seller_id FROM app.seller_lots WHERE id = ?").use { stmt ->
            stmt.setObject(1, lotId)
            stmt.executeQuery().use { rs ->
                return if (rs.next()) UUID.fromString(rs.getString("seller_id")) else null
            }
        }
    }
}
```

**Files:**
- `services/seller-service/src/main/kotlin/eu/auctionplatform/seller/infrastructure/nats/AuctionEventSellerConsumer.kt` -- complete rewrite
- New: `services/seller-service/src/main/kotlin/eu/auctionplatform/seller/infrastructure/nats/CatalogEventSellerConsumer.kt`
- New: `services/seller-service/src/main/kotlin/eu/auctionplatform/seller/infrastructure/nats/PaymentEventSellerConsumer.kt`
- `services/seller-service/src/main/kotlin/eu/auctionplatform/seller/infrastructure/persistence/repository/SellerProfileRepository.kt` -- add `findSellerIdByLotId()`, fix `settlePayment()` to use `netAmount`

---

### Gap 5: User-Service -- Bid/Purchase/Watchlist Projections

**Problem:** `GET /me/purchases`, `GET /me/bids`, `GET /me/watchlist` return hardcoded `emptyList<Any>()` (lines 244-294 in UserResource.kt). No NATS consumers exist.

#### New: `V004__add_user_activity_tables.sql`
```sql
CREATE TABLE app.user_bids (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    auction_id UUID NOT NULL,
    lot_id UUID,
    amount NUMERIC(15,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'EUR',
    bid_at TIMESTAMPTZ NOT NULL,
    UNIQUE(user_id, auction_id, bid_at)
);
CREATE INDEX idx_user_bids_user ON app.user_bids(user_id);

CREATE TABLE app.user_purchases (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    lot_id UUID NOT NULL,
    auction_id UUID,
    hammer_price NUMERIC(15,2),
    currency VARCHAR(3) DEFAULT 'EUR',
    status VARCHAR(32) DEFAULT 'PENDING_PAYMENT',
    awarded_at TIMESTAMPTZ NOT NULL,
    UNIQUE(user_id, lot_id)
);
CREATE INDEX idx_user_purchases_user ON app.user_purchases(user_id);

CREATE TABLE app.user_watchlist (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    lot_id UUID NOT NULL,
    added_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, lot_id)
);
CREATE INDEX idx_user_watchlist_user ON app.user_watchlist(user_id);
```

**File:** `services/user-service/src/main/resources/db/migration/V004__add_user_activity_tables.sql`

#### New: `AuctionEventUserConsumer.kt`
- `@ApplicationScoped @Startup`, inject CDI `Connection` and `AgroalDataSource`
- Inner NatsConsumer on stream `AUCTION`, filter `auction.>`, durable `user-auction-consumer`
- `auction.bid.placed` -> INSERT into `user_bids` (bidderId, auctionId, lotId, amount, timestamp)
- `auction.lot.awarded` -> INSERT into `user_purchases` (winnerId, lotId, hammerPrice, status=PENDING_PAYMENT)

**File:** `services/user-service/src/main/kotlin/eu/auctionplatform/user/infrastructure/nats/AuctionEventUserConsumer.kt`

#### New: `PaymentEventUserConsumer.kt`
- Inner NatsConsumer on stream `PAYMENT`, filter `payment.checkout.completed`, durable `user-payment-consumer`
- `payment.checkout.completed` -> UPDATE `user_purchases` SET status=PAID WHERE lot_id=? AND user_id=?

**File:** `services/user-service/src/main/kotlin/eu/auctionplatform/user/infrastructure/nats/PaymentEventUserConsumer.kt`

#### Modified: `UserResource.kt`
- Replace 3 stub endpoints with real JDBC queries on new tables
- Add `POST /me/watchlist/{lotId}` and `DELETE /me/watchlist/{lotId}` endpoints
- Inject `AgroalDataSource`

**File:** `services/user-service/src/main/kotlin/eu/auctionplatform/user/api/v1/resource/UserResource.kt`

---

### Gap 7: Media Service -- Fix NATS Publish

**Problem:** `ImageProcessingService.publishProcessedEvent()` (lines 226-242) builds JSON payload but never publishes it.

#### Modified: `ImageProcessingService.kt`
- Inject `NatsPublisher` CDI bean (add to constructor)
- In `publishProcessedEvent()`: construct `ImageProcessedEvent` from nats-events with `originalUrl`, `webpUrl`, `thumbnails`, then call `natsPublisher.publish(NatsSubjects.MEDIA_IMAGE_PROCESSED, event)`

**File:** `services/media-service/src/main/kotlin/eu/auctionplatform/media/application/service/ImageProcessingService.kt`

#### Modified: `ImageUploadConsumer.kt`
- Replace manual `Nats.connect(natsUrl)` (line 65) with CDI-injected `Connection`
- Remove manual connection management

**File:** `services/media-service/src/main/kotlin/eu/auctionplatform/media/infrastructure/nats/ImageUploadConsumer.kt`

---

## Phase 3: Payment Flow

*The full payment lifecycle with NATS integration.*

### Gap 4: Payment Service -- Full Lifecycle Simulation

**Problem:** Payment service has zero NATS integration -- no consumers, no publishers. Uses placeholder data (`hammerPrice = BigDecimal.ZERO`, `sellerId = lotId`) in `PaymentResource.kt:94-107`. Outbox table exists (V004 migration) but no OutboxPoller.

#### New: `LotAwardedConsumer.kt`
- `@ApplicationScoped @Startup`, inject CDI `Connection`, `CheckoutService`
- Inner NatsConsumer on stream `AUCTION`, filter `auction.lot.awarded`, durable `payment-lot-awarded-consumer`
- On `LotAwardedEvent`: extract `winnerId`, `lotId`, `hammerPrice`, `currency`, `buyerPremiumRate`
- Call `checkoutService.initiateCheckout()` with real data from event
- Simulate PSP: call `processPayment()`, then auto-complete with success=true

**File:** `services/payment-service/src/main/kotlin/eu/auctionplatform/payment/infrastructure/nats/LotAwardedConsumer.kt`

#### New: `PaymentOutboxPoller.kt`
- Extend `OutboxPoller` (`shared/kotlin-commons/src/main/kotlin/eu/auctionplatform/commons/outbox/OutboxPoller.kt`)
- `tableName = "app.outbox"`, `batchSize = 50`
- `@Scheduled(every = "5s")` on `poll()`

**File:** `services/payment-service/src/main/kotlin/eu/auctionplatform/payment/infrastructure/nats/PaymentOutboxPoller.kt`

#### Modified: `CheckoutService.kt`
- After `generateInvoices()` + `settlementService.createSettlement()`: write `CheckoutCompletedEvent` to outbox with subject `payment.checkout.completed`
- In `SettlementService.createSettlement()`: write `SettlementReadyEvent` to outbox with subject `payment.settlement.ready`
- `processPayment()`: simulate PSP -- generate fake `pspReference` (e.g. `SIM-{UUID}`), auto-complete

**File:** `services/payment-service/src/main/kotlin/eu/auctionplatform/payment/application/service/CheckoutService.kt`

#### Modified: `PaymentResource.kt`
- Remove `BigDecimal.ZERO` placeholders (lines 94-107). Use real data from request or `LotAwardedEvent`
- Add admin endpoints:
  - `PATCH /payments/{id}/settle` -- trigger settlement processing
  - `POST /payments/{id}/refund` -- trigger refund flow
  - `POST /payments/{id}/reminder` -- queue payment reminder notification
- Fix `GET /invoices` (line 234) -- filter by caller's userId from SecurityContext
- Add enriched response fields for admin dashboard (Gap 11): `lotTitle`, `buyerName`, `sellerName`

**File:** `services/payment-service/src/main/kotlin/eu/auctionplatform/payment/api/v1/resource/PaymentResource.kt`

#### New Migration: `V006__add_payment_names.sql`
```sql
ALTER TABLE app.payments ADD COLUMN IF NOT EXISTS lot_title TEXT;
ALTER TABLE app.payments ADD COLUMN IF NOT EXISTS buyer_name TEXT;
ALTER TABLE app.payments ADD COLUMN IF NOT EXISTS seller_name TEXT;
```
Populate when creating payment from `LotAwardedEvent` data.

**File:** `services/payment-service/src/main/resources/db/migration/V006__add_payment_names.sql`

---

### Gap 13: NonPaymentService Events

**Problem:** `blockBuyer()` and `requestLotRelist()` have TODO stubs (lines 179, 196).

#### Modified: `NonPaymentService.kt`
Write events to outbox instead of TODO logging:
```kotlin
// In blockBuyer():
writeOutboxEntry("NonPaymentPenaltyEvent", "payment.non-payment.penalty", mapOf(
    "buyerId" to buyerId.toString(),
    "paymentId" to paymentId.toString(),
    "forfeitAmount" to forfeitAmount.toString()
))

// In requestLotRelist():
writeOutboxEntry("LotRelistRequestedEvent", "payment.lot.relist-requested", mapOf(
    "lotId" to lotId.toString(),
    "auctionId" to auctionId.toString(),
    "paymentId" to paymentId.toString()
))
```

Add new subjects to `NatsSubjects.kt`:
```kotlin
const val PAYMENT_NON_PAYMENT_PENALTY = "payment.non-payment.penalty"
const val PAYMENT_LOT_RELIST = "payment.lot.relist-requested"
```

**Files:**
- `services/payment-service/src/main/kotlin/eu/auctionplatform/payment/application/service/NonPaymentService.kt`
- `shared/kotlin-commons/src/main/kotlin/eu/auctionplatform/commons/messaging/NatsSubjects.kt`

---

### Gap 14: Invoice HTML Generation

**Problem:** `pdfUrl` is always null (CheckoutService line 295). No PDF generation.

#### New: `InvoiceHtmlGenerator.kt`
Server-rendered HTML invoice with payment details, VAT breakdown, buyer/seller info. Returns HTML string.

**File:** `services/payment-service/src/main/kotlin/eu/auctionplatform/payment/infrastructure/invoice/InvoiceHtmlGenerator.kt`

#### Modified: `PaymentResource.kt`
- Add `GET /invoices/{id}/html` endpoint returning `text/html`
- Update invoice record with URL path after generation

**File:** `services/payment-service/src/main/kotlin/eu/auctionplatform/payment/api/v1/resource/PaymentResource.kt`

---

## Phase 4: Notifications

### Gap 6: 11 Missing Email Templates

**Problem:** 11 of 15 `NotificationType` values have no HTML template file. `EmailSender.renderTemplate()` throws `IllegalArgumentException` for missing templates.

#### 11 new HTML files
Follow existing template style (blue header `#1e40af`, white body, data table, green CTA button, gray footer):

| Template | Subject | Key Variables |
|----------|---------|---------------|
| `auto_bid_triggered.html` | Your auto-bid was triggered | `{lotTitle}`, `{bidAmount}`, `{maxAmount}`, `{lotUrl}` |
| `closing_soon.html` | Auction closing soon | `{lotTitle}`, `{closingTime}`, `{currentBid}`, `{lotUrl}` |
| `payment_due.html` | Complete your payment | `{lotTitle}`, `{totalAmount}`, `{dueDate}`, `{checkoutUrl}` |
| `payment_received.html` | Payment confirmed | `{lotTitle}`, `{totalAmount}`, `{paymentId}`, `{ordersUrl}` |
| `pickup_reminder.html` | Pickup reminder | `{lotTitle}`, `{pickupAddress}`, `{pickupDeadline}` |
| `settlement_paid.html` | Settlement paid | `{lotTitle}`, `{netAmount}`, `{bankReference}`, `{settlementsUrl}` |
| `lot_published.html` | Lot now live | `{lotTitle}`, `{startingBid}`, `{lotUrl}` |
| `new_bid_seller.html` | New bid on your lot | `{lotTitle}`, `{bidAmount}`, `{bidCount}`, `{lotUrl}` |
| `kyc_approved.html` | Identity verified | `{userName}`, `{profileUrl}` |
| `deposit_confirmed.html` | Deposit confirmed | `{amount}`, `{auctionTitle}`, `{dashboardUrl}` |
| `non_payment_warning.html` | Payment overdue | `{lotTitle}`, `{totalAmount}`, `{dueDate}`, `{penalty}`, `{checkoutUrl}` |

**Directory:** `services/notification-service/src/main/resources/templates/en/`

#### Fix: NATS consumers must include email in data map

Currently only `UserEventNotificationConsumer.handleUserRegistered()` populates the `"email"` key. All other consumers skip email delivery silently.

**Solution:** Add REST client call to user-service to resolve email by userId:
```kotlin
// Helper method in consumer
private fun resolveUserEmail(userId: UUID): String? {
    // Use Quarkus REST client or simple HTTP call
    // GET http://localhost:8083/api/v1/users/{userId}
    // Extract email from response
}
```

Then add `data["email"] = resolveUserEmail(userId)` in:
- `AuctionEventNotificationConsumer.handleBidPlaced()` -- for bidder and previous bidder
- `AuctionEventNotificationConsumer.handleBidProxy()` -- for bidder
- `AuctionEventNotificationConsumer.handleLotClosed()` -- for winner
- `PaymentEventNotificationConsumer.handleCheckoutCompleted()` -- for buyer
- `PaymentEventNotificationConsumer.handleSettlementReady()` -- for seller

**Files:**
- `services/notification-service/src/main/kotlin/eu/auctionplatform/notification/infrastructure/nats/AuctionEventNotificationConsumer.kt`
- `services/notification-service/src/main/kotlin/eu/auctionplatform/notification/infrastructure/nats/PaymentEventNotificationConsumer.kt`

---

### Gap 15: Frontend Notification Type Mismatch

**Problem:** buyer-web `Notification` type has 5 types; backend has 15. Uses `limit` param where backend expects `size`.

#### Modified: `types/notification.ts`
```typescript
// Expand type union to match all 15 backend types + system
type: 'overbid' | 'bid_confirmed' | 'auto_bid_triggered' | 'closing_soon' |
      'auction_won' | 'payment_due' | 'payment_received' | 'pickup_reminder' |
      'settlement_paid' | 'lot_published' | 'new_bid_seller' | 'kyc_approved' |
      'deposit_confirmed' | 'non_payment_warning' | 'welcome' | 'system'
```

**File:** `frontend/buyer-web/types/notification.ts`

#### Modified: `useNotifications.ts`
Change `limit: 20` to `size: 20` to match backend parameter name.

**File:** `frontend/buyer-web/composables/useNotifications.ts`

---

### Gap 12: Wire Seller/Admin Frontends to Notification Service

**Problem:** seller-portal `TopBar.vue` and admin-dashboard `AdminTopBar.vue` use hardcoded mock notification arrays (`ref([...])`) completely disconnected from backend.

#### New: `useNotifications.ts` (seller-portal)
Follow buyer-web pattern using `useApi()` composable:
- `fetchNotifications()` -- `GET /notifications?page=1&size=20`
- `markAsRead(id)` -- `PUT /notifications/{id}/read`
- `markAllAsRead()` -- `PUT /notifications/read-all`
- `unreadCount` computed from fetched data or `GET /notifications/unread-count`

**File:** `frontend/seller-portal/src/composables/useNotifications.ts`

#### New: `useNotifications.ts` (admin-dashboard)
Same pattern.

**File:** `frontend/admin-dashboard/src/composables/useNotifications.ts`

#### Modified: `TopBar.vue` (seller-portal)
Replace hardcoded `notifications` ref (lines 15-19) and `unreadCount` (line 21) with composable data. Call `fetchNotifications()` on mount.

**File:** `frontend/seller-portal/src/components/layout/TopBar.vue`

#### Modified: `AdminTopBar.vue` (admin-dashboard)
Same pattern.

**File:** `frontend/admin-dashboard/src/components/layout/AdminTopBar.vue`

---

## Phase 5: Remaining Gaps

### Gap 8: Compliance Service Mock Improvements

**Problem:** `GET /fraud/alerts` returns empty list. `AmlService.triggerScreening()` never calls provider.

#### Modified: `ComplianceResource.kt`
Return 3-4 realistic mock fraud alerts with different severities instead of empty list.

#### Modified: `AmlService.kt`
Simulate async screening: set status to `IN_PROGRESS`, schedule completion after 3s delay that updates to `CLEAR` (90%) or `FLAGGED` (10%).

**Files:**
- `services/compliance-service/src/main/kotlin/eu/auctionplatform/compliance/api/v1/resource/ComplianceResource.kt`
- `services/compliance-service/src/main/kotlin/eu/auctionplatform/compliance/application/service/AmlService.kt`

---

### Gap 9: Seller Analytics from Real Data

**Problem:** `SellerService.getAnalytics()` returns hardcoded `topCategories = emptyList()` and `monthlyRevenue = emptyList()`.

#### Modified: `SellerService.kt`
Replace hardcoded empty lists with SQL queries on `seller_lots` and `seller_settlements` tables:
```sql
-- topCategories:
SELECT category_id, COUNT(*) as lot_count, COALESCE(SUM(current_bid), 0) as revenue
FROM app.seller_lots WHERE seller_id = ? GROUP BY category_id ORDER BY revenue DESC LIMIT 5

-- monthlyRevenue:
SELECT DATE_TRUNC('month', settled_at) as month, SUM(net_amount) as amount, COUNT(*) as count
FROM app.seller_settlements WHERE seller_id = ? AND settled_at IS NOT NULL
GROUP BY month ORDER BY month DESC LIMIT 12
```

**Files:**
- `services/seller-service/src/main/kotlin/eu/auctionplatform/seller/application/service/SellerService.kt`
- `services/seller-service/src/main/kotlin/eu/auctionplatform/seller/infrastructure/persistence/repository/SellerProfileRepository.kt`

---

### Gap 10: Seller-Portal Settlement/Lot Endpoints

**Problem:** Frontend calls non-existent endpoints: `downloadInvoice()`, `fetchMonthlySettlements()`, `fetchStatusCounts()` (fetches 1000 lots to count locally).

#### Modified: `SellerResource.kt`
Add endpoints:
- `GET /sellers/me/settlements/{id}/invoice` -- redirect to payment-service invoice
- `GET /sellers/me/settlements/monthly` -- monthly settlement aggregation
- `GET /sellers/me/lots/status-counts` -- `SELECT status, COUNT(*) FROM seller_lots WHERE seller_id = ? GROUP BY status`

#### Modified: Frontend composables
- `useSettlements.ts` -- fix `downloadInvoice()` endpoint, wire `fetchMonthlySettlements()` to new backend endpoint
- `useLots.ts` -- replace 1000-lot fetch in `fetchStatusCounts()` with new `/status-counts` endpoint call

**Files:**
- `services/seller-service/src/main/kotlin/eu/auctionplatform/seller/api/v1/resource/SellerResource.kt`
- `frontend/seller-portal/src/composables/useSettlements.ts`
- `frontend/seller-portal/src/composables/useLots.ts`

---

### Gap 11: Admin Dashboard Payment Type Enrichment

**Problem:** Frontend `Payment` type expects `auctionTitle`, `lotTitle`, `buyerName`, `sellerName` but backend returns raw UUIDs. Frontend calls non-existent admin action endpoints.

**Addressed by Gap 4:**
- Denormalized name columns added via `V006__add_payment_names.sql`
- `PaymentStatusResponse` DTO enriched with optional `lotTitle`, `buyerName`, `sellerName`
- Admin endpoints (`PATCH /{id}/settle`, `POST /{id}/refund`, `POST /{id}/reminder`) added

No additional work needed beyond Gap 4.

---

### Gap 16: Seller CO2 Table Population

**Problem:** `seller_co2` table exists but is never populated.

#### New: `Co2EventSellerConsumer.kt`
- Inner NatsConsumer on stream `CO2`, filter `co2.calculated`, durable `seller-co2-consumer`
- INSERT into `app.seller_co2 (seller_id, lot_id, co2_saved_kg, created_at)`
- Resolve sellerId from `seller_lots` table using lotId (same pattern as Gap 3)

**File:** `services/seller-service/src/main/kotlin/eu/auctionplatform/seller/infrastructure/nats/Co2EventSellerConsumer.kt`

---

### Gap 18: Push Notification Improvements

**Problem:** `PushNotificationSender.sendToDevice()` only logs. Notifications marked as SENT even though nothing is delivered.

#### Modified: `PushNotificationSender.kt`
- Add config: `@ConfigProperty(name = "push.enabled", defaultValue = "false") pushEnabled: Boolean`
- If `!pushEnabled`: log at INFO level, return success (notification still marked SENT)
- If `pushEnabled`: placeholder for future Firebase Admin SDK integration

**File:** `services/notification-service/src/main/kotlin/eu/auctionplatform/notification/infrastructure/push/PushNotificationSender.kt`

---

## Dependency Graph

```
Gap 1 (NATS routing) ──────> ALL other gaps
Gap 17 (user events) ──────> Gap 2 (analytics user growth)

Gap 1 ──> Gap 3 (seller consumers) ──> Gap 9 (seller analytics)
                                   ──> Gap 10 (seller endpoints)
                                   ──> Gap 16 (CO2)

Gap 1 ──> Gap 5 (user projections)

Gap 1 ──> Gap 4 (payment) ──> Gap 13 (non-payment events)
                           ──> Gap 14 (invoice HTML)
                           ──> Gap 11 (admin payment types)

Gap 6 (templates) ── standalone after Gap 1
Gap 12, 15, 18 ── standalone
Gap 8 ── standalone
```

---

## Verification Plan

After each phase:
1. `./gradlew build -x test` -- all services compile
2. Start full stack via `/run-full-stack`
3. Run `/test-happy-path all` -- verify no regressions

### Phase-specific checks:
- **Phase 1:** Use `nats sub "auction.>"` to verify bid events arrive without brand prefix. Create a lot and verify `catalog.lot.created` appears on NATS.
- **Phase 2:** Place a bid -> check `user_bids`, `seller_lots`, `auction_metrics` tables are populated. Register a user -> check `user_growth` is incremented.
- **Phase 3:** Award a lot -> verify payment created with real `hammerPrice`. Check `CheckoutCompletedEvent` appears on NATS `payment.checkout.completed`. Verify `seller_settlements` row appears.
- **Phase 4:** Trigger bid event -> verify email appears in MailHog (localhost:8025). Check seller/admin dashboard notifications load from API.
- **Phase 5:** Check seller analytics shows real categories and monthly revenue. Fraud alerts show mock entries. Settlement invoice download works.

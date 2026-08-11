# Closiq Backend

Spring Boot 3 modular monolith for the Closiq clothing rental marketplace.

## Phase 6 Progress

| Module | Status |
|---|---|
| **Common + Config** | ✅ Response wrapper, RFC 7807 errors, JWT security, Redis, OpenAPI |
| **Identity / Auth** | ✅ Phone OTP, JWT, refresh tokens, password reset |
| **User** | ✅ Profile, addresses, wishlist, settings, pincode serviceability |
| **Seller** | ✅ Applications, KYC upload, dashboard, wallet, bank accounts, analytics |
| **Product / Catalog** | ✅ Home discovery, categories, products, search, filters, offers |
| **Inventory** | ✅ Physical units, availability calendar, LOW_STOCK badges, seller stock APIs |
| **Booking** | ✅ Holds, checkout session, history, cancel, trial/return lifecycle |
| **Payment / Checkout** | ✅ Razorpay stub, coupons, verify → CONFIRMED + inventory hold conversion |
| **Shipment** | ✅ Shadowfax stub, outbound/return tracking, webhooks, seller ready-for-pickup |
| **Review** | ✅ Create review, product/seller listings, rating aggregates |
| **Notification** | ✅ In-app feed, read/mark-all, lifecycle dispatch hooks |
| **Seller Products** | ✅ CRUD, publish, images, listing inventory (§16.8–16.10 existing) |
| **Seller Bookings** | ✅ List, detail, accept/reject, ready-for-pickup, history |

## Tech Stack

- Java 21, Spring Boot 3.4
- PostgreSQL 16, Redis 7
- Spring Security + JWT (15 min access / 30 day refresh)
- Flyway migrations, MapStruct, Testcontainers

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker (for PostgreSQL, Redis, integration tests)

## Quick Start

```bash
# Start infrastructure
docker compose up -d

# Run backend
cd backend
mvn spring-boot:run
```

API base URL: `http://localhost:8080/api/v1`  
Swagger UI: `http://localhost:8080/swagger-ui.html`

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `closiq` | Database name |
| `DB_USER` | `closiq` | Database user |
| `DB_PASSWORD` | `closiq` | Database password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `JWT_SECRET` | (dev default) | HMAC secret for JWT signing — **change in production** |
| `RAZORPAY_KEY_ID` | `rzp_test_stub` | Razorpay key (stub in dev) |
| `RAZORPAY_KEY_SECRET` | (stub default) | Razorpay webhook/signing secret |
| `RAZORPAY_STUB_ENABLED` | `true` | Use stub payment gateway |
| `SHADOWFAX_WEBHOOK_SECRET` | (stub default) | Shadowfax webhook HMAC secret |
| `SHADOWFAX_STUB_ENABLED` | `true` | Use stub logistics gateway |
| `CLOUDINARY_CLOUD_NAME` | (empty) | Cloudinary cloud name — required when `CLOUDINARY_STUB_ENABLED=false` |
| `CLOUDINARY_API_KEY` | (empty) | Cloudinary API key — must match the same account as the secret |
| `CLOUDINARY_API_SECRET` | (empty) | Cloudinary API secret — never commit; pair with `CLOUDINARY_API_KEY` |
| `CLOUDINARY_URL` | (empty) | Alternative: `cloudinary://api_key:api_secret@cloud_name` (used when individual vars are unset) |
| `CLOUDINARY_FOLDER` | `closiq` | Root folder for uploaded media |
| `CLOUDINARY_STUB_ENABLED` | `true` | Skip real Cloudinary uploads in local dev |

`CLOSIQ_CLOUDINARY_*` env vars are also accepted (same meaning as above). When using IntelliJ/env files **and** `application-test.properties`, env vars **override** the properties file — ensure the secret matches exactly (including leading `-` if present in the dashboard).

## Auth Module (`/api/v1/auth`)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/register` | Send OTP to start registration |
| POST | `/login` | Send OTP to registered phone |
| POST | `/verify-otp` | Verify OTP, issue JWT + refresh cookie |
| POST | `/refresh` | Rotate refresh token, issue new access token |
| POST | `/logout` | Revoke refresh token |
| GET | `/me` | Current user summary |
| POST | `/forgot-password` | Email password reset (optional email auth) |
| POST | `/reset-password` | Set new password |

### Dev OTP

In `dev` profile, OTP codes are logged to the console:

```
OTP for +9198765**** (REGISTER): 123456
```

### Example: Register + Verify

```bash
# 1. Start registration
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"phone":"+919876543210","acceptTerms":true}' | jq

# 2. Verify OTP (use OTP from server logs in dev)
curl -s -X POST http://localhost:8080/api/v1/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{
    "otpSessionId":"<session-id>",
    "otp":"123456",
    "purpose":"REGISTER",
    "profile":{"firstName":"Ananya","lastName":"Sharma"}
  }' | jq
```

## User Module (`/api/v1/users/me`)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/users/me` | Full profile with preferences and seller stub |
| PATCH | `/users/me` | Update name, email, avatar, shopping preferences |
| GET | `/users/me/addresses` | List delivery addresses |
| POST | `/users/me/addresses` | Add address (max 10) |
| PATCH | `/users/me/addresses/{id}` | Update address |
| DELETE | `/users/me/addresses/{id}` | Soft-delete address |
| GET | `/users/me/wishlist` | Cursor-paginated wishlist |
| POST | `/users/me/wishlist` | Add product (max 100 items) |
| DELETE | `/users/me/wishlist/{productId}` | Remove from wishlist |
| GET | `/users/me/settings/notifications` | Notification preferences |
| PATCH | `/users/me/settings/notifications` | Update notification preferences |
| GET | `/users/me/settings` | Account settings (language, marketing) |
| PATCH | `/users/me/settings` | Update account settings |

Public pincode check: `GET /api/v1/serviceability/pincodes/{pincode}`

Preferences (shopping, notifications, account) are stored in `user_profile.preferences` JSONB. In-app notifications use the `notification` table; channel preferences will move to dedicated tables in a later phase.

## Seller Module (`/api/v1/seller`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/seller/applications` | CUSTOMER | Submit seller application |
| GET | `/seller/applications/me` | Any | Application + KYC status |
| POST | `/seller/applications/me/kyc-documents/upload-url` | Applicant | Presigned KYC upload URL |
| POST | `/seller/applications/me/kyc-documents/confirm` | Applicant | Confirm KYC upload |
| GET | `/seller/dashboard` | SELLER | Dashboard metrics |
| GET | `/seller/analytics` | SELLER | Views, revenue, top products |
| GET | `/seller/wallet` | SELLER | Balances + transactions |
| POST | `/seller/wallet/payouts` | SELLER | Request payout (min ₹500) |
| GET/POST/DELETE | `/seller/bank-accounts` | SELLER | Payout bank accounts |

Seller verification (admin approval → `SELLER` role + `seller_profile`) is handled by the Admin module (pending).

## Catalog Module (public)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/home/featured-products` | Featured product rails |
| GET | `/home/trending-products` | Trending products |
| GET | `/home/recommendations` | Personalized (auth) or trending (guest) |
| GET | `/categories` | Occasion categories |
| GET | `/categories/{slug}/products` | Products in category |
| GET | `/offers` | Active promotional offers |
| GET | `/products` | Paginated listing with filters |
| GET | `/products/search?q=` | Full-text search |
| GET | `/products/filters` | Facet counts for PLP |
| GET | `/products/{slug}` | Product detail (PDP) |
| GET | `/products/{slug}/availability` | Variant availability calendar (see Inventory module) |
| GET | `/products/{slug}/related` | Related products |
| GET | `/products/{slug}/reviews` | Paginated verified reviews (see Review module) |
| GET | `/products/{slug}/images` | Image gallery |

Seed data (3 products, 5 categories) ships in Flyway `V5__catalog_seed.sql`.

## Inventory Module

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/products/{slug}/availability?variantId=` | Public | Calendar with booked/blocked dates |
| GET | `/seller/products/{productId}/inventory` | SELLER | Variant quantities + booking counts |
| PATCH | `/seller/products/{productId}/inventory` | SELLER | Adjust stock quantities |
| POST | `/seller/inventory/blocks` | SELLER | Block dates (cleaning/maintenance) |
| DELETE | `/seller/inventory/blocks/{blockId}` | SELLER | Remove block |

Physical units live in `inventory_item` (one row per rentable piece). `inventory_reservation` uses a PostgreSQL exclusion constraint to prevent overlapping active bookings. Product cards show `LOW_STOCK` when total available units ≤ `closiq.inventory.low-stock-threshold` (default 2).

Seed inventory in `V7__inventory_seed.sql` includes 9 units across demo variants plus a sample Aug 20–23 booking on emerald size S.

## Booking Module (`/api/v1/bookings`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/bookings` | CUSTOMER | Create hold + checkout session (requires `Idempotency-Key` header) |
| GET | `/bookings` | CUSTOMER | Booking history (cursor pagination) |
| GET | `/bookings/{id}` | CUSTOMER | Full booking detail (UUID or `BK-YYYY-NNNNNN`) |
| POST | `/bookings/{id}/cancel` | CUSTOMER | Cancel `PENDING_PAYMENT` or `CONFIRMED` |
| GET | `/bookings/{id}/timeline` | CUSTOMER | Status timeline |
| POST | `/bookings/{id}/trial/accept` | CUSTOMER | Accept home trial (`TRIAL_READY`) |
| POST | `/bookings/{id}/trial/reject` | CUSTOMER | Reject during trial |
| POST | `/bookings/{id}/return-request` | CUSTOMER | Schedule return pickup |

Hold TTL is 15 minutes (configurable). Creates an `inventory_reservation` HOLD row and Redis lock `booking:hold:{variantId}:{start}:{end}`. Expired holds are released by a scheduled job every 60s.

## Payment & Checkout

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/checkout/calculate` | Public | Price breakdown with optional coupon |
| POST | `/checkout/coupons/validate` | CUSTOMER | Validate coupon for booking |
| POST | `/checkout/sessions` | CUSTOMER | Bind address, apply coupon, `readyForPayment` |
| GET | `/checkout/sessions/{id}/summary` | CUSTOMER | Checkout review summary |
| POST | `/payments/razorpay/orders` | CUSTOMER | Create Razorpay order (`Idempotency-Key`) |
| POST | `/payments/razorpay/verify` | CUSTOMER | Verify signature → `CONFIRMED` booking |
| GET | `/payments` | CUSTOMER | Payment history |
| GET | `/payments/{id}/refund-status` | CUSTOMER | Refund tracking |

Dev uses **stub Razorpay** (`closiq.razorpay.stub-enabled: true`). Signature = HMAC-SHA256(`orderId|paymentId`, secret). On successful verify: payment `CAPTURED`, booking `CONFIRMED`, checkout `COMPLETED`, inventory HOLD → CONFIRMED.

Env: `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_STUB_ENABLED`.

## Shipment Module (`/api/v1/shipments`, `/api/v1/webhooks`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/shipments/{bookingId}/track?type=OUTBOUND\|RETURN` | CUSTOMER / SELLER | Live tracking with event history |
| GET | `/shipments/{bookingId}/status?type=OUTBOUND\|RETURN` | CUSTOMER / SELLER | Lightweight status poll |
| POST | `/shipments/{bookingId}/return-pickup` | CUSTOMER / SELLER | Schedule return pickup (creates RETURN shipment) |
| POST | `/webhooks/shadowfax` | Public (HMAC) | Inbound Shadowfax status updates |

Dev uses **stub Shadowfax** (`closiq.shadowfax.stub-enabled: true`). Webhook signature = HMAC-SHA256(raw body, `webhook-secret`). Shipment events drive booking transitions (e.g. OUTBOUND `DELIVERED` → `TRIAL_READY`, RETURN `DELIVERED` → `RETURNED`).

Env: `SHADOWFAX_WEBHOOK_SECRET`, `SHADOWFAX_STUB_ENABLED`.

## Review Module (`/api/v1/reviews`, `/api/v1/sellers`, `/api/v1/products`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/reviews` | CUSTOMER | Submit verified review (`Idempotency-Key` required) |
| GET | `/products/{slug}/reviews` | Public | Paginated product reviews |
| GET | `/sellers/{sellerId}/reviews` | Public | Seller reviews + aggregate rating |

Reviews require booking status `COMPLETED` or `DEPOSIT_REFUNDED`. One review per booking. Product/seller `avg_rating` and `review_count` updated on publish.

## Notification Module (`/api/v1/notifications`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/notifications?cursor=&limit=&unreadOnly=` | CUSTOMER / SELLER | Feed with `meta.unreadCount` |
| PATCH | `/notifications/{id}/read` | Owner | Mark read/unread `{ "read": true }` |
| POST | `/notifications/read-all` | Owner | Bulk mark all read |

Notifications are emitted on booking confirm, shipment status changes, return scheduling, and seller payouts. Push/email delivery is future work; preferences remain in `user_profile.preferences` until dedicated preference tables land.

## Seller Product Management (`/api/v1/seller/products`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/seller/products` | SELLER | Create draft listing (`Idempotency-Key`) |
| GET | `/seller/products?status=&cursor=` | SELLER | Seller's own listings |
| PATCH | `/seller/products/{productId}` | SELLER | Update listing fields |
| DELETE | `/seller/products/{productId}` | SELLER | Archive listing (soft delete) |
| POST | `/seller/products/{productId}/publish` | SELLER | Publish draft → `ACTIVE` (requires ≥1 image) |
| POST | `/seller/products/{productId}/images/upload` | SELLER | Upload product image (multipart, via backend) |
| POST | `/seller/products/{productId}/images/upload-url` | SELLER | Presigned product image upload (legacy) |
| POST | `/seller/products/{productId}/images` | SELLER | Confirm and attach image |
| DELETE | `/seller/products/{productId}/images/{imageId}` | SELLER | Remove image |
| GET/PATCH | `/seller/products/{productId}/inventory` | SELLER | Stock levels (Inventory module) |
| POST/DELETE | `/seller/inventory/blocks` | SELLER | Availability blocks (Inventory module) |

Create flow also seeds `product_variant` rows and physical `inventory_item` units per variant quantity.

## Seller Booking Management (`/api/v1/seller/bookings`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/seller/bookings?status=&productId=&startDate=&endDate=&sort=&cursor=` | SELLER | Incoming and active bookings (cursor pagination) |
| GET | `/seller/bookings/history?page=&limit=&startDate=&endDate=&status=` | SELLER | Completed/cancelled history |
| GET | `/seller/bookings/{bookingId}` | SELLER | Full booking detail with earnings breakdown |
| POST | `/seller/bookings/{bookingId}/accept` | SELLER | Accept within SLA → `SELLER_ACCEPTED` |
| POST | `/seller/bookings/{bookingId}/reject` | SELLER | Reject → cancel, release inventory, stub refund |
| POST | `/seller/bookings/{bookingId}/ready-for-pickup` | SELLER | Mark prepared → create OUTBOUND shipment |

`PENDING_ACCEPTANCE` filter maps to internal `CONFIRMED`. Customer contact is masked until after acceptance. Commission rate from `closiq.booking.commission-rate-bps` (default 15%).

## Testing

```bash
cd backend
mvn test
```

Integration tests use Testcontainers (PostgreSQL + Redis).

## Project Structure

```
backend/src/main/java/com/closiq/
├── ClosiqApplication.java
├── config/                 # App, Redis, OpenAPI properties
├── common/
│   ├── domain/             # AuditableEntity
│   ├── exception/          # GlobalExceptionHandler, ErrorCode
│   ├── security/           # JWT, SecurityConfig
│   ├── util/               # IdGenerator, HashUtils
│   └── web/                # ApiResponse, ClosiqRequestIdFilter
└── identity/               # Auth domain
    └── ...
└── user/                   # Profile, addresses, wishlist, settings
    ├── domain/
    ├── repository/
    ├── service/
    ├── mapper/
    └── web/
└── catalog/                # Categories, products, discovery APIs
    └── ...
└── inventory/              # Physical units, reservations, availability
    └── ...
└── booking/                # Holds, checkout sessions, order lifecycle
    └── ...
└── payment/                # Checkout, Razorpay stub, coupons
    └── ...
└── shipment/               # Shadowfax stub, tracking, webhooks
    └── ...
└── review/                 # Verified purchase reviews, aggregates
    └── ...
└── notification/           # In-app feed and dispatch hooks
    ├── domain/
    ├── repository/
    ├── service/
    └── web/
```

## Documentation

- [Phase 3 API Contract](../docs/PHASE-3-API-CONTRACT.md)
- [Phase 4 Database Design](../docs/PHASE-4-DATABASE-DOMAIN-DESIGN.md)

# Closiq — Project Overview

**What it is:** Premium peer-to-peer clothing rental marketplace. Sellers list occasion wear; customers rent by date range with a mandatory 15-minute home trial. Launch city: Mumbai; architecture is pan-India ready.

**Repo layout:**

```
Closiq/
├── frontend/     Next.js web app (customer + seller + admin UI)
├── backend/      Spring Boot modular monolith (REST API)
├── docs/         Planning, API contract, DB design, this file
└── docker-compose.yml   Local Postgres + Redis + full stack
```

---

## Tech Stack — What & Why

| Layer | Technology | Purpose |
|-------|------------|---------|
| **Frontend** | Next.js 16 (App Router) | SSR/SSG, route groups per persona (shop, account, seller), API proxy to backend |
| | React 19 + TypeScript | Type-safe UI components |
| | Tailwind CSS 4 | Design tokens (paper/ink/gold/rose premium aesthetic) |
| | shadcn-style UI (Radix) | Accessible primitives (dialog, select, tabs, etc.) |
| | TanStack Query | Server state, caching, loading/error for API calls |
| | React Hook Form + Zod | Form validation with schema safety |
| | Framer Motion | Subtle animations (menus, transitions) |
| **Backend** | Java 21 + Spring Boot 3.4 | Modular monolith — fast MVP, modules extractable later |
| | Spring Security + JWT | Auth: 15 min access token + 30 day refresh cookie |
| | Spring Data JPA | ORM over PostgreSQL |
| | PostgreSQL 16 | Source of truth: users, bookings, inventory, payments |
| | Redis 7 | Ephemeral locks, rate limits, shipment cache (not durable data) |
| | Flyway | Versioned DB migrations |
| | MapStruct | Compile-time DTO ↔ entity mapping |
| | SpringDoc OpenAPI | Swagger UI for API exploration |
| **Infra (dev/prod)** | Docker Compose | Local Postgres, Redis, backend, frontend |
| | Vercel | Frontend hosting |
| | AWS EC2 / App Runner | Backend hosting (Mumbai) |
| | Supabase | Managed Postgres (dev) |
| | Upstash | Managed Redis with TLS (dev) |
| **Integrations** | Razorpay | Payments (rental + deposit); stub in local dev |
| | Shadowfax | Last-mile delivery/returns; stub in local dev |
| | Cloudinary | Product/KYC image uploads (S3 stub also available) |
| | Brevo | Transactional email (optional; OTP logs to console in dev) |
| **Testing** | JUnit + Testcontainers | Integration tests with real Postgres + Redis in Docker |

---

## Backend Modules

| Module | Responsibility |
|--------|----------------|
| `identity` | Phone OTP, JWT, refresh tokens, password reset |
| `user` | Profile, addresses, wishlist, pincode serviceability |
| `catalog` | Products, categories, search, home discovery |
| `inventory` | Physical units, availability calendar, stock blocks |
| `booking` | Holds, checkout, trial/return lifecycle, cancellations |
| `payment` | Razorpay orders, coupons, verify payment, refunds |
| `shipment` | Outbound/return logistics, webhooks, tracking |
| `seller` | Applications, KYC, products, wallet, analytics, bookings |
| `review` | Product/seller reviews and rating aggregates |
| `notification` | In-app feed + lifecycle hooks |
| `admin` | Seller verification, catalog moderation |
| `cart` | Multi-item checkout batch (extends single-item MVP) |
| `storage` | File upload abstraction (Cloudinary/S3) |
| `common` | Security, errors (RFC 7807), pagination, IDs |

---

## Frontend Structure

```
frontend/src/
├── app/           Routes: (shop), (auth), (account), (checkout), (seller)
├── features/      auth, products, checkout, orders, seller, wishlist, …
│   └── services/  Interface + mock OR api impl (swap via NEXT_PUBLIC_USE_MOCK)
├── components/ui/ Design system primitives
└── providers/     Auth, Query, AppMode contexts
```

**Pattern:** UI never imports JSON directly. All data flows through `features/*/services` — swap mock ↔ real API without touching components.

---

## Advanced Patterns (Why We Use Them)

| Pattern | Where | Why |
|---------|-------|-----|
| **Modular monolith** | Backend packages | Ship MVP quickly; clear module boundaries for future microservices |
| **Gateway / adapter** | `PaymentGateway`, `LogisticsGateway`, `StorageProvider` | Swap Razorpay, Shadowfax, Cloudinary without changing domain logic |
| **Layered booking safety** | Redis `SET NX` lock + DB hold + TTL expiry scheduler | Prevent double-booking under concurrent checkout |
| **Booking state machine** | `BookingStatusTransitions` | Enforce valid lifecycle (trial → rental → return → deposit refund) |
| **Idempotency keys** | Bookings, reviews, payouts | Safe retries from client/network without duplicate side effects |
| **Cursor pagination** | `PageTokenCodec` | Stable, efficient paging for large lists (no offset drift) |
| **OTP rate limiting** | Redis `INCR` + fallback in-memory | Abuse prevention; degrades gracefully if Redis is down |
| **Stub gateways** | Razorpay, Shadowfax, Cloudinary | Develop and test full flows without real API keys |
| **Next.js API proxy** | `app/api/v1/[...path]/route.ts` | Same-origin API calls from Vercel; avoids CORS; hides backend URL |
| **Service abstraction (FE)** | `mockProductService` ↔ `apiProductService` | Frontend built against contracts first; backend plugged in later |

---

## Key Business Rules

- **15-minute home trial** — mandatory for all rentals; encoded in booking states
- **Phone OTP** — required at registration (India market trust)
- **Mumbai first** — pincode check at checkout; browse is pan-India
- **Single seller per booking** — P2P inventory model

---

## Quick Start

```bash
docker compose up -d          # Postgres :5433, Redis :6379, backend :8081, frontend :3000
cd backend && mvn spring-boot:run   # Or use Docker backend service
cd frontend && npm install && npm run dev
```

Swagger: `http://localhost:8081/swagger-ui.html`

---

## Where to Read More

| Doc | When to use |
|-----|-------------|
| [README.md](../README.md) | Project status and doc index |
| [PHASE-3-API-CONTRACT.md](./PHASE-3-API-CONTRACT.md) | Every REST endpoint (frontend/backend contract) |
| [PHASE-4-DATABASE-DOMAIN-DESIGN.md](./PHASE-4-DATABASE-DOMAIN-DESIGN.md) | Tables, ER diagrams, domain model |
| [REDIS-FLOW.md](./REDIS-FLOW.md) | Redis keys, TTLs, failure behavior |
| [backend/README.md](../backend/README.md) | Backend setup, env vars, module progress |
| [frontend/README.md](../frontend/README.md) | Frontend architecture, mock login |
| [backend/docs/DEV-DEPLOYMENT.md](../backend/docs/DEV-DEPLOYMENT.md) | AWS + Supabase + Upstash deployment |

---

*Last updated: August 2026 — Phases 1–6 complete (auth through seller booking management).*

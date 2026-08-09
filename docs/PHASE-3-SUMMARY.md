# Phase 3 — What We Did

**Project:** Closiq (Premium Clothing Rental Marketplace)  
**Phase:** 3 — API Contract Specification  
**Date:** August 4, 2026  
**Status:** Complete (documentation only — no code, SQL, or Java)

---

## Overview

Phase 3 produced the **REST API contract** between the Next.js frontend and Spring Boot backend. Every endpoint is documented with request/response schemas, validation rules, error codes, and business rules — aligned with Phase 1 architecture decisions and Phase 2 screen/service mappings.

---

## Deliverable

| # | Deliverable | Location | Summary |
|---|---|---|---|
| 1 | API Contract Specification | [PHASE-3-API-CONTRACT.md](./PHASE-3-API-CONTRACT.md) | Full REST API reference — 13 domains, 70+ operations |

---

## API Domains Covered

| Domain | Endpoints | Highlights |
|---|---|---|
| **Authentication** | 8 | Phone OTP register/login, JWT + refresh cookie |
| **User** | 14 | Profile, addresses, wishlist, settings, pincode serviceability |
| **Seller** | 10 | Application, KYC upload, dashboard, wallet, bank accounts, analytics |
| **Home** | 6 | Featured, trending, categories, offers, recommendations |
| **Product** | 8 | List, search, filters, availability calendar, reviews |
| **Booking** | 8 | Hold, trial accept/reject, cancel, return, timeline |
| **Checkout** | 4 | Price calculation, coupons, session, summary |
| **Payment** | 4 | Razorpay order, verify, history, refund status |
| **Shipment** | 3 | Track, status, return pickup (Shadowfax) |
| **Review** | 3 | Create, product reviews, seller reviews |
| **Notifications** | 3 | List, mark read, mark all read |
| **Seller Products** | 11 | CRUD, images (S3 presigned), inventory, date blocks |
| **Seller Bookings** | 6 | Accept, reject, ready for pickup, history |

---

## Global Standards Defined

| Standard | Decision |
|---|---|
| Base URL | `/api/v1/` |
| Response wrapper | `{ success, data, meta }` |
| Errors | RFC 7807 Problem Details |
| Pagination | Cursor (catalog); offset (reports) |
| Money | Integer INR (whole rupees) |
| Dates | ISO 8601 UTC |
| File upload | S3 presigned URLs |
| Idempotency | `Idempotency-Key` on resource-creating POSTs |
| Auth | JWT 15min + HttpOnly refresh cookie 30d |
| Roles | `CUSTOMER`, `SELLER` (same user, both roles) |

---

## Key Business Rules Encoded

- **15-minute home trial** — `TRIAL_READY` → accept/reject endpoints; state machine in §18.2
- **Mumbai pincode gate** — serviceability check before payment
- **Booking concurrency** — 15-min hold TTL; Redis lock + DB constraint (Phase 1 ADR)
- **Single-item checkout** — no cart resource in v1
- **Mandatory trial** — `includesTrial: true` on all clothing bookings

---

## Inherited from Prior Phases

| Phase | What carried forward |
|---|---|
| Phase 1 | Naming conventions, modular monolith modules, gateway pattern, booking ADRs |
| Phase 2 | Mock data shapes (`products.json`, `orders.json`), screen → API mapping (§18.3) |

---

## What Was Explicitly NOT Done (By Design)

- ❌ Spring Boot code
- ❌ SQL schemas
- ❌ Java entities
- ❌ Frontend code
- ❌ Admin application APIs (Phase 6)
- ❌ Webhook implementation (referenced only)

---

## Open Decisions (Before Backend Implementation)

1. **Minimum rental period** — default 1 day in contract; confirm with product
2. **Deposit refund SLA** — 3–5 business days suggested
3. **Commission model** — flat 15% placeholder
4. **Cancellation refund tiers** — policy engine TBD

---

## Recommended Next Steps (Phase 4)

1. Generate OpenAPI 3.1 spec from this contract (`openapi/v1/openapi.yaml`)
2. Backend scaffolding — Spring Boot modules per domain
3. Implement auth + catalog APIs first (unblocks frontend integration)
4. Booking hold + payment flow (critical path)
5. Razorpay + Shadowfax sandbox integration
6. Contract tests — frontend service layer against mock server

---

## Document Index

| Document | Purpose |
|---|---|
| [PHASE-1-FOUNDATION.md](./PHASE-1-FOUNDATION.md) | Architecture and planning |
| [PHASE-2-FRONTEND-ARCHITECTURE.md](./PHASE-2-FRONTEND-ARCHITECTURE.md) | Frontend screens and components |
| [PHASE-3-API-CONTRACT.md](./PHASE-3-API-CONTRACT.md) | REST API contract (this phase) |
| [PHASE-3-SUMMARY.md](./PHASE-3-SUMMARY.md) | Executive summary (this file) |

---

*Phase 3 complete. API contract ready for backend implementation and frontend integration.*

# Phase 1 — What We Did

**Project:** Closiq (Premium Clothing Rental Marketplace)  
**Phase:** 1 — Foundation & Product Planning  
**Date:** August 4, 2026  
**Status:** Complete (planning only — no code, APIs, or database schemas)

---

## Overview

Phase 1 established the complete engineering and product foundation for Closiq before any implementation begins. Work was informed by the supplied HTML prototype (`clothing-rental-prototype-updated.html`) as a **design reference only** — not copied screen-by-screen.

---

## Stakeholder Decisions (Updated)

| Decision | Status | Detail |
|---|---|---|
| **Brand name** | ✅ Confirmed | **Closiq** |
| **15-minute home trial** | ✅ Confirmed | **Mandatory platform-wide** — not seller opt-in |
| **Phone OTP at registration** | ✅ Confirmed | Required at signup |
| **Launch geography** | ✅ Confirmed | **Mumbai first**; architecture built for **pan-India** |
| **Minimum rental period** | ⏳ Deferred | Decide before checkout (Phase 3) |
| **Deposit refund SLA** | ⏳ Deferred | Decide before payments (Phase 4) |
| **Commission model** | ⏳ Open | Flat % vs category-based — Phase 2 |

---

## Inputs Reviewed

### Prototype Analysis

We analyzed the "Atelier" prototype and extracted:

- **Design language:** Warm paper/ink palette, gold accents, rose highlights, Fraunces serif + Inter + IBM Plex Mono
- **Core UX patterns:** Occasion-based discovery, date calendar booking rail, deposit + daily pricing, 15-minute home trial flow, vertical order timeline
- **Premium aesthetic:** Generous whitespace, minimal border radius (2px), uppercase tracking on labels, editorial photography

### Product Requirements

Consolidated from the product brief:

- Peer-to-peer rental marketplace (sellers own inventory)
- Same account can be customer and seller
- Date-based inventory with no double booking
- Integrations: Razorpay, Shadowfax, AWS S3, Firebase FCM, email
- Tech stack: Next.js 15, React 19, Spring Boot, PostgreSQL, Redis
- Long-term expansion to electronics, furniture, cameras, accessories, sports equipment

---

## Deliverables Produced

| # | Deliverable | Location | Summary |
|---|---|---|---|
| 1 | Product Architecture | [PHASE-1-FOUNDATION.md](./PHASE-1-FOUNDATION.md) §1 | System diagram, customer/seller/admin flows, domain modules |
| 2 | Project Structure | [PHASE-1-FOUNDATION.md](./PHASE-1-FOUNDATION.md) §2 | Full Next.js and Spring Boot folder structures with module explanations |
| 3 | Architecture Decisions | [PHASE-1-FOUNDATION.md](./PHASE-1-FOUNDATION.md) §3 | Technology rationale, alternatives, ADR summaries |
| 4 | Development Standards | [PHASE-1-FOUNDATION.md](./PHASE-1-FOUNDATION.md) §4 | Naming, API guidelines, branch/commit strategy, coding standards |
| 5 | Development Roadmap | [PHASE-1-FOUNDATION.md](./PHASE-1-FOUNDATION.md) §5 | 8 phases, MVP feature matrix, timeline |
| 6 | Development Environment | [PHASE-1-FOUNDATION.md](./PHASE-1-FOUNDATION.md) §6 | IDE, extensions, DB tools, testing, documentation stack |
| 7 | Team Structure | [PHASE-1-FOUNDATION.md](./PHASE-1-FOUNDATION.md) §7 | 8–10 person MVP team, workstream ownership, ceremonies |
| 8 | Technical Risks | [PHASE-1-FOUNDATION.md](./PHASE-1-FOUNDATION.md) §8 | Concurrency, payments, logistics, scalability, security |
| 9 | Future Readiness | [PHASE-1-FOUNDATION.md](./PHASE-1-FOUNDATION.md) §9 | Multi-provider, category expansion, AI, subscriptions, warehouse, international |

---

## Key Decisions Made

| Decision | Choice | Reason |
|---|---|---|
| Backend architecture | Modular monolith | Fast MVP; extractable modules later |
| Frontend architecture | Next.js App Router with route groups | Persona-based auth boundaries (shop, seller, admin) |
| Booking safety | Redis lock + DB constraint + hold TTL | Layered defense against double booking |
| External integrations | Gateway/adapter pattern | Swap Razorpay/Shadowfax without domain changes |
| Catalog model | Category-agnostic with dynamic attributes | Supports future verticals without redesign |
| Repository layout | Monorepo (`frontend/`, `backend/`, `docs/`) | Single source of truth |
| MVP checkout | Single-item (no cart) | Reduce complexity; cart in Phase 2 |
| Brand | **Closiq** | Stakeholder confirmed |
| Home trial | **Mandatory platform-wide** | Core trust differentiator; enforced in booking state machine |
| Registration | **Phone OTP required** | India market trust and identity |
| Geography | **Mumbai launch, pan-India architecture** | Pincode serviceability abstraction from day one |

---

## UX Improvements Proposed (vs. Prototype)

These improvements maintain the premium minimal aesthetic while strengthening the product:

1. **Unified account hub** — Wishlist, orders, profile, and seller activation in one place
2. **Availability-first PDP** — Server-driven calendar; unavailable dates disabled upfront
3. **Guest browse** — Login gated at checkout only (registration requires phone OTP)
4. **Progressive checkout** — Address → date confirm → payment in discrete steps
5. **Notification deep links** — Trial-ready and delivery events open order detail directly
6. **Seller calendar as primary view** — Date-based inventory is the operational center
7. **Consistent design system** — Seller/admin use same tokens, not generic admin UI
8. **Pincode gate at checkout** — Mumbai serviceability validated before payment; pan-India browse allowed

---

## What Was Explicitly NOT Done (By Design)

Per Phase 1 scope:

- ❌ No application source code
- ❌ No API endpoint definitions
- ❌ No database table schemas
- ❌ No Docker/infrastructure configs
- ❌ No CI/CD pipelines
- ❌ No frontend or backend project initialization

These are scheduled for **Phase 2 — Project Scaffolding**.

---

## Repository Structure Created

```
closiq/                           # rename from vatriq/ recommended in Phase 2
└── docs/
    ├── PHASE-1-FOUNDATION.md    # Full planning document
    └── PHASE-1-SUMMARY.md       # This file
```

---

## Diagrams Included

The foundation document contains Mermaid diagrams for:

- Overall system architecture
- Customer rental flow (mandatory trial)
- Seller operational flow
- Admin flow
- Mumbai → pan-India launch strategy
- Booking concurrency sequence
- Multi-provider gateway pattern
- Implementation Gantt chart
- MVP team structure

---

## MVP Scope Summary

**In MVP (P0/P1):**

- Auth with **phone OTP at registration**, browse, search, product detail with calendar
- Booking with concurrency control
- Razorpay checkout (rental + deposit)
- Shadowfax delivery and return (**Mumbai pincodes**)
- Pincode serviceability check (pan-India-ready)
- **Mandatory** 15-minute home trial flow
- Seller listing management and basic wallet
- Admin seller verification and flat commission

**Deferred post-MVP:**

- Wishlist, reviews, multi-item cart
- FCM push notifications
- Advanced analytics and commission tiers
- Expansion beyond Mumbai (architecture ready)

**Deferred decisions (before implementation):**

- Minimum rental period
- Deposit refund SLA

---

## Remaining Open Questions

1. **Commission model** — flat % vs category-based (Phase 2 admin design)
2. **Minimum rental period** — decide before checkout (Phase 3)
3. **Deposit refund SLA** — decide before payments (Phase 4)

---

## Recommended Next Steps (Phase 2)

1. Rename repo folder `vatriq/` → `closiq/` (optional but recommended for consistency)
2. Initialize monorepo: `frontend/` (Next.js 15) + `backend/` (Spring Boot 3, `com.closiq`)
3. Implement design tokens from prototype palette in Tailwind
4. Docker Compose for PostgreSQL + Redis
5. Auth scaffold with **phone OTP** (JWT) on both sides
6. Pincode serviceability module (Mumbai Shadowfax config)
7. CI pipeline (lint, test, build)

---

## Document Index

| Document | Purpose |
|---|---|
| [PHASE-1-FOUNDATION.md](./PHASE-1-FOUNDATION.md) | Complete architecture and planning reference |
| [PHASE-1-SUMMARY.md](./PHASE-1-SUMMARY.md) | Executive summary of Phase 1 work (this file) |

---

*Phase 1 complete. Stakeholder decisions recorded. Ready for Phase 2 scaffolding.*

# Phase 2 — What We Did

**Project:** Closiq (Premium Clothing Rental Marketplace)  
**Phase:** 2 — Frontend Architecture & UI Development  
**Date:** August 4, 2026  
**Status:** Architecture complete (no frontend code scaffolded yet)

---

## Overview

Phase 2 defines the complete frontend architecture for the Closiq customer + seller web application. All data is mocked. No backend, API, or database work was done.

**Admin application is out of scope** — separate app per Phase 1.

---

## Deliverables

| # | Deliverable | Location |
|---|---|---|
| 1 | Complete screen list (52 routes + 8 utility states) | [PHASE-2-FRONTEND-ARCHITECTURE.md §1](./PHASE-2-FRONTEND-ARCHITECTURE.md) |
| 2 | Navigation architecture | [§2](./PHASE-2-FRONTEND-ARCHITECTURE.md) |
| 3 | Next.js routing structure | [§3](./PHASE-2-FRONTEND-ARCHITECTURE.md) |
| 4 | Component library (72 components) | [§4](./PHASE-2-FRONTEND-ARCHITECTURE.md) |
| 5 | Design system | [§5](./PHASE-2-FRONTEND-ARCHITECTURE.md) |
| 6 | State management strategy | [§6](./PHASE-2-FRONTEND-ARCHITECTURE.md) |
| 7 | Mock JSON data (12 files) | [mock-data/](./mock-data/) |
| 8 | Folder structure (feature-based) | [§8](./PHASE-2-FRONTEND-ARCHITECTURE.md) |
| 9 | UX improvements | [§9](./PHASE-2-FRONTEND-ARCHITECTURE.md) |
| 10 | Frontend best practices | [§10](./PHASE-2-FRONTEND-ARCHITECTURE.md) |

---

## Mock Data Files

| File | Purpose |
|---|---|
| `products.json` | 4 rental products with variants, pricing, Mumbai pincodes |
| `categories.json` | 5 occasions (Wedding, Festival, Office, Party, New in) |
| `orders.json` | 3 customer orders with full timelines |
| `bookings.json` | 3 seller bookings including blocked dates |
| `wishlist.json` | 3 wishlist items |
| `seller-dashboard.json` | Dashboard summary, tasks, verification |
| `reviews.json` | 4 customer reviews with photos |
| `notifications.json` | 5 notifications (trial, delivery, seller) |
| `user-profile.json` | Dual-role user (customer + seller), Mumbai addresses |
| `wallet.json` | Earnings, payouts, commission transactions |
| `analytics.json` | Seller metrics and top products |
| `availability.json` | Date availability per product variant |

---

## Key Architecture Decisions

| Decision | Choice |
|---|---|
| App model | Single app, Customer ↔ Seller mode switch |
| Admin | Separate application (not in this frontend) |
| Data layer | Service interface + mock impl → API impl swap |
| Server state | TanStack Query with query key factories |
| Forms | React Hook Form + Zod |
| Routing | Next.js 15 App Router with route groups |
| Auth | Phone OTP; middleware + layout guards |
| Design | Closiq tokens from prototype (Fraunces, paper/ink/gold/rose) |
| Trial UX | Mandatory platform-wide; TrialTicket on order detail |
| Geography | Browse pan-India; pincode gate at checkout (Mumbai MVP) |

---

## Navigation Summary

| Context | Desktop | Mobile |
|---|---|---|
| Customer | Top nav (occasions, search, wishlist, account) | Bottom tab bar (5 tabs) |
| Seller | Left sidebar | Tab bar + "More" sheet |
| Checkout | Minimal header + stepper | Same, full-width |
| Auth | Centered card, no nav | Same |

---

## What Was NOT Done (By Design)

- ❌ No Next.js project initialization
- ❌ No React components implemented
- ❌ No backend / Spring Boot code
- ❌ No API definitions
- ❌ No database schemas

---

## Recommended Next Steps (Phase 2 Implementation)

1. `pnpm create next-app` in `frontend/` with TypeScript, Tailwind, App Router
2. Install shadcn/ui, TanStack Query, RHF, Zod, Lucide, Framer Motion
3. Configure design tokens in `globals.css` + `tailwind.config.ts`
4. Copy mock data to `frontend/src/mocks/data/`
5. Implement providers (Query, Auth, AppMode)
6. Build shared layout (TopNav, BottomNav, SellerSidebar)
7. Implement auth screens (Login, Signup, OTP)
8. Build Home → PLP → PDP → Checkout flow with mock services
9. Build Orders + TrialTicket
10. Build Seller dashboard shell + listings

---

## Document Index

| Document | Purpose |
|---|---|
| [PHASE-1-FOUNDATION.md](./PHASE-1-FOUNDATION.md) | Product & platform planning |
| [PHASE-2-FRONTEND-ARCHITECTURE.md](./PHASE-2-FRONTEND-ARCHITECTURE.md) | Frontend architecture (this phase) |
| [PHASE-2-SUMMARY.md](./PHASE-2-SUMMARY.md) | This file |

---

*Phase 2 architecture complete. Ready for frontend implementation.*

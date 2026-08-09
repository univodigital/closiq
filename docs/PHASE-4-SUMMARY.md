# Phase 4 — What We Did

**Project:** Closiq (Premium Clothing Rental Marketplace)  
**Phase:** 4 — Database & Domain Model Design  
**Date:** August 4, 2026  
**Status:** Complete (documentation only — no code, SQL, or Java)

---

## Overview

Phase 4 produced the **production-grade database schema and domain model** that backend engineers will implement. The design follows Domain-Driven Design, aligns with Phase 1 architecture decisions and Phase 3 API contracts, and supports multi-vertical expansion without major redesign.

---

## Deliverable

| # | Deliverable | Location | Summary |
|---|---|---|---|
| 1 | Database & Domain Design | [PHASE-4-DATABASE-DOMAIN-DESIGN.md](./PHASE-4-DATABASE-DOMAIN-DESIGN.md) | Full specification — 11 domains, 63 entities, ER diagrams, deep dives |

---

## Domains Defined

| Domain | Key Aggregates |
|---|---|
| **Identity** | User, Role, Address, OtpSession |
| **Catalog** | Product, ProductVariant, Category, Brand, MediaAsset |
| **Inventory** | InventoryItem, InventoryReservation, InventoryBlock |
| **Booking** | Booking, TrialSession, ReturnRequest, Inspection |
| **Payments** | Payment, Refund, Wallet, Settlement, Coupon |
| **Logistics** | Shipment, ShipmentEvent, LogisticsProvider |
| **Reviews** | Review, ReviewModeration |
| **Notifications** | Notification, NotificationPreference, NotificationDelivery |
| **Admin** | SellerApplication, KycDocument, AuditLog, PlatformConfig |
| **Analytics** | Event-driven read models (future) |
| **Support** | SupportTicket, SupportTicketMessage |

---

## Key Design Decisions

| Decision | Choice | Reason |
|---|---|---|
| Physical inventory model | ProductVariant → N InventoryItems | Accurate date availability; per-unit lifecycle |
| Double-booking prevention | Redis lock + PostgreSQL exclusion constraint + serializable TX | Phase 1 ADR-004 layered defense |
| Money storage | BIGINT whole INR + currency_code | Matches Phase 3 API contract |
| History tables | Append-only (timeline, history, events, ledger) | Full audit; immutable financial records |
| Category extensibility | CategoryAttribute + ProductAttributeValue (EAV) | Future verticals without schema migration |
| Provider abstraction | provider_code FKs + gateway pattern | Razorpay/Shadowfax swappable |
| Trial enforcement | TrialSession 1:1 with Booking | Mandatory 15-min platform-wide trial |
| Deposit model | Platform escrow via Razorpay | Never flows through seller wallet |

---

## Entity Count

| Category | Count |
|---|---|
| Total entities specified | 63 |
| Core transactional | 45 |
| Append-only history/event | 10 |
| Future-ready (documented) | 8+ |

---

## Diagrams Included

- Domain interaction map (Mermaid flowchart)
- Complete ER diagram (63 entities, relationships)
- Inventory & booking focus ER diagram
- Inventory lifecycle state machine
- Booking lifecycle state machine (Phase 3 aligned)
- Availability engine sequence diagram
- Payment flow sequence diagram
- Logistics webhook flow
- Future extensibility mind map

---

## Inherited from Prior Phases

| Phase | What carried forward |
|---|---|
| Phase 1 | Modular monolith domains, Redis+DB booking ADRs, gateway pattern, category-agnostic catalog |
| Phase 3 | Booking status enum (16 states), payment/shipment/refund enums, money format, trial flow, idempotency |

---

## What Was Explicitly NOT Done (By Design)

- ❌ Java entities or JPA mappings
- ❌ Spring Boot code
- ❌ SQL DDL scripts
- ❌ Liquibase/Flyway migrations
- ❌ Repository implementations

---

## Recommended Next Steps (Phase 5 — Backend Scaffolding)

1. Initialize Spring Boot modular monolith (`backend/`) per Phase 1 structure
2. Generate Flyway migrations from §5 entity specifications
3. Implement Identity + Catalog domains first (unblocks frontend integration)
4. Inventory availability engine + booking hold (critical path)
5. Razorpay + Shadowfax sandbox with webhook handlers
6. Contract tests against Phase 3 API spec
7. OpenAPI generation from implemented controllers

---

## Document Index

| Document | Purpose |
|---|---|
| [PHASE-1-FOUNDATION.md](./PHASE-1-FOUNDATION.md) | Architecture and planning |
| [PHASE-2-FRONTEND-ARCHITECTURE.md](./PHASE-2-FRONTEND-ARCHITECTURE.md) | Frontend screens and components |
| [PHASE-3-API-CONTRACT.md](./PHASE-3-API-CONTRACT.md) | REST API contract |
| [PHASE-4-DATABASE-DOMAIN-DESIGN.md](./PHASE-4-DATABASE-DOMAIN-DESIGN.md) | Database & domain model (this phase) |
| [PHASE-4-SUMMARY.md](./PHASE-4-SUMMARY.md) | Executive summary (this file) |

---

*Phase 4 complete. Database and domain design ready for backend implementation.*

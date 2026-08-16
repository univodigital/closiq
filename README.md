# Closiq

Premium peer-to-peer clothing rental marketplace.

## Status

**Phase 1 — Foundation & Product Planning** ✅  
**Phase 2 — Frontend Architecture & UI Development** ✅ (architecture + mock data)  
**Phase 3 — API Contract Specification** ✅ (REST API documentation)  
**Phase 4 — Database & Domain Model Design** ✅ (schema & domain specification)  
**Phase 5 — Frontend Development** ✅ (Next.js app with mock services)  
**Phase 6 — Backend Development** ✅ (Auth through Seller Booking Management; Admin excluded per API contract)

Application code lives in `frontend/` and `backend/`. See `/docs` for planning and API contract.

## Documentation

| Document | Description |
|---|---|
| [docs/PROJECT-OVERVIEW.md](./docs/PROJECT-OVERVIEW.md) | **Start here** — tech stack, modules, advanced patterns (compact) |
| [docs/PHASE-1-FOUNDATION.md](./docs/PHASE-1-FOUNDATION.md) | Product architecture, roadmap, risks |
| [docs/PHASE-1-SUMMARY.md](./docs/PHASE-1-SUMMARY.md) | Phase 1 deliverables summary |
| [docs/PHASE-2-FRONTEND-ARCHITECTURE.md](./docs/PHASE-2-FRONTEND-ARCHITECTURE.md) | Frontend architecture, screens, components, design system |
| [docs/PHASE-2-SUMMARY.md](./docs/PHASE-2-SUMMARY.md) | Phase 2 deliverables summary |
| [docs/PHASE-3-API-CONTRACT.md](./docs/PHASE-3-API-CONTRACT.md) | REST API contract — frontend/backend single source of truth |
| [docs/PHASE-3-SUMMARY.md](./docs/PHASE-3-SUMMARY.md) | Phase 3 deliverables summary |
| [docs/PHASE-4-DATABASE-DOMAIN-DESIGN.md](./docs/PHASE-4-DATABASE-DOMAIN-DESIGN.md) | Database schema, domain model, ER diagrams |
| [docs/PHASE-4-SUMMARY.md](./docs/PHASE-4-SUMMARY.md) | Phase 4 deliverables summary |
| [backend/README.md](./backend/README.md) | Backend setup, module progress, API reference, env vars |
| [frontend/README.md](./frontend/README.md) | Frontend setup, architecture, mock login |
| [docs/mock-data/](./docs/mock-data/) | Mock JSON datasets (source; copied to `frontend/src/mocks/data/`) |

## Confirmed Decisions

- **Brand:** Closiq
- **Trial:** 15-minute home trial mandatory platform-wide
- **Auth:** Phone OTP required at registration
- **Launch:** Mumbai first, pan-India-ready architecture

## Tech Stack (Planned)

- **Frontend:** Next.js 15, React 19, TypeScript, Tailwind CSS, shadcn/ui
- **Backend:** Java 21, Spring Boot, PostgreSQL, Redis
- **Integrations:** AWS S3, Razorpay, Shadowfax, Firebase FCM

## License

Proprietary — All rights reserved.

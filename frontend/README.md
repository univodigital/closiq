# Closiq Frontend

Next.js customer + seller web application. **Mock data only** — no backend integration yet.

## Stack

- Next.js 16 (App Router) · React 19 · TypeScript
- Tailwind CSS 4 · shadcn-style UI primitives
- TanStack Query · React Hook Form · Zod
- Framer Motion · Lucide Icons · Sonner

## Quick start

```bash
cd frontend
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000)

### Mock login

1. Go to `/login`
2. Enter any 10-digit mobile number
3. OTP: **`123456`**

After login, protected routes (wishlist, orders, checkout, seller) work via session cookie.

## Architecture

```
src/
├── app/              # Routes (route groups: shop, auth, account, checkout, seller)
├── features/         # Feature modules (auth, products, orders, seller, …)
│   └── */services/   # Interface + mock impl — swap index.ts for API later
├── shared/           # Cross-feature components, types, constants
├── components/ui/    # Design system primitives
├── mocks/data/       # JSON from docs/mock-data
└── providers/        # Auth, Query, AppMode contexts
```

**Rule:** Components never import JSON directly. All data flows through `features/*/services`.

### Switching to real APIs

1. Implement `ApiProductService`, `ApiAuthService`, etc.
2. Update each feature's `services/index.ts`:

```typescript
export const productService =
  process.env.NEXT_PUBLIC_USE_MOCK === "true"
    ? mockProductService
    : apiProductService;
```

Components and hooks stay unchanged.

## Environment

| Variable | Default | Description |
|---|---|---|
| `NEXT_PUBLIC_USE_MOCK` | `true` | Use mock services |
| `NEXT_PUBLIC_API_URL` | — | Backend base URL (future) |

## Routes

| Area | Paths |
|---|---|
| Shop | `/`, `/products`, `/products/[slug]`, `/search`, `/occasions/[slug]` |
| Auth | `/login`, `/signup`, `/signup/verify` |
| Account | `/profile`, `/orders`, `/wishlist`, `/notifications` |
| Checkout | `/checkout/address`, `/review`, `/payment`, `/success` |
| Seller | `/seller`, `/seller/products`, `/seller/bookings`, `/seller/wallet`, … |

## Design

Premium minimal aesthetic from Phase 2:

- **Paper** `#FBF7F0` · **Ink** `#1E2A22` · **Gold** `#B8923F` · **Rose** `#A9525A`
- Fonts: Fraunces (display), Inter (body), IBM Plex Mono (prices)
- 2px radius · border elevation · 15-min trial USP

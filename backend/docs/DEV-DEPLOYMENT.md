# Dev deployment — GCP Cloud Run + Upstash Redis + Supabase

Deploy the Closiq backend on **GCP Cloud Run** (`asia-south1` / Mumbai) using the **`dev` Spring profile**, a dedicated **Supabase** dev database, and **Upstash Redis** (free tier, AWS Mumbai).

Frontend stays on Vercel: `https://closiq-three.vercel.app`

---

## Architecture

```
Vercel (closiq-three.vercel.app)
        │  NEXT_PUBLIC_API_URL
        ▼
GCP Cloud Run (Spring Boot, profile=dev, asia-south1)
        ├── Supabase Postgres (dev project, pooler :6543)
        └── Upstash Redis (AWS ap-south-1, TLS)
```

---

## 1. Local dev profile setup

### 1.1 Create `application-dev.properties`

This file is **gitignored**. Use the committed template:

```bash
cd backend
cp src/main/resources/application-dev.properties.example \
   src/main/resources/application-dev.properties
```

Edit `application-dev.properties` or export env vars (env vars override properties).

Optional: copy env reference for Cloud Run:

```bash
cp .env.dev.example .env.dev
# fill in secrets — .env.dev is gitignored
```

### 1.2 Active profile

`application.properties` sets:

```properties
spring.profiles.active=dev
```

Override locally if needed:

```bash
export SPRING_PROFILES_ACTIVE=dev
```

---

## 2. Supabase dev database

1. Create a **new Supabase project** for dev (separate from any test/prod data).
2. Region: **South Asia (Mumbai)** — `ap-south-1`.
3. From **Project Settings → Database → Connection string**:
   - Mode: **Transaction** (pooler)
   - IPv4
   - Port **6543**
4. Note:
   - Host: `aws-0-ap-south-1.pooler.supabase.com` (may vary slightly per project)
   - User: `postgres.<project-ref>`
   - Database: `postgres`

### 2.1 Flyway migrations — DDL only (no seed data)

Flyway runs automatically on backend startup (`spring.flyway.enabled=true`).

For a **fresh empty dev database**, apply **schema only** first. Do **not** run seed/data scripts until you want demo catalog users and products.

#### DDL migrations (run these — schema + safe backfills on empty DB)

| Version | File | Purpose |
|--------:|------|---------|
| V1 | `V1__identity_schema.sql` | Auth, users, OTP, refresh tokens |
| V2 | `V2__user_module_schema.sql` | Profiles, addresses, wishlist |
| V3 | `V3__seller_module_schema.sql` | Seller applications, KYC, wallet |
| V4 | `V4__catalog_schema.sql` | Categories, products, variants, images |
| V6 | `V6__inventory_schema.sql` | Inventory items, reservations, blocks |
| V8 | `V8__booking_schema.sql` | Bookings, checkout sessions |
| V9 | `V9__payment_schema.sql` | Payments, coupons, refunds |
| V10 | `V10__shipment_schema.sql` | Shipments, webhooks, events |
| V11 | `V11__review_schema.sql` | Reviews |
| V12 | `V12__notification_schema.sql` | Notifications |
| V13 | `V13__seller_product_idempotency.sql` | Seller product idempotency |
| V14 | `V14__seller_booking_fields.sql` | Seller booking columns |
| V15 | `V15__audit_columns.sql` | Audit columns |
| V16 | `V16__currency_code_varchar.sql` | Currency column type |
| V17 | `V17__business_identifiers.sql` | Product/user codes, sequences (backfill OK on empty DB) |
| V19 | `V19__media_asset_storage_provider.sql` | Media storage provider column |
| V20 | `V20__address_phone.sql` | Address phone column |
| V27 | `V27__user_profile_gender.sql` | Profile gender column |
| V28 | `V28__account_security.sql` | Account security |
| V29 | `V29__cart_and_checkout_batch.sql` | Cart, checkout batch |
| V30 | `V30__refund_idempotency.sql` | Refund idempotency |
| V31 | `V31__booking_inspection.sql` | Booking inspection |
| V32 | `V32__category_name_unique.sql` | Category unique name |
| V33 | `V33__checkout_batch_currency_varchar.sql` | Checkout batch currency |

#### Mixed migration — schema part required, seed part optional

| Version | File | Notes |
|--------:|------|-------|
| V21 | `V21__product_audience.sql` | **Required DDL:** `audience`, `garment_type` columns. **Optional data:** INSERTs for men/kids demo products (requires V5 categories). On empty DB, run only the `ALTER TABLE` / `UPDATE` / `NOT NULL` lines, or skip entire file until you run V5. |

#### Seed / data migrations — skip for now

| Version | File | Purpose |
|--------:|------|---------|
| V5 | `V5__catalog_seed.sql` | Demo categories, brands, products |
| V7 | `V7__inventory_seed.sql` | Demo inventory + sample booking |
| V22 | `V22__category_occasion_images.sql` | Updates category images (needs V5) |
| V23 | `V23__fix_broken_product_images.sql` | Data fix for seed products |
| V24 | `V24__inventory_men_kids_seed.sql` | Inventory for V21 products |
| V25 | `V25__seller_user_seed.sql` | Demo seller/customer users for OTP testing |

All migration files live in:

```
backend/src/main/resources/db/migration/
```

#### Applying DDL only on a fresh Supabase dev DB

**Option A — Let Flyway run on first Cloud Run deploy (simplest)**

Default Flyway runs **all** migrations including seeds. For a truly empty schema-only dev DB, temporarily **rename** seed files before first deploy (e.g. `V5__catalog_seed.sql.skip`), deploy, then restore names and mark those versions ignored — or use Option B.

**Option B — Manual SQL in Supabase (DDL only, recommended for clean dev DB)**

1. Supabase → **SQL Editor**
2. Run each **DDL** file above in version order (V1 → V33, skip V5, V7, V22–V25; handle V21 ALTER parts only if skipping seed INSERTs)
3. Insert into `flyway_schema_history` so Flyway skips re-applying (or set `spring.flyway.baseline-on-migrate=true` and baseline version **33** after manual apply)

**Option C — Local Flyway with targets (skip seeds by jumping versions)**

```bash
cd backend
# Requires local Maven + env vars pointing at Supabase dev
mvn flyway:migrate -Dflyway.target=4    # through catalog schema
mvn flyway:migrate -Dflyway.target=6    # skip V5 seed
mvn flyway:migrate -Dflyway.target=20   # skip V7, V21–V22 if not yet applied
# Continue through V33 as needed — see DDL table above
```

> **Do not run** V5, V7, V22, V23, V24, V25 until you want demo data.

---

## 3. Upstash Redis (dev)

Already created: **`closiq-redis`**, AWS **Mumbai (`ap-south-1`)**, **Free** plan.

From [Upstash Console](https://console.upstash.com) → database **Details**:

| Upstash field | Env var |
|---------------|---------|
| Endpoint | `REDIS_HOST` |
| Port (6379) | `REDIS_PORT` |
| Password | `REDIS_PASSWORD` |
| TLS enabled | `REDIS_SSL=true` |

Test from your machine ([Upstash getting started](https://upstash.com/docs/redis/overall/getstarted)):

```bash
redis-cli --tls -a YOUR_PASSWORD -h YOUR_ENDPOINT.upstash.io -p 6379 ping
# Expected: PONG
```

Spring Boot (dev profile) maps these via `application-dev.properties.example`.

---

## 4. GCP Cloud Run setup (step by step)

### 4.1 Prerequisites

- [Google Cloud account](https://cloud.google.com)
- [gcloud CLI](https://cloud.google.com/sdk/docs/install)
- Docker (local build) or Cloud Build

### 4.2 Create GCP project

```bash
gcloud auth login
gcloud projects create closiq-dev-YOUR_SUFFIX --name="Closiq Dev"
gcloud config set project closiq-dev-YOUR_SUFFIX

# Link billing (required for Cloud Run; free tier still applies)

gcloud services enable \
  run.googleapis.com \
  artifactregistry.googleapis.com \
  cloudbuild.googleapis.com \
  secretmanager.googleapis.com
```

Region for everything: **`asia-south1`** (Mumbai).

### 4.3 Store secrets in Secret Manager

```bash
echo -n "YOUR_SUPABASE_DB_PASSWORD" | gcloud secrets create db-password --data-file=-
echo -n "YOUR_JWT_SECRET_MIN_256_BITS" | gcloud secrets create jwt-secret --data-file=-
echo -n "YOUR_UPSTASH_REDIS_PASSWORD" | gcloud secrets create redis-password --data-file=-
echo -n "YOUR_CLOUDINARY_API_SECRET" | gcloud secrets create cloudinary-api-secret --data-file=-
echo -n "YOUR_RAZORPAY_KEY_SECRET" | gcloud secrets create razorpay-key-secret --data-file=-
```

Grant Cloud Run service account access (after first deploy or preemptively):

```bash
PROJECT_NUMBER=$(gcloud projects describe $(gcloud config get-value project) --format='value(projectNumber)')
SA="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"

for SECRET in db-password jwt-secret redis-password cloudinary-api-secret razorpay-key-secret; do
  gcloud secrets add-iam-policy-binding "$SECRET" \
    --member="serviceAccount:${SA}" \
    --role="roles/secretmanager.secretAccessor"
done
```

### 4.4 Build and push Docker image

```bash
cd /path/to/Closiq

gcloud artifacts repositories create closiq \
  --repository-format=docker \
  --location=asia-south1 \
  --description="Closiq backend images"

gcloud auth configure-docker asia-south1-docker.pkg.dev

export PROJECT_ID=$(gcloud config get-value project)
docker build -t asia-south1-docker.pkg.dev/${PROJECT_ID}/closiq/backend:dev-v1 ./backend
docker push asia-south1-docker.pkg.dev/${PROJECT_ID}/closiq/backend:dev-v1
```

### 4.5 Deploy to Cloud Run

Replace placeholders (`YOUR_*`) with your values:

```bash
export PROJECT_ID=$(gcloud config get-value project)

gcloud run deploy closiq-backend-dev \
  --image asia-south1-docker.pkg.dev/${PROJECT_ID}/closiq/backend:dev-v1 \
  --region asia-south1 \
  --platform managed \
  --allow-unauthenticated \
  --port 8081 \
  --memory 1Gi \
  --cpu 1 \
  --min-instances 0 \
  --max-instances 3 \
  --timeout 300 \
  --set-env-vars "\
SPRING_PROFILES_ACTIVE=dev,\
SERVER_PORT=8081,\
DB_HOST=aws-0-ap-south-1.pooler.supabase.com,\
DB_PORT=6543,\
DB_NAME=postgres,\
DB_USER=postgres.YOUR_SUPABASE_PROJECT_REF,\
DB_POOL_SIZE=3,\
REDIS_HOST=YOUR_ENDPOINT.upstash.io,\
REDIS_PORT=6379,\
REDIS_SSL=true,\
CORS_ALLOWED_ORIGINS=https://closiq-three.vercel.app,http://localhost:3000,\
APP_BASE_URL=https://closiq-three.vercel.app,\
REFRESH_COOKIE_SECURE=false,\
SUPABASE_PROJECT_NAME=closiq-dev,\
SUPABASE_REGION=ap-south-1,\
CLOUDINARY_STUB_ENABLED=false,\
CLOUDINARY_FOLDER=closiq-dev,\
RAZORPAY_STUB_ENABLED=true,\
SHADOWFAX_STUB_ENABLED=true,\
MAIL_ENABLED=false" \
  --set-secrets "\
DB_PASSWORD=db-password:latest,\
JWT_SECRET=jwt-secret:latest,\
REDIS_PASSWORD=redis-password:latest,\
CLOUDINARY_API_SECRET=cloudinary-api-secret:latest,\
RAZORPAY_KEY_SECRET=razorpay-key-secret:latest"
```

Non-secret env vars you still set via `--set-env-vars` or Cloud Run console:

```env
CLOUDINARY_CLOUD_NAME=your-cloud
CLOUDINARY_API_KEY=your-key
RAZORPAY_KEY_ID=rzp_test_xxx
```

### 4.6 Verify deployment

```bash
export SERVICE_URL=$(gcloud run services describe closiq-backend-dev \
  --region asia-south1 --format='value(status.url)')

curl "${SERVICE_URL}/actuator/health"
curl "${SERVICE_URL}/api/v1/categories"
```

Check Cloud Run logs if Flyway fails:

```bash
gcloud run services logs read closiq-backend-dev --region asia-south1 --limit 50
```

---

## 5. Vercel frontend

**Settings → Environment Variables:**

| Variable | Value |
|----------|-------|
| `NEXT_PUBLIC_API_URL` | `https://YOUR-CLOUD-RUN-URL/api/v1` |
| `NEXT_PUBLIC_RAZORPAY_KEY_ID` | Razorpay test key (if not using stub) |

Redeploy Vercel after changing env vars.

---

## 6. Complete environment variable reference (dev profile)

### Required

| Variable | Description | Example |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Spring profile | `dev` |
| `SERVER_PORT` | HTTP port (Cloud Run must match container port) | `8081` |
| `DB_HOST` | Supabase pooler host | `aws-0-ap-south-1.pooler.supabase.com` |
| `DB_PORT` | Supabase pooler port | `6543` |
| `DB_NAME` | Database name | `postgres` |
| `DB_USER` | Supabase user | `postgres.xxxxxxxxx` |
| `DB_PASSWORD` | Supabase password | *(secret)* |
| `DB_POOL_SIZE` | Hikari pool size | `3` |
| `REDIS_HOST` | Upstash endpoint | `xxx.upstash.io` |
| `REDIS_PORT` | Redis port | `6379` |
| `REDIS_PASSWORD` | Upstash password | *(secret)* |
| `REDIS_SSL` | TLS for Upstash | `true` |
| `JWT_SECRET` | JWT signing key (≥256 bits) | *(secret)* |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origins (comma-separated, no trailing slash) | `https://closiq-three.vercel.app,http://localhost:3000` |

### Recommended

| Variable | Description | Dev default |
|----------|-------------|-------------|
| `APP_BASE_URL` | Links in emails/notifications | `https://closiq-three.vercel.app` |
| `REFRESH_COOKIE_SECURE` | HTTPS-only refresh cookie | `false` (cross-origin dev); `true` with custom domain |
| `SUPABASE_PROJECT_NAME` | Metadata | `closiq-dev` |
| `SUPABASE_REGION` | Metadata | `ap-south-1` |

### Cloudinary (real uploads)

| Variable | Description |
|----------|-------------|
| `CLOUDINARY_CLOUD_NAME` | Cloud name |
| `CLOUDINARY_API_KEY` | API key |
| `CLOUDINARY_API_SECRET` | API secret *(secret)* |
| `CLOUDINARY_FOLDER` | Upload folder | `closiq-dev` |
| `CLOUDINARY_STUB_ENABLED` | Skip real uploads | `false` |

`CLOSIQ_CLOUDINARY_*` aliases are also supported.

### Payments & logistics

| Variable | Description | Dev default |
|----------|-------------|-------------|
| `RAZORPAY_KEY_ID` | Razorpay key | test key |
| `RAZORPAY_KEY_SECRET` | Razorpay secret *(secret)* | |
| `RAZORPAY_STUB_ENABLED` | Stub gateway | `true` |
| `SHADOWFAX_WEBHOOK_SECRET` | Webhook HMAC | stub value |
| `SHADOWFAX_STUB_ENABLED` | Stub logistics | `true` |

### Email (optional)

| Variable | Description | Dev default |
|----------|-------------|-------------|
| `MAIL_ENABLED` | Send via Brevo | `false` |
| `BREVO_API_KEY` | Brevo API key | |
| `BREVO_SENDER_EMAIL` | Sender email | |
| `MAIL_FROM` | From address | `noreply@closiq.com` |

### Spring Boot direct bindings (optional alternatives)

These work even if not listed in `application-dev.properties`:

```env
SPRING_DATA_REDIS_PASSWORD=<same as REDIS_PASSWORD>
SPRING_DATA_REDIS_SSL_ENABLED=true
```

---

## 7. Auth cookies (Vercel + Cloud Run)

Frontend: `closiq-three.vercel.app`  
Backend: `*.run.app`  

Different sites → refresh token cookie (`SameSite=Strict`) may not renew after 15 minutes.

Dev mitigations:

1. Set `REFRESH_COOKIE_SECURE=false` (already in dev template) — still may fail cross-site with Strict
2. Use **Vercel rewrites** to proxy `/api/v1` to Cloud Run (same-origin)
3. Later: custom domains `app.closiq.com` + `api.closiq.com`

---

## 8. Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| 60s+ first request | Cloud Run cold start + JVM | Normal on free tier; set `--min-instances 1` (costs $) |
| Health UP, booking fails | Redis env wrong | Check `REDIS_SSL=true`, password, Upstash endpoint |
| Flyway fails on deploy | Seed migration without deps | Apply DDL-only path (§2.1) |
| CORS error in browser | Origin mismatch | Exact match in `CORS_ALLOWED_ORIGINS` |
| 401 after 15 min | Cross-site refresh cookie | See §7 |
| Supabase connection timeout | Wrong pooler port | Use **6543**, not 5432 |

---

## 9. File reference

| File | Purpose |
|------|---------|
| `src/main/resources/application-dev.properties.example` | Committed template — copy to gitignored `application-dev.properties` |
| `backend/.env.dev.example` | Env var checklist for Cloud Run |
| `backend/.gitignore` | Ignores `application-dev.properties`, `.env.dev` |
| `backend/Dockerfile` | Container build (Java 21, port 8081) |
| `src/main/resources/db/migration/V*.sql` | Flyway migrations |

---

## 10. Next steps after infra is up

1. Confirm `/actuator/health` → `UP`
2. Register/login from Vercel (OTP in logs if `closiq.otp.console-log-enabled=true`)
3. When you need demo catalog data, run seed migrations V5, V7, V21 (INSERTs), V22–V25
4. Add custom domain on Cloud Run when ready for production-like auth

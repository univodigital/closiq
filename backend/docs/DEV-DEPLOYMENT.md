# Dev deployment — AWS + Upstash Redis + Supabase

Deploy the Closiq backend on **AWS** (`ap-south-1` / Mumbai) using the **`dev` Spring profile**, a dedicated **Supabase** dev database, and **Upstash Redis** (free tier, AWS Mumbai).

Frontend stays on Vercel: `https://closiq-three.vercel.app`

---

## Architecture

```
Vercel (closiq-three.vercel.app)
        │  NEXT_PUBLIC_API_URL
        ▼
AWS (ap-south-1 Mumbai)
  ├── Option A: EC2 + Docker          ← recommended (always-on, free tier eligible)
  └── Option B: App Runner + ECR      ← simpler managed containers
        │
        ├── Supabase Postgres (dev project, pooler :6543)
        └── Upstash Redis (AWS ap-south-1, TLS)
```

| Component | Service | Region |
|-----------|---------|--------|
| Frontend | Vercel | Edge |
| Backend | **AWS EC2** or **App Runner** | `ap-south-1` |
| Database | Supabase | `ap-south-1` |
| Redis | Upstash | AWS `ap-south-1` |

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

Optional: copy env reference for AWS deployment:

```bash
cp .env.dev.example .env.dev
# fill in secrets — .env.dev is gitignored
```

### 1.2 Active profile

`application.properties` sets:

```properties
spring.profiles.active=dev
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
   - Host: `aws-0-ap-south-1.pooler.supabase.com` (may vary per project)
   - User: `postgres.<project-ref>`
   - Database: `postgres`

### 2.1 Database schema — DDL only (no seed data)

For a **fresh empty dev database**, run the consolidated DDL file:

```
backend/docs/dev-ddl/closiq-dev-schema.sql
```

#### Quick apply (Supabase SQL Editor)

1. Supabase → **SQL Editor** → New query
2. Paste the contents of `backend/docs/dev-ddl/closiq-dev-schema.sql`
3. Run
4. Set in `application-dev.properties` (or AWS env):

```properties
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=33
```

Future Flyway migrations (V34+) apply automatically on backend startup.

#### What's included vs excluded

| Included | Excluded (run later for demo data) |
|----------|-------------------------------------|
| V1–V4, V6, V8–V21 (schema), V26–V33 | V5, V7, V22, V23, V24, V25 |
| Reference: roles, pincodes, logistics provider | Demo catalog, inventory, seller users |

See [`dev-ddl/README.md`](dev-ddl/README.md) for details.

---

## 3. Upstash Redis (dev)

Database: **`closiq-redis`**, AWS **Mumbai (`ap-south-1`)**, **Free** plan.

From [Upstash Console](https://console.upstash.com) → **Details**:

| Upstash field | Env var | Example |
|---------------|---------|---------|
| Endpoint | `REDIS_HOST` | `loving-gnat-93714.upstash.io` |
| Port | `REDIS_PORT` | `6379` |
| Password | `REDIS_PASSWORD` | *(from console)* |
| TLS | `REDIS_SSL` | `true` |

Test:

```bash
redis-cli --tls -u "redis://default:YOUR_PASSWORD@YOUR_ENDPOINT.upstash.io:6379" ping
# Expected: PONG
```

> Upstash REST URL/token (`UPSTASH_REDIS_REST_*`) is **not** used by Spring Boot — use TCP + TLS above.

---

## 4. AWS backend deployment

AWS requires a billing account on file. New accounts get **12 months free tier** for `t2.micro` / `t3.micro` EC2 (750 hours/month in `ap-south-1`).

### Choose a deployment option

| Option | Best for | Cost (dev) | Always-on |
|--------|----------|------------|-----------|
| **A — EC2 + Docker** | Pre-revenue, full control | Free tier → ~₹600–800/mo after | Yes |
| **B — App Runner + ECR** | Managed containers, less ops | ~$5–15/mo | Yes (min capacity) |

---

## 4A. EC2 + Docker (recommended)

Run the existing `backend/Dockerfile` on a small EC2 instance in Mumbai.

### 4A.1 Prerequisites

- [AWS account](https://aws.amazon.com)
- [AWS CLI v2](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html) configured (`aws configure`)
- SSH key pair

### 4A.2 Launch EC2 instance

**AWS Console → EC2 → Launch instance**

| Setting | Value |
|---------|-------|
| Name | `closiq-backend-dev` |
| AMI | Ubuntu 24.04 LTS |
| Instance type | `t3.micro` (free tier eligible) or `t3.small` (1 GiB RAM is tight for Spring Boot — prefer **t3.small** if not on free tier) |
| Key pair | Create or select |
| Region | **Asia Pacific (Mumbai) `ap-south-1`** |

**Security group — inbound rules:**

| Type | Port | Source | Purpose |
|------|------|--------|---------|
| SSH | 22 | Your IP | Admin |
| HTTP | 80 | `0.0.0.0/0` | Caddy/Let's Encrypt |
| HTTPS | 443 | `0.0.0.0/0` | Public API |
| Custom TCP | 8081 | Your IP only (optional) | Direct test before HTTPS |

**Storage:** 20 GB gp3 (free tier eligible).

Launch instance and note the **public IP** or attach an **Elastic IP** (recommended — static IP for DNS).

### 4A.3 Install Docker on EC2

```bash
ssh -i ~/.ssh/your-key.pem ubuntu@YOUR_EC2_PUBLIC_IP

sudo apt-get update
sudo apt-get install -y docker.io docker-compose-v2 git
sudo usermod -aG docker ubuntu
# log out and back in
```

### 4A.4 Deploy backend container

**Option 1 — Build on the server**

```bash
git clone https://github.com/YOUR_ORG/Closiq.git
cd Closiq/backend

# Create env file from template
cp .env.dev.example .env.dev
nano .env.dev   # fill in all secrets (see §6)

docker build -t closiq-backend:dev .
docker run -d \
  --name closiq-backend \
  --env-file .env.dev \
  -p 8081:8081 \
  --restart unless-stopped \
  closiq-backend:dev
```

**Option 2 — Push to ECR, pull on EC2** (same image as App Runner path in §4B)

### 4A.5 HTTPS with Caddy (free TLS)

Install Caddy on EC2 to terminate HTTPS and proxy to port 8081:

```bash
sudo apt install -y debian-keyring debian-archive-keyring apt-transport-https curl
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | sudo tee /etc/apt/sources.list.d/caddy-stable.list
sudo apt update && sudo apt install -y caddy
```

`/etc/caddy/Caddyfile`:

```
api.yourdomain.com {
    reverse_proxy localhost:8081
}
```

Or for quick dev testing without a domain, use the EC2 public IP on port 8081 (HTTP only — set `REFRESH_COOKIE_SECURE=false`).

```bash
sudo systemctl reload caddy
```

Point DNS `A` record → EC2 Elastic IP.

### 4A.6 Verify

```bash
curl http://localhost:8081/actuator/health
curl https://api.yourdomain.com/actuator/health
curl https://api.yourdomain.com/api/v1/categories
```

View logs:

```bash
docker logs -f closiq-backend
```

---

## 4B. App Runner + ECR (managed containers)

Simpler than EC2 if you prefer not to manage the VM. Requires ECR + App Runner (small monthly cost).

### 4B.1 Create ECR repository

```bash
aws configure   # set region ap-south-1
export AWS_REGION=ap-south-1
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

aws ecr create-repository \
  --repository-name closiq/backend \
  --region $AWS_REGION
```

### 4B.2 Build and push Docker image

```bash
cd /path/to/Closiq/backend

aws ecr get-login-password --region $AWS_REGION | \
  docker login --username AWS --password-stdin \
  ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com

docker build -t closiq-backend:dev .
docker tag closiq-backend:dev \
  ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/closiq/backend:dev-v1
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/closiq/backend:dev-v1
```

### 4B.3 Create App Runner service

**AWS Console → App Runner → Create service**

| Setting | Value |
|---------|-------|
| Source | Container registry → Amazon ECR |
| Image | `closiq/backend:dev-v1` |
| Port | `8081` |
| CPU / Memory | 1 vCPU, 2 GB (Spring Boot needs ≥1 GB) |
| Environment variables | From §6 (non-secrets in plain env) |
| Secrets | Use **AWS Secrets Manager** or **SSM Parameter Store** for passwords |

CLI example (after creating an App Runner access role — see AWS docs):

```bash
# Store secrets in SSM Parameter Store (Standard tier — free)
aws ssm put-parameter --name /closiq/dev/DB_PASSWORD --type SecureString --value "YOUR_SUPABASE_PASSWORD"
aws ssm put-parameter --name /closiq/dev/JWT_SECRET --type SecureString --value "YOUR_JWT_SECRET"
aws ssm put-parameter --name /closiq/dev/REDIS_PASSWORD --type SecureString --value "YOUR_UPSTASH_PASSWORD"
```

In App Runner env config, reference SSM secrets per [AWS App Runner docs](https://docs.aws.amazon.com/apprunner/latest/dg/manage-configure.html).

**Health check:** path `/actuator/health`, port `8081`.

After deploy, note the App Runner URL: `https://xxxxx.ap-south-1.awsapprunner.com`

### 4B.4 Redeploy on code changes

```bash
docker build -t closiq-backend:dev .
docker tag closiq-backend:dev ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/closiq/backend:dev-v2
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/closiq/backend:dev-v2
# Update App Runner service to new tag (Console or aws apprunner update-service)
```

---

## 5. Vercel frontend

**Settings → Environment Variables:**

| Variable | Value |
|----------|-------|
| `NEXT_PUBLIC_API_URL` | `https://closiq-three.vercel.app/api/v1` (same-origin proxy) |
| `API_PROXY_TARGET` | Backend base URL, no path — e.g. `http://YOUR_EC2_PUBLIC_IP:8081` or App Runner origin without `/api/v1` |
| `NEXT_PUBLIC_RAZORPAY_KEY_ID` | Razorpay test key (if not using stub) |

The Next.js route at `frontend/src/app/api/v1/[...path]/route.ts` proxies browser requests to `API_PROXY_TARGET`. Without it, `/api/v1/*` returns 503 on Vercel.

Redeploy Vercel after changing env vars.

Set backend CORS to match:

```env
CORS_ALLOWED_ORIGINS=https://closiq-three.vercel.app,http://localhost:3000
```

---

## 6. Complete environment variable reference (dev profile)

Use in `.env.dev` (EC2 Docker), App Runner env config, or `application-dev.properties`.

### Required

| Variable | Description | Example |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Spring profile | `dev` |
| `SERVER_PORT` | HTTP port | `8081` |
| `DB_HOST` | Supabase pooler host | `aws-0-ap-south-1.pooler.supabase.com` |
| `DB_PORT` | Supabase pooler port | `6543` |
| `DB_NAME` | Database name | `postgres` |
| `DB_USER` | Supabase user | `postgres.xxxxxxxxx` |
| `DB_PASSWORD` | Supabase password | *(secret)* |
| `DB_POOL_SIZE` | Hikari pool size | `3` |
| `REDIS_HOST` | Upstash endpoint | `loving-gnat-93714.upstash.io` |
| `REDIS_PORT` | Redis port | `6379` |
| `REDIS_PASSWORD` | Upstash password | *(secret)* |
| `REDIS_SSL` | TLS for Upstash | `true` |
| `JWT_SECRET` | JWT signing key (≥256 bits) | *(secret)* |
| `CORS_ALLOWED_ORIGINS` | Frontend origins (comma-separated, no trailing slash) | `https://closiq-three.vercel.app,http://localhost:3000` |

### Recommended

| Variable | Description | Dev default |
|----------|-------------|-------------|
| `APP_BASE_URL` | Links in emails/notifications | `https://closiq-three.vercel.app` |
| `REFRESH_COOKIE_SECURE` | HTTPS-only refresh cookie | `false` until HTTPS domain; `true` with `api.yourdomain.com` |
| `SUPABASE_PROJECT_NAME` | Metadata | `closiq-dev` |
| `SUPABASE_REGION` | Metadata | `ap-south-1` |

### Flyway (after manual DDL apply)

| Variable | Value |
|----------|-------|
| `SPRING_FLYWAY_BASELINE_ON_MIGRATE` | `true` |
| `SPRING_FLYWAY_BASELINE_VERSION` | `33` |

Or in `application-dev.properties`:

```properties
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=33
```

### Cloudinary

| Variable | Description |
|----------|-------------|
| `CLOUDINARY_CLOUD_NAME` | Cloud name |
| `CLOUDINARY_API_KEY` | API key |
| `CLOUDINARY_API_SECRET` | API secret *(secret)* |
| `CLOUDINARY_FOLDER` | `closiq-dev` |
| `CLOUDINARY_STUB_ENABLED` | `false` |

### Payments & logistics

| Variable | Dev default |
|----------|-------------|
| `RAZORPAY_KEY_ID` | test key |
| `RAZORPAY_KEY_SECRET` | *(secret)* |
| `RAZORPAY_STUB_ENABLED` | `true` |
| `SHADOWFAX_WEBHOOK_SECRET` | stub value |
| `SHADOWFAX_STUB_ENABLED` | `true` |

### Email (optional)

| Variable | Dev default |
|----------|-------------|
| `MAIL_ENABLED` | `false` |
| `BREVO_API_KEY` | |
| `BREVO_SENDER_EMAIL` | |

### Spring Boot direct bindings (optional)

```env
SPRING_DATA_REDIS_PASSWORD=<same as REDIS_PASSWORD>
SPRING_DATA_REDIS_SSL_ENABLED=true
```

---

## 7. Storing secrets on AWS

| Service | Cost | Use for |
|---------|------|---------|
| **SSM Parameter Store** (Standard) | Free | Dev secrets (`SecureString`) |
| **Secrets Manager** | ~$0.40/secret/mo | Production rotation |
| **`.env.dev` on EC2** | Free | Simple dev — restrict file permissions (`chmod 600`) |

Example SSM paths:

```
/closiq/dev/DB_PASSWORD
/closiq/dev/JWT_SECRET
/closiq/dev/REDIS_PASSWORD
/closiq/dev/CLOUDINARY_API_SECRET
/closiq/dev/RAZORPAY_KEY_SECRET
```

---

## 8. Auth cookies (Vercel + AWS backend)

Frontend: `closiq-three.vercel.app`  
Backend: `api.yourdomain.com` or `*.awsapprunner.com`

Cross-origin refresh cookies (`SameSite=Strict`) may fail after ~15 minutes unless:

1. Use subdomains on same site: `app.closiq.com` + `api.closiq.com`, or
2. **Vercel rewrite** proxy for `/api/v1` → AWS backend (same-origin), or
3. Code change to `SameSite=None; Secure` (future)

Dev: keep `REFRESH_COOKIE_SECURE=false` only for HTTP testing — use HTTPS in production.

---

## 9. Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| Container OOM / slow start | `t3.micro` (1 GB RAM) too small | Use `t3.small` (2 GB) or App Runner 2 GB |
| Health UP, booking fails | Redis env wrong | `REDIS_SSL=true`, correct Upstash password |
| Flyway fails on deploy | Seeds on empty DB | Run `closiq-dev-schema.sql` + baseline V33 |
| CORS error | Origin mismatch | Exact match in `CORS_ALLOWED_ORIGINS` |
| 401 after 15 min | Cross-site cookie | See §8 |
| Supabase timeout | Wrong port | Use pooler **6543** |
| EC2 unreachable | Security group | Open 443 (and 22 from your IP) |

---

## 10. File reference

| File | Purpose |
|------|---------|
| `src/main/resources/application-dev.properties.example` | Template — copy to gitignored `application-dev.properties` |
| `backend/.env.dev.example` | Env checklist for EC2 Docker / App Runner |
| `backend/Dockerfile` | Container build (Java 21, port 8081) |
| `backend/docs/dev-ddl/closiq-dev-schema.sql` | Consolidated DDL for Supabase |
| `backend/.gitignore` | Ignores `application-dev.properties`, `.env.dev` |

---

## 11. Next steps

1. Run `closiq-dev-schema.sql` in Supabase dev project
2. Set `spring.flyway.baseline-version=33`
3. Deploy backend on **EC2** or **App Runner** (Mumbai)
4. Set Vercel `NEXT_PUBLIC_API_URL=https://closiq-three.vercel.app/api/v1` and `API_PROXY_TARGET` to your AWS backend origin
5. Verify `/actuator/health` and login flow
6. Run seed migrations (V5, V7, V25) when you want demo data

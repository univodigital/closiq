# EC2 operations runbook — env updates & backend redeploy

Day-to-day steps for the **Closiq backend on EC2** (Docker).  
Initial setup (launch instance, first deploy, Caddy, Supabase): see [`DEV-DEPLOYMENT.md`](DEV-DEPLOYMENT.md).

**Important:** Pushing to GitHub does **not** update EC2 automatically. You must SSH in and redeploy (or set up CI later).

---

## Before you start

| Item | Typical value |
|------|----------------|
| EC2 user | `ubuntu` |
| SSH key | `~/.ssh/your-key.pem` |
| EC2 public IP / Elastic IP | `YOUR_EC2_IP` |
| Repo on server | `~/Closiq` (after `git clone`) |
| Env file on server | `~/Closiq/backend/.env.dev` |
| Docker container name | `closiq-backend` |
| Docker image tag | `closiq-backend:dev` |
| App port | `8081` |

Connect:

```bash
ssh -i ~/.ssh/your-key.pem ubuntu@YOUR_EC2_IP
```

---

## Part 1 — Update environment variables

Use this when you change secrets or config (DB password, Redis, JWT, CORS, Cloudinary, etc.) **without** changing Java/ backend code.

### 1.1 Open the env file on EC2

```bash
cd ~/Closiq/backend
nano .env.dev
```

If `.env.dev` does not exist yet:

```bash
cd ~/Closiq/backend
cp .env.dev.example .env.dev
chmod 600 .env.dev
nano .env.dev
```

Reference template (on your laptop): `backend/.env.dev.example`  
Full variable list: [`DEV-DEPLOYMENT.md` §6](DEV-DEPLOYMENT.md#6-complete-environment-variable-reference-dev-profile).

### 1.2 Edit and save in `nano`

1. Move with arrow keys; edit values after `=`.
2. **No quotes** around values unless the value itself contains spaces.
3. **No trailing spaces** after values.
4. Save: `Ctrl + O` → Enter  
5. Exit: `Ctrl + X`

Restrict permissions (recommended):

```bash
chmod 600 .env.dev
```

### 1.3 Apply changes — restart container (no rebuild)

Env vars are read at **container start**. You do **not** need `docker build` for env-only changes.

```bash
cd ~/Closiq/backend

docker stop closiq-backend
docker rm closiq-backend

docker run -d \
  --name closiq-backend \
  --env-file .env.dev \
  -p 8081:8081 \
  --restart unless-stopped \
  closiq-backend:dev
```

If the image `closiq-backend:dev` does not exist yet, do a one-time build first (see Part 2).

### 1.4 Verify

```bash
docker logs -f closiq-backend
# Ctrl+C to stop following logs

curl -s http://localhost:8081/actuator/health
```

Expect: `{"status":"UP"}` (or similar).

From your laptop (if security group allows):

```bash
curl -s http://YOUR_EC2_IP:8081/actuator/health
```

### 1.5 Vercel env vars (frontend)

If you changed the **backend URL** or anything the frontend needs:

| Variable | Where | When to change |
|----------|--------|----------------|
| `API_PROXY_TARGET` | Vercel → Project → Settings → Environment Variables | EC2 IP/domain or port changed |
| `NEXT_PUBLIC_API_URL` | Same | Usually stays `https://closiq-three.vercel.app/api/v1` |
| `CORS_ALLOWED_ORIGINS` | EC2 `.env.dev` | Must include exact Vercel origin |

After changing Vercel env vars: **Redeploy** the frontend (Vercel dashboard → Deployments → Redeploy, or push a commit).

Backend CORS must match the browser origin, e.g.:

```env
CORS_ALLOWED_ORIGINS=https://closiq-three.vercel.app,http://localhost:3000
```

---

## Part 2 — Backend code push (GitHub → EC2)

Use this when you changed Java code, `pom.xml`, `Dockerfile`, or Flyway migrations under `backend/src/main/resources/db/migration/`.

### Flow overview

```
Your laptop                    GitHub                    EC2
──────────                    ──────                    ───
edit code  ──git push──►  main branch          (nothing happens yet)
                              │
                              └── you SSH in ──► git pull
                                                 docker build
                                                 restart container
                                                 Flyway runs on startup
```

### 2.1 On your laptop — push to GitHub

```bash
cd /path/to/Closiq

git status
git add .
git commit -m "describe your backend change"
git push origin main
```

Use your actual branch name if not `main`.

`.env.dev` and `application-dev.properties` are **gitignored** — they are **not** pushed. Update secrets only on EC2 (Part 1).

### 2.2 On EC2 — pull latest code

```bash
ssh -i ~/.ssh/your-key.pem ubuntu@YOUR_EC2_IP

cd ~/Closiq
git pull origin main
```

If the repo is not cloned yet:

```bash
git clone https://github.com/YOUR_ORG/Closiq.git
cd Closiq/backend
cp .env.dev.example .env.dev
nano .env.dev   # fill secrets once
chmod 600 .env.dev
```

### 2.3 Rebuild Docker image

```bash
cd ~/Closiq/backend

docker build -t closiq-backend:dev .
```

First build can take several minutes (Maven download + compile).

### 2.4 Restart container with current env

```bash
docker stop closiq-backend 2>/dev/null || true
docker rm closiq-backend 2>/dev/null || true

docker run -d \
  --name closiq-backend \
  --env-file .env.dev \
  -p 8081:8081 \
  --restart unless-stopped \
  closiq-backend:dev
```

### 2.5 Verify deploy

```bash
docker ps
docker logs --tail 100 closiq-backend

curl -s http://localhost:8081/actuator/health
curl -s http://localhost:8081/api/v1/categories
```

Check Flyway in logs if you added migrations (e.g. `V34__...sql`):

```bash
docker logs closiq-backend 2>&1 | grep -i flyway
```

### 2.6 End-to-end check (optional)

From browser: open `https://closiq-three.vercel.app` and exercise login or an API call.  
Vercel proxies `/api/v1/*` to EC2 via `API_PROXY_TARGET`.

---

## Quick reference — copy/paste blocks

### Env-only restart

```bash
cd ~/Closiq/backend && \
docker stop closiq-backend && docker rm closiq-backend && \
docker run -d --name closiq-backend --env-file .env.dev -p 8081:8081 --restart unless-stopped closiq-backend:dev && \
sleep 5 && curl -s http://localhost:8081/actuator/health
```

### Full redeploy after `git push`

```bash
cd ~/Closiq && git pull origin main && \
cd backend && \
docker build -t closiq-backend:dev . && \
docker stop closiq-backend 2>/dev/null; docker rm closiq-backend 2>/dev/null; \
docker run -d --name closiq-backend --env-file .env.dev -p 8081:8081 --restart unless-stopped closiq-backend:dev && \
sleep 15 && curl -s http://localhost:8081/actuator/health
```

### View logs / debug

```bash
docker logs -f closiq-backend
docker exec -it closiq-backend sh   # shell inside container (Alpine)
docker images | grep closiq
```

---

## Env-only vs rebuild — when to use which

| Change | Action |
|--------|--------|
| Password, API keys, CORS, Redis, JWT in `.env.dev` | Part 1 — restart only |
| Java source, `pom.xml`, `Dockerfile` | Part 2 — pull + **build** + restart |
| New Flyway migration file in repo | Part 2 — pull + build + restart (migration runs on startup) |
| Vercel `API_PROXY_TARGET` | Vercel dashboard + redeploy frontend |
| Database schema on empty Supabase | Run `docs/dev-ddl/closiq-dev-schema.sql` once (see DEV-DEPLOYMENT) |

---

## Troubleshooting

| Problem | What to try |
|---------|-------------|
| Container exits immediately | `docker logs closiq-backend` — often bad DB/Redis env |
| `git pull` conflicts | Stash or reset local changes on EC2; avoid editing code on server |
| Health check fails after deploy | Wait 30–60s; Spring Boot + Flyway need time on small instances |
| CORS errors in browser | Fix `CORS_ALLOWED_ORIGINS` in `.env.dev`, restart (Part 1) |
| Vercel 503 on `/api/v1` | Set `API_PROXY_TARGET` to `http://YOUR_EC2_IP:8081` and redeploy Vercel |
| Out of memory on build | Use `t3.small` (2 GB) or build image locally/CI and push to ECR |

More detail: [`DEV-DEPLOYMENT.md` §9](DEV-DEPLOYMENT.md#9-troubleshooting).

---

## Future: automatic deploy on push

Not configured today. To automate Part 2 you could add GitHub Actions that SSH to EC2 or push to ECR. Until then, **every backend release = push to GitHub + manual steps on EC2**.

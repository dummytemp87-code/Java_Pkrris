# StudyHub

An AI-powered study assistant: set a learning goal, get a personalized day-by-day study plan,
learn with an AI tutor, take quizzes, and track your progress — all in one place.

- **Frontend**: Next.js 15 (App Router), deployed to Vercel
- **Backend**: Spring Boot 3.3, deployed on an Oracle Cloud Always Free AMD micro VM
- **Database**: Postgres (Supabase)
- **AI provider**: Gemini (configurable)

## Local development

### Backend

Copy `spring-backend/.env.example` to `spring-backend/.env` and fill in real values, then:

```bash
cd spring-backend
./start.sh          # or start.ps1 on Windows PowerShell
```

`start.sh`/`start.ps1` load `.env` and run `mvn clean spring-boot:run`. `.env` is gitignored —
never commit it. Alternatively, run without the script by exporting the variables inline:

```bash
cd spring-backend
DB_USERNAME=... DB_PASSWORD=... JWT_SECRET=... YOUTUBE_API_KEY=... GEMINI_API_KEY=... mvn spring-boot:run
```

Required environment variables:

| Variable | Description |
|---|---|
| `DB_USERNAME` | Postgres username |
| `DB_PASSWORD` | Postgres password |
| `JWT_SECRET` | Random secret for signing auth tokens, at least 32 bytes. Generate with `openssl rand -base64 48` |
| `YOUTUBE_API_KEY` | YouTube Data API key, used for lesson video search |
| `GEMINI_API_KEY` | Google Gemini API key, used for chat/quiz/study-plan/article generation |
| `CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed frontend origins. Defaults to `http://localhost:3000` for local dev |
| `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` | Razorpay API credentials (Dashboard → Settings → API Keys). Test mode keys work for development. |
| `RAZORPAY_WEBHOOK_SECRET` | Set when configuring the webhook endpoint (Dashboard → Settings → Webhooks) — verifies incoming webhook calls are genuinely from Razorpay. |
| `RAZORPAY_PLAN_STARTER` / `RAZORPAY_PLAN_PRO` | Razorpay Plan IDs (Dashboard → Subscriptions → Plans, created manually — ₹199/mo and ₹399/mo monthly plans). |

The app fails fast at startup if `DB_USERNAME`, `DB_PASSWORD`, or `JWT_SECRET` are missing —
this is intentional, so it can never silently run against a stale or default credential.

### Frontend

```bash
pnpm install
pnpm dev
```

Optional environment variable:

| Variable | Description |
|---|---|
| `NEXT_PUBLIC_BACKEND_URL` | Backend URL. Defaults to `http://localhost:8080` for local dev |

Frontend runs at `http://localhost:3000`, backend at `http://localhost:8080`.

## Deployment

**Frontend (Vercel)**: connect this repo, set `NEXT_PUBLIC_BACKEND_URL` to your deployed backend
URL. Auto-deploys on push.

**Backend (Oracle Cloud Always Free)**: see [`spring-backend/deploy/README.md`](spring-backend/deploy/README.md)
for the full runbook — provisioning the AMD micro VM, opening firewall ports, and running
`setup-vm.sh`, which installs Java/Caddy, builds the app, sets it up as a systemd service, and
gets free HTTPS via nip.io (no domain needed). Set `CORS_ALLOWED_ORIGINS` to your deployed Vercel
URL — without this, the deployed frontend cannot call the deployed backend. A health check
endpoint is available at `/actuator/health`. A `Dockerfile` also still exists if you'd rather
deploy to a container platform (Render/Railway/Fly.io) instead.

## Security notes

- Never commit real secrets to `application.properties` — all credentials are read from
  environment variables (see table above), with no defaults for the required ones.
- Login, registration, and the AI chat endpoint are rate-limited per IP to reduce abuse/brute-force
  and cost exposure.

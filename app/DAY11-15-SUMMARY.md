# Days 11–15 summary — Demo UI + deploy notes

**Date:** 2026-09-24 (JST)  
**Founder:** Haroon Ali Shah  
**Path:** `payevidence/app/`  
**Version:** `0.6.0-SNAPSHOT`

## What shipped

### Clickable demo UI (Thymeleaf)
- Server-rendered pages (no React/Vite): `/` → `/demo`, gate, dashboard, control detail.
- Summary chips: pass / gap / stale (clickable filters).
- Controls table → detail with reason + contributing artifacts.
- Actions: Reset default (16/4), withChangeRecords (18/2), ingest change records only, Generate evidence pack.
- Recent packs list with View HTML / Download HTML / Download PDF (reuses existing REST download URLs).

### Demo secret gate
- Config: `payevidence.demo-token` / env `PAYEVIDENCE_DEMO_TOKEN` (default **`nordic-demo`**).
- UI accepts `?token=` or cookie `payevidence_demo` (HttpOnly, 12h).
- **`/api/**` stays open** for local curl; document + put basic-auth in front on VPS (`DEPLOY.md`).

### Deploy artifacts (no VPS purchased)
- `app/Dockerfile` — multi-stage Java 21 image with Day 1/2–3 samples baked in.
- `app/docker-compose.deploy.yml` — app + Caddy.
- `app/Caddyfile` — HTTPS + basic auth template.
- `app/DEPLOY.md` — EU VPS steps, nginx snippet, hardening checklist.

### Demo script
- `DEMO-SCRIPT-5MIN.md` — click path matches the UI; aligns with `day2-3/PITCH-60S.md`.

### API additive
- `GET /api/orgs/{id}/evidence-packs` — list recent packs (does not break existing routes).

### Tests
- UI home with token returns 200 + NordicPay; gate is public; list packs endpoint covered.
- Prior Days 5–10 tests remain green.

## How to run

```bash
cd /workspace/haroon-estonia/payevidence/app
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
mvn -q test
mvn -q spring-boot:run
# UI: http://localhost:8080/demo?token=nordic-demo
```

## Success criteria

| Criterion | Status |
|-----------|--------|
| `mvn -q test` passes | Target of this slice |
| Open UI with token, see controls, generate pack, download PDF | Documented + exercised |
| Deploy notes without buying VPS | `DEPLOY.md` |
| 5-minute demo script | `DEMO-SCRIPT-5MIN.md` |

## Not in this slice
- Real VPS purchase / live public URL
- Fancy auth (SSO), React SPA, DORA pack, CHD

## Deviations / notes
1. Thymeleaf chosen over static+vanilla JS for one `mvn spring-boot:run` and simpler forms.
2. Pack download links hit `/api/...` from the browser; with UI cookie set, user already passed the gate; APIs remain unauthenticated by design.
3. Caddyfile ships a **placeholder** bcrypt hash — must be replaced before public use.

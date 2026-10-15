# Free HTTPS demo (Render)

Committee-facing demo without buying a VPS.

## What you get
- Public HTTPS URL like `https://payevidence-demo.onrender.com`
- Demo UI gated by `?token=nordic-demo` (change before sharing widely)
- Free instance **sleeps after ~15 minutes idle** — open the URL 1–2 minutes before the committee call so it wakes

## Steps
1. Push this `payevidence/` tree to a **public** GitHub repo (e.g. `jharoonalishah/payevidence-demo`).
2. Sign up at [render.com](https://render.com) with GitHub (free, no card required for free web services as of 2026 docs).
3. **New → Blueprint** and select the repo, **or** New Web Service → Docker → this repo, root `.`, Dockerfile `./app/Dockerfile`, instance **Free**.
4. Set env vars (do not commit the values):
   - `PAYEVIDENCE_DEMO_TOKEN` (recommended: a longer secret for the committee link).
   - `DATABASE_URL` — Neon free Postgres connection string (`postgresql://…` or `postgres://…`, usually with `sslmode=require`). The app turns that into a JDBC URL and stores signups there. Render’s free web service has no persistent disk, so without this the H2 file disappears on spin-down; signups are still written to the service logs as a fallback.
   - `ADMIN_TOKEN` — long random secret. Opens `https://<service>.onrender.com/admin/signups`. Leave it unset to hide that page (404).
5. Deploy. Open: `https://<service>.onrender.com/` for the landing page, and `https://<service>.onrender.com/demo?token=<your-token>` for NordicPay.

## Signups

| | |
|--|--|
| Public form | `POST /signup` (also inline on `/`) |
| Admin UI | `GET /admin/signups` after the token form, or `curl -H "Authorization: Bearer $ADMIN_TOKEN" https://<service>.onrender.com/admin/signups` |
| SQL | `SELECT created_at, name, email, company, job_role, message FROM signups ORDER BY created_at DESC;` |

`?token=` on the admin URL also works. Prefer the form or the Bearer header so the secret is less likely to sit in access logs.

## Custom domain (payevidence.site)

The domain is registered on Vercel and DNS is managed by Vercel. The app stays on Render; Vercel is only DNS.

1. In the Render dashboard, open the web service → **Settings → Custom Domains**.
2. Add `payevidence.site` and `www.payevidence.site`. Render shows the exact records to create and then issues TLS after they resolve. Use those values if they differ from the table below.
3. In the Vercel dashboard, open **Domains → payevidence.site → DNS Records** and create:

**Preferred on Vercel (ALIAS at the apex):**

| Type | Name | Value |
|------|------|--------|
| ALIAS | `@` | `<service>.onrender.com` |
| CNAME | `www` | `<service>.onrender.com` |

**If Render’s screen instead asks for an A record** (their load balancer; confirm the IP in the dashboard, it is currently `216.24.57.1` in [Render’s DNS docs](https://render.com/docs/configure-other-dns)):

| Type | Name | Value |
|------|------|--------|
| A | `@` | `216.24.57.1` |
| CNAME | `www` | `<service>.onrender.com` |

4. Remove any `AAAA` records for `@` and `www`. Render is IPv4-only, and an `AAAA` record can stop domain verification.
5. Leave the records DNS-only. Do not point the names at a Vercel deployment; the site is the Render service.
6. Wait until Render shows the certificate as issued, then open `https://payevidence.site/` and `https://payevidence.site/demo/gate`.

Free-instance sleep still applies on the custom domain.

## Smoke
```bash
curl -sI "https://<service>.onrender.com/" | head -5
curl -sI "https://<service>.onrender.com/demo/gate" | head -5
curl -sI "https://<service>.onrender.com/demo?token=nordic-demo" | head -5
```

## Notes
- Synthetic NordicPay data only — no real card data.
- `/api/**` is reachable without the demo token; do not put real secrets in the demo.
- For always-on EU hosting later, use a small paid VPS + `app/DEPLOY.md` (Caddy + basic auth).

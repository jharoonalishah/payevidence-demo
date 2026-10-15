# PayEvidence

PCI continuous-evidence demo for the Estonia Startup Committee. PayEvidence turns payment-stack artifacts into a live map of PCI DSS controls — pass, gap, or stale — and exports dated evidence packs. This repository is the **NordicPay** demo tenant: synthetic logs, config snapshots, and control tests only.

## Public site

`/` is the landing page (PCI DSS continuous evidence: payment-stack artifacts mapped to controls as pass / gap / stale, dated evidence packs). Early access is the form on that page and at `/signup`.

Signups are stored in Postgres when `DATABASE_URL` is set (Neon `postgres://` or `postgresql://` URLs are converted to JDBC). When it is unset, the app keeps the local H2 database and writes each signup to the log so a laptop or the demo still runs. `/admin/signups` lists them when `ADMIN_TOKEN` is set; if that variable is empty the page is hidden.

## Demo gate

The committee UI is token-gated. Open:

`/demo?token=nordic-demo`

The token form is `/demo/gate`. The app reads `PAYEVIDENCE_DEMO_TOKEN` and defaults to `nordic-demo`. The landing page and signup form do not use this token.

## Run locally

JDK 21 and Maven 3.9+. From the repo root:

```bash
cd app
mvn -q spring-boot:run
```

The server listens on port `${PORT:8080}` (http://localhost:8080 by default). First start seeds NordicPay from `day1/` and `day2-3/`. Full prerequisites and the curl script are in `LOCAL-SETUP.md`.

## Render (free HTTPS)

`render.yaml` is a free-plan Docker blueprint (`plan: free`, Dockerfile `./app/Dockerfile`). Render gives a public HTTPS URL. A free web service **spins down after idle** (about 15 minutes); open the URL a minute or two before the committee call so it wakes. Set `DATABASE_URL` (Neon) and `ADMIN_TOKEN` in the Render dashboard — they are declared in the blueprint with `sync: false`, so values are not stored in git. Custom domain `payevidence.site` (DNS on Vercel) is documented in `FREE-DEPLOY.md`.

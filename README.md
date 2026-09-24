# PayEvidence

PCI continuous-evidence demo for the Estonia Startup Committee. PayEvidence turns payment-stack artifacts into a live map of PCI DSS controls — pass, gap, or stale — and exports dated evidence packs. This repository is the **NordicPay** demo tenant: synthetic logs, config snapshots, and control tests only.

## Demo gate

The committee UI is token-gated. Open:

`/demo?token=nordic-demo`

The token form is `/demo/gate`. The app reads `PAYEVIDENCE_DEMO_TOKEN` and defaults to `nordic-demo`.

## Run locally

JDK 21 and Maven 3.9+. From the repo root:

```bash
cd app
mvn -q spring-boot:run
```

The server listens on port `${PORT:8080}` (http://localhost:8080 by default). First start seeds NordicPay from `day1/` and `day2-3/`. Full prerequisites and the curl script are in `LOCAL-SETUP.md`.

## Render (free HTTPS)

`render.yaml` is a free-plan Docker blueprint (`plan: free`, Dockerfile `./app/Dockerfile`). Render gives a public HTTPS URL. A free web service **spins down after idle** (about 15 minutes); open the URL a minute or two before the committee call so it wakes. Steps and smoke checks are in `FREE-DEPLOY.md`.

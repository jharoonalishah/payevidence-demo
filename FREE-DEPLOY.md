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
4. Set env `PAYEVIDENCE_DEMO_TOKEN` (recommended: a longer secret for the committee link).
5. Deploy. Open: `https://<service>.onrender.com/demo?token=<your-token>`

## Smoke
```bash
curl -sI "https://<service>.onrender.com/demo/gate" | head -5
curl -sI "https://<service>.onrender.com/demo?token=nordic-demo" | head -5
```

## Notes
- Synthetic NordicPay data only — no real card data.
- `/api/**` is reachable without the demo token; do not put real secrets in the demo.
- For always-on EU hosting later, use a small paid VPS + `app/DEPLOY.md` (Caddy + basic auth).

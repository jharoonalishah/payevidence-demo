# PayEvidence — deploy notes (Days 11–15)

**Goal:** HTTPS Docker demo on a cheap EU VPS, password-protected.  
**This doc does not buy a VPS** — bring your own host (Hetzner/Contabo/DigitalOcean FRA/AMS/HEL, etc.).

## Auth story (pick one clear path)

| Layer | What | Default |
|-------|------|---------|
| **Demo UI gate** | Query `?token=` or cookie `payevidence_demo` | Token `nordic-demo` (`PAYEVIDENCE_DEMO_TOKEN`) |
| **REST `/api/**`** | **Open** for local curl / Postman | Intentional — document + lock down in prod |
| **VPS edge** | Caddy (or nginx) **HTTP basic auth** + TLS | Required on any public URL |

Local demo: UI needs the token; APIs work without it.  
Public VPS: basic-auth blocks strangers; demo token still gates the UI for committee/design partners.

## Local UI (no VPS)

```bash
cd /workspace/haroon-estonia/payevidence/app
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
# optional: export PAYEVIDENCE_DEMO_TOKEN=nordic-demo
mvn -q spring-boot:run
```

Open:

- Gate: http://localhost:8080/demo/gate  
- Direct: http://localhost:8080/demo?token=nordic-demo  
- Root redirects to `/demo` (then gate if no cookie)

## Docker image (build from `payevidence/`)

```bash
cd /workspace/haroon-estonia/payevidence
docker build -f app/Dockerfile -t payevidence:0.6 .
docker run --rm -p 8080:8080 \
  -e PAYEVIDENCE_DEMO_TOKEN=nordic-demo \
  payevidence:0.6
```

Catalogue + Day 1/2–3 samples are baked into the image under `/data`.

## Full stack: app + Caddy HTTPS + basic auth

1. Point DNS `A`/`AAAA` for e.g. `demo.yourdomain.ee` at the VPS.
2. Open ports **80** and **443**.
3. Generate a basic-auth hash:

```bash
docker run --rm caddy:2.8-alpine caddy hash-password --plaintext 'replace-with-strong-password'
```

4. Edit `app/Caddyfile`: replace the placeholder hash and set username if desired.
5. Launch:

```bash
cd /workspace/haroon-estonia/payevidence
export PAYEVIDENCE_DOMAIN=demo.yourdomain.ee
export PAYEVIDENCE_DEMO_TOKEN='long-random-demo-token'
docker compose -f app/docker-compose.deploy.yml up -d --build
```

6. Visit `https://demo.yourdomain.ee/demo` → browser basic-auth → then demo token gate → NordicPay.

### Optional nginx snippet (instead of Caddy)

```nginx
server {
  listen 443 ssl http2;
  server_name demo.yourdomain.ee;
  # ssl_certificate / ssl_certificate_key via certbot

  auth_basic "PayEvidence demo";
  auth_basic_user_file /etc/nginx/.htpasswd;  # htpasswd -B -c ...

  location / {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
  }
}
```

Run only the app container (or `mvn spring-boot:run`) behind nginx.

## Hardening checklist before showing strangers

- [ ] Change `PAYEVIDENCE_DEMO_TOKEN`
- [ ] Set Caddy/nginx basic-auth password (not `demo`/`demo`)
- [ ] Prefer binding app to localhost and exposing only Caddy
- [ ] Optional: block `/api/**` at the proxy except for yourself, **or** accept that APIs are open behind basic-auth
- [ ] No real CHD — synthetic NordicPay only
- [ ] Back up nothing sensitive; H2 volume is demo data

## Cheap EU VPS sketch (manual)

1. Create a small shared-CPU VM in EU (1 vCPU / 1–2 GB RAM is enough for demo).
2. Install Docker + Compose plugin.
3. Clone/copy this `payevidence/` tree onto the box.
4. Follow “Full stack” above.
5. Rehearse `DEMO-SCRIPT-5MIN.md` against the public URL.

## Smoke after deploy

```bash
# Through basic auth (example)
curl -u demo:YOUR_PASSWORD -s -o /dev/null -w '%{http_code}\n' \
  'https://demo.yourdomain.ee/demo?token=YOUR_DEMO_TOKEN'

# API still reachable behind basic auth
curl -u demo:YOUR_PASSWORD -s -X POST 'https://demo.yourdomain.ee/api/demo/seed' | head
```

## Blockers / notes

- Let’s Encrypt needs a real public DNS name; `localhost` Caddyfile will not get a public cert.
- First build downloads Maven deps — allow several minutes on a small VPS.
- Postgres profile remains optional (`docker-compose.yml`); deploy compose uses embedded H2 file volume for simplicity.

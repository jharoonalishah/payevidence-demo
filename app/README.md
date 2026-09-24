# PayEvidence — Spring Boot app (Days 5–15)

PCI continuous evidence SaaS (not a PSP). Demo tenant: **NordicPay**.  
**v0.6.0-SNAPSHOT** — Thymeleaf demo UI + deploy notes (Days 11–15).

## Stack

- Java **21**, Spring Boot **3.3.4**, Maven, Spring Data JPA, Thymeleaf, OpenPDF
- Default profile **`h2`**: file DB at `./data/payevidence`
- Profile **`postgres`**: `application-postgres.yml` + optional `docker-compose.yml`

## Run (H2 — default)

```bash
cd /workspace/haroon-estonia/payevidence/app
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"

mvn -q spring-boot:run
# or: mvn -q -DskipTests package && java -jar target/payevidence-0.6.0-SNAPSHOT.jar
```

### Demo UI

| URL | Purpose |
|-----|---------|
| http://localhost:8080/demo?token=nordic-demo | Dashboard (sets cookie) |
| http://localhost:8080/demo/gate | Token form |
| http://localhost:8080/ | Redirects to `/demo` |

**Default demo token:** `nordic-demo` (`payevidence.demo-token` / `PAYEVIDENCE_DEMO_TOKEN`).  
**Auth story:** UI pages require the token (query or cookie). **`/api/**` stays open** for local curl. On a public VPS, put Caddy/nginx basic-auth in front — see `DEPLOY.md`.

Happy path: open UI → chips → filter Gaps → control detail → Reset / withChangeRecords → Generate pack → View HTML / Download PDF.

Seed runs on startup (idempotent, **default** profile → pass=16 gap=4). Paths default to sibling `../day1` and `../day2-3`.

Optional env overrides:

- `PAYEVIDENCE_CATALOGUE`, `PAYEVIDENCE_SAMPLES`, `PAYEVIDENCE_CHANGE_RECORD`, `PAYEVIDENCE_PACKS`
- `PAYEVIDENCE_SEED_ON_STARTUP=false`
- `PAYEVIDENCE_DEMO_TOKEN`

## Optional Postgres

```bash
docker compose up -d
mvn -q spring-boot:run -Dspring-boot.run.profiles=postgres
```

Tests **do not** need Docker — `mvn test` always uses in-memory H2.

## Tests

```bash
mvn -q test
```

## Demo profiles (scoring)

| Profile | How | Expected scores | Gaps |
|---------|-----|-----------------|------|
| **default** | `POST /api/demo/seed` or UI “Reset demo” | pass=**16** gap=**4** stale=0 | 6.5, 6.3, 6.4, 11.3 |
| **withChangeRecords** | `?demoProfile=withChangeRecords` or UI button | pass=**18** gap=**2** stale=0 | 6.4, 11.3 only |

Teaching contrast: `POST /api/demo/ingest-change-records` (or UI) loads change records alone — 6.5/6.3 **stay gap**.

## Curl — full demo script

```bash
BASE=http://localhost:8080

# 1) Default seed (16/4)
curl -s -X POST "$BASE/api/demo/seed" | jq .

# 2) Resolve NordicPay by name
ORG=$(curl -s "$BASE/api/orgs/by-name/NordicPay" | jq -r .id)
echo "ORG=$ORG"

# 3) List gaps only
curl -s "$BASE/api/orgs/$ORG/controls?status=gap" | jq '{pass,gap,stale,filter,gaps:[.controls[].controlId]}'

# 4) Create evidence pack (HTML + PDF on disk under ./packs/)
PACK=$(curl -s -X POST "$BASE/api/orgs/$ORG/evidence-packs" | jq -r .id)
echo "PACK=$PACK"

# 5) JSON snapshot + HTML view + downloads
curl -s "$BASE/api/orgs/$ORG/evidence-packs/$PACK" | jq '{id,createdAt,htmlAvailable,pdfAvailable,snapshot:{pass:.snapshot.pass,gap:.snapshot.gap,stale:.snapshot.stale}}'
curl -s "$BASE/api/orgs/$ORG/evidence-packs/$PACK?format=html" | head -n 20
curl -s -o /tmp/nordicpay-pack.html "$BASE/api/orgs/$ORG/evidence-packs/$PACK/download?format=html"
curl -s -o /tmp/nordicpay-pack.pdf  "$BASE/api/orgs/$ORG/evidence-packs/$PACK/download?format=pdf"
file /tmp/nordicpay-pack.html /tmp/nordicpay-pack.pdf

# 6) UI smoke (follow redirects off)
curl -s -o /dev/null -w '%{http_code}\n' "$BASE/demo?token=nordic-demo"

# 7) Change-record demo profile (18/2)
curl -s -X POST "$BASE/api/demo/seed?demoProfile=withChangeRecords" | jq .
```

## API map

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/demo/seed?demoProfile=default\|withChangeRecords` | Idempotent NordicPay reset + score |
| POST | `/api/demo/ingest-change-records` | Load change_record only (tests still fail) |
| GET | `/api/orgs` | List orgs |
| GET | `/api/orgs/by-name/{name}` | Resolve org (e.g. NordicPay) |
| GET | `/api/orgs/{id}/controls?status=gap` | Controls + optional status filter |
| GET | `/api/orgs/{id}/controls/{controlId}` | Detail + contributing artifacts |
| POST | `/api/orgs/{id}/artifacts` | Ingest (allowed types only) |
| POST | `/api/orgs/{id}/score` | Recompute all |
| POST | `/api/orgs/{id}/evidence-packs` | Create dated snapshot (HTML+PDF+JSON) |
| GET | `/api/orgs/{id}/evidence-packs` | List packs (newest first) |
| GET | `/api/orgs/{id}/evidence-packs/{packId}` | Fetch (`?format=json\|html\|pdf`) |
| GET | `/api/orgs/{id}/evidence-packs/{packId}/download` | Attachment download (`?format=html\|pdf`) |

Errors return stable JSON: `{status, error, message, timestamp}` (404 not_found / 400 bad_request).

Allowed artifact types: `api_access_log`, `config_snapshot`, `control_test_result`, `change_record`.

Scoring contract: `../day2-3/SCORING.md`. Control IDs frozen from `../day1/controls/pci-mvp-catalogue.yaml`.

## Deploy

See **`DEPLOY.md`** (Dockerfile, Caddy/nginx basic-auth, EU VPS checklist — no VPS purchased here).  
5-minute rehearsal: **`../DEMO-SCRIPT-5MIN.md`**.  
Summaries: `DAY5-10-SUMMARY.md`, `../DAY11-15-SUMMARY.md`.

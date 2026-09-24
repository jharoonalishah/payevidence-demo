# Days 5–10 summary — PayEvidence backend hardening

**Date:** 2026-09-24 (JST)  
**Founder:** Haroon Ali Shah  
**Path:** `payevidence/app/`  
**Version:** `0.5.0-SNAPSHOT`

## What changed (vs Day 4)

### Evidence pack quality
- Richer HTML: banner title, **NordicPay** org name, generated timestamp (UTC), summary chips (pass/gap/stale/artifact counts), control table (status + reason), artifact table (type / captured / id / path).
- **PDF export** via OpenPDF (lean). Written next to HTML under `./packs/`; downloadable via API.
- Endpoints: `?format=html|pdf|json` and `/download?format=html|pdf`.

### API hardening
- Stable error bodies: `{status, error, message, timestamp}` — **404** for missing org/control/pack; **400** for bad artifact type / bad status filter.
- `GET /api/orgs/by-name/NordicPay`
- `GET /api/orgs/{id}/controls?status=gap` (also `pass`, `stale`)
- Artifact ingest validates allowed types only
- Idempotent demo seed still works

### Change-record demo path (honest with SCORING.md)
| Profile | Scores | Gaps |
|---------|--------|------|
| **default** | pass=**16** gap=**4** stale=0 | 6.5, 6.3, 6.4, 11.3 |
| **withChangeRecords** | pass=**18** gap=**2** stale=0 | 6.4, 11.3 |

`withChangeRecords` = ingest `change_record.json` **+** flip 6.5/6.3 test rows to pass. Vuln 6.4/11.3 stay fail→gap.  
Hard rule unchanged: fail test → gap. We adjust **demo data**, not the rule.

Contrast endpoint `POST /api/demo/ingest-change-records` still shows “change file alone does not flip greens.”

### Postgres profile
- `src/main/resources/application-postgres.yml`
- `docker-compose.yml` (Postgres 16) — optional
- H2 remains default; **`mvn test` never needs Docker**

### Tests
- Default 16/4; withChangeRecords 18/2; gap filter; pack HTML contains NordicPay + counts; PDF magic `%PDF`; by-name; 404/400 validation; change-record-only keeps 4 gaps.

## How to run

```bash
cd /workspace/haroon-estonia/payevidence/app
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
mvn -q test
mvn -q spring-boot:run
```

Full curl script: see `README.md`.

## Before / after scores

| | Day 4 / default | Days 5–10 withChangeRecords |
|--|-----------------|-----------------------------|
| pass | 16 | 18 |
| gap | 4 | 2 |
| stale | 0 | 0 |
| Story | Change + vuln red | Change fixed; vuln still red |

## Not in this slice
- React / demo UI (Day 11+)
- CHD storage (never)
- Renaming control IDs (frozen)

## Deviations / notes
1. PDF uses OpenPDF table layout (not HTML→PDF); content mirrors the HTML pack.
2. `withChangeRecords` synthesizes an adjusted `control_test_result` in memory (does not overwrite Day 1 sample files on disk).
3. Postgres profile is ready but unverified in CI here; H2 is the supported demo/test path.

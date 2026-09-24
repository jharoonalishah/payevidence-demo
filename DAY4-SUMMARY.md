# Day 4 summary — PayEvidence Spring Boot backend

**Date:** 2026-09-24 (JST)  
**Founder:** Haroon Ali Shah  
**Path:** `payevidence/app/`

## What works

- Spring Boot **3.3.4** + Java **21** + Maven + Spring Data JPA
- Default **H2 file** DB (`./data/payevidence`); `postgres` Spring profile ready
- Entities: Organization, Control, Artifact, ControlStatus, EvidencePack
- Catalogue load from `day1/controls/pci-mvp-catalogue.yaml` (IDs frozen, 20 controls)
- Demo tenant seed: exact name **`NordicPay`**
- Day 1 sample ingest: `api_access_log`, `config_snapshot`, `control_test_result`
- Scoring per `day2-3/SCORING.md` (OR evidence types; fail test → gap; freshness)
- REST APIs listed in `README.md`
- Evidence pack: JSON snapshot + HTML table (PDF deferred)
- Optional `POST /api/demo/ingest-change-records`
- `mvn -q test` passes (ApplicationContext + seed + controls + pack)

## How to run

```bash
cd /workspace/haroon-estonia/payevidence/app
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
mvn -q spring-boot:run
```

## Sample curl outputs (after seed)

```
POST /api/demo/seed
→ organizationName=NordicPay, controls=20, pass=16, gap=4, stale=0, artifactsIngested=3

GET /api/orgs/{id}/controls gaps:
→ PCI-REQ-6.5-CHANGE-MGMT
→ PCI-REQ-6.3-SECURE-CHANGE
→ PCI-REQ-6.4-VULN-MGMT
→ PCI-REQ-11.3-VULN-SCAN

POST /api/orgs/{id}/evidence-packs
→ dated pack with statusesJson + htmlContent (+ file under ./packs/)
```

Narrative matches Day 1 / SCORING.md demo mode **without** `change_record`: ~16 greens, **4 reds** (change + vuln).

## Deviations / notes vs SCORING.md

1. **Hard rule honored:** failing `control_test_result` rows force **gap** for 6.5/6.3/6.4/11.3 even if config claims approvals. Ingesting `change_record.json` alone does **not** flip 6.5/6.3 (documented teaching moment).
2. **Pass-rule implementation** is a deterministic Java `switch` on frozen control IDs (as pseudocode in SCORING.md), not an English NLP interpreter.
3. **PCI-REQ-11.3** MVP: trusts test `result` for detail checks (`critical_open_count` / `last_scan_at` not separately parsed from free-text detail); Day 1 sample fails the test → gap anyway.
4. **H2** used for Day 4 instead of Postgres; entities/SQL dialect-friendly via `MODE=PostgreSQL`. Profile `postgres` documented for later.
5. **No React UI** (Day 11+). No PDF yet (HTML pack OK for MVP).

## Test result

`mvn -q test` — **PASS** (2026-09-24 JST): seed asserts pass=16, gap=4; evidence pack create OK.

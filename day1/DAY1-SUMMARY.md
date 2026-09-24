# Day 1 summary — PayEvidence

**Date:** 2026-09-24 (JST)  
**Founder:** Haroon Ali Shah  
**Scope done:** Freeze wedge + catalogue + sample artifacts (no Spring Boot yet)

## What was frozen

1. **ICP** (`ICP.md`)
   - One-sentence ICP for EU payment platforms / PSPs / fintechs with their own card or payment-API stack.
   - Buyer: Head of Compliance / PCI program owner (primary); CISO co-buyer; platform eng influencer.
   - Anti-ICP: merchants without a backend, generic GRC-only buyers, CHD processors-as-customers, pre-product startups.
   - Beachhead: **EE + Nordics → DE/NL**.

2. **PCI MVP catalogue** (`controls/pci-mvp-catalogue.yaml`)
   - **20** controls, evidence-heavy slice only (logging, access, change, vuln/patch evidence, encryption config, incident logging).
   - Each control: `id`, `pci_ref` (best-effort / “mapped for MVP demo”), `title`, `description`, `evidence_types`, `freshness_days`, `pass_rule`.
   - Explicitly **not** full PCI DSS; not legal advice.

3. **Sample artifacts** (`samples/`)
   - `api_access_log.jsonl` — 42 synthetic scrubbed access events.
   - `config_snapshot.json` — TLS / MFA / RBAC / logging / encryption-at-rest / network.
   - `control_test_result.json` — 19 tests: **15 pass / 4 fail** (change + vuln gaps for demo).
   - `samples/README.md` — maps artifacts → controls → product loop.
   - PAN-like 16-digit check: clean.

4. **One-pager**
   - `MVP-ONE-PAGER.md` ICP sentence aligned with frozen ICP.

## Demo story you can tell in 60 seconds

> NordicPay connects three signals. Most logging, access, TLS, and encryption controls go green. Four controls stay red — no change records and no fresh vuln scans — so the evidence pack shows pass + gap, not a fake clean bill of health.

## What’s next

### Day 2–3 (finish wedge)
- [ ] Optional fourth sample: `change_record.json` to flip 6.5 / 6.3 when ingested (or keep failing for stronger demo).
- [ ] One-pager polish (PDF export later); confirm 5–15 named design-partner targets list skeleton.
- [ ] Rehearse 60-second pitch; lock catalogue IDs (avoid renames after Day 4 seeding).
- [ ] Sketch scoring pseudocode from `pass_rule` + `freshness_days` (no app yet).

### Day 4+ (backend — do not start before Day 4)
- Spring Boot + Postgres: Org, Artifact, Control, ControlStatus, EvidencePack.
- Seed demo tenant `NordicPay`; load this YAML + samples.
- APIs: list controls, ingest artifact metadata, recompute pass/gap/stale, create evidence pack.
- Deterministic scoring; HTML/PDF pack export.

## Exit criteria check (Days 1–3 plan)

| Criterion | Status |
|-----------|--------|
| One-sentence ICP | Done |
| ~20 PCI-mapped controls (YAML) | Done (20) |
| 3 sample artifact files | Done |
| One-pager references / aligned | Done (light update) |
| Spring Boot app | **Not started** (correct — Day 4+) |

## File tree

```
day1/
├── ICP.md
├── DAY1-SUMMARY.md
├── controls/
│   └── pci-mvp-catalogue.yaml
└── samples/
    ├── README.md
    ├── api_access_log.jsonl
    ├── config_snapshot.json
    └── control_test_result.json
```

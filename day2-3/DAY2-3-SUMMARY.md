# Day 2–3 summary — PayEvidence

**Date:** 2026-09-24 (JST)  
**Founder:** Haroon Ali Shah  
**Scope done:** Scoring contract, optional change_record sample, design-partner skeleton, pitch/demo scripts, polished one-pager. **No Spring Boot.**

---

## What locked

| Deliverable | Path | Notes |
|-------------|------|--------|
| Scoring algorithm | `day2-3/SCORING.md` | pass / gap / stale; OR across `evidence_types`; fail test → gap; freshness via `freshness_days` |
| Change records (optional) | `day2-3/samples/change_record.json` | 5 synthetic payment-platform changes; approvals present |
| Change sample guide | `day2-3/samples/README-change-record.md` | Which control IDs flip |
| Design partners | `day2-3/DESIGN-PARTNERS.md` | 14 research targets EE/Nordics/DE/NL — not warm intros |
| 60s + 5-min demo | `day2-3/PITCH-60S.md` | Spoken pitch + NordicPay script |
| One-pager (export-ready MD) | `day2-3/ONE-PAGER.md` | Aligned with frozen ICP + 20-day plan |
| Catalogue (unchanged) | `day1/controls/pci-mvp-catalogue.yaml` | See freeze warning below |

**Demo tenant name (standardized):** **`NordicPay`**  
(Day 1 samples, README, and summary already used NordicPay; Day 4 seed must use exactly this string — not `Nordic Pay`, `nordicpay`, or `NordicPayee`.)

**Change sample location:** Prefer `day2-3/samples/` only (not copied into `day1/samples/`). Day 4 ingest path: `/workspace/haroon-estonia/payevidence/day2-3/samples/change_record.json`.

---

## Catalogue IDs — FROZEN

**Do not rename control IDs after this point.** Day 4 will seed Postgres from YAML; renames break samples, tests, and demos.

- Catalogue file: `day1/controls/pci-mvp-catalogue.yaml` (filename may differ in docs; **IDs inside are authoritative**)
- Catalogue id: `pci-mvp-v0`
- Count: **20** controls
- Example IDs (complete list is the YAML):  
  `PCI-REQ-10.2-LOG-ACCESS`, `PCI-REQ-10.2-LOG-ADMIN`, `PCI-REQ-10.3-LOG-FIELDS`, `PCI-REQ-10.4-LOG-REVIEW`, `PCI-REQ-10.5-LOG-INTEGRITY`, `PCI-REQ-10.6-TIME-SYNC`, `PCI-REQ-10.7-LOG-RETENTION`, `PCI-REQ-7.2-ACCESS-RBAC`, `PCI-REQ-8.2-AUTH-MFA`, `PCI-REQ-8.3-AUTH-STRONG`, `PCI-REQ-8.6-API-AUTH`, `PCI-REQ-6.5-CHANGE-MGMT`, `PCI-REQ-6.3-SECURE-CHANGE`, `PCI-REQ-6.4-VULN-MGMT`, `PCI-REQ-11.3-VULN-SCAN`, `PCI-REQ-4.2-TLS-CONFIG`, `PCI-REQ-3.5-ENC-REST`, `PCI-REQ-2.2-SECURE-CONFIG`, `PCI-REQ-12.10-INCIDENT-LOG`, `PCI-REQ-1.2-NET-SEGMENT`

**IDs frozen.** Edits allowed to descriptions/`pass_rule` wording only with care; never to `id` strings.

---

## Days 1–3 exit criteria

| Criterion (20-day plan) | Status |
|-------------------------|--------|
| One-sentence ICP | Done (`day1/ICP.md`) |
| ~20 PCI-mapped controls (YAML) | Done (20) |
| Sample artifacts | Done (3 in day1 + optional change_record in day2-3) |
| One-pager | Done (`day2-3/ONE-PAGER.md`; root `MVP-ONE-PAGER.md` lightly consistent) |
| Explain product in 60 seconds | Done (`PITCH-60S.md`) |
| Scoring rules for Day 4 | Done (`SCORING.md`) |
| Design-partner skeleton | Done (14 named targets) |
| Spring Boot | **Not started** (correct — Day 4+) |

---

## Day 4 kickoff checklist

- [ ] Create Spring Boot + Postgres project (do not invent new scoring — implement `SCORING.md`).
- [ ] Entities: `Org`, `Artifact`, `Control`, `ControlStatus`, `EvidencePack` (names may vary; keep concepts).
- [ ] Seed org/tenant: **`NordicPay`** (exact spelling).
- [ ] Load controls from `day1/controls/pci-mvp-catalogue.yaml` — preserve IDs.
- [ ] Ingest Day 1 samples from `day1/samples/` (`api_access_log.jsonl`, `config_snapshot.json`, `control_test_result.json`).
- [ ] Optional flag/path to ingest `day2-3/samples/change_record.json`.
- [ ] Remember: Day 1 control tests fail 6.5/6.3 → gap until tests updated (see `SCORING.md` demo toggle).
- [ ] APIs: list controls, ingest artifact metadata, recompute pass/gap/stale, create evidence pack.
- [ ] Deterministic scoring; HTML or PDF pack with timestamp + control table + artifact list.
- [ ] PAN/CHD safety: reject or scrub; samples already clean.

### Naming consistency

| Concept | Canonical value |
|---------|-----------------|
| Demo tenant | `NordicPay` |
| Catalogue id | `pci-mvp-v0` |
| Status enum | `pass` \| `gap` \| `stale` |
| Artifact types | `api_access_log`, `config_snapshot`, `control_test_result`, `change_record` |

---

## File tree (Day 2–3)

```
day2-3/
├── SCORING.md
├── DESIGN-PARTNERS.md
├── PITCH-60S.md
├── ONE-PAGER.md
├── DAY2-3-SUMMARY.md
└── samples/
    ├── change_record.json
    └── README-change-record.md
```

---

## What’s next (Day 4+)

Backend core per `20-DAY-BUILD-PLAN.md` Days 4–10. Do not expand catalogue or rename IDs. Outreach soft-start can wait until Day 8+ per plan.

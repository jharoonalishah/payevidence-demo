# PayEvidence — 20-Day Sprint Plan (committee-demable MVP)

**Goal:** In 20 days, have a clickable demo (sample data → controls → gaps → evidence PDF) plus a one-pager and first outreach started.  
**Tradeoff:** Narrow scope hard. PCI subset only. Ugly UI OK. No DORA pack. No real CHD. No polish.

**Daily rhythm:** ~4–6h build most days; outreach starts Day 8 so it overlaps build.

---

## Days 1–3 — Freeze the wedge + catalogue
- [ ] One-sentence ICP: EU/Nordic PSP or fintech payment platform; buyer = Compliance/CISO
- [ ] Write **20 PCI-mapped controls** only (YAML/CSV): id, title, evidence type, freshness days
- [ ] Create 3 sample artifact files: API access logs, config snapshot, synthetic control-test JSON
- [ ] One-pager draft (problem → loop → who pays → why you)

**Exit:** Catalogue + sample files exist; you can explain the product in 60 seconds.

---

## Days 4–10 — Backend core
- [ ] Spring Boot + Postgres: Org, Artifact, Control, ControlStatus, EvidencePack
- [ ] Seed demo tenant `NordicPay`
- [ ] APIs: list controls, ingest/upload artifact metadata, recompute pass/gap/stale, create evidence pack record
- [ ] Deterministic scoring rules (fresh artifact of right type within N days = pass)
- [ ] Generate pack as HTML or PDF (timestamp + control table + artifact list)

**Exit:** curl/Postman can run the full loop on sample data.

---

## Days 11–15 — Demo UI + deploy
- [x] Minimal screens: Controls table, Control detail, Generate pack, Download
- [x] Demo login or secret link (no fancy auth)
- [x] Deploy HTTPS (Docker on cheap EU VPS); password-protect *(notes + Dockerfile/Caddy — VPS not purchased)*
- [x] Rehearse **5-minute demo script** twice *(script written: DEMO-SCRIPT-5MIN.md — founder rehearses aloud)*

**Exit:** A stranger can click the happy path without you narrating every click.

---

## Days 16–20 — Proof you’re a company, not a slide
- [ ] Polish one-pager PDF; 60-second Loom of the demo
- [ ] List **10 named targets** (EE/Nordics/DE/NL)
- [ ] Send **10 outreach** messages; aim for **3 conversations** booked or completed
- [ ] Capture notes in one doc (even “not now” counts as signal)
- [ ] Draft committee narrative outline: product, scalability, roadmap, why Estonia, founder CV bullets

**Exit (Day 20 “done”):**
1. Live demo URL  
2. Evidence PDF from the demo  
3. One-pager + Loom  
4. Outreach log  
5. Decision: proceed to OÜ + Startup Committee prep **or** tighten wedge one more week

---

## Cut list (do NOT do in 20 days)
- Full PCI DSS coverage / DORA pack
- Kafka (use DB + simple jobs)
- Real customer data ingest
- Marketing site, blog, brand kit
- Mobile, SSO, billing, multi-region
- Becoming a PSP or handling cards

## If time slips
Protect in this order: **scoring loop → PDF export → clickable UI → deploy → outreach**. Drop UI beauty, DORA, and extra controls first.

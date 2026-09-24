# PayEvidence — 60-Day Build Plan (from Tokyo → committee-ready)

**Goal:** Demable MVP + outreach evidence + company/committee materials started.  
**Capital lens:** ~€16k total runway for living proof + cloud + company setup — build lean.

---

## Days 1–14 — Spec, wedge, and skeleton

**Product**
- [ ] Freeze ICP: “EU PSP / fintech payment platform, compliance buyer”
- [ ] Pick **PCI DSS v4** subset: ~30 controls that are evidence-heavy (logging, access, change, crypto/key handling *process*, vulnerability, monitoring). Document IDs in a CSV/YAML catalogue.
- [ ] Write control schema: `control_id`, `framework`, `title`, `evidence_types[]`, `freshness_days`, `pass_rules`
- [ ] Sample datasets: fake payment-API access logs, config JSON, synthetic “control test” JSON

**Build**
- [ ] Repo + Spring Boot service + Postgres
- [ ] Models: Organization, User, Source, Artifact, Control, ControlStatus, EvidencePack
- [ ] API: upsert artifacts, recompute statuses, get dashboard summary, generate pack metadata
- [ ] One demo tenant seeded

**GTM**
- [ ] One-pager PDF (problem, loop, who it’s for, founder)
- [ ] List **15 named targets** (EE/FI/SE/DE/NL PSPs & fintechs) with LinkedIn/email if public
- [ ] Outreach script (5 sentences): problem, 15-min call, no pitch deck spam

**Exit gate:** Catalogue + seeded demo API returning pass/gap for sample data.

---

## Days 15–35 — Demo UI + evidence export

**Build**
- [ ] Minimal UI: login (or magic demo link), Controls table, Control detail, Sources, Generate pack
- [ ] Evidence Pack: PDF (or HTML→PDF) with timestamp, org, control list, artifact references
- [ ] “Drift” view: status history for 30 days (can be synthetic timeline)
- [ ] Deploy public HTTPS demo (password-protected)

**GTM**
- [ ] Send 15 outreach messages
- [ ] Book / complete **5 discovery calls** (or async written answers)
- [ ] Capture notes: current evidence process, tools, willingness to pilot

**Exit gate:** Stranger can click through the demo script in &lt;5 minutes; at least some conversations started.

---

## Days 36–60 — Harden story + Estonia track

**Product**
- [ ] Add 1–2 DORA-flavoured ICT controls (continuity, ICT incident logging) — label clearly as “preview pack”
- [ ] README + architecture one-pager for technical reviewers
- [ ] Basic audit log of who generated which pack (meta-compliance)

**Company / immigration track (parallel)**
- [ ] Decide company name; check .ee / name availability
- [ ] Plan **e-Residency** application (digital ID for company mgmt — not residence)
- [ ] Outline OÜ formation steps (can finalize after/with e-Residency)
- [ ] Draft Startup Committee narrative: problem, product, scalability, team, roadmap, why Estonia
- [ ] Living-cost / income proof plan (~4× subsistence for TRP; keep docs ready)

**Exit gate for “committee-ready enough to apply soon after”**
- [ ] Live demo URL
- [ ] 60-second and 5-minute demo scripts
- [ ] Discovery call notes (aim ≥2 “would pilot / want to see more”)
- [ ] Draft committee application packet (iterate with Startup Estonia checklist)

---

## Explicit non-goals (60 days)
- Full PCI coverage
- Production ingest of real cardholder data
- Mobile apps, marketplace, AI chatbot wrapper
- Paid ads
- Hiring a team

---

## Weekly cadence (suggested)
- Mon–Thu: build
- Fri: outreach + demo polish
- Sun: 30-min founder review — demo script still true?

## Decision point (Day 60)
- **Proceed to OÜ + Startup Expert Committee** if demo is solid and ≥2 warm signals  
- **Narrow wedge** (e.g. only “payment API logging evidence”) if interest is mushy  
- **Pivot method to PartnerLedger** only if PayEvidence discovery is cold after real attempts

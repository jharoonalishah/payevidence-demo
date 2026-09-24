# PayEvidence

**Continuous PCI evidence for payment platforms**  
Haroon Ali Shah · Estonia Startup Expert Committee path · 2026-09-24 (JST)

---

## One-line pitch

PayEvidence turns payment-stack signals into continuous, auditor-ready PCI DSS evidence — so fintechs and PSPs stop rebuilding compliance proof in spreadsheets every audit cycle.

---

## Problem

- Card and payment firms must **prove** controls for **PCI DSS** (and, in the EU, rising **DORA / ICT** expectations).
- Evidence today is manual: screenshots, spreadsheets, consultant war rooms, once-a-year panic.
- Between audits, nobody has a live map of “what still proves this control.”

---

## Solution

A focused B2B SaaS **evidence layer** (not a bank, not a PSP, not a QSA):

1. **Ingest** scrubbed signals — API access logs, config snapshots, control-test results, change records.
2. **Map** to a curated PCI-first catalogue (MVP: 20 evidence-heavy controls).
3. **Score** each control **pass / gap / stale** on freshness rules.
4. **Export** dated evidence packs (PDF + JSON manifest) for Compliance / CISO / QSAs.
5. **Dashboard** that payment engineers will not dismiss as GRC theatre.

---

## Ideal customer (frozen)

**EU payment platforms, PSPs, and fintechs that operate their own card or payment-API stack and need continuous, auditor-ready PCI DSS evidence for Compliance / CISO buyers — starting in Estonia and the Nordics, then DE/NL.**

| | |
|--|--|
| **Primary buyer** | Head of Compliance / PCI program owner |
| **Co-buyer** | CISO / Head of Security |
| **Beachhead** | EE + Nordics → Germany & Netherlands |
| **Not for** | Merchants with no payment backend; all-of-GRC shoppers; CHD vaulting; “replace our QSA” |

---

## Why this founder

18+ years backend / platform engineering; **Rakuten Payment Gateway (PCI DSS)**; mobile APIs and large-scale membership platforms. Designs controls and integrations payment engineers respect.

## Why Estonia

Fintech and trust / e-governance ecosystem; exportable **EU RegTech SaaS**; scalable product company for the Startup Committee narrative — not local retail or billable consulting wrapped as a startup.

---

## What the MVP is

- Working demo: sample ingest → control map → gaps → evidence export.
- Multi-tenant shape; polished demo tenant **`NordicPay`**.
- PCI-first catalogue subset (not all of PCI DSS).
- Named design-partner target list + 60s / 5-min scripts.
- Synthetic / scrubbed data only — **no live CHD**.

## What the MVP is not

- Full PCI DSS coverage of every requirement.
- Licensed EMI/PSP or processing cardholder data.
- Polished marketing site + sales team.
- Billable consulting sold as “startup.”

---

## Core product loop (demo)

1. Log in as **NordicPay**.
2. See sources: payment API logs + config snapshot + control tests (+ optional change records).
3. Open Controls: 20 PCI-mapped controls with pass / gap / stale.
4. Click a failing control → missing or stale evidence + suggested artifact.
5. Generate **Evidence Pack** (timestamp, control IDs, artifact list).
6. Honest story: most logging/access/TLS/encryption green; change + vuln red until connected.

---

## 20-day plan (summary)

| Days | Focus |
|------|--------|
| 1–3 | Freeze ICP, 20-control YAML, samples, scoring, pitch, targets *(this pack)* |
| 4–10 | Spring Boot + Postgres; seed NordicPay; score + evidence pack APIs |
| 11–15 | Minimal UI + EU deploy + rehearse demo |
| 16–20 | One-pager PDF, Loom, 10 outreach, committee narrative outline |

**Cut list:** full PCI / DORA pack, Kafka, real CHD, marketing site, billing.

---

## Business model (committee story)

SaaS subscription by company size / control packs (PCI first; DORA ICT pack later). Land with discounted design-partner pilots; expand seats and packs. Software margins; EU-wide expansion from an Estonian OÜ.

---

## Ask / next step

Design-partner discovery: 20-minute demo on synthetic NordicPay data + feedback on how you gather PCI evidence today.

**Contact:** Haroon Ali Shah · PayEvidence · Estonia

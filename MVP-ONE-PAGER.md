# PayEvidence — MVP One-Pager

**Status:** Go (Haroon Ali Shah — Estonia Startup Expert Committee path)  
**Date:** 2026-09-24  
**Stage:** Pre-MVP → demable MVP in 20 days (aggressive sprint)

## One-line pitch
PayEvidence turns payment-stack signals into continuous, auditor-ready PCI DSS (and DORA-relevant ICT) evidence — so fintechs and PSPs stop rebuilding compliance proof in spreadsheets every audit cycle.

## Problem
- Card and payment firms must prove controls for **PCI DSS** and, in the EU, increasingly for **DORA / ICT risk** expectations.
- Evidence today is manual: screenshots, spreadsheets, consultant war rooms, once-a-year panic.
- Gaps appear between audits; nobody has a live map of “what still proves this control.”

## Solution
A focused B2B SaaS **evidence layer** (not a bank, not a PSP):

1. **Ingest** sample/production-like signals (logs, config snapshots, control-test results).
2. **Map** them to a curated control catalogue (PCI first; DORA ICT subset second).
3. **Score** pass / gap / stale continuously.
4. **Export** dated, control-mapped evidence packs (PDF + JSON manifest).
5. **Dashboard** for Compliance / CISO / PCI program owners.

## Ideal customer profile (ICP)
**One sentence:** EU payment platforms, PSPs, and fintechs that operate their own card or payment-API stack and need continuous, auditor-ready PCI DSS evidence for Compliance / CISO buyers — starting in Estonia and the Nordics, then DE/NL.

- Buyer: Head of Compliance / PCI program owner (primary); CISO co-buyer.
- Beachhead: EE + Nordics → DE/NL (see `day1/ICP.md`).
- Not: merchants with no payment backend; not generic all-of-GRC; not CHD processing / PSP licensing.

## Why you (founder edge)
18+ years backend/platform; Rakuten Payment Gateway (PCI DSS), mobile APIs, affiliate, membership platforms. You can design controls and integrations that payment engineers respect.

## Why Estonia
Fintech / trust / e-governance ecosystem; exportable EU RegTech SaaS; scalable product company (committee-readable), not local retail or consultancy.

## What MVP is (and is not)

**Is**
- Working demo: upload/sample ingest → control map → gaps → evidence export.
- Multi-tenant-ready shape (orgs, users) even if only one demo tenant is polished.
- Clear PCI-first catalogue (subset of high-value controls, not all of PCI DSS).
- 5–15 named design-partner targets and outreach script.

**Is not**
- Full PCI DSS coverage of every requirement.
- Licensed EMI/PSP or handling live cardholder data in production.
- Polished marketing site + sales team.
- Billable consulting wrapped as “startup.”

## Core product loop (demo script)
1. Log in as “NordicPay” demo tenant.
2. See connected sources (sample payment API logs + config snapshot + synthetic control tests).
3. Open Controls: **20** PCI-mapped controls with status (catalogue frozen in `day1/controls/pci-mvp-catalogue.yaml`).
4. Click a failing control → see missing/stale evidence and suggested artifact.
5. Generate **Evidence Pack** PDF (timestamp, control IDs, artifact hashes/list).
6. Show “last 30 days drift” — one control flipped from pass to gap.

## Architecture (lean)
- Backend: Java/Kotlin + Spring Boot (your stack)
- Events: Kafka or a simple queue (can start with DB outbox if Kafka is heavy for day 1)
- Store: Postgres
- Objects: S3-compatible for artifacts
- Frontend: simple admin UI (React or server-rendered — speed over beauty)
- Deploy: Docker on a cheap EU cloud (Hetzner/AWS eu-north-1/GCP europe-north1)

**Hard rule for MVP:** use **synthetic / anonymized sample data only**. Do not ingest real CHD. Position as “connect to your logs and configs,” not “send us card data.”

## Business model (committee story)
- SaaS subscription by company size / control packs (PCI pack, later DORA pack).
- Land with design partners (discounted pilot); expand seats and packs.
- Scalable software margins; no inventory; EU-wide TAM.

## Success for committee package
- Estonian OÜ registered
- Working MVP URL + short loom/demo
- 1-pager + business plan
- Notes from discovery calls (even “interested / not now”)
- Founder CV showing payment/PCI credibility

---

## Day 2–3 pointers
- Polished export-ready one-pager: `day2-3/ONE-PAGER.md`
- Scoring contract for implementers: `day2-3/SCORING.md`
- Demo tenant spelling: **NordicPay** (canonical)
- Optional change evidence: `day2-3/samples/change_record.json`

# PayEvidence — Ideal Customer Profile (Day 1 freeze)

**Frozen:** 2026-09-24 (JST)

## One-sentence ICP

EU payment platforms, PSPs, and fintechs that operate their own card or payment-API stack and need continuous, auditor-ready PCI DSS evidence for Compliance / CISO buyers — starting in Estonia and the Nordics, then DE/NL.

## Buyer

| Role | Why they care |
|------|----------------|
| **Primary:** Head of Compliance / PCI program owner | Owns audit cycles, QSA relationships, evidence packs; buys tooling that shortens prep and closes gaps between audits. |
| **Champion / co-buyer:** CISO / Head of Security | Owns control design, logging, access, change, vuln process; wants continuous status not annual screenshots. |
| **Influencer:** Platform / payments engineering lead | Must connect logs and configs; rejects “GRC theatre” that ignores payment stack reality. |

Economic buyer is usually Compliance or Security budget (SaaS subscription by company size / control packs). Design-partner pilots may sit under CTO for the first 1–2 logos.

## Anti-ICP (who we are NOT for)

- Merchants or shops with **no** card/payment backend of their own (they inherit PCI via a gateway; they do not generate the evidence we ingest).
- Generic “all-of-GRC” buyers wanting ISO 27001 / SOC 2 / everything in one tool — we are **PCI-first evidence**, not an enterprise GRC suite.
- Companies that only need a **one-off** consultant spreadsheet for next year’s ROC and will not connect systems.
- Startups still pre-product with no payment API, logs, or config surfaces to evidence.
- Anyone expecting us to **store or process live cardholder data (CHD)** — PayEvidence is an evidence layer on synthetic / scrubbed / metadata signals, not a CHD processor or PSP.
- Non-regulated consumer apps with no PCI or DORA-adjacent pressure.

## Beachhead geography

1. **Primary beachhead:** Estonia + Nordics (FI, SE, NO, DK) — fintech density, English-friendly buyers, Estonia Startup Committee path aligns with founder base.
2. **Near expansion:** Germany and Netherlands — large PSP / open-banking / acquiring ecosystems; same EU regulatory frame (PCI + rising DORA ICT expectations).
3. **Explicitly later:** UK (post-MVP), US (not Day 1–20), Asia (founder network only for intros, not ICP).

## Design-partner filter (use in outreach)

Qualify if **all** are true:

1. Runs or deeply integrates a payment / card / open-banking API (not only Shopify + Stripe Checkout with zero in-house controls).
2. Has (or is preparing for) PCI DSS scope — SAQ D, ROC, or service-provider assessment.
3. Buyer title in Compliance, CISO, or PCI program — or engineering lead with Compliance sponsorship.
4. HQ or material ops in EE / Nordics / DE / NL (or expanding there).

Disqualify if they ask for CHD vaulting, full PCI consulting delivery, or “replace our QSA.”

# PayEvidence — 60-second pitch + 5-minute demo script

**Demo tenant (canonical):** `NordicPay`  
**Aligned with:** Day 1 story in `day1/DAY1-SUMMARY.md` and `day1/samples/README.md`

---

## Exact 60-second spoken pitch (rehearse aloud)

> Fintechs and PSPs in Europe still rebuild PCI proof in spreadsheets every audit cycle — screenshots, war rooms, then silence until next year.  
> I’m Haroon — eighteen-plus years in backend and payments, including Rakuten’s payment gateway under PCI DSS. I’m building **PayEvidence** from Estonia: an evidence layer, not a bank and not a PSP.  
> We ingest payment-stack signals — API access logs, config snapshots, control tests — map them to a curated PCI control catalogue, and continuously score each control **pass, gap, or stale**. Then we export a dated evidence pack Compliance and CISOs can hand a QSA.  
> We don’t store cardholder data. We don’t claim full PCI coverage. We make the controls you already run *provable* between audits.  
> Beachhead is Estonia and the Nordics, then Germany and the Netherlands. I’d love twenty minutes to show the NordicPay demo and hear how you prep evidence today.

**Timing check:** ~55–65 seconds at calm pace. Cut the founder sentence if over time.

---

## One-liner (elevator / LinkedIn)

> PayEvidence turns payment-stack logs and configs into continuous, auditor-ready PCI evidence — pass, gap, or stale — without touching card data.

---

## 5-minute demo script (match Day 1 story)

**UI click path (Days 11–15):** see `../DEMO-SCRIPT-5MIN.md` (Thymeleaf NordicPay dashboard).

**Setup:** Seed tenant **NordicPay** with Day 1 three artifacts **without** `change_record` first (intentional reds). Optional second pass: ingest `day2-3/samples/change_record.json` + aligned tests.

| Min | Step | Say / show |
|-----|------|------------|
| 0:00–0:30 | Context | “You’re Compliance at a Nordic PSP. Audit in six weeks. You need proof, not another slide.” |
| 0:30–1:00 | Login | Open demo as **NordicPay**. “Synthetic data only — no CHD.” |
| 1:00–1:45 | Sources | Show connected sources: `api_access_log`, `config_snapshot`, `control_test_result`. |
| 1:45–2:45 | Controls table | “Twenty PCI-mapped controls — MVP subset, not all of PCI. Most logging, access, TLS, encryption are green.” |
| 2:45–3:45 | Fail deep-dive | Click a red control: **PCI-REQ-6.5-CHANGE-MGMT** or **6.3** — “No change_record.” Then **6.4 / 11.3** — “Vuln process / scan not connected.” “We show gaps on purpose — not a fake clean bill of health.” |
| 3:45–4:30 | Evidence pack | Generate pack: timestamp, control IDs, artifact list/hashes. “This is what you email the QSA or drop in the audit folder.” |
| 4:30–5:00 | Close | “Optional: ingest change records → change controls can flip green; vuln stays red until you connect scans. Design-partner ask: twenty minutes on your real evidence pain — still synthetic ingest only.” |

### Optional 30s “with change_record” coda

> “If we load the synthetic change_record sample, change-management controls can go green — still leaving vuln gaps — so the pack shows progress, not perfection.”

---

## Objection cheats

| Objection | Reply |
|-----------|--------|
| “We already have a GRC tool.” | “Great — we feed evidence into the PCI story; we’re not replacing your GRC suite.” |
| “Our QSA does this.” | “QSA assesses; you still gather proof. We shorten the gather loop.” |
| “Will you hold card data?” | “No. Metadata, scrubbed logs, configs, test results only.” |
| “Full PCI?” | “No — evidence-heavy subset first. Honest gaps beat fake coverage.” |

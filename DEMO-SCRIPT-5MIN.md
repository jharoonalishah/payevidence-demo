# PayEvidence — 5-minute demo script (Days 11–15 UI)

**Rehearse twice aloud.** Match clicks to the Thymeleaf UI.  
**Tenant:** NordicPay · **Token:** `nordic-demo` (or your `PAYEVIDENCE_DEMO_TOKEN`)  
**URL:** `http://localhost:8080/demo?token=nordic-demo` (or VPS HTTPS + basic auth, then same path)

Also see spoken 60s pitch: `day2-3/PITCH-60S.md`.

---

## Pre-flight (30s before call)

1. `mvn -q spring-boot:run` (or Docker) is up.
2. Open `/demo?token=nordic-demo` — you should see **pass / gap / stale** chips.
3. Click **Reset demo (default 16/4)** so the story starts with four intentional gaps.

---

## Minute-by-minute click path

| Time | Click / show | Say |
|------|----------------|-----|
| **0:00–0:30** | Land on NordicPay dashboard | “You’re Compliance at a Nordic PSP. Audit in six weeks. You need proof, not another slide. Synthetic data only — we never touch cardholder data.” |
| **0:30–1:00** | Point at chips: pass≈16, gap≈4, stale≈0 | “We continuously score a curated PCI subset — pass, gap, or stale — from payment-stack artifacts.” |
| **1:00–1:45** | Click chip **gap** (filter) | “Four gaps on purpose. We’re not selling a fake clean bill of health.” |
| **1:45–2:45** | Open **PCI-REQ-6.5-CHANGE-MGMT** (or 6.3) | “Reason: no change_record / failing change test. Contributing artifacts listed. This is the gather loop QSAs still make you do by hand.” |
| **2:45–3:15** | Back → open a **vuln** gap (6.4 or 11.3) | “Vuln process/scan not green. Different story than change management — stays red until you connect scans.” |
| **3:15–4:00** | **Generate evidence pack** → **View HTML** / **Download PDF** | “Dated pack: control table + artifacts. Email the QSA or drop in the audit folder.” |
| **4:00–4:40** | **Apply change-record profile (18/2)** | “Progress without perfection: change gaps clear; vuln gaps remain. Optional teaching button ‘ingest change records only’ keeps tests failing — hard scoring rule unchanged.” |
| **4:40–5:00** | Stay on dashboard | “Beachhead Estonia/Nordics. Design-partner ask: twenty minutes on how you prep evidence today — still synthetic ingest only.” |

---

## Optional 20s API aside (if technical buyer)

> “Same loop is curlable: `POST /api/demo/seed`, list gaps, `POST .../evidence-packs`, download PDF. UI is the committee path; API is the integration path.”

---

## Reset between rehearsals

- **Reset demo (default 16/4)** before each run.  
- Logout only if you need to show the gate screen.

## Objection cheats (from pitch)

| Objection | Reply |
|-----------|--------|
| GRC tool already | We feed PCI evidence; we don’t replace GRC. |
| QSA does this | QSA assesses; you still gather. We shorten gather. |
| Card data? | No. Metadata, scrubbed logs, configs, tests. |
| Full PCI? | No — evidence-heavy subset first. |

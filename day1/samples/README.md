# Sample artifacts — how they map into the product loop

These files are **100% synthetic** for the NordicPay demo tenant. No real CHD, no PANs, no production secrets.

## Artifact types

| File | Type | What it is |
|------|------|------------|
| `api_access_log.jsonl` | `api_access_log` | ~42 scrubbed payment-API access events (`/v1/payments`, `/v1/refunds`, admin, webhooks). |
| `config_snapshot.json` | `config_snapshot` | Payment-service security config: TLS, auth/MFA/RBAC, logging, encryption-at-rest, network posture, time sync. |
| `control_test_result.json` | `control_test_result` | Automated suite results for 19 of 20 catalogue controls (mix of pass/fail for demo gaps). |

`change_record` is a fourth evidence type in the catalogue but **intentionally absent** in Day 1 samples so change-management controls fail until Day 4+ ingest can accept a fourth sample.

## Product loop (demo script)

1. **Ingest** these three artifacts for tenant `NordicPay`.
2. **Map** each artifact’s `evidence_types` to controls in `../controls/pci-mvp-catalogue.yaml`.
3. **Score** each control:
   - Has a fresh artifact of an allowed `evidence_types` within `freshness_days`, and `pass_rule` holds → **pass**
   - Wrong/missing type or stale → **gap** / **stale**
4. **Show gaps** — Day 1 story: change-mgmt + vuln scan fail (see below).
5. **Export** evidence pack listing control IDs + artifact hashes/paths + timestamp.

## Control → artifact coverage (MVP demo)

### Satisfied primarily by `api_access_log`

| Control ID | Role of this artifact |
|------------|------------------------|
| `PCI-REQ-10.2-LOG-ACCESS` | Proves payment/refund paths are audited |
| `PCI-REQ-10.2-LOG-ADMIN` | Admin paths (`/v1/admin/*`) present |
| `PCI-REQ-10.3-LOG-FIELDS` | Required fields + `scrubbed: true` |
| `PCI-REQ-8.6-API-AUTH` | Actor present on authenticated calls |
| `PCI-REQ-12.10-INCIDENT-LOG` | Correlation source for IR (with tests) |

### Satisfied primarily by `config_snapshot`

| Control ID | Role of this artifact |
|------------|------------------------|
| `PCI-REQ-10.5-LOG-INTEGRITY` | `logging.integrity_protection` |
| `PCI-REQ-10.6-TIME-SYNC` | `time_sync.enabled` |
| `PCI-REQ-10.7-LOG-RETENTION` | `logging.retention_days >= 90` |
| `PCI-REQ-7.2-ACCESS-RBAC` | `auth.rbac_enabled` |
| `PCI-REQ-8.2-AUTH-MFA` | `auth.admin_mfa_required` |
| `PCI-REQ-8.3-AUTH-STRONG` | `auth.modes` / no password-only admin |
| `PCI-REQ-4.2-TLS-CONFIG` | `tls.min_version` / disabled legacy |
| `PCI-REQ-3.5-ENC-REST` | `encryption.at_rest` |
| `PCI-REQ-2.2-SECURE-CONFIG` | `hardening.*` |
| `PCI-REQ-1.2-NET-SEGMENT` | `network.*` |

### Satisfied primarily by `control_test_result`

| Control ID | Notes |
|------------|--------|
| `PCI-REQ-10.4-LOG-REVIEW` | Review cadence evidenced by test pass |
| Most config/log controls above | Tests **reinforce** snapshot/log evidence |
| `PCI-REQ-6.4-VULN-MGMT` | **fail** in sample — gap for demo |
| `PCI-REQ-11.3-VULN-SCAN` | **fail** in sample — gap for demo |
| `PCI-REQ-6.5-CHANGE-MGMT` | **fail** — needs `change_record` |
| `PCI-REQ-6.3-SECURE-CHANGE` | **fail** — needs `change_record` |

### Intentionally failing in Day 1 demo (narrative)

Use these four fails when clicking “failing control → missing evidence”:

1. **PCI-REQ-6.5-CHANGE-MGMT** — no `change_record` artifact.
2. **PCI-REQ-6.3-SECURE-CHANGE** — approval dual-control not evidenced.
3. **PCI-REQ-6.4-VULN-MGMT** — vuln tracker not connected.
4. **PCI-REQ-11.3-VULN-SCAN** — scan freshness missing.

Suggested remediation in UI copy: “Upload a change_record” / “Connect vuln scan export” — do not invent CHD.

## Freshness cheat-sheet (from catalogue)

| Evidence class | Typical freshness_days |
|----------------|------------------------|
| Access logs / API auth | 7 |
| Log review tests | 7 |
| Admin logs | 14 |
| Config + most control tests | 30 |
| Vuln scans | 90 |

Scoring engine (Day 4+) should treat `captured_at` / `completed_at` / last log `ts` as the freshness clock.

## Safety rules

- Never put real PANs, track data, or CVV in samples.
- Keep `scrubbed: true` and `chd_present: false` on log lines.
- Treat catalogue `pci_ref` labels as **mapped for MVP demo**, not legal advice.

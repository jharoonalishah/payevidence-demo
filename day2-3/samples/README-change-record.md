# change_record.json — which controls flip

**Path:** `day2-3/samples/change_record.json` (kept here; not copied into `day1/samples/`).  
**Tenant:** `NordicPay`  
**Optional:** Demo runs **with or without** this file.

## With this file ingested (and tests aligned)

| Control ID | Effect |
|------------|--------|
| `PCI-REQ-6.5-CHANGE-MGMT` | → **pass** — fresh payment-api change records within 30 days |
| `PCI-REQ-6.3-SECURE-CHANGE` | → **pass** — records have `approved_by` ≠ `author` |
| `PCI-REQ-10.2-LOG-ADMIN` | → **reinforced / may pass via OR** — privileged RBAC + credential changes count as admin evidence alongside api_access_log |

## Does **not** flip (stay red for honest demo)

| Control ID | Why |
|------------|-----|
| `PCI-REQ-6.4-VULN-MGMT` | Still needs vuln-process / test pass |
| `PCI-REQ-11.3-VULN-SCAN` | Still needs fresh scan evidence |

## Without this file

Keep Day 1 story: **6.5** and **6.3** remain **gap** (plus vuln gaps). More intentional reds in the evidence pack.

## Scoring gotcha

Day 1 `control_test_result.json` marks 6.5 / 6.3 as `fail`. Per `SCORING.md`, a failing test **forces gap** even if change records exist. To show greens after ingest:

- re-run / seed a test suite that passes 6.5 & 6.3, **or**
- use the Day 4 demo toggle described in `SCORING.md`.

## Safety

No secrets, PANs, or live credentials — IDs and metadata only (`secret_material_included: false`).

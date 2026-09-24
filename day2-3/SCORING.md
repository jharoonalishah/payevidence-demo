# PayEvidence — Control scoring algorithm (Day 2–3 freeze)

**Audience:** Day 4 implementers (Spring Boot scoring job)  
**Frozen:** 2026-09-24 (JST)  
**Inputs:** `day1/controls/pci-mvp-catalogue.yaml` + ingested artifacts (type + timestamp)  
**Outputs per control:** `pass` | `gap` | `stale`

This document is the **authoritative** scoring contract. Do not invent alternate rules in code without updating this file.

---

## Status meanings

| Status | Meaning |
|--------|---------|
| **pass** | At least one qualifying evidence path satisfies the control’s `pass_rule`, and evidence is within `freshness_days`. |
| **stale** | Evidence of an allowed type exists and would otherwise satisfy the rule, but the freshness clock is older than `freshness_days`. |
| **gap** | No usable evidence, wrong type only, `pass_rule` not met, **or** a relevant `control_test_result` for this control is `fail` (see hard rule below). |

Priority when multiple signals conflict: **failing control_test_result → gap** beats pass from config/logs. Stale is preferred over gap only when evidence exists, matches type, and would pass if fresh (and no fail test).

---

## Evidence-type interaction: OR (documented choice)

**Rule:** `evidence_types` on a control is an **OR list**.

- Any **one** listed type may satisfy the control if that artifact passes the control’s `pass_rule` and freshness.
- You do **not** need every listed type present.
- Example: `PCI-REQ-10.2-LOG-ADMIN` lists `[api_access_log, change_record]` — either admin API logs **or** a privileged change_record can pass.

**AND only appears inside a single artifact’s `pass_rule`** (e.g. config must have both `rbac_enabled` and snapshot freshness). Multi-type AND across artifacts is **not** required for MVP.

Reinforcement: when both a config/log artifact and a `control_test_result` with `result=pass` exist, treat as pass (test is optional garnish unless `pass_rule` requires the test exclusively).

---

## Hard rule: `control_test_result` fail → gap

If an ingested `control_test_result` artifact contains a result row for `control_id` with `result == "fail"` (and that test row’s `completed_at` is within the control’s `freshness_days`, or is the latest test for that control):

→ status is **`gap`**, even if a `config_snapshot` / `api_access_log` / `change_record` would otherwise pass.

**Rationale:** Day 1 samples intentionally fail change + vuln controls via tests; config may still claim `require_approval_for_prod: true`. Product story: automated tests are the truth signal when they fail.

If no test row exists for the control, score from other artifact types only.

If the latest test is `pass` and other evidence also passes → **pass**.  
If the latest test is `pass` but only stale config exists → **stale** (test pass does not erase staleness of required config when `pass_rule` depends on config fields — MVP simplification: if test alone is enough per `pass_rule`, test freshness wins).

**MVP shortcut for Day 4:** evaluate in this order per control:

1. Latest `control_test_result` row for this `control_id` (if any, and not stale by control freshness).
2. If `fail` → **gap** (stop).
3. Else evaluate other allowed artifact types + `pass_rule`.
4. Else if only stale matching evidence → **stale**.
5. Else → **gap**.

---

## Freshness clock

| Artifact type | Clock field |
|---------------|-------------|
| `api_access_log` | Max `ts` among events in the artifact (or artifact-level `captured_at` if present) |
| `config_snapshot` | `captured_at` |
| `control_test_result` | Per-row `completed_at`, else suite `completed_at` |
| `change_record` | Per-record `closed_at` or `implemented_at` (prefer closed/implemented); else `created_at` |

```
age_days = floor( (now_utc - evidence_ts_utc) / 86400 )
fresh     = age_days <= control.freshness_days
```

Use catalogue field **`freshness_days`** (integer). There is no separate `freshness` string in v0 catalogue — if a future field `freshness` appears, treat it as alias for `freshness_days`.

Future timestamps (`evidence_ts > now`): treat as **invalid** → ignore that artifact for scoring (same as missing). Do not pass on future-dated evidence.

---

## Pseudocode (language-agnostic)

```
function score_control(control, artifacts, now):
  allowed = set(control.evidence_types)
  candidates = artifacts where artifact.type in allowed
                and artifact.tenant == org
                and clock(artifact) is not null
                and clock(artifact) <= now   # reject future

  # --- Hard fail from control tests ---
  tests = candidates where type == control_test_result
  latest_row = newest result row in tests for control.id
                where clock(row) <= now
  if latest_row exists and age_days(latest_row, now) <= control.freshness_days:
    if latest_row.result == "fail":
      return GAP
    # if pass, continue — may still need other evidence per pass_rule

  # --- Evaluate non-test (and test-pass) evidence against pass_rule ---
  fresh_ok = []
  stale_ok = []
  for a in candidates:
    if not matches_pass_rule(control, a, candidates):
      continue
    if age_days(clock(a), now) <= control.freshness_days:
      fresh_ok.append(a)
    else:
      stale_ok.append(a)

  if fresh_ok is not empty:
    # If pass_rule requires control_test_result exclusively and latest is fail,
    # already returned GAP above.
    return PASS

  if stale_ok is not empty:
    return STALE

  return GAP


function matches_pass_rule(control, primary_artifact, all_candidates):
  # Implement the English pass_rule from YAML as deterministic checks.
  # Day 4 minimum viable implementations:

  switch control.id:

    # Logs
    case PCI-REQ-10.2-LOG-ACCESS:
      return has_api_log_covering_paths(["/v1/payments","/v1/refunds"], required_fields)

    case PCI-REQ-10.2-LOG-ADMIN:
      return has_api_log_admin_paths() OR has_change_record(privileged=true)

    case PCI-REQ-10.3-LOG-FIELDS:
      return (test_pass(control.id) OR api_log_fields_complete())

    case PCI-REQ-10.4-LOG-REVIEW:
      return test_pass(control.id)

    # Config-driven (examples)
    case PCI-REQ-10.5-LOG-INTEGRITY:
      return config.logging.integrity_protection == true
    case PCI-REQ-10.6-TIME-SYNC:
      return config.time_sync.enabled == true AND config.time_sync.ntp_or_equiv set
    case PCI-REQ-10.7-LOG-RETENTION:
      return config.logging.retention_days >= 90
    case PCI-REQ-7.2-ACCESS-RBAC:
      return config.auth.rbac_enabled == true
    case PCI-REQ-8.2-AUTH-MFA:
      return config.auth.admin_mfa_required == true
    case PCI-REQ-8.3-AUTH-STRONG:
      return config.auth.modes intersects STRONG_MODES
             AND config.auth.password_only_admin == false
    case PCI-REQ-4.2-TLS-CONFIG:
      return config.tls.min_version in {"1.2","1.3"}
             AND no legacy in versions_enabled
    case PCI-REQ-3.5-ENC-REST:
      return config.encryption.at_rest.enabled == true
             AND config.encryption.at_rest.key_management != "none"
    case PCI-REQ-2.2-SECURE-CONFIG:
      return config.hardening.debug_endpoints_enabled == false
             AND config.hardening.default_credentials_allowed == false
    case PCI-REQ-1.2-NET-SEGMENT:
      return config.network.admin_public == false
             AND config.network.allowlist_or_private_ingress == true

    case PCI-REQ-8.6-API-AUTH:
      return api_auth_ratio_ok() OR test_pass(control.id)

    # Change / vuln
    case PCI-REQ-6.5-CHANGE-MGMT:
      return has_fresh_change_record(service="payment-api") OR test_pass(control.id)

    case PCI-REQ-6.3-SECURE-CHANGE:
      return has_change_record(approved_by present AND approved_by != author)
             OR test_pass(control.id)

    case PCI-REQ-6.4-VULN-MGMT:
      return test_pass(control.id)   # MVP: tests or change tying remediation

    case PCI-REQ-11.3-VULN-SCAN:
      return test_pass(control.id) AND detail.last_scan fresh AND critical_open_count == 0
             # if only test fail/missing → gap (Day 1 sample)

    case PCI-REQ-12.10-INCIDENT-LOG:
      return test_pass(control.id)
             OR (config.logging.security_event_sink set AND api_access_log present)

  default:
    return false   # unknown id → gap until catalogue updated
```

`test_pass(id)` means latest non-stale `control_test_result` row for `id` has `result == "pass"`.

---

## Edge cases (must handle)

| Case | Behavior |
|------|----------|
| Missing artifact | No candidates → **gap** |
| Wrong `artifact_type` | Ignored for that control |
| Empty api_access_log file | Not covering paths → does not satisfy log controls |
| `change_record` absent | 6.5 / 6.3 remain **gap** unless test pass (Day 1 demo = gap) |
| Future timestamp | Ignore artifact |
| Clock skew / null ts | Ignore artifact (treat as missing) |
| Multiple artifacts same type | Use newest that matches `pass_rule`; if newest fails rule but older passes and is fresh, MVP may use any fresh matching — prefer **newest first**, then scan older |
| Catalogue control with empty `evidence_types` | Always **gap** |
| Tenant mismatch | Artifact does not score for other orgs |

---

## Demo modes (Day 1 story preserved)

| Mode | Artifacts | Expected narrative |
|------|-----------|--------------------|
| **Without** `change_record` | Day 1 three files only | ~16 pass-ish greens; **4 reds**: 6.5, 6.3, 6.4, 11.3 |
| **With** `day2-3/samples/change_record.json` | + change records | **6.5 and 6.3 flip toward pass** *if* scoring trusts change_record **and** you either omit the failing test rows for those IDs or ingest an updated control_test_result with pass — see note below |
| Vuln gaps | Keep failing tests | **6.4 and 11.3 stay gap** for honest demo |

**Important interaction:** Day 1 `control_test_result.json` marks 6.5 and 6.3 as `fail`. Per hard rule, ingesting change_record alone will **not** flip them to pass until tests are updated or excluded. Day 4 options:

1. **Recommended demo toggle:** seed two test suites — `baseline` (4 fails) and `with-change` (6.5/6.3 pass, vuln still fail); or  
2. Re-score change controls from `change_record` only when `PASS_RULE_PREFER_ARTIFACT=true` (document in API); or  
3. Provide companion `control_test_result_with_change.json` later.

Default product honesty: **failing test wins** → document in UI: “Connect change records **and** re-run control tests.”

For Day 4 seed without extra complexity: keep baseline 4 gaps; optionally show “ingest change_record → still gap until tests re-run” as teaching moment, **or** seed the with-change test variant when the sample is loaded.

---

## Catalogue IDs (do not rename)

All scoring keys off frozen IDs in `day1/controls/pci-mvp-catalogue.yaml` (`pci-mvp-v0`). Renaming after DB seed breaks demos — see `DAY2-3-SUMMARY.md`.

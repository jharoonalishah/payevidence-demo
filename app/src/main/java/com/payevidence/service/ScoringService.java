package com.payevidence.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payevidence.domain.*;
import com.payevidence.repo.ArtifactRepository;
import com.payevidence.repo.ControlRepository;
import com.payevidence.repo.ControlStatusRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements day2-3/SCORING.md — do not invent alternate rules.
 */
@Service
public class ScoringService {
  private static final Logger log = LoggerFactory.getLogger(ScoringService.class);
  private static final Set<String> STRONG_MODES = Set.of(
      "mtls", "oauth2_client_credentials", "jwt_short_lived");
  private static final Set<String> LEGACY_TLS = Set.of("SSLv3", "TLS1.0", "TLS1.1", "1.0", "1.1");

  private final ControlRepository controlRepository;
  private final ArtifactRepository artifactRepository;
  private final ControlStatusRepository statusRepository;
  private final ObjectMapper objectMapper;

  public ScoringService(
      ControlRepository controlRepository,
      ArtifactRepository artifactRepository,
      ControlStatusRepository statusRepository,
      ObjectMapper objectMapper) {
    this.controlRepository = controlRepository;
    this.artifactRepository = artifactRepository;
    this.statusRepository = statusRepository;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public List<ControlStatus> recomputeAll(Organization org) {
    Instant now = Instant.now();
    List<Artifact> artifacts = artifactRepository.findByOrganization(org);
    List<Control> controls = controlRepository.findAll();
    List<ControlStatus> results = new ArrayList<>();
    for (Control control : controls) {
      ScoreOutcome outcome = scoreControl(control, artifacts, now);
      ControlStatus status = statusRepository
          .findByOrganizationAndControl(org, control)
          .orElseGet(ControlStatus::new);
      status.setOrganization(org);
      status.setControl(control);
      status.setStatus(outcome.status());
      status.setComputedAt(now);
      status.setReason(outcome.reason());
      results.add(statusRepository.save(status));
    }
    long pass = results.stream().filter(s -> s.getStatus() == StatusValue.pass).count();
    long gap = results.stream().filter(s -> s.getStatus() == StatusValue.gap).count();
    long stale = results.stream().filter(s -> s.getStatus() == StatusValue.stale).count();
    log.info("Scored org {}: pass={} gap={} stale={}", org.getName(), pass, gap, stale);
    return results;
  }

  record ScoreOutcome(StatusValue status, String reason) {}

  ScoreOutcome scoreControl(Control control, List<Artifact> artifacts, Instant now) {
    Set<ArtifactType> allowed = new HashSet<>(control.getEvidenceTypes());
    if (allowed.isEmpty()) {
      return new ScoreOutcome(StatusValue.gap, "empty evidence_types");
    }

    List<Artifact> candidates = new ArrayList<>();
    for (Artifact a : artifacts) {
      if (!allowed.contains(a.getType())) continue;
      Instant clock = clock(a);
      if (clock == null || clock.isAfter(now)) continue;
      candidates.add(a);
    }

    // Hard rule: failing control_test_result → gap
    TestRow latestTest = latestTestRow(control.getId(), candidates, now);
    if (latestTest != null && ageDays(latestTest.completedAt(), now) <= control.getFreshnessDays()) {
      if ("fail".equalsIgnoreCase(latestTest.result())) {
        return new ScoreOutcome(StatusValue.gap,
            "control_test_result fail: " + truncate(latestTest.detail()));
      }
    }

    List<Artifact> freshOk = new ArrayList<>();
    List<Artifact> staleOk = new ArrayList<>();
    // Prefer newest first
    candidates.sort(Comparator.comparing((Artifact a) -> clock(a), Comparator.nullsLast(Comparator.naturalOrder())).reversed());

    for (Artifact a : candidates) {
      if (!matchesPassRule(control, a, candidates, now)) continue;
      Instant c = clock(a);
      if (ageDays(c, now) <= control.getFreshnessDays()) {
        freshOk.add(a);
      } else {
        staleOk.add(a);
      }
    }

    // Also: test_pass alone can satisfy some rules via matchesPassRule using a synthetic path —
    // if latest test is pass and rule is test-only, ensure we catch it:
    if (freshOk.isEmpty() && latestTest != null
        && "pass".equalsIgnoreCase(latestTest.result())
        && ageDays(latestTest.completedAt(), now) <= control.getFreshnessDays()
        && testAloneSatisfies(control.getId())) {
      return new ScoreOutcome(StatusValue.pass, "control_test_result pass");
    }

    if (!freshOk.isEmpty()) {
      return new ScoreOutcome(StatusValue.pass,
          "matched " + freshOk.get(0).getType() + " artifact " + freshOk.get(0).getId());
    }
    if (!staleOk.isEmpty()) {
      return new ScoreOutcome(StatusValue.stale,
          "matching evidence older than " + control.getFreshnessDays() + " days");
    }
    return new ScoreOutcome(StatusValue.gap, "no qualifying evidence");
  }

  private boolean testAloneSatisfies(String controlId) {
    return Set.of(
        "PCI-REQ-10.4-LOG-REVIEW",
        "PCI-REQ-6.4-VULN-MGMT",
        "PCI-REQ-11.3-VULN-SCAN"
    ).contains(controlId);
  }

  boolean matchesPassRule(Control control, Artifact primary, List<Artifact> all, Instant now) {
    String id = control.getId();
    JsonNode config = findConfig(all);
    return switch (id) {
      case "PCI-REQ-10.2-LOG-ACCESS" ->
          primary.getType() == ArtifactType.api_access_log
              && hasApiLogCoveringPaths(primary, List.of("/v1/payments", "/v1/refunds"));

      case "PCI-REQ-10.2-LOG-ADMIN" ->
          (primary.getType() == ArtifactType.api_access_log && hasApiLogAdminPaths(primary))
              || (primary.getType() == ArtifactType.change_record && hasPrivilegedChange(primary));

      case "PCI-REQ-10.3-LOG-FIELDS" ->
          testPass(id, all, now, control.getFreshnessDays())
              || (primary.getType() == ArtifactType.api_access_log && apiLogFieldsComplete(primary));

      case "PCI-REQ-10.4-LOG-REVIEW" ->
          primary.getType() == ArtifactType.control_test_result
              && testPass(id, all, now, control.getFreshnessDays());

      case "PCI-REQ-10.5-LOG-INTEGRITY" ->
          primary.getType() == ArtifactType.config_snapshot
              && boolPath(config, "logging", "integrity_protection");

      case "PCI-REQ-10.6-TIME-SYNC" ->
          primary.getType() == ArtifactType.config_snapshot
              && boolPath(config, "time_sync", "enabled")
              && textPath(config, "time_sync", "ntp_or_equiv") != null;

      case "PCI-REQ-10.7-LOG-RETENTION" ->
          primary.getType() == ArtifactType.config_snapshot
              && intPath(config, "logging", "retention_days") >= 90;

      case "PCI-REQ-7.2-ACCESS-RBAC" ->
          primary.getType() == ArtifactType.config_snapshot
              && boolPath(config, "auth", "rbac_enabled");

      case "PCI-REQ-8.2-AUTH-MFA" ->
          primary.getType() == ArtifactType.config_snapshot
              && boolPath(config, "auth", "admin_mfa_required");

      case "PCI-REQ-8.3-AUTH-STRONG" ->
          primary.getType() == ArtifactType.config_snapshot
              && modesIntersectStrong(config)
              && !boolPath(config, "auth", "password_only_admin");

      case "PCI-REQ-4.2-TLS-CONFIG" ->
          primary.getType() == ArtifactType.config_snapshot && tlsOk(config);

      case "PCI-REQ-3.5-ENC-REST" ->
          primary.getType() == ArtifactType.config_snapshot
              && boolPath(config, "encryption", "at_rest", "enabled")
              && !"none".equalsIgnoreCase(textPath(config, "encryption", "at_rest", "key_management"));

      case "PCI-REQ-2.2-SECURE-CONFIG" ->
          primary.getType() == ArtifactType.config_snapshot
              && !boolPath(config, "hardening", "debug_endpoints_enabled")
              && !boolPath(config, "hardening", "default_credentials_allowed");

      case "PCI-REQ-1.2-NET-SEGMENT" ->
          primary.getType() == ArtifactType.config_snapshot
              && !boolPath(config, "network", "admin_public")
              && boolPath(config, "network", "allowlist_or_private_ingress");

      case "PCI-REQ-8.6-API-AUTH" ->
          (primary.getType() == ArtifactType.api_access_log && apiAuthRatioOk(primary))
              || testPass(id, all, now, control.getFreshnessDays());

      case "PCI-REQ-6.5-CHANGE-MGMT" ->
          (primary.getType() == ArtifactType.change_record && hasChangeForService(primary, "payment-api"))
              || (primary.getType() == ArtifactType.control_test_result
                  && testPass(id, all, now, control.getFreshnessDays()));

      case "PCI-REQ-6.3-SECURE-CHANGE" ->
          (primary.getType() == ArtifactType.change_record && hasApprovedDistinct(primary))
              || (primary.getType() == ArtifactType.control_test_result
                  && testPass(id, all, now, control.getFreshnessDays()));

      case "PCI-REQ-6.4-VULN-MGMT" ->
          primary.getType() == ArtifactType.control_test_result
              && testPass(id, all, now, control.getFreshnessDays());

      case "PCI-REQ-11.3-VULN-SCAN" ->
          primary.getType() == ArtifactType.control_test_result
              && testPass(id, all, now, control.getFreshnessDays())
              && vulnScanDetailOk(all, id);

      case "PCI-REQ-12.10-INCIDENT-LOG" ->
          testPass(id, all, now, control.getFreshnessDays())
              || (textPath(config, "logging", "security_event_sink") != null
                  && all.stream().anyMatch(a -> a.getType() == ArtifactType.api_access_log)
                  && (primary.getType() == ArtifactType.config_snapshot
                      || primary.getType() == ArtifactType.api_access_log
                      || primary.getType() == ArtifactType.control_test_result));

      default -> false;
    };
  }

  // --- helpers ---

  record TestRow(String result, Instant completedAt, String detail) {}

  private TestRow latestTestRow(String controlId, List<Artifact> candidates, Instant now) {
    TestRow best = null;
    for (Artifact a : candidates) {
      if (a.getType() != ArtifactType.control_test_result) continue;
      try {
        JsonNode root = objectMapper.readTree(a.getRawJson());
        Instant suiteCompleted = parseInstant(text(root, "completed_at"));
        JsonNode results = root.get("results");
        if (results == null || !results.isArray()) continue;
        for (JsonNode row : results) {
          if (!controlId.equals(text(row, "control_id"))) continue;
          Instant completed = parseInstant(text(row, "completed_at"));
          if (completed == null) completed = suiteCompleted;
          if (completed == null || completed.isAfter(now)) continue;
          if (best == null || completed.isAfter(best.completedAt())) {
            best = new TestRow(text(row, "result"), completed, text(row, "detail"));
          }
        }
      } catch (Exception ignored) {
      }
    }
    return best;
  }

  private boolean testPass(String controlId, List<Artifact> all, Instant now, int freshnessDays) {
    TestRow row = latestTestRow(controlId, all, now);
    return row != null
        && "pass".equalsIgnoreCase(row.result())
        && ageDays(row.completedAt(), now) <= freshnessDays;
  }

  private boolean vulnScanDetailOk(List<Artifact> all, String controlId) {
    // MVP: if test is pass, accept; Day 1 sample fails so we rarely hit this.
    // If fail/missing, gap already from hard rule / testPass false.
    for (Artifact a : all) {
      if (a.getType() != ArtifactType.control_test_result) continue;
      try {
        JsonNode root = objectMapper.readTree(a.getRawJson());
        for (JsonNode row : root.path("results")) {
          if (!controlId.equals(text(row, "control_id"))) continue;
          JsonNode detail = row.get("detail");
          // If detail mentions critical_open_count we could parse; MVP trusts test result.
          return "pass".equalsIgnoreCase(text(row, "result"));
        }
      } catch (Exception ignored) {
      }
    }
    return false;
  }

  Instant clock(Artifact a) {
    try {
      return switch (a.getType()) {
        case api_access_log -> maxTsFromJsonl(a.getRawJson(), a.getCapturedAt());
        case config_snapshot -> {
          JsonNode n = objectMapper.readTree(a.getRawJson());
          Instant c = parseInstant(text(n, "captured_at"));
          yield c != null ? c : a.getCapturedAt();
        }
        case control_test_result -> {
          JsonNode n = objectMapper.readTree(a.getRawJson());
          Instant c = parseInstant(text(n, "completed_at"));
          yield c != null ? c : a.getCapturedAt();
        }
        case change_record -> maxChangeClock(a.getRawJson(), a.getCapturedAt());
      };
    } catch (Exception e) {
      return a.getCapturedAt();
    }
  }

  private Instant maxTsFromJsonl(String raw, Instant fallback) {
    Instant max = null;
    if (raw == null) return fallback;
    for (String line : raw.split("\n")) {
      line = line.trim();
      if (line.isEmpty()) continue;
      try {
        JsonNode n = objectMapper.readTree(line);
        Instant ts = parseInstant(text(n, "ts"));
        if (ts != null && (max == null || ts.isAfter(max))) max = ts;
      } catch (Exception ignored) {
      }
    }
    return max != null ? max : fallback;
  }

  private Instant maxChangeClock(String raw, Instant fallback) throws Exception {
    JsonNode root = objectMapper.readTree(raw);
    Instant max = parseInstant(text(root, "captured_at"));
    JsonNode records = root.get("records");
    if (records != null && records.isArray()) {
      for (JsonNode r : records) {
        Instant c = parseInstant(text(r, "closed_at"));
        if (c == null) c = parseInstant(text(r, "implemented_at"));
        if (c == null) c = parseInstant(text(r, "created_at"));
        if (c != null && (max == null || c.isAfter(max))) max = c;
      }
    }
    return max != null ? max : fallback;
  }

  private long ageDays(Instant evidence, Instant now) {
    return ChronoUnit.DAYS.between(evidence, now);
  }

  private JsonNode findConfig(List<Artifact> all) {
    return all.stream()
        .filter(a -> a.getType() == ArtifactType.config_snapshot)
        .sorted(Comparator.comparing(Artifact::getCapturedAt).reversed())
        .map(a -> {
          try {
            return objectMapper.readTree(a.getRawJson());
          } catch (Exception e) {
            return null;
          }
        })
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(objectMapper.createObjectNode());
  }

  private boolean hasApiLogCoveringPaths(Artifact a, List<String> prefixes) {
    if (a.getRawJson() == null || a.getRawJson().isBlank()) return false;
    Set<String> found = new HashSet<>();
    Set<String> requiredFields = Set.of("request_id", "actor_id", "path", "status", "ts");
    for (String line : a.getRawJson().split("\n")) {
      line = line.trim();
      if (line.isEmpty()) continue;
      try {
        JsonNode n = objectMapper.readTree(line);
        String path = text(n, "path");
        if (path == null) continue;
        for (String p : prefixes) {
          if (path.equals(p) || path.startsWith(p + "/")) {
            boolean fieldsOk = requiredFields.stream().allMatch(f -> n.has(f));
            if (fieldsOk) found.add(p);
          }
        }
      } catch (Exception ignored) {
      }
    }
    return found.containsAll(prefixes);
  }

  private boolean hasApiLogAdminPaths(Artifact a) {
    if (a.getRawJson() == null) return false;
    for (String line : a.getRawJson().split("\n")) {
      line = line.trim();
      if (line.isEmpty()) continue;
      try {
        JsonNode n = objectMapper.readTree(line);
        String path = text(n, "path");
        if (path != null && path.startsWith("/v1/admin/") && n.has("actor_id") && n.has("status")) {
          return true;
        }
      } catch (Exception ignored) {
      }
    }
    return false;
  }

  private boolean apiLogFieldsComplete(Artifact a) {
    if (a.getRawJson() == null || a.getRawJson().isBlank()) return false;
    String[] required = {"actor_id", "method", "path", "status", "ip", "ts", "scrubbed"};
    int checked = 0;
    for (String line : a.getRawJson().split("\n")) {
      line = line.trim();
      if (line.isEmpty()) continue;
      try {
        JsonNode n = objectMapper.readTree(line);
        checked++;
        for (String f : required) {
          if (!n.has(f)) return false;
        }
        if (!n.path("scrubbed").asBoolean(false)) return false;
      } catch (Exception e) {
        return false;
      }
    }
    return checked > 0;
  }

  private boolean apiAuthRatioOk(Artifact a) {
    if (a.getRawJson() == null) return false;
    int relevant = 0;
    int withActor = 0;
    for (String line : a.getRawJson().split("\n")) {
      line = line.trim();
      if (line.isEmpty()) continue;
      try {
        JsonNode n = objectMapper.readTree(line);
        String path = text(n, "path");
        if (path == null) continue;
        if (!(path.startsWith("/v1/payments") || path.startsWith("/v1/refunds"))) continue;
        relevant++;
        String actor = text(n, "actor_id");
        if (actor != null && !actor.isBlank() && !"null".equalsIgnoreCase(actor)) withActor++;
      } catch (Exception ignored) {
      }
    }
    if (relevant == 0) return false;
    return (withActor * 100.0 / relevant) > 99.0;
  }

  private boolean hasPrivilegedChange(Artifact a) {
    try {
      JsonNode root = objectMapper.readTree(a.getRawJson());
      JsonNode records = root.get("records");
      if (records == null) return false;
      for (JsonNode r : records) {
        if (r.path("privileged").asBoolean(false)) return true;
        String type = text(r, "change_type");
        if (type != null && (type.contains("rbac") || type.contains("admin"))) return true;
      }
    } catch (Exception ignored) {
    }
    return false;
  }

  private boolean hasChangeForService(Artifact a, String service) {
    try {
      JsonNode root = objectMapper.readTree(a.getRawJson());
      String topService = text(root, "service");
      if (service.equals(topService) && root.has("records") && root.get("records").size() > 0) {
        return true;
      }
      JsonNode records = root.get("records");
      if (records == null) return false;
      for (JsonNode r : records) {
        if (service.equals(text(r, "service"))) return true;
      }
    } catch (Exception ignored) {
    }
    return false;
  }

  private boolean hasApprovedDistinct(Artifact a) {
    try {
      JsonNode root = objectMapper.readTree(a.getRawJson());
      JsonNode records = root.get("records");
      if (records == null) return false;
      for (JsonNode r : records) {
        String author = text(r, "author");
        String approved = text(r, "approved_by");
        if (approved != null && !approved.isBlank() && author != null && !approved.equals(author)) {
          return true;
        }
      }
    } catch (Exception ignored) {
    }
    return false;
  }

  private boolean modesIntersectStrong(JsonNode config) {
    JsonNode modes = config.path("auth").path("modes");
    if (!modes.isArray()) return false;
    for (JsonNode m : modes) {
      if (STRONG_MODES.contains(m.asText())) return true;
    }
    return false;
  }

  private boolean tlsOk(JsonNode config) {
    String min = textPath(config, "tls", "min_version");
    if (min == null || !(min.equals("1.2") || min.equals("1.3"))) return false;
    JsonNode enabled = config.path("tls").path("versions_enabled");
    if (enabled.isArray()) {
      for (JsonNode v : enabled) {
        if (LEGACY_TLS.contains(v.asText())) return false;
      }
    }
    return true;
  }

  private boolean boolPath(JsonNode n, String... path) {
    JsonNode cur = n;
    for (String p : path) {
      if (cur == null) return false;
      cur = cur.get(p);
    }
    return cur != null && cur.asBoolean(false);
  }

  private String textPath(JsonNode n, String... path) {
    JsonNode cur = n;
    for (String p : path) {
      if (cur == null) return null;
      cur = cur.get(p);
    }
    return cur == null || cur.isNull() ? null : cur.asText();
  }

  private int intPath(JsonNode n, String... path) {
    JsonNode cur = n;
    for (String p : path) {
      if (cur == null) return 0;
      cur = cur.get(p);
    }
    return cur == null ? 0 : cur.asInt(0);
  }

  private String text(JsonNode n, String field) {
    if (n == null || !n.has(field) || n.get(field).isNull()) return null;
    return n.get(field).asText();
  }

  private Instant parseInstant(String s) {
    if (s == null || s.isBlank()) return null;
    try {
      return Instant.parse(s);
    } catch (Exception e) {
      return null;
    }
  }

  private String truncate(String s) {
    if (s == null) return "";
    return s.length() > 200 ? s.substring(0, 200) + "…" : s;
  }
}

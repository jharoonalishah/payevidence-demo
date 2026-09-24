package com.payevidence.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.payevidence.config.PayEvidenceProperties;
import com.payevidence.domain.*;
import com.payevidence.repo.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeedService {
  public static final String DEMO_TENANT = "NordicPay";
  public static final String PROFILE_DEFAULT = "default";
  public static final String PROFILE_WITH_CHANGE_RECORDS = "withChangeRecords";

  /** Change controls flipped green in withChangeRecords demo; vuln stays red. */
  private static final Set<String> CHANGE_CONTROLS_TO_PASS = Set.of(
      "PCI-REQ-6.5-CHANGE-MGMT",
      "PCI-REQ-6.3-SECURE-CHANGE");

  private static final Logger log = LoggerFactory.getLogger(SeedService.class);

  private final PayEvidenceProperties props;
  private final CatalogueLoader catalogueLoader;
  private final OrganizationRepository orgRepository;
  private final ArtifactRepository artifactRepository;
  private final ControlStatusRepository statusRepository;
  private final EvidencePackRepository packRepository;
  private final ScoringService scoringService;
  private final ObjectMapper objectMapper;

  public SeedService(
      PayEvidenceProperties props,
      CatalogueLoader catalogueLoader,
      OrganizationRepository orgRepository,
      ArtifactRepository artifactRepository,
      ControlStatusRepository statusRepository,
      EvidencePackRepository packRepository,
      ScoringService scoringService,
      ObjectMapper objectMapper) {
    this.props = props;
    this.catalogueLoader = catalogueLoader;
    this.orgRepository = orgRepository;
    this.artifactRepository = artifactRepository;
    this.statusRepository = statusRepository;
    this.packRepository = packRepository;
    this.scoringService = scoringService;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public Map<String, Object> seedDemo() {
    return seedDemo(PROFILE_DEFAULT);
  }

  /**
   * Idempotent NordicPay seed.
   *
   * <ul>
   *   <li>{@code default} — Day 1 samples only → pass=16 gap=4 (change + vuln reds)</li>
   *   <li>{@code withChangeRecords} — also ingests change_record.json and rewrites the
   *       failing 6.5/6.3 test rows to pass so scoring can go green for those two while
   *       6.4/11.3 stay gap. Honors SCORING.md hard rule (fail test → gap) by adjusting
   *       demo data, not the rule.</li>
   * </ul>
   */
  @Transactional
  public Map<String, Object> seedDemo(String demoProfile) {
    String profile = normalizeProfile(demoProfile);
    Path catalogue = resolve(props.getCataloguePath());
    catalogueLoader.loadFromYaml(catalogue);

    Organization org = orgRepository.findByName(DEMO_TENANT)
        .orElseGet(() -> orgRepository.save(new Organization(DEMO_TENANT)));

    // Idempotent reset of demo artifacts/statuses/packs for NordicPay
    packRepository.deleteByOrganization(org);
    statusRepository.deleteByOrganization(org);
    artifactRepository.deleteByOrganization(org);
    artifactRepository.flush();

    Path samples = resolve(props.getSamplesDir());
    ingestFile(org, samples.resolve("api_access_log.jsonl"), ArtifactType.api_access_log);
    ingestFile(org, samples.resolve("config_snapshot.json"), ArtifactType.config_snapshot);

    int artifactsIngested = 3;
    if (PROFILE_WITH_CHANGE_RECORDS.equals(profile)) {
      // (b) Flip 6.5/6.3 test rows to pass; leave 6.4/11.3 fail
      String adjusted = adjustTestsForChangeDemo(samples.resolve("control_test_result.json"));
      ingestInline(org, ArtifactType.control_test_result, adjusted,
          samples.resolve("control_test_result.json").toAbsolutePath().normalize()
              + "#withChangeRecords");
      // (a) Ingest change records
      Path changePath = resolve(props.getChangeRecordPath());
      ingestFile(org, changePath, ArtifactType.change_record);
      artifactsIngested = 4;
    } else {
      ingestFile(org, samples.resolve("control_test_result.json"), ArtifactType.control_test_result);
    }

    List<ControlStatus> statuses = scoringService.recomputeAll(org);

    Map<String, Long> counts = countStatuses(statuses);
    Map<String, Object> result = new HashMap<>();
    result.put("organizationId", org.getId().toString());
    result.put("organizationName", org.getName());
    result.put("demoProfile", profile);
    result.put("controls", statuses.size());
    result.put("pass", counts.get("pass"));
    result.put("gap", counts.get("gap"));
    result.put("stale", counts.get("stale"));
    result.put("artifactsIngested", artifactsIngested);
    if (PROFILE_WITH_CHANGE_RECORDS.equals(profile)) {
      result.put("note",
          "withChangeRecords: change_record ingested + 6.5/6.3 test rows set to pass; "
              + "6.4/11.3 remain fail→gap (SCORING.md hard rule honored via demo data)");
      result.put("expectedGaps", List.of("PCI-REQ-6.4-VULN-MGMT", "PCI-REQ-11.3-VULN-SCAN"));
    } else {
      result.put("note",
          "default: Day 1 samples; gaps = change (6.5/6.3) + vuln (6.4/11.3). "
              + "Use demoProfile=withChangeRecords to demo change fixes.");
      result.put("expectedGaps", List.of(
          "PCI-REQ-6.5-CHANGE-MGMT",
          "PCI-REQ-6.3-SECURE-CHANGE",
          "PCI-REQ-6.4-VULN-MGMT",
          "PCI-REQ-11.3-VULN-SCAN"));
    }
    log.info("Demo seed complete for {} profile={}: {}", DEMO_TENANT, profile, counts);
    return result;
  }

  /**
   * Ingest change_record only (does NOT flip failing tests). Teaching endpoint:
   * after this, 6.5/6.3 stay gap because of SCORING.md hard rule.
   */
  @Transactional
  public Map<String, Object> ingestChangeRecords(Organization org) {
    Path path = resolve(props.getChangeRecordPath());
    Artifact a = ingestFile(org, path, ArtifactType.change_record);
    List<ControlStatus> statuses = scoringService.recomputeAll(org);
    Map<String, Long> counts = countStatuses(statuses);
    Map<String, Object> result = new HashMap<>();
    result.put("artifactId", a.getId().toString());
    result.put("demoProfile", "changeRecordOnly");
    result.put("note",
        "Failing control_test_result still forces gap for 6.5/6.3 per SCORING.md hard rule. "
            + "Use POST /api/demo/seed?demoProfile=withChangeRecords to demo greens for those two.");
    result.put("pass", counts.get("pass"));
    result.put("gap", counts.get("gap"));
    result.put("stale", counts.get("stale"));
    return result;
  }

  @Transactional
  public Artifact ingestMetadata(
      Organization org,
      ArtifactType type,
      Instant capturedAt,
      String storagePath,
      String inlineJson) {
    if (type == null) {
      throw new IllegalArgumentException(
          "type is required. Allowed: " + allowedTypesCsv());
    }
    Artifact a = new Artifact();
    a.setOrganization(org);
    a.setType(type);
    a.setCapturedAt(capturedAt != null ? capturedAt : Instant.now());
    a.setStoragePath(storagePath);
    if (inlineJson != null && !inlineJson.isBlank()) {
      a.setRawJson(inlineJson);
    } else if (storagePath != null) {
      try {
        a.setRawJson(Files.readString(resolve(storagePath)));
        if (capturedAt == null) {
          Instant inferred = inferCapturedAt(type, a.getRawJson());
          if (inferred != null) a.setCapturedAt(inferred);
        }
      } catch (Exception e) {
        throw new IllegalArgumentException("Cannot read storagePath: " + storagePath, e);
      }
    }
    return artifactRepository.save(a);
  }

  /** Parse artifact type string; reject unknown with a clear 400 message. */
  public static ArtifactType requireArtifactType(String type) {
    if (type == null || type.isBlank()) {
      throw new IllegalArgumentException("type is required. Allowed: " + allowedTypesCsv());
    }
    try {
      return ArtifactType.valueOf(type.trim());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid artifact type '" + type + "'. Allowed: " + allowedTypesCsv());
    }
  }

  public static String allowedTypesCsv() {
    StringBuilder sb = new StringBuilder();
    for (ArtifactType t : ArtifactType.values()) {
      if (sb.length() > 0) sb.append(", ");
      sb.append(t.name());
    }
    return sb.toString();
  }

  public static String normalizeProfile(String demoProfile) {
    if (demoProfile == null || demoProfile.isBlank()) {
      return PROFILE_DEFAULT;
    }
    String p = demoProfile.trim();
    if (PROFILE_DEFAULT.equalsIgnoreCase(p) || "baseline".equalsIgnoreCase(p)) {
      return PROFILE_DEFAULT;
    }
    if (PROFILE_WITH_CHANGE_RECORDS.equalsIgnoreCase(p)
        || "with_change_records".equalsIgnoreCase(p)
        || "change".equalsIgnoreCase(p)) {
      return PROFILE_WITH_CHANGE_RECORDS;
    }
    throw new IllegalArgumentException(
        "Unknown demoProfile '" + demoProfile + "'. Allowed: default, withChangeRecords");
  }

  /**
   * Rewrite Day 1 control_test_result so 6.5/6.3 are pass (after change records exist),
   * while vuln controls 6.4/11.3 remain fail.
   */
  private String adjustTestsForChangeDemo(Path originalPath) {
    try {
      JsonNode root = objectMapper.readTree(Files.readString(originalPath));
      ObjectNode copy = root.deepCopy();
      ArrayNode results = (ArrayNode) copy.get("results");
      int passCount = 0;
      int failCount = 0;
      Instant completed = Instant.parse("2026-09-23T18:30:00Z");
      for (int i = 0; i < results.size(); i++) {
        ObjectNode row = (ObjectNode) results.get(i);
        String cid = row.path("control_id").asText();
        if (CHANGE_CONTROLS_TO_PASS.contains(cid)) {
          row.put("result", "pass");
          row.put("detail",
              "Demo withChangeRecords: change_record.json present with approved payment-api changes; "
                  + "test re-run after ingest");
          row.put("completed_at", completed.toString());
          ArrayNode refs = objectMapper.createArrayNode();
          refs.add("day2-3/samples/change_record.json");
          row.set("evidence_refs", refs);
        }
        if ("pass".equalsIgnoreCase(row.path("result").asText())) {
          passCount++;
        } else {
          failCount++;
        }
      }
      copy.put("completed_at", completed.toString());
      copy.put("suite", "pci-mvp-v0-smoke-withChangeRecords");
      ObjectNode summary = copy.has("summary") && copy.get("summary").isObject()
          ? (ObjectNode) copy.get("summary")
          : copy.putObject("summary");
      summary.put("total", results.size());
      summary.put("pass", passCount);
      summary.put("fail", failCount);
      copy.put("disclaimer",
          "Synthetic withChangeRecords demo: 6.5/6.3 flipped to pass after change_record ingest; "
              + "vuln 6.4/11.3 remain fail. Not a QSA assessment.");
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(copy);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to adjust control_test_result for withChangeRecords", e);
    }
  }

  private Artifact ingestInline(Organization org, ArtifactType type, String raw, String storagePath) {
    Instant captured = inferCapturedAt(type, raw);
    if (captured == null) captured = Instant.parse("2026-09-23T18:30:00Z");
    Artifact a = new Artifact();
    a.setOrganization(org);
    a.setType(type);
    a.setCapturedAt(captured);
    a.setStoragePath(storagePath);
    a.setRawJson(raw);
    return artifactRepository.save(a);
  }

  private Artifact ingestFile(Organization org, Path path, ArtifactType type) {
    try {
      String raw = Files.readString(path);
      Instant captured = inferCapturedAt(type, raw);
      if (captured == null) captured = Instant.parse("2026-09-23T12:00:00Z");
      Artifact a = new Artifact();
      a.setOrganization(org);
      a.setType(type);
      a.setCapturedAt(captured);
      a.setStoragePath(path.toAbsolutePath().normalize().toString());
      a.setRawJson(raw);
      return artifactRepository.save(a);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to ingest " + path, e);
    }
  }

  private Instant inferCapturedAt(ArtifactType type, String raw) {
    try {
      return switch (type) {
        case api_access_log -> {
          Instant max = null;
          for (String line : raw.split("\n")) {
            if (line.isBlank()) continue;
            JsonNode n = objectMapper.readTree(line);
            if (n.has("ts")) {
              Instant ts = Instant.parse(n.get("ts").asText());
              if (max == null || ts.isAfter(max)) max = ts;
            }
          }
          yield max;
        }
        case config_snapshot, change_record -> {
          JsonNode n = objectMapper.readTree(raw);
          yield n.has("captured_at") ? Instant.parse(n.get("captured_at").asText()) : null;
        }
        case control_test_result -> {
          JsonNode n = objectMapper.readTree(raw);
          yield n.has("completed_at") ? Instant.parse(n.get("completed_at").asText()) : null;
        }
      };
    } catch (Exception e) {
      return null;
    }
  }

  private Path resolve(String p) {
    Path path = Path.of(p);
    if (path.isAbsolute() && Files.exists(path)) return path;
    Path cwd = Path.of("").toAbsolutePath();
    Path a = cwd.resolve(p).normalize();
    if (Files.exists(a)) return a;
    Path b = cwd.resolve("..").resolve(p.replaceFirst("^\\.\\./", "")).normalize();
    if (Files.exists(b)) return b;
    Path ws = Path.of("/workspace/haroon-estonia/payevidence").resolve(
        p.startsWith("../") ? p.substring(3) : p).normalize();
    if (Files.exists(ws)) return ws;
    return a;
  }

  private Map<String, Long> countStatuses(List<ControlStatus> statuses) {
    Map<String, Long> m = new HashMap<>();
    m.put("pass", statuses.stream().filter(s -> s.getStatus() == StatusValue.pass).count());
    m.put("gap", statuses.stream().filter(s -> s.getStatus() == StatusValue.gap).count());
    m.put("stale", statuses.stream().filter(s -> s.getStatus() == StatusValue.stale).count());
    return m;
  }
}

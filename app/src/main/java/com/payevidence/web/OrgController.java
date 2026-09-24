package com.payevidence.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payevidence.domain.*;
import com.payevidence.repo.*;
import com.payevidence.service.EvidencePackService;
import com.payevidence.service.ScoringService;
import com.payevidence.service.SeedService;
import com.payevidence.web.dto.ArtifactIngestRequest;
import com.payevidence.web.error.NotFoundException;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orgs/{orgId}")
public class OrgController {
  private final OrganizationRepository orgRepository;
  private final ControlStatusRepository statusRepository;
  private final ArtifactRepository artifactRepository;
  private final ControlRepository controlRepository;
  private final ScoringService scoringService;
  private final SeedService seedService;
  private final EvidencePackService packService;
  private final EvidencePackRepository packRepository;
  private final ObjectMapper objectMapper;

  public OrgController(
      OrganizationRepository orgRepository,
      ControlStatusRepository statusRepository,
      ArtifactRepository artifactRepository,
      ControlRepository controlRepository,
      ScoringService scoringService,
      SeedService seedService,
      EvidencePackService packService,
      EvidencePackRepository packRepository,
      ObjectMapper objectMapper) {
    this.orgRepository = orgRepository;
    this.statusRepository = statusRepository;
    this.artifactRepository = artifactRepository;
    this.controlRepository = controlRepository;
    this.scoringService = scoringService;
    this.seedService = seedService;
    this.packService = packService;
    this.packRepository = packRepository;
    this.objectMapper = objectMapper;
  }

  @GetMapping("/controls")
  public Map<String, Object> listControls(
      @PathVariable UUID orgId,
      @RequestParam(required = false) String status) {
    Organization org = requireOrg(orgId);
    List<ControlStatus> statuses = statusRepository.findByOrganization(org);

    StatusValue filter = null;
    if (status != null && !status.isBlank()) {
      try {
        filter = StatusValue.valueOf(status.trim().toLowerCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "Invalid status '" + status + "'. Allowed: pass, gap, stale");
      }
    }

    final StatusValue statusFilter = filter;
    List<ControlStatus> filtered = statusFilter == null
        ? statuses
        : statuses.stream().filter(s -> s.getStatus() == statusFilter).toList();

    List<Map<String, Object>> items = filtered.stream().map(this::statusSummary).toList();
    long pass = statuses.stream().filter(s -> s.getStatus() == StatusValue.pass).count();
    long gap = statuses.stream().filter(s -> s.getStatus() == StatusValue.gap).count();
    long stale = statuses.stream().filter(s -> s.getStatus() == StatusValue.stale).count();
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("organizationId", org.getId().toString());
    body.put("organizationName", org.getName());
    body.put("pass", pass);
    body.put("gap", gap);
    body.put("stale", stale);
    if (statusFilter != null) {
      body.put("filter", statusFilter.name());
      body.put("filteredCount", items.size());
    }
    body.put("controls", items);
    return body;
  }

  @GetMapping("/controls/{controlId}")
  public Map<String, Object> controlDetail(@PathVariable UUID orgId, @PathVariable String controlId) {
    Organization org = requireOrg(orgId);
    Control control = controlRepository.findById(controlId)
        .orElseThrow(() -> new NotFoundException("Unknown control: " + controlId));
    ControlStatus status = statusRepository.findByOrganizationAndControl(org, control)
        .orElseThrow(() -> new NotFoundException(
            "No status for control " + controlId + "; POST /api/orgs/{id}/score first"));
    Set<ArtifactType> allowed = new HashSet<>(control.getEvidenceTypes());
    List<Map<String, Object>> contributing = artifactRepository.findByOrganization(org).stream()
        .filter(a -> allowed.contains(a.getType()))
        .map(a -> {
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("id", a.getId().toString());
          m.put("type", a.getType().name());
          m.put("capturedAt", a.getCapturedAt().toString());
          m.put("storagePath", a.getStoragePath());
          return m;
        })
        .collect(Collectors.toList());
    Map<String, Object> body = statusSummary(status);
    body.put("description", control.getDescription());
    body.put("pciRef", control.getPciRef());
    body.put("passRule", control.getPassRule());
    body.put("evidenceTypes", control.getEvidenceTypes().stream().map(Enum::name).toList());
    body.put("freshnessDays", control.getFreshnessDays());
    body.put("contributingArtifacts", contributing);
    return body;
  }

  @PostMapping("/artifacts")
  public Map<String, Object> ingestArtifact(
      @PathVariable UUID orgId, @RequestBody ArtifactIngestRequest req) {
    Organization org = requireOrg(orgId);
    ArtifactType type = SeedService.requireArtifactType(req.getType());
    String inline = req.getRawJson();
    if (inline == null && req.getPayload() != null) {
      try {
        inline = objectMapper.writeValueAsString(req.getPayload());
      } catch (Exception e) {
        throw new IllegalArgumentException("payload not serializable");
      }
    }
    Artifact a = seedService.ingestMetadata(
        org, type, req.getCapturedAt(), req.getStoragePath(), inline);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("id", a.getId().toString());
    body.put("type", a.getType().name());
    body.put("capturedAt", a.getCapturedAt().toString());
    body.put("storagePath", a.getStoragePath());
    return body;
  }

  @PostMapping("/score")
  public Map<String, Object> score(@PathVariable UUID orgId) {
    Organization org = requireOrg(orgId);
    List<ControlStatus> statuses = scoringService.recomputeAll(org);
    long pass = statuses.stream().filter(s -> s.getStatus() == StatusValue.pass).count();
    long gap = statuses.stream().filter(s -> s.getStatus() == StatusValue.gap).count();
    long stale = statuses.stream().filter(s -> s.getStatus() == StatusValue.stale).count();
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("organizationId", org.getId().toString());
    body.put("pass", pass);
    body.put("gap", gap);
    body.put("stale", stale);
    body.put("controls", statuses.stream().map(this::statusSummary).toList());
    return body;
  }


  @GetMapping("/evidence-packs")
  public Map<String, Object> listPacks(@PathVariable UUID orgId) {
    Organization org = requireOrg(orgId);
    List<EvidencePack> packs = packRepository.findByOrganizationOrderByCreatedAtDesc(org);
    List<Map<String, Object>> items = packs.stream().map(p -> packBody(p, false)).toList();
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("organizationId", org.getId().toString());
    body.put("organizationName", org.getName());
    body.put("count", items.size());
    body.put("packs", items);
    return body;
  }

  @PostMapping("/evidence-packs")
  public Map<String, Object> createPack(@PathVariable UUID orgId) {
    Organization org = requireOrg(orgId);
    EvidencePack pack = packService.createPack(org);
    return packBody(pack, false);
  }

  @GetMapping("/evidence-packs/{packId}")
  public ResponseEntity<?> getPack(
      @PathVariable UUID orgId,
      @PathVariable UUID packId,
      @RequestParam(defaultValue = "json") String format) {
    Organization org = requireOrg(orgId);
    EvidencePack pack = requirePackForOrg(org, packId);
    if ("html".equalsIgnoreCase(format)) {
      return ResponseEntity.ok()
          .contentType(MediaType.TEXT_HTML)
          .body(pack.getHtmlContent());
    }
    if ("pdf".equalsIgnoreCase(format)) {
      byte[] pdf = packService.getPdfBytes(pack);
      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_PDF)
          .header(HttpHeaders.CONTENT_DISPOSITION,
              "inline; filename=\"payevidence-" + org.getName() + "-" + packId + ".pdf\"")
          .body(pdf);
    }
    return ResponseEntity.ok(packBody(pack, true));
  }

  /** Download HTML or PDF as attachment. */
  @GetMapping("/evidence-packs/{packId}/download")
  public ResponseEntity<byte[]> downloadPack(
      @PathVariable UUID orgId,
      @PathVariable UUID packId,
      @RequestParam(defaultValue = "html") String format) {
    Organization org = requireOrg(orgId);
    EvidencePack pack = requirePackForOrg(org, packId);
    String base = "payevidence-" + org.getName() + "-" + packId;
    if ("pdf".equalsIgnoreCase(format)) {
      byte[] pdf = packService.getPdfBytes(pack);
      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_PDF)
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + base + ".pdf\"")
          .body(pdf);
    }
    if (!"html".equalsIgnoreCase(format)) {
      throw new IllegalArgumentException("Invalid download format '" + format + "'. Allowed: html, pdf");
    }
    byte[] html = pack.getHtmlContent() != null
        ? pack.getHtmlContent().getBytes(java.nio.charset.StandardCharsets.UTF_8)
        : new byte[0];
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_HTML)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + base + ".html\"")
        .body(html);
  }

  private EvidencePack requirePackForOrg(Organization org, UUID packId) {
    EvidencePack pack = packService.getPack(packId);
    if (!pack.getOrganization().getId().equals(org.getId())) {
      throw new NotFoundException("Evidence pack not found for org: " + packId);
    }
    return pack;
  }

  private Map<String, Object> packBody(EvidencePack pack, boolean includeSnapshot) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("id", pack.getId().toString());
    body.put("organizationId", pack.getOrganization().getId().toString());
    body.put("organizationName", pack.getOrganization().getName());
    body.put("createdAt", pack.getCreatedAt().toString());
    body.put("storagePath", pack.getStoragePath());
    body.put("pdfStoragePath", pack.getPdfStoragePath());
    body.put("htmlAvailable", pack.getHtmlContent() != null);
    body.put("pdfAvailable", pack.getPdfStoragePath() != null);
    if (includeSnapshot) {
      try {
        body.put("snapshot", objectMapper.readTree(pack.getStatusesJson()));
      } catch (Exception e) {
        body.put("statusesJson", pack.getStatusesJson());
      }
      body.put("html", pack.getHtmlContent());
    }
    return body;
  }

  private Map<String, Object> statusSummary(ControlStatus cs) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("controlId", cs.getControl().getId());
    m.put("title", cs.getControl().getTitle());
    m.put("status", cs.getStatus().name());
    m.put("reason", cs.getReason());
    m.put("computedAt", cs.getComputedAt().toString());
    m.put("freshnessDays", cs.getControl().getFreshnessDays());
    return m;
  }

  private Organization requireOrg(UUID orgId) {
    return orgRepository.findById(orgId)
        .orElseThrow(() -> new NotFoundException("Organization not found: " + orgId));
  }
}

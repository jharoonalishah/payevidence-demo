package com.payevidence.web.ui;

import com.payevidence.config.DemoTokenFilter;
import com.payevidence.config.PayEvidenceProperties;
import com.payevidence.domain.ArtifactType;
import com.payevidence.domain.Control;
import com.payevidence.domain.ControlStatus;
import com.payevidence.domain.EvidencePack;
import com.payevidence.domain.Organization;
import com.payevidence.domain.StatusValue;
import com.payevidence.repo.ArtifactRepository;
import com.payevidence.repo.ControlRepository;
import com.payevidence.repo.ControlStatusRepository;
import com.payevidence.repo.EvidencePackRepository;
import com.payevidence.repo.OrganizationRepository;
import com.payevidence.service.EvidencePackService;
import com.payevidence.service.SeedService;
import com.payevidence.web.error.NotFoundException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DemoUiController {
  private final OrganizationRepository orgRepository;
  private final ControlStatusRepository statusRepository;
  private final ControlRepository controlRepository;
  private final ArtifactRepository artifactRepository;
  private final EvidencePackRepository packRepository;
  private final SeedService seedService;
  private final EvidencePackService packService;
  private final PayEvidenceProperties properties;

  public DemoUiController(
      OrganizationRepository orgRepository,
      ControlStatusRepository statusRepository,
      ControlRepository controlRepository,
      ArtifactRepository artifactRepository,
      EvidencePackRepository packRepository,
      SeedService seedService,
      EvidencePackService packService,
      PayEvidenceProperties properties) {
    this.orgRepository = orgRepository;
    this.statusRepository = statusRepository;
    this.controlRepository = controlRepository;
    this.artifactRepository = artifactRepository;
    this.packRepository = packRepository;
    this.seedService = seedService;
    this.packService = packService;
    this.properties = properties;
  }

  @GetMapping("/")
  public String root() {
    return "redirect:/demo";
  }

  @GetMapping("/demo/gate")
  public String gate(
      @RequestParam(required = false) String next,
      @RequestParam(required = false) String error,
      Model model) {
    model.addAttribute("next", next != null ? next : "/demo");
    model.addAttribute("error", error);
    model.addAttribute("hint", "Default token is nordic-demo (override PAYEVIDENCE_DEMO_TOKEN).");
    return "gate";
  }

  @PostMapping("/demo/gate")
  public String gateSubmit(
      @RequestParam String token,
      @RequestParam(defaultValue = "/demo") String next,
      HttpServletResponse response,
      RedirectAttributes redirect) {
    String expected = properties.getDemoToken();
    if (expected != null && expected.equals(token)) {
      Cookie cookie = new Cookie(DemoTokenFilter.COOKIE_NAME, expected);
      cookie.setPath("/");
      cookie.setHttpOnly(true);
      cookie.setMaxAge(60 * 60 * 12);
      response.addCookie(cookie);
      String dest = (next == null || next.isBlank() || !next.startsWith("/")) ? "/demo" : next;
      return "redirect:" + dest;
    }
    redirect.addAttribute("error", "invalid");
    redirect.addAttribute("next", next);
    return "redirect:/demo/gate";
  }

  @PostMapping("/demo/logout")
  public String logout(HttpServletResponse response) {
    Cookie cookie = new Cookie(DemoTokenFilter.COOKIE_NAME, "");
    cookie.setPath("/");
    cookie.setMaxAge(0);
    response.addCookie(cookie);
    return "redirect:/demo/gate";
  }

  @GetMapping("/demo")
  public String dashboard(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String packId,
      Model model) {
    Organization org = requireNordicPay();
    List<ControlStatus> statuses = statusRepository.findByOrganization(org);

    StatusValue filter = null;
    if (status != null && !status.isBlank()) {
      try {
        filter = StatusValue.valueOf(status.trim().toLowerCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
        filter = null;
      }
    }
    final StatusValue statusFilter = filter;
    List<ControlStatus> filtered = statusFilter == null
        ? statuses
        : statuses.stream().filter(s -> s.getStatus() == statusFilter).toList();

    long pass = statuses.stream().filter(s -> s.getStatus() == StatusValue.pass).count();
    long gap = statuses.stream().filter(s -> s.getStatus() == StatusValue.gap).count();
    long stale = statuses.stream().filter(s -> s.getStatus() == StatusValue.stale).count();

    List<EvidencePack> packs = packRepository.findByOrganizationOrderByCreatedAtDesc(org);
    if (packs.size() > 8) {
      packs = packs.subList(0, 8);
    }

    model.addAttribute("org", org);
    model.addAttribute("pass", pass);
    model.addAttribute("gap", gap);
    model.addAttribute("stale", stale);
    model.addAttribute("filter", statusFilter != null ? statusFilter.name() : "");
    model.addAttribute("controls", filtered);
    model.addAttribute("packs", packs);
    model.addAttribute("highlightPackId", packId);
    return "dashboard";
  }

  @GetMapping("/demo/controls/{controlId}")
  public String controlDetail(@PathVariable String controlId, Model model) {
    Organization org = requireNordicPay();
    Control control = controlRepository.findById(controlId)
        .orElseThrow(() -> new NotFoundException("Unknown control: " + controlId));
    ControlStatus status = statusRepository.findByOrganizationAndControl(org, control)
        .orElseThrow(() -> new NotFoundException("No status for control " + controlId));
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

    model.addAttribute("org", org);
    model.addAttribute("control", control);
    model.addAttribute("status", status);
    model.addAttribute("contributing", contributing);
    return "control-detail";
  }

  @PostMapping("/demo/actions/seed")
  public String seed(
      @RequestParam(defaultValue = SeedService.PROFILE_DEFAULT) String demoProfile,
      RedirectAttributes redirect) {
    Map<String, Object> result = seedService.seedDemo(demoProfile);
    redirect.addFlashAttribute(
        "flash",
        "Seeded " + result.get("demoProfile")
            + " — pass=" + result.get("pass")
            + " gap=" + result.get("gap")
            + " stale=" + result.get("stale"));
    return "redirect:/demo";
  }

  @PostMapping("/demo/actions/ingest-change-records")
  public String ingestChangeRecords(RedirectAttributes redirect) {
    Organization org = requireNordicPay();
    Map<String, Object> result = seedService.ingestChangeRecords(org);
    redirect.addFlashAttribute(
        "flash",
        "Ingested change records only — pass=" + result.get("pass")
            + " gap=" + result.get("gap")
            + ". Note: " + result.get("note"));
    return "redirect:/demo";
  }

  @PostMapping("/demo/actions/generate-pack")
  public String generatePack(RedirectAttributes redirect) {
    Organization org = requireNordicPay();
    EvidencePack pack = packService.createPack(org);
    redirect.addFlashAttribute(
        "flash",
        "Evidence pack created: " + pack.getId()
            + " (view HTML / download HTML / download PDF below)");
    redirect.addAttribute("packId", pack.getId().toString());
    return "redirect:/demo";
  }

  private Organization requireNordicPay() {
    return orgRepository.findByName(SeedService.DEMO_TENANT)
        .orElseThrow(() -> new NotFoundException(
            "NordicPay not seeded; use Reset demo or POST /api/demo/seed"));
  }
}

package com.payevidence.web;

import com.payevidence.domain.Organization;
import com.payevidence.repo.OrganizationRepository;
import com.payevidence.service.SeedService;
import com.payevidence.web.error.NotFoundException;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
public class DemoController {
  private final SeedService seedService;
  private final OrganizationRepository orgRepository;

  public DemoController(SeedService seedService, OrganizationRepository orgRepository) {
    this.seedService = seedService;
    this.orgRepository = orgRepository;
  }

  /**
   * Idempotent NordicPay seed.
   *
   * @param demoProfile {@code default} (16 pass / 4 gap) or {@code withChangeRecords}
   *                    (change gaps cleared; vuln gaps remain)
   */
  @PostMapping("/seed")
  public Map<String, Object> seed(
      @RequestParam(defaultValue = SeedService.PROFILE_DEFAULT) String demoProfile) {
    return seedService.seedDemo(demoProfile);
  }

  /**
   * Ingest change_record.json only — does NOT flip failing tests.
   * Teaching path: shows SCORING.md hard rule still keeps 6.5/6.3 as gap.
   */
  @PostMapping("/ingest-change-records")
  public Map<String, Object> ingestChangeRecords() {
    Organization org = orgRepository.findByName(SeedService.DEMO_TENANT)
        .orElseThrow(() -> new NotFoundException(
            "NordicPay not seeded; POST /api/demo/seed first"));
    return seedService.ingestChangeRecords(org);
  }
}

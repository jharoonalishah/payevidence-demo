package com.payevidence.web;

import com.payevidence.domain.Organization;
import com.payevidence.repo.OrganizationRepository;
import com.payevidence.web.error.NotFoundException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orgs")
public class OrgListController {
  private final OrganizationRepository orgRepository;

  public OrgListController(OrganizationRepository orgRepository) {
    this.orgRepository = orgRepository;
  }

  @GetMapping
  public List<Map<String, Object>> list() {
    return orgRepository.findAll().stream().map(this::toBody).toList();
  }

  @GetMapping("/by-name/{name}")
  public Map<String, Object> byName(@PathVariable String name) {
    Organization org = orgRepository.findByName(name)
        .orElseThrow(() -> new NotFoundException("Organization not found: " + name));
    return toBody(org);
  }

  private Map<String, Object> toBody(Organization o) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", o.getId().toString());
    m.put("name", o.getName());
    m.put("createdAt", o.getCreatedAt().toString());
    return m;
  }
}

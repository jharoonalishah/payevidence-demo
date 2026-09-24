package com.payevidence;

import static org.assertj.core.api.Assertions.assertThat;

import com.payevidence.service.SeedService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PayEvidenceApplicationTests {

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    Path root = Path.of("/workspace/haroon-estonia/payevidence").toAbsolutePath();
    r.add("payevidence.catalogue-path", () -> root.resolve("day1/controls/pci-mvp-catalogue.yaml").toString());
    r.add("payevidence.samples-dir", () -> root.resolve("day1/samples").toString());
    r.add("payevidence.change-record-path", () -> root.resolve("day2-3/samples/change_record.json").toString());
    r.add("payevidence.seed-on-startup", () -> "true");
    r.add("payevidence.packs-dir", () -> root.resolve("app/packs-test").toString());
    r.add("spring.datasource.url", () -> "jdbc:h2:mem:payevidence_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
    r.add("payevidence.demo-token", () -> "nordic-demo");
  }

  @Autowired SeedService seedService;
  @Autowired TestRestTemplate rest;

  @Test
  void defaultSeedGivesSixteenPassFourGap() {
    Map<String, Object> seed = seedService.seedDemo(SeedService.PROFILE_DEFAULT);
    assertThat(seed.get("organizationName")).isEqualTo("NordicPay");
    assertThat(seed.get("demoProfile")).isEqualTo("default");
    assertThat(((Number) seed.get("controls")).intValue()).isEqualTo(20);
    assertThat(((Number) seed.get("gap")).longValue()).isEqualTo(4L);
    assertThat(((Number) seed.get("pass")).longValue()).isEqualTo(16L);
  }

  @Test
  void withChangeRecordsClearsChangeGapsLeavesVuln() {
    Map<String, Object> seed = seedService.seedDemo(SeedService.PROFILE_WITH_CHANGE_RECORDS);
    assertThat(seed.get("demoProfile")).isEqualTo("withChangeRecords");
    assertThat(((Number) seed.get("pass")).longValue()).isEqualTo(18L);
    assertThat(((Number) seed.get("gap")).longValue()).isEqualTo(2L);
    assertThat(((Number) seed.get("stale")).longValue()).isEqualTo(0L);

    String orgId = (String) seed.get("organizationId");
    ResponseEntity<Map> gaps = rest.getForEntity(
        "/api/orgs/" + orgId + "/controls?status=gap", Map.class);
    assertThat(gaps.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(gaps.getBody().get("filter")).isEqualTo("gap");
    assertThat(gaps.getBody().get("filteredCount")).isEqualTo(2);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> controls = (List<Map<String, Object>>) gaps.getBody().get("controls");
    List<String> ids = controls.stream().map(c -> (String) c.get("controlId")).toList();
    assertThat(ids).containsExactlyInAnyOrder(
        "PCI-REQ-6.4-VULN-MGMT", "PCI-REQ-11.3-VULN-SCAN");
  }

  @Test
  void gapFilterAndPackHtmlContainNordicPayAndCounts() {
    Map<String, Object> seed = seedService.seedDemo();
    String orgId = (String) seed.get("organizationId");

    ResponseEntity<Map> gaps = rest.getForEntity(
        "/api/orgs/" + orgId + "/controls?status=gap", Map.class);
    assertThat(gaps.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(gaps.getBody().get("gap")).isEqualTo(4);
    assertThat(gaps.getBody().get("filteredCount")).isEqualTo(4);

    ResponseEntity<Map> packResp = rest.postForEntity(
        "/api/orgs/" + orgId + "/evidence-packs", null, Map.class);
    assertThat(packResp.getStatusCode().is2xxSuccessful()).isTrue();
    String packId = (String) packResp.getBody().get("id");
    assertThat(packResp.getBody().get("htmlAvailable")).isEqualTo(true);
    assertThat(packResp.getBody().get("pdfAvailable")).isEqualTo(true);

    ResponseEntity<String> html = rest.getForEntity(
        "/api/orgs/" + orgId + "/evidence-packs/" + packId + "?format=html", String.class);
    assertThat(html.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(html.getBody()).contains("NordicPay");
    assertThat(html.getBody()).contains("pass: 16");
    assertThat(html.getBody()).contains("gap: 4");
    assertThat(html.getBody()).contains("PayEvidence Evidence Pack");

    ResponseEntity<byte[]> pdfDl = rest.getForEntity(
        "/api/orgs/" + orgId + "/evidence-packs/" + packId + "/download?format=pdf", byte[].class);
    assertThat(pdfDl.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(pdfDl.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
    assertThat(pdfDl.getBody()).isNotNull();
    assertThat(pdfDl.getBody().length).isGreaterThan(100);
    // PDF magic
    assertThat(new String(pdfDl.getBody(), 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");

    ResponseEntity<byte[]> htmlDl = rest.getForEntity(
        "/api/orgs/" + orgId + "/evidence-packs/" + packId + "/download?format=html", byte[].class);
    assertThat(htmlDl.getStatusCode()).isEqualTo(HttpStatus.OK);
    String htmlBody = new String(htmlDl.getBody(), StandardCharsets.UTF_8);
    assertThat(htmlBody).contains("NordicPay");
  }

  @Test
  void resolveOrgByNameAndValidationErrors() {
    seedService.seedDemo();

    ResponseEntity<Map> byName = rest.getForEntity("/api/orgs/by-name/NordicPay", Map.class);
    assertThat(byName.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(byName.getBody().get("name")).isEqualTo("NordicPay");
    String orgId = (String) byName.getBody().get("id");

    ResponseEntity<Map> missingOrg = rest.getForEntity(
        "/api/orgs/00000000-0000-0000-0000-000000000099/controls", Map.class);
    assertThat(missingOrg.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(missingOrg.getBody().get("error")).isEqualTo("not_found");
    assertThat(missingOrg.getBody().get("status")).isEqualTo(404);

    ResponseEntity<Map> missingName = rest.getForEntity("/api/orgs/by-name/NoSuchOrg", Map.class);
    assertThat(missingName.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    ResponseEntity<Map> badControl = rest.getForEntity(
        "/api/orgs/" + orgId + "/controls/PCI-REQ-DOES-NOT-EXIST", Map.class);
    assertThat(badControl.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> badType = new HttpEntity<>(
        "{\"type\":\"not_a_real_type\",\"rawJson\":\"{}\"}", headers);
    ResponseEntity<Map> badArt = rest.exchange(
        "/api/orgs/" + orgId + "/artifacts",
        HttpMethod.POST,
        badType,
        new ParameterizedTypeReference<Map>() {});
    assertThat(badArt.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(badArt.getBody().get("error")).isEqualTo("bad_request");
    assertThat(String.valueOf(badArt.getBody().get("message"))).contains("Invalid artifact type");

    ResponseEntity<Map> badStatus = rest.getForEntity(
        "/api/orgs/" + orgId + "/controls?status=purple", Map.class);
    assertThat(badStatus.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    ResponseEntity<Map> missingPack = rest.getForEntity(
        "/api/orgs/" + orgId + "/evidence-packs/00000000-0000-0000-0000-000000000088", Map.class);
    assertThat(missingPack.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void changeRecordOnlyEndpointKeepsChangeGaps() {
    Map<String, Object> seed = seedService.seedDemo();
    assertThat(((Number) seed.get("gap")).longValue()).isEqualTo(4L);

    ResponseEntity<Map> resp = rest.postForEntity("/api/demo/ingest-change-records", null, Map.class);
    assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    // Hard rule: still 4 gaps because failing tests remain
    assertThat(resp.getBody().get("gap")).isEqualTo(4);
    assertThat(String.valueOf(resp.getBody().get("note"))).contains("hard rule");
  }

  @Test
  void seedEndpointAcceptsDemoProfileQueryParam() {
    ResponseEntity<Map> resp = rest.postForEntity(
        "/api/demo/seed?demoProfile=withChangeRecords", null, Map.class);
    assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(resp.getBody().get("demoProfile")).isEqualTo("withChangeRecords");
    assertThat(resp.getBody().get("pass")).isEqualTo(18);
    assertThat(resp.getBody().get("gap")).isEqualTo(2);
  }

  @Test
  void demoUiHomeReturns200WithToken() {
    // TestRestTemplate follows redirects: bare /demo lands on gate
    ResponseEntity<String> denied = rest.getForEntity("/demo", String.class);
    assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(denied.getBody()).contains("Demo token");
    assertThat(denied.getBody()).doesNotContain("Generate evidence pack");

    ResponseEntity<String> ok = rest.getForEntity("/demo?token=nordic-demo", String.class);
    assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(ok.getBody()).contains("NordicPay");
    assertThat(ok.getBody()).contains("pass:");
    assertThat(ok.getBody()).contains("gap:");
    assertThat(ok.getBody()).contains("Generate evidence pack");
  }

  @Test
  void demoGatePageIsPublic() {
    ResponseEntity<String> gate = rest.getForEntity("/demo/gate", String.class);
    assertThat(gate.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(gate.getBody()).contains("Demo token");
  }

  @Test
  void listEvidencePacksEndpoint() {
    Map<String, Object> seed = seedService.seedDemo();
    String orgId = (String) seed.get("organizationId");
    rest.postForEntity("/api/orgs/" + orgId + "/evidence-packs", null, Map.class);
    ResponseEntity<Map> list = rest.getForEntity("/api/orgs/" + orgId + "/evidence-packs", Map.class);
    assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(((Number) list.getBody().get("count")).intValue()).isGreaterThanOrEqualTo(1);
  }
}

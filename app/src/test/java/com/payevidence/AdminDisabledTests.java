package com.payevidence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "payevidence.admin-token=",
      "payevidence.seed-on-startup=false",
      "spring.datasource.url=jdbc:h2:mem:adminoff;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password="
    })
class AdminDisabledTests {

  @Autowired TestRestTemplate rest;

  @Test
  void adminSignupsAreHiddenWhenTokenUnset() {
    ResponseEntity<String> resp = rest.getForEntity("/admin/signups", String.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(resp.getBody()).doesNotContain("Early-access signups");
  }
}

package com.payevidence.config;

import com.payevidence.config.DatabaseUrlParser.PostgresTarget;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * When {@code DATABASE_URL} is a Postgres URL, point the Spring datasource at it
 * before auto-configuration. Unset or unrecognized values leave the H2 profile in place.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {
  static final String SOURCE_NAME = "payevidence-database-url";
  private static final Logger log = LoggerFactory.getLogger(DatabaseUrlEnvironmentPostProcessor.class);

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    String raw = environment.getProperty("DATABASE_URL");
    if (raw == null || raw.isBlank()) {
      return;
    }
    PostgresTarget target = DatabaseUrlParser.parse(raw);
    if (target == null) {
      log.warn("DATABASE_URL is set but is not a Postgres URL; keeping the default datasource");
      return;
    }
    String username = target.username() != null
        ? target.username()
        : environment.getProperty("DATABASE_USER", "payevidence");
    String password = target.password() != null
        ? target.password()
        : environment.getProperty("DATABASE_PASSWORD", "payevidence");

    Map<String, Object> props = new HashMap<>();
    props.put("spring.datasource.url", target.jdbcUrl());
    props.put("spring.datasource.username", username);
    props.put("spring.datasource.password", password);
    props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
    props.put("spring.jpa.hibernate.ddl-auto", "update");
    props.put("spring.jpa.open-in-view", "false");
    props.put("spring.datasource.hikari.maximum-pool-size", "5");
    environment.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, props));
    log.info("DATABASE_URL selects Postgres ({})", hostOf(target.jdbcUrl()));
  }

  static String hostOf(String jdbcUrl) {
    String rest = jdbcUrl.startsWith("jdbc:postgresql://")
        ? jdbcUrl.substring("jdbc:postgresql://".length())
        : jdbcUrl;
    int slash = rest.indexOf('/');
    return slash < 0 ? rest : rest.substring(0, slash);
  }
}

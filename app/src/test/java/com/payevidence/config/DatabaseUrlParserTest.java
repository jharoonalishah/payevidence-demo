package com.payevidence.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.payevidence.config.DatabaseUrlParser.PostgresTarget;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class DatabaseUrlParserTest {

  @Test
  void convertsNeonUrlAndKeepsSslModeOutOfThePassword() {
    PostgresTarget target = DatabaseUrlParser.parse(
        "postgresql://neondb_owner:s3cret@ep-abc.us-east-1.aws.neon.tech/neondb?sslmode=require");
    assertThat(target).isNotNull();
    assertThat(target.jdbcUrl())
        .isEqualTo("jdbc:postgresql://ep-abc.us-east-1.aws.neon.tech/neondb?sslmode=require");
    assertThat(target.username()).isEqualTo("neondb_owner");
    assertThat(target.password()).isEqualTo("s3cret");
    assertThat(target.jdbcUrl()).doesNotContain("s3cret");
  }

  @Test
  void decodesPasswordAndKeepsPort() {
    PostgresTarget target = DatabaseUrlParser.parse(
        "postgres://app:p%40ss%3Aword@localhost:5432/payevidence");
    assertThat(target.jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/payevidence");
    assertThat(target.username()).isEqualTo("app");
    assertThat(target.password()).isEqualTo("p@ss:word");
  }

  @Test
  void passesJdbcUrlThroughWithoutEmbeddedCredentials() {
    PostgresTarget target = DatabaseUrlParser.parse("jdbc:postgresql://localhost:5432/payevidence");
    assertThat(target.jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/payevidence");
    assertThat(target.username()).isNull();
    assertThat(target.password()).isNull();
  }

  @Test
  void ignoresBlankAndNonPostgres() {
    assertThat(DatabaseUrlParser.parse(null)).isNull();
    assertThat(DatabaseUrlParser.parse("  ")).isNull();
    assertThat(DatabaseUrlParser.parse("jdbc:h2:mem:test")).isNull();
    assertThat(DatabaseUrlParser.parse("postgresql://no-host")).isNull();
  }

  @Test
  void postProcessorOverridesDatasourceWhenDatabaseUrlIsSet() {
    StandardEnvironment env = new StandardEnvironment();
    Map<String, Object> raw = new HashMap<>();
    raw.put("DATABASE_URL", "postgresql://user:secret@db.example:5432/payevidence?sslmode=require");
    env.getPropertySources().addFirst(new MapPropertySource("test-env", raw));

    new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(env, new SpringApplication());

    assertThat(env.getProperty("spring.datasource.url"))
        .isEqualTo("jdbc:postgresql://db.example:5432/payevidence?sslmode=require");
    assertThat(env.getProperty("spring.datasource.username")).isEqualTo("user");
    assertThat(env.getProperty("spring.datasource.password")).isEqualTo("secret");
    assertThat(env.getProperty("spring.datasource.driver-class-name")).isEqualTo("org.postgresql.Driver");
    assertThat(env.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
  }

  @Test
  void postProcessorLeavesH2AloneWhenUnset() {
    StandardEnvironment env = new StandardEnvironment();
    new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(env, new SpringApplication());
    assertThat(env.getPropertySources().contains(DatabaseUrlEnvironmentPostProcessor.SOURCE_NAME)).isFalse();
  }
}

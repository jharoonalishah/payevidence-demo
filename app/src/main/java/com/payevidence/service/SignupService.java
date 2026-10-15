package com.payevidence.service;

import com.payevidence.domain.Signup;
import com.payevidence.repo.SignupRepository;
import com.payevidence.web.ui.SignupForm;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SignupService {
  public enum Result { CREATED, ALREADY_LISTED }

  private static final Logger log = LoggerFactory.getLogger(SignupService.class);

  private final SignupRepository repository;
  private final Environment environment;
  private boolean postgres;

  public SignupService(SignupRepository repository, Environment environment) {
    this.repository = repository;
    this.environment = environment;
  }

  @PostConstruct
  void detectStore() {
    String url = environment.getProperty("spring.datasource.url", "");
    postgres = url.startsWith("jdbc:postgresql:");
    if (postgres) {
      log.info("Signup storage: Postgres ({})", hostOf(url));
    } else {
      log.info(
          "Signup storage: local H2. Each signup is also written to this log. "
              + "Set DATABASE_URL to a Neon Postgres URL for durable storage.");
    }
  }

  public boolean isPostgres() {
    return postgres;
  }

  @Transactional
  public Result register(SignupForm form) {
    String email = form.normalizedEmail();
    if (repository.existsByEmailIgnoreCase(email)) {
      logFallback(form, email, true);
      return Result.ALREADY_LISTED;
    }
    Signup signup = new Signup();
    signup.setName(form.getName().trim());
    signup.setEmail(email);
    signup.setCompany(form.getCompany().trim());
    signup.setRole(blankToNull(form.getRole()));
    signup.setMessage(blankToNull(form.getMessage()));
    try {
      repository.saveAndFlush(signup);
    } catch (DataIntegrityViolationException ex) {
      logFallback(form, email, true);
      return Result.ALREADY_LISTED;
    }
    logFallback(form, email, false);
    if (postgres) {
      log.info("Signup stored in Postgres: email={} company={}", email, signup.getCompany());
    }
    return Result.CREATED;
  }

  private void logFallback(SignupForm form, String email, boolean already) {
    if (postgres) {
      return;
    }
    log.info(
        "Signup {} (H2 fallback; set DATABASE_URL for Postgres): name={} email={} company={} role={} message={}",
        already ? "already listed" : "stored",
        form.getName().trim(),
        email,
        form.getCompany().trim(),
        blankToNull(form.getRole()),
        oneLine(blankToNull(form.getMessage())));
  }

  private static String blankToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static String oneLine(String value) {
    if (value == null) {
      return null;
    }
    return value.replace('\n', ' ').replace('\r', ' ');
  }

  static String hostOf(String jdbcUrl) {
    String rest = jdbcUrl.startsWith("jdbc:postgresql://")
        ? jdbcUrl.substring("jdbc:postgresql://".length())
        : jdbcUrl;
    int slash = rest.indexOf('/');
    String host = slash < 0 ? rest : rest.substring(0, slash);
    int query = host.indexOf('?');
    return query < 0 ? host : host.substring(0, query);
  }
}

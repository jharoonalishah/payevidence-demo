package com.payevidence.config;

import com.payevidence.service.SeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupSeeder implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(StartupSeeder.class);
  private final PayEvidenceProperties props;
  private final SeedService seedService;

  public StartupSeeder(PayEvidenceProperties props, SeedService seedService) {
    this.props = props;
    this.seedService = seedService;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!props.isSeedOnStartup()) {
      log.info("Startup seed disabled");
      return;
    }
    try {
      var result = seedService.seedDemo();
      log.info("Startup seed: {}", result);
    } catch (Exception e) {
      log.error("Startup seed failed (call POST /api/demo/seed later): {}", e.getMessage(), e);
    }
  }
}

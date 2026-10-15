package com.payevidence.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "payevidence")
public class PayEvidenceProperties {
  private String cataloguePath = "../day1/controls/pci-mvp-catalogue.yaml";
  private String samplesDir = "../day1/samples";
  private String changeRecordPath = "../day2-3/samples/change_record.json";
  private String packsDir = "./packs";
  private boolean seedOnStartup = true;
  /**
   * Freshness clock for NordicPay scoring. Defaults to the day the sample artifacts were frozen
   * so a later calendar date does not turn the demo's passing controls stale.
   */
  private String asOf = "2026-09-24T12:00:00Z";
  /** Shared secret for demo UI gate. Override with PAYEVIDENCE_DEMO_TOKEN. */
  private String demoToken = "nordic-demo";
  /** Shared secret for /admin/signups. Override with ADMIN_TOKEN. Blank disables the page. */
  private String adminToken = "";

  public String getCataloguePath() { return cataloguePath; }
  public void setCataloguePath(String cataloguePath) { this.cataloguePath = cataloguePath; }
  public String getSamplesDir() { return samplesDir; }
  public void setSamplesDir(String samplesDir) { this.samplesDir = samplesDir; }
  public String getChangeRecordPath() { return changeRecordPath; }
  public void setChangeRecordPath(String changeRecordPath) { this.changeRecordPath = changeRecordPath; }
  public String getPacksDir() { return packsDir; }
  public void setPacksDir(String packsDir) { this.packsDir = packsDir; }
  public boolean isSeedOnStartup() { return seedOnStartup; }
  public void setSeedOnStartup(boolean seedOnStartup) { this.seedOnStartup = seedOnStartup; }
  public String getAsOf() { return asOf; }
  public void setAsOf(String asOf) { this.asOf = asOf; }
  public String getDemoToken() { return demoToken; }
  public void setDemoToken(String demoToken) { this.demoToken = demoToken; }
  public String getAdminToken() { return adminToken; }
  public void setAdminToken(String adminToken) { this.adminToken = adminToken; }
}

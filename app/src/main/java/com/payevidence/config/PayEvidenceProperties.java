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
  /** Shared secret for demo UI gate. Override with PAYEVIDENCE_DEMO_TOKEN. */
  private String demoToken = "nordic-demo";

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
  public String getDemoToken() { return demoToken; }
  public void setDemoToken(String demoToken) { this.demoToken = demoToken; }
}

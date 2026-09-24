package com.payevidence.web.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Artifact ingest body. {@code type} is a string so invalid values become a stable 400
 * (not a Jackson enum deserialize failure).
 */
public class ArtifactIngestRequest {
  private String type;
  private Instant capturedAt;
  private String storagePath;
  private Map<String, Object> payload;
  private String rawJson;

  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public Instant getCapturedAt() { return capturedAt; }
  public void setCapturedAt(Instant capturedAt) { this.capturedAt = capturedAt; }
  public String getStoragePath() { return storagePath; }
  public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
  public Map<String, Object> getPayload() { return payload; }
  public void setPayload(Map<String, Object> payload) { this.payload = payload; }
  public String getRawJson() { return rawJson; }
  public void setRawJson(String rawJson) { this.rawJson = rawJson; }
}

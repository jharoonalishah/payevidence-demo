package com.payevidence.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "artifacts")
public class Artifact {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private Organization organization;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private ArtifactType type;

  @Column(nullable = false)
  private Instant capturedAt;

  private String storagePath;

  @Lob
  @Column(columnDefinition = "CLOB")
  private String rawJson;

  public Artifact() {}

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public Organization getOrganization() { return organization; }
  public void setOrganization(Organization organization) { this.organization = organization; }
  public ArtifactType getType() { return type; }
  public void setType(ArtifactType type) { this.type = type; }
  public Instant getCapturedAt() { return capturedAt; }
  public void setCapturedAt(Instant capturedAt) { this.capturedAt = capturedAt; }
  public String getStoragePath() { return storagePath; }
  public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
  public String getRawJson() { return rawJson; }
  public void setRawJson(String rawJson) { this.rawJson = rawJson; }
}

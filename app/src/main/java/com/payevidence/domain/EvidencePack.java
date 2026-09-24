package com.payevidence.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evidence_packs")
public class EvidencePack {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  private Organization organization;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  @Lob
  @Column(columnDefinition = "CLOB")
  private String statusesJson;

  @Lob
  @Column(columnDefinition = "CLOB")
  private String htmlContent;

  private String storagePath;

  /** Absolute path to generated PDF on disk, if available. */
  private String pdfStoragePath;

  public EvidencePack() {}

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public Organization getOrganization() { return organization; }
  public void setOrganization(Organization organization) { this.organization = organization; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public String getStatusesJson() { return statusesJson; }
  public void setStatusesJson(String statusesJson) { this.statusesJson = statusesJson; }
  public String getHtmlContent() { return htmlContent; }
  public void setHtmlContent(String htmlContent) { this.htmlContent = htmlContent; }
  public String getStoragePath() { return storagePath; }
  public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
  public String getPdfStoragePath() { return pdfStoragePath; }
  public void setPdfStoragePath(String pdfStoragePath) { this.pdfStoragePath = pdfStoragePath; }
}

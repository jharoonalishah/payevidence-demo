package com.payevidence.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "controls")
public class Control {
  @Id
  @Column(length = 64)
  private String id;

  private String catalogueId;
  private String pciRef;
  private String title;

  @Column(length = 4000)
  private String description;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "control_evidence_types", joinColumns = @JoinColumn(name = "control_id"))
  @Column(name = "evidence_type")
  @Enumerated(EnumType.STRING)
  private List<ArtifactType> evidenceTypes = new ArrayList<>();

  private int freshnessDays;

  @Column(length = 2000)
  private String passRule;

  public Control() {}

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getCatalogueId() { return catalogueId; }
  public void setCatalogueId(String catalogueId) { this.catalogueId = catalogueId; }
  public String getPciRef() { return pciRef; }
  public void setPciRef(String pciRef) { this.pciRef = pciRef; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public List<ArtifactType> getEvidenceTypes() { return evidenceTypes; }
  public void setEvidenceTypes(List<ArtifactType> evidenceTypes) { this.evidenceTypes = evidenceTypes; }
  public int getFreshnessDays() { return freshnessDays; }
  public void setFreshnessDays(int freshnessDays) { this.freshnessDays = freshnessDays; }
  public String getPassRule() { return passRule; }
  public void setPassRule(String passRule) { this.passRule = passRule; }
}

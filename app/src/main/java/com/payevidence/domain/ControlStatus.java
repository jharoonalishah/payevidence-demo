package com.payevidence.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "control_statuses",
       uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "control_id"}))
public class ControlStatus {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private Organization organization;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  private Control control;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private StatusValue status;

  @Column(nullable = false)
  private Instant computedAt;

  @Column(length = 2000)
  private String reason;

  public ControlStatus() {}

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public Organization getOrganization() { return organization; }
  public void setOrganization(Organization organization) { this.organization = organization; }
  public Control getControl() { return control; }
  public void setControl(Control control) { this.control = control; }
  public StatusValue getStatus() { return status; }
  public void setStatus(StatusValue status) { this.status = status; }
  public Instant getComputedAt() { return computedAt; }
  public void setComputedAt(Instant computedAt) { this.computedAt = computedAt; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
}

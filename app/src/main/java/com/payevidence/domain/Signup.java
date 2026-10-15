package com.payevidence.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Entity
@Table(name = "signups")
public class Signup {
  private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
      .withZone(ZoneOffset.UTC);

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, unique = true, length = 200)
  private String email;

  @Column(nullable = false, length = 160)
  private String company;

  /** Named job_role because ROLE is reserved in Postgres. */
  @Column(name = "job_role", length = 120)
  private String role;

  @Column(length = 2000)
  private String message;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getCompany() { return company; }
  public void setCompany(String company) { this.company = company; }
  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }
  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

  public String getCreatedAtLabel() {
    return createdAt == null ? "" : LABEL.format(createdAt);
  }
}

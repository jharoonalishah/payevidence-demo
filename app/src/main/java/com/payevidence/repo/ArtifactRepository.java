package com.payevidence.repo;

import com.payevidence.domain.Artifact;
import com.payevidence.domain.ArtifactType;
import com.payevidence.domain.Organization;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtifactRepository extends JpaRepository<Artifact, UUID> {
  List<Artifact> findByOrganization(Organization organization);
  List<Artifact> findByOrganizationAndType(Organization organization, ArtifactType type);
  void deleteByOrganization(Organization organization);
}

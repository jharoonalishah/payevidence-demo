package com.payevidence.repo;

import com.payevidence.domain.EvidencePack;
import com.payevidence.domain.Organization;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidencePackRepository extends JpaRepository<EvidencePack, UUID> {
  List<EvidencePack> findByOrganizationOrderByCreatedAtDesc(Organization organization);
  void deleteByOrganization(Organization organization);
}

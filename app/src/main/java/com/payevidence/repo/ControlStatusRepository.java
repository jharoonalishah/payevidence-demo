package com.payevidence.repo;

import com.payevidence.domain.Control;
import com.payevidence.domain.ControlStatus;
import com.payevidence.domain.Organization;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ControlStatusRepository extends JpaRepository<ControlStatus, UUID> {
  List<ControlStatus> findByOrganization(Organization organization);
  Optional<ControlStatus> findByOrganizationAndControl(Organization organization, Control control);
  void deleteByOrganization(Organization organization);
}

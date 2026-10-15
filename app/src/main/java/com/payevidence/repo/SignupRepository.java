package com.payevidence.repo;

import com.payevidence.domain.Signup;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignupRepository extends JpaRepository<Signup, UUID> {
  Optional<Signup> findByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCase(String email);

  List<Signup> findAllByOrderByCreatedAtDesc();
}

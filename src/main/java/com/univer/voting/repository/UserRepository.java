package com.univer.voting.repository;

import com.univer.voting.models.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<Users, UUID> {

    Optional<Users> findByUsername(String username);
    Optional<Users> findByEmail(String email);
    Optional<Users> findByNationalId(String nationalId);
    Optional<Users> findByActivationToken(UUID activationToken);
    Optional<Users> findByVerificationToken(UUID verificationToken);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByNationalId(String nationalId);
}

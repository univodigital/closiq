package com.closiq.identity.repository;

import com.closiq.identity.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByUserId(UUID userId);

    Optional<UserProfile> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);
}

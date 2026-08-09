package com.closiq.user.repository;

import com.closiq.user.domain.SellerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SellerProfileRepository extends JpaRepository<SellerProfile, UUID> {

    Optional<SellerProfile> findByUserId(UUID userId);
}

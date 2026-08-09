package com.closiq.user.repository;

import com.closiq.user.domain.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findByUserIdAndDeletedAtIsNullOrderByIsDefaultDescCreatedAtAsc(UUID userId);

    long countByUserIdAndDeletedAtIsNull(UUID userId);

    Optional<Address> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    @Modifying
    @Transactional
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId AND a.deletedAt IS NULL")
    void clearDefaultForUser(@Param("userId") UUID userId);
}

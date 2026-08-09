package com.closiq.seller.repository;

import com.closiq.seller.domain.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {

    List<BankAccount> findBySellerProfileIdAndStatusNotOrderByIsDefaultDescCreatedAtAsc(
            UUID sellerProfileId, String excludedStatus);

    Optional<BankAccount> findByIdAndSellerProfileId(UUID id, UUID sellerProfileId);

    long countBySellerProfileIdAndStatusNot(UUID sellerProfileId, String excludedStatus);

    @Modifying
    @Transactional
    @Query("UPDATE BankAccount b SET b.isDefault = false WHERE b.sellerProfile.id = :sellerId")
    void clearDefaultForSeller(@Param("sellerId") UUID sellerId);
}

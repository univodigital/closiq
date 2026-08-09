package com.closiq.seller.repository;

import com.closiq.seller.domain.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findBySellerProfileId(UUID sellerProfileId);
}

package com.closiq.seller.repository;

import com.closiq.seller.domain.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Page<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);

    boolean existsByReferenceTypeAndReferenceIdAndTxnType(
            String referenceType, String referenceId, String txnType);

    Optional<WalletTransaction> findByReferenceTypeAndReferenceIdAndTxnType(
            String referenceType, String referenceId, String txnType);
}

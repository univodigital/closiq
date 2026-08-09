package com.closiq.seller.repository;

import com.closiq.seller.domain.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KycDocumentRepository extends JpaRepository<KycDocument, UUID> {

    List<KycDocument> findByApplicationIdOrderByUploadedAtAsc(UUID applicationId);
}

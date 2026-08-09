package com.closiq.catalog.service;

import com.closiq.catalog.mapper.ProductMapper;
import com.closiq.catalog.repository.PromotionalOfferRepository;
import com.closiq.catalog.web.dto.OfferResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OfferService {

    private static final String ACTIVE = "ACTIVE";

    private final PromotionalOfferRepository offerRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public List<OfferResponse> listActiveOffers() {
        return offerRepository.findByStatusAndValidUntilAfterOrderByValidUntilAsc(ACTIVE, Instant.now()).stream()
                .map(productMapper::toOffer)
                .toList();
    }
}

package com.closiq.seller.service;

import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.repository.BookingRepository;
import com.closiq.catalog.repository.ProductRepository;
import com.closiq.user.domain.SellerProfile;
import com.closiq.seller.web.dto.DashboardSummaryResponse;
import com.closiq.seller.web.dto.SellerDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerDashboardService {

    private static final String ACTIVE_PRODUCT = "ACTIVE";

    private final SellerContextService sellerContextService;
    private final ProductRepository productRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public SellerDashboardResponse getDashboard(UUID userId) {
        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);

        long activeListings = productRepository.countBySellerProfileIdAndStatusAndDeletedAtIsNull(
                seller.getId(), ACTIVE_PRODUCT);
        long pendingBookings = bookingRepository.countBySellerProfileIdAndStatus(
                seller.getId(), BookingStatus.CONFIRMED);

        return SellerDashboardResponse.builder()
                .summary(DashboardSummaryResponse.builder()
                        .activeListings(activeListings)
                        .pendingBookings(pendingBookings)
                        .earningsThisMonth(0)
                        .currency("INR")
                        .build())
                .tasks(List.of())
                .recentBookings(List.of())
                .build();
    }
}

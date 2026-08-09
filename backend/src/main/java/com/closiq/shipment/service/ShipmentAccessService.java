package com.closiq.shipment.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.repository.BookingRepository;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.identity.service.UserService;
import com.closiq.seller.service.SellerContextService;
import com.closiq.user.domain.SellerProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShipmentAccessService {

    private final BookingRepository bookingRepository;
    private final SellerContextService sellerContextService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Booking resolveAccessibleBooking(UUID userId, String bookingIdOrNumber) {
        Booking booking = resolveBooking(bookingIdOrNumber);
        if (booking.getCustomerId().equals(userId)) {
            return booking;
        }

        if (userService.getUserRoles(userId).contains(com.closiq.common.security.RoleType.SELLER)) {
            SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);
            if (seller.getId().equals(booking.getSellerProfileId())) {
                return booking;
            }
        }

        throw new ClosiqException(ErrorCode.FORBIDDEN);
    }

    @Transactional(readOnly = true)
    public Booking resolveSellerBooking(UUID sellerUserId, String bookingIdOrNumber) {
        SellerProfile seller = sellerContextService.requireVerifiedSeller(sellerUserId);
        Booking booking = resolveBooking(bookingIdOrNumber);
        if (!seller.getId().equals(booking.getSellerProfileId())) {
            throw new ClosiqException(ErrorCode.FORBIDDEN);
        }
        return booking;
    }

    private Booking resolveBooking(String bookingIdOrNumber) {
        if (bookingIdOrNumber.startsWith("VST-RNT-") || bookingIdOrNumber.startsWith("BK-")) {
            return bookingRepository.findByRentalNumber(bookingIdOrNumber)
                    .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Booking not found"));
        }
        if (bookingIdOrNumber.startsWith("VST-ORD-")) {
            return bookingRepository.findByOrderNumber(bookingIdOrNumber)
                    .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Booking not found"));
        }
        try {
            UUID id = UUID.fromString(bookingIdOrNumber);
            return bookingRepository.findById(id)
                    .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Booking not found"));
        } catch (IllegalArgumentException ex) {
            throw new ClosiqException(ErrorCode.NOT_FOUND, "Booking not found");
        }
    }
}

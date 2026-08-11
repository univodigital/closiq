package com.closiq.user.service;

import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.repository.BookingRepository;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.security.RoleType;
import com.closiq.identity.domain.User;
import com.closiq.identity.domain.UserStatus;
import com.closiq.identity.repository.UserRepository;
import com.closiq.identity.service.RefreshTokenService;
import com.closiq.identity.service.UserService;
import com.closiq.user.web.dto.DeleteAccountPreviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private static final List<String> ACTIVE_BOOKING_STATUSES = List.of(
            BookingStatus.PENDING_PAYMENT,
            BookingStatus.CONFIRMED,
            BookingStatus.SELLER_ACCEPTED,
            BookingStatus.PREPARING,
            BookingStatus.OUT_FOR_DELIVERY,
            BookingStatus.TRIAL_READY,
            BookingStatus.RENTAL_ACTIVE,
            BookingStatus.RETURN_SCHEDULED,
            BookingStatus.RETURN_IN_TRANSIT,
            BookingStatus.REFUND_PENDING);

    private final UserRepository userRepository;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public DeleteAccountPreviewResponse previewDeleteAccount(UUID userId) {
        userService.requireActiveUser(userId);
        long activeBookings = bookingRepository.countByCustomerIdAndStatusIn(userId, ACTIVE_BOOKING_STATUSES);
        return DeleteAccountPreviewResponse.builder()
                .activeBookings(activeBookings)
                .canDelete(activeBookings == 0)
                .message(activeBookings > 0
                        ? "You have active bookings. Complete or cancel them before deleting your account."
                        : "Deleting your account will deactivate access. Booking history is retained for records.")
                .build();
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        User user = userService.requireActiveUser(userId);

        if (userService.getUserRoles(userId).contains(RoleType.ADMIN)) {
            throw new ClosiqException(ErrorCode.FORBIDDEN, "Admin accounts cannot be self-deleted");
        }

        long activeBookings = bookingRepository.countByCustomerIdAndStatusIn(userId, ACTIVE_BOOKING_STATUSES);
        if (activeBookings > 0) {
            throw new ClosiqException(
                    ErrorCode.INVALID_STATE_TRANSITION,
                    "Cannot delete account while you have active bookings. Complete or cancel them first.");
        }

        user.setStatus(UserStatus.DELETED);
        user.setDeletedAt(Instant.now());
        user.setUpdatedBy(userId);
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(userId);
    }
}

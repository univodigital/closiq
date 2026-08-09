package com.closiq.inventory.service;

import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.util.IdGenerator;
import com.closiq.inventory.domain.InventoryItem;
import com.closiq.inventory.domain.InventoryReservation;
import com.closiq.inventory.repository.InventoryReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryHoldService {

    public static final String ACTIVE = "ACTIVE";
    public static final String RELEASED = "RELEASED";
    public static final String EXPIRED = "EXPIRED";
    public static final String HOLD = "HOLD";
    public static final String CONFIRMED = "CONFIRMED";

    private final InventoryReservationRepository reservationRepository;

    @Transactional
    public InventoryReservation createHold(
            InventoryItem item,
            UUID bookingId,
            LocalDate startDate,
            LocalDate effectiveEndDate,
            Instant holdExpiresAt) {

        InventoryReservation reservation = InventoryReservation.builder()
                .id(IdGenerator.uuidV7())
                .inventoryItem(item)
                .bookingId(bookingId)
                .startDate(startDate)
                .endDate(effectiveEndDate)
                .reservationType(HOLD)
                .status(ACTIVE)
                .holdExpiresAt(holdExpiresAt)
                .build();

        try {
            return reservationRepository.save(reservation);
        } catch (DataIntegrityViolationException ex) {
            throw new ClosiqException(ErrorCode.BOOKING_CONFLICT, "Selected dates are no longer available");
        }
    }

    @Transactional
    public void releaseReservation(UUID reservationId, String newStatus) {
        reservationRepository.findById(reservationId).ifPresent(reservation -> {
            reservation.setStatus(newStatus);
            reservation.setHoldExpiresAt(null);
            reservationRepository.save(reservation);
        });
    }

    @Transactional
    public void releaseByBookingId(UUID bookingId, String newStatus) {
        reservationRepository.findByBookingIdAndStatus(bookingId, ACTIVE).forEach(reservation -> {
            reservation.setStatus(newStatus);
            reservation.setHoldExpiresAt(null);
            reservationRepository.save(reservation);
        });
    }

    @Transactional
    public void confirmHold(UUID bookingId) {
        reservationRepository.findByBookingIdAndStatus(bookingId, ACTIVE).forEach(reservation -> {
            if (HOLD.equals(reservation.getReservationType())) {
                reservation.setReservationType(CONFIRMED);
                reservation.setHoldExpiresAt(null);
                reservationRepository.save(reservation);
            }
        });
    }
}

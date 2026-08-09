package com.closiq.shipment.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.repository.BookingRepository;
import com.closiq.booking.service.BookingLifecycleService;
import com.closiq.booking.service.BookingTimelineService;
import com.closiq.notification.service.NotificationDispatchService;
import com.closiq.shipment.domain.Shipment;
import com.closiq.shipment.domain.ShipmentStatus;
import com.closiq.shipment.domain.ShipmentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShipmentBookingSyncService {

    private final BookingRepository bookingRepository;
    private final BookingTimelineService timelineService;
    private final BookingLifecycleService bookingLifecycleService;
    private final NotificationDispatchService notificationDispatchService;

    @Transactional
    public void syncBookingStatus(Shipment shipment, String newShipmentStatus, UUID actorId) {
        Booking booking = bookingRepository.findById(shipment.getBookingId())
                .orElseThrow();

        String bookingStatus = mapToBookingStatus(shipment.getShipmentType(), newShipmentStatus, booking.getStatus());
        if (bookingStatus == null || bookingStatus.equals(booking.getStatus())) {
            return;
        }

        booking.setStatus(bookingStatus);
        bookingRepository.save(booking);
        timelineService.append(
                booking.getId(),
                actorId,
                bookingStatus,
                shipmentLabel(shipment.getShipmentType(), newShipmentStatus));

        if (BookingStatus.TRIAL_READY.equals(bookingStatus)) {
            bookingLifecycleService.ensureTrialSession(booking.getId());
            notificationDispatchService.trialReady(booking);
        } else if (BookingStatus.OUT_FOR_DELIVERY.equals(bookingStatus)) {
            notificationDispatchService.outForDelivery(booking);
        }
    }

    private String mapToBookingStatus(String shipmentType, String shipmentStatus, String currentBookingStatus) {
        if (ShipmentType.OUTBOUND.equals(shipmentType)) {
            return switch (shipmentStatus) {
                case ShipmentStatus.PICKED_UP, ShipmentStatus.IN_TRANSIT -> BookingStatus.OUT_FOR_DELIVERY;
                case ShipmentStatus.OUT_FOR_DELIVERY -> BookingStatus.OUT_FOR_DELIVERY;
                case ShipmentStatus.DELIVERED -> BookingStatus.TRIAL_READY;
                default -> null;
            };
        }

        if (ShipmentType.RETURN.equals(shipmentType)) {
            return switch (shipmentStatus) {
                case ShipmentStatus.PICKED_UP, ShipmentStatus.IN_TRANSIT, ShipmentStatus.OUT_FOR_DELIVERY ->
                        BookingStatus.RETURN_IN_TRANSIT;
                case ShipmentStatus.DELIVERED -> BookingStatus.RETURNED;
                default -> null;
            };
        }

        return null;
    }

    private String shipmentLabel(String shipmentType, String shipmentStatus) {
        String direction = ShipmentType.RETURN.equals(shipmentType) ? "Return" : "Outbound";
        return direction + " shipment: " + shipmentStatus.replace('_', ' ').toLowerCase();
    }
}

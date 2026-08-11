package com.closiq.shipment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.repository.BookingRepository;
import com.closiq.booking.service.BookingStatusTransitions;
import com.closiq.booking.service.BookingTimelineService;
import com.closiq.booking.web.dto.ReturnRequestRequest;
import com.closiq.booking.web.dto.ReturnScheduleResponse;
import com.closiq.shipment.service.ReturnPickupScheduleService.ScheduledReturnPickup;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.util.IdGenerator;
import com.closiq.notification.service.NotificationDispatchService;
import com.closiq.shipment.domain.LogisticsProvider;
import com.closiq.shipment.domain.Shipment;
import com.closiq.shipment.domain.ShipmentEvent;
import com.closiq.shipment.domain.ShipmentStatus;
import com.closiq.shipment.domain.ShipmentType;
import com.closiq.shipment.gateway.CreateShipmentCommand;
import com.closiq.shipment.gateway.CreateShipmentResult;
import com.closiq.shipment.gateway.LogisticsGateway;
import com.closiq.shipment.gateway.TrackingSnapshot;
import com.closiq.shipment.repository.LogisticsProviderRepository;
import com.closiq.shipment.repository.ShipmentEventRepository;
import com.closiq.shipment.repository.ShipmentRepository;
import com.closiq.shipment.web.dto.ReadyForPickupRequest;
import com.closiq.shipment.web.dto.ShipmentEventResponse;
import com.closiq.shipment.web.dto.ShipmentResponse;
import com.closiq.shipment.web.dto.ShipmentStatusResponse;
import com.closiq.shipment.web.dto.ShipmentTrackResponse;
import com.closiq.user.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentService {

    private static final String SHADOWFAX_CODE = "SHADOWFAX";
    private static final Duration TRACK_CACHE_TTL = Duration.ofSeconds(60);

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    private final ShipmentAccessService accessService;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventRepository shipmentEventRepository;
    private final LogisticsProviderRepository logisticsProviderRepository;
    private final LogisticsGateway logisticsGateway;
    private final BookingRepository bookingRepository;
    private final BookingTimelineService timelineService;
    private final AddressRepository addressRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationDispatchService notificationDispatchService;
    private final ReturnPickupScheduleService returnPickupScheduleService;

    @Transactional(readOnly = true)
    public ShipmentTrackResponse track(UUID userId, String bookingIdOrNumber, String type) {
        Booking booking = accessService.resolveAccessibleBooking(userId, bookingIdOrNumber);
        Shipment shipment = findShipment(booking.getId(), normalizeType(type));
        LogisticsProvider provider = logisticsProviderRepository.findById(shipment.getLogisticsProviderId())
                .orElseThrow();

        refreshAgentInfoIfCached(shipment);

        List<ShipmentEventResponse> events = shipmentEventRepository
                .findByShipmentIdOrderByOccurredAtAsc(shipment.getId())
                .stream()
                .map(this::toEventResponse)
                .toList();

        return ShipmentTrackResponse.builder()
                .shipmentId(shipment.getId())
                .bookingId(booking.getId())
                .type(shipment.getShipmentType())
                .provider(provider.getCode())
                .trackingNumber(shipment.getTrackingNumber())
                .status(shipment.getStatus())
                .pickupScheduledAt(shipment.getPickupScheduledAt())
                .pickupTimeSlot(shipment.getPickupTimeSlot())
                .estimatedDeliveryAt(shipment.getEstimatedDeliveryAt())
                .events(events)
                .agentName(shipment.getAgentName())
                .agentPhone(shipment.getAgentPhoneMasked())
                .build();
    }

    @Transactional(readOnly = true)
    public ShipmentStatusResponse getStatus(UUID userId, String bookingIdOrNumber, String type) {
        Booking booking = accessService.resolveAccessibleBooking(userId, bookingIdOrNumber);
        Shipment shipment = findShipment(booking.getId(), normalizeType(type));
        return ShipmentStatusResponse.builder()
                .status(shipment.getStatus())
                .updatedAt(shipment.getUpdatedAt())
                .build();
    }

    @Transactional
    public ReturnScheduleResponse scheduleReturnPickup(UUID userId, String bookingIdOrNumber, ReturnRequestRequest request) {
        Booking booking = accessService.resolveAccessibleBooking(userId, bookingIdOrNumber);

        var existing = shipmentRepository.findByBookingIdAndShipmentType(booking.getId(), ShipmentType.RETURN);
        if (existing.isPresent()) {
            return toReturnScheduleResponse(booking, existing.get(), true);
        }

        ShipmentResponse created = createReturnShipment(booking, userId, request);
        Shipment shipment = shipmentRepository.findById(created.getShipmentId()).orElseThrow();
        return toReturnScheduleResponse(booking, shipment, false);
    }

    @Transactional
    public ShipmentResponse markReadyForPickup(UUID sellerUserId, String bookingIdOrNumber, ReadyForPickupRequest request) {
        Booking booking = accessService.resolveSellerBooking(sellerUserId, bookingIdOrNumber);

        if (!BookingStatus.SELLER_ACCEPTED.equals(booking.getStatus())
                && !BookingStatus.PREPARING.equals(booking.getStatus())) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION);
        }

        addressRepository.findByIdAndUserIdAndDeletedAtIsNull(request.getPickupAddressId(), sellerUserId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Pickup address not found"));

        if (booking.getDeliveryAddressId() == null) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Booking has no delivery address");
        }

        return shipmentRepository.findByBookingIdAndShipmentType(booking.getId(), ShipmentType.OUTBOUND)
                .map(shipment -> toShipmentResponse(shipment, SHADOWFAX_CODE))
                .orElseGet(() -> createOutboundShipment(booking, sellerUserId, request));
    }

    private ShipmentResponse createOutboundShipment(Booking booking, UUID actorId, ReadyForPickupRequest request) {
        LogisticsProvider provider = requireShadowfaxProvider();
        CreateShipmentResult providerResult = callGateway(CreateShipmentCommand.builder()
                .shipmentType(ShipmentType.OUTBOUND)
                .bookingId(booking.getId())
                .rentalNumber(booking.getRentalNumber())
                .bookingNumber(booking.getRentalNumber())
                .originAddressId(request.getPickupAddressId())
                .destinationAddressId(booking.getDeliveryAddressId())
                .pickupTimeSlot(request.getPickupTimeSlot())
                .handoffNotes(request.getHandoffNotes())
                .build());

        Shipment shipment = Shipment.builder()
                .id(IdGenerator.uuidV7())
                .bookingId(booking.getId())
                .logisticsProviderId(provider.getId())
                .originAddressId(request.getPickupAddressId())
                .destinationAddressId(booking.getDeliveryAddressId())
                .shipmentType(ShipmentType.OUTBOUND)
                .providerShipmentId(providerResult.getProviderShipmentId())
                .trackingNumber(providerResult.getTrackingNumber())
                .status(ShipmentStatus.CREATED)
                .pickupScheduledAt(providerResult.getPickupScheduledAt())
                .pickupTimeSlot(request.getPickupTimeSlot())
                .estimatedDeliveryAt(providerResult.getEstimatedDeliveryAt())
                .handoffNotes(request.getHandoffNotes())
                .build();
        shipmentRepository.save(shipment);
        appendEvent(shipment.getId(), ShipmentStatus.CREATED, "Shipment created", null, "evt_created_" + shipment.getId(), null);

        booking.setStatus(BookingStatus.PREPARING);
        bookingRepository.save(booking);
        timelineService.append(booking.getId(), actorId, BookingStatus.PREPARING, "Item ready — pickup scheduled");

        return toShipmentResponse(shipment, provider.getCode());
    }

    private ShipmentResponse createReturnShipment(Booking booking, UUID actorId, ReturnRequestRequest request) {
        if (!BookingStatus.RENTAL_ACTIVE.equals(booking.getStatus())
                && !BookingStatus.TRIAL_REJECTED.equals(booking.getStatus())) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION, "Return pickup not allowed in current state");
        }

        ScheduledReturnPickup schedule = returnPickupScheduleService.resolve(booking);

        UUID customerAddressId = resolveCustomerAddress(booking, actorId, request != null ? request.getAddressId() : null);
        Shipment outbound = shipmentRepository
                .findByBookingIdAndShipmentType(booking.getId(), ShipmentType.OUTBOUND)
                .orElseThrow(() -> new ClosiqException(
                        ErrorCode.NOT_FOUND, "Outbound shipment not found for return"));

        LogisticsProvider provider = requireShadowfaxProvider();
        CreateShipmentResult providerResult = callGateway(CreateShipmentCommand.builder()
                .shipmentType(ShipmentType.RETURN)
                .bookingId(booking.getId())
                .rentalNumber(booking.getRentalNumber())
                .bookingNumber(booking.getRentalNumber())
                .originAddressId(customerAddressId)
                .destinationAddressId(outbound.getOriginAddressId())
                .pickupTimeSlot(schedule.getPickupWindow())
                .handoffNotes(null)
                .build());

        Instant pickupScheduled = providerResult.getPickupScheduledAt() != null
                ? providerResult.getPickupScheduledAt()
                : schedule.getPickupScheduledAt();

        Shipment shipment = Shipment.builder()
                .id(IdGenerator.uuidV7())
                .bookingId(booking.getId())
                .logisticsProviderId(provider.getId())
                .originAddressId(customerAddressId)
                .destinationAddressId(outbound.getOriginAddressId())
                .shipmentType(ShipmentType.RETURN)
                .providerShipmentId(providerResult.getProviderShipmentId())
                .trackingNumber(providerResult.getTrackingNumber())
                .status(ShipmentStatus.CREATED)
                .pickupScheduledAt(pickupScheduled)
                .pickupTimeSlot(schedule.getPickupWindow())
                .estimatedDeliveryAt(providerResult.getEstimatedDeliveryAt())
                .build();
        shipmentRepository.save(shipment);
        appendEvent(
                shipment.getId(),
                ShipmentStatus.CREATED,
                "Return pickup scheduled",
                null,
                "evt_return_created_" + shipment.getId(),
                null);

        if (!BookingStatus.RETURN_SCHEDULED.equals(booking.getStatus())) {
            BookingStatusTransitions.assertTransition(booking.getStatus(), BookingStatus.RETURN_SCHEDULED);
            booking.setStatus(BookingStatus.RETURN_SCHEDULED);
            bookingRepository.save(booking);
        }
        timelineService.append(
                booking.getId(),
                actorId,
                BookingStatus.RETURN_SCHEDULED,
                "Return pickup scheduled for " + schedule.getPickupDate() + " (" + schedule.getPickupWindow() + ")");

        notificationDispatchService.returnScheduled(booking);

        return toShipmentResponse(shipment, provider.getCode());
    }

    private ReturnScheduleResponse toReturnScheduleResponse(Booking booking, Shipment shipment, boolean alreadyScheduled) {
        LocalDate pickupDate = shipment.getPickupScheduledAt() != null
                ? shipment.getPickupScheduledAt().atZone(BUSINESS_ZONE).toLocalDate()
                : booking.getRentalEndDate();

        return ReturnScheduleResponse.builder()
                .status(booking.getStatus())
                .shipmentId(shipment.getId())
                .returnReference(shipment.getTrackingNumber())
                .pickupDate(pickupDate)
                .pickupWindow(formatPickupWindow(shipment.getPickupTimeSlot()))
                .pickupScheduledAt(shipment.getPickupScheduledAt())
                .alreadyScheduled(alreadyScheduled)
                .build();
    }

    private String formatPickupWindow(String slot) {
        if (slot == null || slot.isBlank()) {
            return null;
        }
        return slot.replace("-", " – ").replace(":", ":");
    }

    private UUID resolveCustomerAddress(Booking booking, UUID actorId, UUID addressId) {
        if (addressId != null) {
            addressRepository.findByIdAndUserIdAndDeletedAtIsNull(addressId, booking.getCustomerId())
                    .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Address not found"));
            return addressId;
        }
        if (booking.getDeliveryAddressId() != null) {
            return booking.getDeliveryAddressId();
        }
        throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Pickup address is required");
    }

    private CreateShipmentResult callGateway(CreateShipmentCommand command) {
        try {
            return logisticsGateway.createShipment(command);
        } catch (Exception ex) {
            log.warn("Logistics provider unavailable for booking {}", command.getBookingId(), ex);
            throw new ClosiqException(ErrorCode.LOGISTICS_PROVIDER_UNAVAILABLE);
        }
    }

    private LogisticsProvider requireShadowfaxProvider() {
        return logisticsProviderRepository.findByCode(SHADOWFAX_CODE)
                .orElseThrow(() -> new ClosiqException(ErrorCode.LOGISTICS_PROVIDER_UNAVAILABLE));
    }

    private Shipment findShipment(UUID bookingId, String type) {
        return shipmentRepository.findByBookingIdAndShipmentType(bookingId, type)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Shipment not found"));
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return ShipmentType.OUTBOUND;
        }
        return type.toUpperCase();
    }

    private void refreshAgentInfoIfCached(Shipment shipment) {
        if (shipment.getProviderShipmentId() == null) {
            return;
        }
        String cacheKey = "shipment:track:" + shipment.getId();
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                TrackingSnapshot snapshot = objectMapper.readValue(cached, TrackingSnapshot.class);
                applyTrackingSnapshot(shipment, snapshot);
                return;
            }

            TrackingSnapshot snapshot = logisticsGateway.fetchTracking(shipment.getProviderShipmentId());
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(snapshot), TRACK_CACHE_TTL);
            applyTrackingSnapshot(shipment, snapshot);
        } catch (JsonProcessingException ex) {
            log.debug("Unable to cache shipment tracking for {}", shipment.getId(), ex);
        }
    }

    private void applyTrackingSnapshot(Shipment shipment, TrackingSnapshot snapshot) {
        if (snapshot.getAgentName() != null) {
            shipment.setAgentName(snapshot.getAgentName());
        }
        if (snapshot.getAgentPhoneMasked() != null) {
            shipment.setAgentPhoneMasked(snapshot.getAgentPhoneMasked());
        }
        if (snapshot.getEstimatedDeliveryAt() != null) {
            shipment.setEstimatedDeliveryAt(snapshot.getEstimatedDeliveryAt());
        }
    }

    ShipmentEvent appendEvent(
            UUID shipmentId,
            String status,
            String label,
            String location,
            String providerEventId,
            Map<String, Object> rawPayload) {

        return shipmentEventRepository.save(ShipmentEvent.builder()
                .shipmentId(shipmentId)
                .status(status)
                .label(label)
                .location(location)
                .occurredAt(Instant.now())
                .providerEventId(providerEventId)
                .rawPayload(rawPayload)
                .build());
    }

    private ShipmentEventResponse toEventResponse(ShipmentEvent event) {
        return ShipmentEventResponse.builder()
                .status(event.getStatus())
                .label(event.getLabel())
                .timestamp(event.getOccurredAt())
                .location(event.getLocation())
                .build();
    }

    private ShipmentResponse toShipmentResponse(Shipment shipment, String providerCode) {
        return ShipmentResponse.builder()
                .shipmentId(shipment.getId())
                .bookingId(shipment.getBookingId())
                .type(shipment.getShipmentType())
                .provider(providerCode)
                .trackingNumber(shipment.getTrackingNumber())
                .status(shipment.getStatus())
                .pickupScheduledAt(shipment.getPickupScheduledAt())
                .pickupTimeSlot(shipment.getPickupTimeSlot())
                .estimatedDeliveryAt(shipment.getEstimatedDeliveryAt())
                .build();
    }
}

package com.closiq.booking.repository;

import com.closiq.booking.domain.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BookingItemRepository extends JpaRepository<BookingItem, UUID> {

    Optional<BookingItem> findByBookingId(UUID bookingId);

    @Query(value = """
            SELECT COUNT(DISTINCT b.id) FROM booking b
            INNER JOIN booking_item bi ON bi.booking_id = b.id
            WHERE bi.product_id = :productId
            AND b.status NOT IN ('CANCELLED', 'COMPLETED', 'DEPOSIT_REFUNDED', 'REFUND_PENDING')
            AND b.rental_end_date >= :today
            """, nativeQuery = true)
    long countActiveFutureBookingsForProduct(
            @Param("productId") UUID productId,
            @Param("today") java.time.LocalDate today);
}

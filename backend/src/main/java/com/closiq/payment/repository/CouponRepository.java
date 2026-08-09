package com.closiq.payment.repository;

import com.closiq.payment.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    Optional<Coupon> findByCodeIgnoreCaseAndStatus(String code, String status);
}

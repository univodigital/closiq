package com.closiq.payment.service;

import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.payment.domain.Coupon;
import com.closiq.payment.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CouponService {

    private static final String ACTIVE = "ACTIVE";
    private static final String FIXED = "FIXED";
    private static final String PERCENTAGE = "PERCENTAGE";

    private final CouponRepository couponRepository;

    @Transactional(readOnly = true)
    public CouponValidation validate(String code, long orderSubtotalRupees) {
        if (code == null || code.isBlank()) {
            throw new ClosiqException(ErrorCode.COUPON_INVALID);
        }

        Coupon coupon = couponRepository.findByCodeIgnoreCaseAndStatus(code.trim(), ACTIVE)
                .orElseThrow(() -> new ClosiqException(ErrorCode.COUPON_INVALID));

        Instant now = Instant.now();
        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidUntil())) {
            throw new ClosiqException(ErrorCode.COUPON_INVALID, "Coupon has expired");
        }

        if (coupon.getUsageLimit() != null && coupon.getUsageCount() >= coupon.getUsageLimit()) {
            throw new ClosiqException(ErrorCode.COUPON_INVALID, "Coupon usage limit reached");
        }

        if (orderSubtotalRupees < coupon.getMinOrderAmount()) {
            throw new ClosiqException(ErrorCode.COUPON_INVALID, "Order does not meet minimum amount for coupon");
        }

        long discount = computeDiscount(coupon, orderSubtotalRupees);
        return new CouponValidation(coupon.getCode(), discount, "₹" + discount + " off applied");
    }

    public long computeDiscount(Coupon coupon, long orderSubtotalRupees) {
        long discount = switch (coupon.getDiscountType()) {
            case FIXED -> coupon.getDiscountValue();
            case PERCENTAGE -> orderSubtotalRupees * coupon.getDiscountValue() / 100;
            default -> 0;
        };
        if (coupon.getMaxDiscountAmount() != null) {
            discount = Math.min(discount, coupon.getMaxDiscountAmount());
        }
        return Math.min(discount, orderSubtotalRupees);
    }

    public record CouponValidation(String code, long discountAmount, String message) {
    }
}

package com.closiq.payment.service;

import com.closiq.payment.domain.Coupon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @InjectMocks
    private CouponService couponService;

    @Test
    void computeDiscount_fixedCoupon() {
        Coupon coupon = Coupon.builder()
                .discountType("FIXED")
                .discountValue(500)
                .build();

        assertThat(couponService.computeDiscount(coupon, 7000)).isEqualTo(500);
    }

    @Test
    void computeDiscount_cappedByOrderTotal() {
        Coupon coupon = Coupon.builder()
                .discountType("FIXED")
                .discountValue(500)
                .build();

        assertThat(couponService.computeDiscount(coupon, 300)).isEqualTo(300);
    }
}

package com.closiq.seller.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.config.ClosiqProperties;
import com.closiq.seller.domain.Wallet;
import com.closiq.seller.repository.WalletRepository;
import com.closiq.seller.repository.WalletTransactionRepository;
import com.closiq.user.domain.SellerProfile;
import com.closiq.user.repository.SellerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerEarningServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Mock
    private SellerProfileRepository sellerProfileRepository;

    private SellerEarningService earningService;

    @BeforeEach
    void setUp() {
        ClosiqProperties properties = new ClosiqProperties();
        properties.getBooking().setCommissionRateBps(1500);
        earningService = new SellerEarningService(
                walletRepository, walletTransactionRepository, sellerProfileRepository, properties);
    }

    @Test
    void creditsEarningOnceOnCompletion() {
        UUID sellerId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .rentalNumber("CLQ-1001")
                .sellerProfileId(sellerId)
                .status(BookingStatus.COMPLETED)
                .rentalAmount(3000)
                .currencyCode("INR")
                .build();
        SellerProfile seller = SellerProfile.builder().id(sellerId).build();
        Wallet wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .sellerProfile(seller)
                .availableBalance(0)
                .pendingBalance(0)
                .totalEarned(0)
                .totalWithdrawn(0)
                .currencyCode("INR")
                .build();

        when(walletTransactionRepository.existsByReferenceTypeAndReferenceIdAndTxnType(
                        any(), any(), any()))
                .thenReturn(false, true);
        when(sellerProfileRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        when(walletRepository.findBySellerProfileId(sellerId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        earningService.creditRentalEarningIfEligible(booking);
        earningService.creditRentalEarningIfEligible(booking);

        assertThat(wallet.getAvailableBalance()).isEqualTo(2550);
        verify(walletTransactionRepository).save(any());
    }

    @Test
    void skipsWhenAlreadyCredited() {
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .sellerProfileId(UUID.randomUUID())
                .status(BookingStatus.COMPLETED)
                .rentalAmount(3000)
                .build();

        when(walletTransactionRepository.existsByReferenceTypeAndReferenceIdAndTxnType(
                        any(), any(), any()))
                .thenReturn(true);

        earningService.creditRentalEarningIfEligible(booking);

        verify(walletRepository, never()).save(any());
    }
}

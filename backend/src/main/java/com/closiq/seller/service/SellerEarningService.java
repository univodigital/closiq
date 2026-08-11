package com.closiq.seller.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.repository.BookingRepository;
import com.closiq.booking.service.BookingTimelineService;
import com.closiq.common.util.IdGenerator;
import com.closiq.config.ClosiqProperties;
import com.closiq.inventory.service.InventoryHoldService;
import com.closiq.payment.service.RefundService;
import com.closiq.seller.domain.Wallet;
import com.closiq.seller.domain.WalletTransaction;
import com.closiq.seller.repository.WalletRepository;
import com.closiq.seller.repository.WalletTransactionRepository;
import com.closiq.user.domain.SellerProfile;
import com.closiq.user.repository.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerEarningService {

    private static final String REFERENCE_TYPE_BOOKING = "BOOKING";
    private static final String TXN_CREDIT_EARNING = "CREDIT_EARNING";

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final ClosiqProperties properties;

    @Transactional
    public void creditRentalEarningIfEligible(Booking booking) {
        if (booking.getSellerProfileId() == null) {
            return;
        }
        if (!BookingStatus.COMPLETED.equals(booking.getStatus())) {
            return;
        }

        String referenceId = booking.getId().toString();
        if (walletTransactionRepository.existsByReferenceTypeAndReferenceIdAndTxnType(
                REFERENCE_TYPE_BOOKING, referenceId, TXN_CREDIT_EARNING)) {
            log.debug("Seller earning already credited for booking {}", booking.getRentalNumber());
            return;
        }

        double commissionRate = properties.getBooking().getCommissionRateBps() / 10_000.0;
        long commission = Math.round(booking.getRentalAmount() * commissionRate);
        long netEarning = booking.getRentalAmount() - commission;
        if (netEarning <= 0) {
            return;
        }

        SellerProfile seller = sellerProfileRepository.findById(booking.getSellerProfileId()).orElse(null);
        if (seller == null) {
            return;
        }

        Wallet wallet = walletRepository.findBySellerProfileId(seller.getId()).orElseGet(() -> {
            Wallet created = Wallet.builder()
                    .id(IdGenerator.uuidV7())
                    .sellerProfile(seller)
                    .availableBalance(0)
                    .pendingBalance(0)
                    .totalEarned(0)
                    .totalWithdrawn(0)
                    .currencyCode(booking.getCurrencyCode())
                    .build();
            return walletRepository.save(created);
        });

        wallet.setAvailableBalance(wallet.getAvailableBalance() + netEarning);
        wallet.setTotalEarned(wallet.getTotalEarned() + netEarning);
        wallet.setLastSettledAt(Instant.now());
        walletRepository.save(wallet);

        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .txnType(TXN_CREDIT_EARNING)
                .amount(netEarning)
                .balanceAfter(wallet.getAvailableBalance())
                .referenceType(REFERENCE_TYPE_BOOKING)
                .referenceId(referenceId)
                .description("Rental earning — " + bookingLabel(booking))
                .status("COMPLETED")
                .build());
    }

    private String bookingLabel(Booking booking) {
        if (booking.getOrderNumber() != null) {
            return booking.getOrderNumber();
        }
        return booking.getRentalNumber();
    }
}

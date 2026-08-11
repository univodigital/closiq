package com.closiq.seller.service;

import com.closiq.common.exception.ClosiqException;
import com.closiq.notification.service.NotificationDispatchService;
import com.closiq.seller.domain.BankAccount;
import com.closiq.seller.domain.PayoutRequest;
import com.closiq.seller.domain.Wallet;
import com.closiq.seller.repository.BankAccountRepository;
import com.closiq.seller.repository.PayoutRequestRepository;
import com.closiq.seller.repository.WalletRepository;
import com.closiq.seller.repository.WalletTransactionRepository;
import com.closiq.seller.web.dto.RequestPayoutRequest;
import com.closiq.user.domain.SellerProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerWalletServiceTest {

    @Mock private SellerContextService sellerContextService;
    @Mock private WalletRepository walletRepository;
    @Mock private WalletTransactionRepository walletTransactionRepository;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private PayoutRequestRepository payoutRequestRepository;
    @Mock private NotificationDispatchService notificationDispatchService;

    @InjectMocks private SellerWalletService sellerWalletService;

    private SellerProfile seller;
    private Wallet wallet;
    private BankAccount bankAccount;

    @BeforeEach
    void setUp() {
        seller = SellerProfile.builder().id(UUID.randomUUID()).build();
        wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .sellerProfile(seller)
                .availableBalance(8_500)
                .pendingBalance(0)
                .totalEarned(25_000)
                .totalWithdrawn(0)
                .currencyCode("INR")
                .build();
        bankAccount = BankAccount.builder()
                .id(UUID.randomUUID())
                .sellerProfile(seller)
                .status("VERIFIED")
                .bankName("HDFC")
                .accountNumberLast4("1234")
                .build();
    }

    @Test
    void requestPayout_reservesAvailableBalance() {
        when(sellerContextService.requireVerifiedSeller(any())).thenReturn(seller);
        when(walletRepository.findBySellerProfileId(seller.getId())).thenReturn(Optional.of(wallet));
        when(bankAccountRepository.findByIdAndSellerProfileId(bankAccount.getId(), seller.getId()))
                .thenReturn(Optional.of(bankAccount));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(payoutRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        sellerWalletService.requestPayout(
                UUID.randomUUID(),
                new RequestPayoutRequest(5_000L, bankAccount.getId().toString()),
                "idem-1");

        assertThat(wallet.getAvailableBalance()).isEqualTo(3_500);
        assertThat(wallet.getPendingBalance()).isEqualTo(5_000);

        ArgumentCaptor<PayoutRequest> payoutCaptor = ArgumentCaptor.forClass(PayoutRequest.class);
        verify(payoutRequestRepository).save(payoutCaptor.capture());
        assertThat(payoutCaptor.getValue().getStatus()).isEqualTo("PROCESSING");
    }

    @Test
    void requestPayout_rejectsAmountAboveAvailable() {
        when(sellerContextService.requireVerifiedSeller(any())).thenReturn(seller);
        when(walletRepository.findBySellerProfileId(seller.getId())).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> sellerWalletService.requestPayout(
                        UUID.randomUUID(),
                        new RequestPayoutRequest(9_000L, bankAccount.getId().toString()),
                        null))
                .isInstanceOf(ClosiqException.class);
    }
}

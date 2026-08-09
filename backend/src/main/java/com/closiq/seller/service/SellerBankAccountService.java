package com.closiq.seller.service;

import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.util.IdGenerator;
import com.closiq.user.domain.SellerProfile;
import com.closiq.seller.domain.BankAccount;
import com.closiq.seller.repository.BankAccountRepository;
import com.closiq.seller.web.dto.AddBankAccountRequest;
import com.closiq.seller.web.dto.BankAccountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerBankAccountService {

    private static final String INACTIVE = "INACTIVE";
    private static final String PENDING = "PENDING_VERIFICATION";

    private final SellerContextService sellerContextService;
    private final BankAccountRepository bankAccountRepository;

    @Transactional(readOnly = true)
    public List<BankAccountResponse> listBankAccounts(UUID userId) {
        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);
        return bankAccountRepository
                .findBySellerProfileIdAndStatusNotOrderByIsDefaultDescCreatedAtAsc(seller.getId(), INACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BankAccountResponse addBankAccount(UUID userId, AddBankAccountRequest request) {
        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);

        boolean isFirst = bankAccountRepository.countBySellerProfileIdAndStatusNot(seller.getId(), INACTIVE) == 0;
        if (isFirst) {
            bankAccountRepository.clearDefaultForSeller(seller.getId());
        }

        String last4 = request.getAccountNumber().substring(request.getAccountNumber().length() - 4);
        BankAccount account = BankAccount.builder()
                .id(IdGenerator.uuidV7())
                .sellerProfile(seller)
                .accountHolderName(request.getAccountHolderName())
                .accountNumberEnc(encodeAccountNumber(request.getAccountNumber()))
                .accountNumberLast4(last4)
                .ifscCode(request.getIfscCode().toUpperCase())
                .bankName(request.getBankName())
                .status(PENDING)
                .isDefault(isFirst)
                .build();

        bankAccountRepository.save(account);
        return toResponse(account);
    }

    @Transactional
    public void deleteBankAccount(UUID userId, UUID accountId) {
        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);
        BankAccount account = bankAccountRepository.findByIdAndSellerProfileId(accountId, seller.getId())
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Bank account not found"));

        long activeCount = bankAccountRepository.countBySellerProfileIdAndStatusNot(seller.getId(), INACTIVE);
        if (account.isDefault() && activeCount > 1) {
            throw new ClosiqException(
                    ErrorCode.VALIDATION_ERROR,
                    "Assign another default account before deleting the default account");
        }

        account.setStatus(INACTIVE);
        account.setDefault(false);
        bankAccountRepository.save(account);
    }

    private BankAccountResponse toResponse(BankAccount account) {
        String bank = account.getBankName() != null ? account.getBankName() : "Bank";
        return BankAccountResponse.builder()
                .id(account.getId().toString())
                .label(bank + " ···" + account.getAccountNumberLast4())
                .isDefault(account.isDefault())
                .status(account.getStatus())
                .build();
    }

    private String encodeAccountNumber(String accountNumber) {
        return Base64.getEncoder().encodeToString(accountNumber.getBytes(StandardCharsets.UTF_8));
    }
}

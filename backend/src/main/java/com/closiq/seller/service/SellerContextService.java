package com.closiq.seller.service;

import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.security.RoleType;
import com.closiq.identity.service.UserService;
import com.closiq.user.domain.SellerProfile;
import com.closiq.user.repository.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerContextService {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final SellerProfileRepository sellerProfileRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public SellerProfile requireVerifiedSeller(UUID userId) {
        SellerProfile profile = sellerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ClosiqException(
                        ErrorCode.SELLER_NOT_VERIFIED, ErrorCode.SELLER_NOT_VERIFIED.getDefaultDetail()));

        if (!ACTIVE_STATUS.equals(profile.getStatus())) {
            throw new ClosiqException(
                    ErrorCode.SELLER_NOT_VERIFIED, ErrorCode.SELLER_NOT_VERIFIED.getDefaultDetail());
        }
        return profile;
    }

    @Transactional(readOnly = true)
    public void ensureNotSeller(UUID userId) {
        if (userService.getUserRoles(userId).contains(RoleType.SELLER)) {
            throw new ClosiqException(ErrorCode.FORBIDDEN, "User is already a seller");
        }
    }
}

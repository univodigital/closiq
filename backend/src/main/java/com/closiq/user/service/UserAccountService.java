package com.closiq.user.service;

import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.security.RoleType;
import com.closiq.identity.domain.User;
import com.closiq.identity.domain.UserStatus;
import com.closiq.identity.repository.UserRepository;
import com.closiq.identity.service.RefreshTokenService;
import com.closiq.identity.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public void deleteAccount(UUID userId) {
        User user = userService.requireActiveUser(userId);

        if (userService.getUserRoles(userId).contains(RoleType.ADMIN)) {
            throw new ClosiqException(ErrorCode.FORBIDDEN, "Admin accounts cannot be self-deleted");
        }

        user.setStatus(UserStatus.DELETED);
        user.setDeletedAt(Instant.now());
        user.setUpdatedBy(userId);
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(userId);
    }
}

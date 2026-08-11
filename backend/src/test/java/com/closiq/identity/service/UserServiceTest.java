package com.closiq.identity.service;

import com.closiq.common.exception.ClosiqException;
import com.closiq.common.exception.ErrorCode;
import com.closiq.identity.domain.User;
import com.closiq.identity.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private com.closiq.identity.repository.UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void requireLoginEligible_activeUser_passes() {
        User user = activeUser();
        assertThat(userService.requireLoginEligible(user)).isSameAs(user);
    }

    @Test
    void requireLoginEligible_suspendedUser_rejected() {
        User user = activeUser();
        user.setStatus(UserStatus.SUSPENDED);

        assertThatThrownBy(() -> userService.requireLoginEligible(user))
                .isInstanceOf(ClosiqException.class)
                .hasMessageContaining("suspended")
                .extracting(ex -> ((ClosiqException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void requireLoginEligible_deletedUser_rejected() {
        User user = activeUser();
        user.setDeletedAt(Instant.now());
        user.setStatus(UserStatus.DELETED);

        assertThatThrownBy(() -> userService.requireLoginEligible(user))
                .isInstanceOf(ClosiqException.class)
                .hasMessageContaining("deleted")
                .extracting(ex -> ((ClosiqException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void requireVerifiedUserForLogin_deletedAccount_returnsDeletedMessage() {
        User deleted = activeUser();
        deleted.setDeletedAt(Instant.now());
        deleted.setStatus(UserStatus.DELETED);

        when(userRepository.findByPhoneAndDeletedAtIsNull("+919876543210")).thenReturn(Optional.empty());
        when(userRepository.findFirstByPhone("+919876543210")).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> userService.requireVerifiedUserForLogin(
                new AuthIdentifierResolver.ResolvedIdentifier(
                        AuthIdentifierResolver.Type.PHONE, "+919876543210", null)))
                .isInstanceOf(ClosiqException.class)
                .hasMessageContaining("deleted")
                .extracting(ex -> ((ClosiqException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    private User activeUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .phone("+919876543210")
                .phoneVerified(true)
                .status(UserStatus.ACTIVE)
                .build();
    }
}

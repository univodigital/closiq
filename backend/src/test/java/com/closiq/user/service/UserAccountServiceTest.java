package com.closiq.user.service;

import com.closiq.booking.repository.BookingRepository;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.security.RoleType;
import com.closiq.identity.domain.User;
import com.closiq.identity.domain.UserStatus;
import com.closiq.identity.repository.UserRepository;
import com.closiq.identity.service.RefreshTokenService;
import com.closiq.identity.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserService userService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private BookingRepository bookingRepository;

    @InjectMocks
    private UserAccountService userAccountService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void previewDeleteAccount_blocksWhenActiveBookingsExist() {
        when(userService.requireActiveUser(userId)).thenReturn(activeUser());
        when(bookingRepository.countByCustomerIdAndStatusIn(eq(userId), anyList())).thenReturn(2L);

        var preview = userAccountService.previewDeleteAccount(userId);

        assertThat(preview.getActiveBookings()).isEqualTo(2);
        assertThat(preview.isCanDelete()).isFalse();
    }

    @Test
    void deleteAccount_rejectsWhenActiveBookingsExist() {
        when(userService.requireActiveUser(userId)).thenReturn(activeUser());
        when(userService.getUserRoles(userId)).thenReturn(List.of(RoleType.CUSTOMER));
        when(bookingRepository.countByCustomerIdAndStatusIn(eq(userId), anyList())).thenReturn(1L);

        assertThatThrownBy(() -> userAccountService.deleteAccount(userId))
                .isInstanceOf(ClosiqException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_STATE_TRANSITION);

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteAccount_softDeletesAndRevokesTokens() {
        User user = activeUser();
        when(userService.requireActiveUser(userId)).thenReturn(user);
        when(userService.getUserRoles(userId)).thenReturn(List.of(RoleType.CUSTOMER));
        when(bookingRepository.countByCustomerIdAndStatusIn(eq(userId), anyList())).thenReturn(0L);

        userAccountService.deleteAccount(userId);

        assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(user.getDeletedAt()).isNotNull();
        verify(refreshTokenService).revokeAllForUser(userId);
        verify(userRepository).save(user);
    }

    private User activeUser() {
        return User.builder()
                .id(userId)
                .phone("+919876543210")
                .status(UserStatus.ACTIVE)
                .build();
    }
}

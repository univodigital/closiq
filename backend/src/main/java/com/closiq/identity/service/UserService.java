package com.closiq.identity.service;

import com.closiq.common.security.RoleType;
import com.closiq.common.security.UserPrincipal;
import com.closiq.common.identifier.UserCodeGenerator;
import com.closiq.common.util.IdGenerator;
import com.closiq.identity.domain.Role;
import com.closiq.identity.domain.User;
import com.closiq.identity.domain.UserProfile;
import com.closiq.identity.domain.UserRole;
import com.closiq.identity.domain.UserRoleId;
import com.closiq.identity.domain.UserStatus;
import com.closiq.identity.repository.RoleRepository;
import com.closiq.identity.repository.UserProfileRepository;
import com.closiq.identity.repository.UserRepository;
import com.closiq.identity.repository.UserRoleRepository;
import com.closiq.user.repository.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final UserCodeGenerator userCodeGenerator;

    @Transactional(readOnly = true)
    public User requireActiveUser(UUID userId) {
        return userRepository.findByIdAndStatusAndDeletedAtIsNull(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new com.closiq.common.exception.ClosiqException(
                        com.closiq.common.exception.ErrorCode.NOT_FOUND, "User not found"));
    }

    @Transactional(readOnly = true)
    public UserProfile requireProfile(UUID userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new com.closiq.common.exception.ClosiqException(
                        com.closiq.common.exception.ErrorCode.NOT_FOUND, "User profile not found"));
    }

    @Transactional(readOnly = true)
    public List<RoleType> getUserRoles(UUID userId) {
        return userRoleRepository.findByUserIdWithRole(userId).stream()
                .map(userRole -> RoleType.valueOf(userRole.getRole().getCode()))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserPrincipal buildPrincipal(User user) {
        List<RoleType> roles = getUserRoles(user.getId());
        UUID sellerId = null;
        if (roles.contains(RoleType.SELLER)) {
            sellerId = sellerProfileRepository.findByUserId(user.getId())
                    .map(com.closiq.user.domain.SellerProfile::getId)
                    .orElse(null);
        }
        return new UserPrincipal(user.getId(), roles, user.isPhoneVerified(), sellerId);
    }

    @Transactional
    public User createUserWithProfile(String phone, String firstName, String lastName, String email) {
        User user = User.builder()
                .id(IdGenerator.uuidV7())
                .userCode(userCodeGenerator.nextCode())
                .phone(phone)
                .phoneVerified(true)
                .email(email)
                .emailVerified(false)
                .status(UserStatus.ACTIVE)
                .build();
        user = userRepository.save(user);

        UserProfile profile = UserProfile.builder()
                .user(user)
                .firstName(firstName)
                .lastName(lastName)
                .displayName(buildDisplayName(firstName, lastName))
                .build();
        userProfileRepository.save(profile);

        assignRole(user, RoleType.CUSTOMER);
        return user;
    }

    @Transactional
    public User createUserWithUsername(String phone, String username, String passwordHash, String email) {
        User user = User.builder()
                .id(IdGenerator.uuidV7())
                .userCode(userCodeGenerator.nextCode())
                .phone(phone)
                .phoneVerified(true)
                .email(email)
                .emailVerified(false)
                .passwordHash(passwordHash)
                .status(UserStatus.ACTIVE)
                .build();
        user = userRepository.save(user);

        UserProfile profile = UserProfile.builder()
                .user(user)
                .username(username)
                .firstName(username)
                .lastName("")
                .displayName(username)
                .build();
        userProfileRepository.save(profile);

        assignRole(user, RoleType.CUSTOMER);
        return user;
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userProfileRepository.findByUsernameIgnoreCase(username)
                .map(UserProfile::getUser)
                .filter(user -> user.getDeletedAt() == null && user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new com.closiq.common.exception.ClosiqException(
                        com.closiq.common.exception.ErrorCode.UNAUTHORIZED,
                        "Invalid phone/username or password"));
    }

    @Transactional(readOnly = true)
    public boolean usernameExists(String username) {
        return userProfileRepository.existsByUsernameIgnoreCase(username);
    }

    @Transactional
    public void assignRole(User user, RoleType roleType) {
        Role role = roleRepository.findByCode(roleType.name())
                .orElseThrow(() -> new IllegalStateException("Role not seeded: " + roleType));

        UserRoleId id = new UserRoleId(user.getId(), role.getId());
        if (userRoleRepository.existsById(id)) {
            return;
        }

        UserRole userRole = UserRole.builder()
                .id(id)
                .user(user)
                .role(role)
                .build();
        userRoleRepository.save(userRole);
    }

    public static String buildDisplayName(String firstName, String lastName) {
        if (lastName == null || lastName.isBlank()) {
            return firstName;
        }
        return firstName + " " + lastName.charAt(0) + ".";
    }
}

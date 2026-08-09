package com.closiq.admin.service;

import com.closiq.admin.web.dto.AdminUserDetailResponse;
import com.closiq.admin.web.dto.AdminUserListItemResponse;
import com.closiq.admin.web.dto.CreateAdminUserRequest;
import com.closiq.admin.web.dto.UpdateAdminUserRequest;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.security.RoleType;
import com.closiq.common.web.PageBoundary;
import com.closiq.common.web.PageTokenCodec;
import com.closiq.common.web.PagedResult;
import com.closiq.identity.domain.User;
import com.closiq.identity.domain.UserProfile;
import com.closiq.identity.domain.UserStatus;
import com.closiq.identity.repository.RoleRepository;
import com.closiq.identity.repository.UserProfileRepository;
import com.closiq.identity.repository.UserRepository;
import com.closiq.identity.repository.UserRoleRepository;
import com.closiq.identity.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public PagedResult<AdminUserListItemResponse> listUsers(String status, String pageToken, Integer limit) {
        int pageSize = clampLimit(limit);
        UserStatus userStatus = parseStatus(status);
        PageBoundary boundary = PageTokenCodec.userBoundary(pageToken);

        Specification<User> spec = Specification.<User>where((root, query, cb) -> cb.isNull(root.get("deletedAt")))
                .and(boundary.createdBefore());
        if (userStatus != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), userStatus));
        }

        List<User> users = userRepository.findAll(
                spec,
                PageRequest.of(0, pageSize + 1, Sort.by(Sort.Direction.DESC, "createdAt", "id")))
                .getContent();

        boolean hasMore = users.size() > pageSize;
        List<User> page = hasMore ? users.subList(0, pageSize) : users;

        List<AdminUserListItemResponse> items = page.stream()
                .map(this::toListItem)
                .toList();

        String nextPageToken = null;
        if (hasMore && !page.isEmpty()) {
            User last = page.get(page.size() - 1);
            nextPageToken = PageTokenCodec.encodeUser(new PageTokenCodec.UserPageToken(last.getCreatedAt(), last.getId()));
        }

        return PagedResult.of(items, pageSize, hasMore, nextPageToken);
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUser(UUID userId) {
        User user = requireUser(userId);
        UserProfile profile = userService.requireProfile(userId);
        List<String> roles = userService.getUserRoles(userId).stream().map(Enum::name).toList();

        return AdminUserDetailResponse.builder()
                .id(user.getId().toString())
                .userCode(user.getUserCode())
                .phone(user.getPhone())
                .phoneVerified(user.isPhoneVerified())
                .email(user.getEmail())
                .emailVerified(user.isEmailVerified())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .displayName(profile.getDisplayName())
                .status(user.getStatus().name())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    @Transactional
    public AdminUserDetailResponse createUser(CreateAdminUserRequest request) {
        if (userRepository.existsByPhoneAndPhoneVerifiedTrueAndDeletedAtIsNull(request.getPhone())) {
            throw new ClosiqException(ErrorCode.ALREADY_EXISTS, "Phone number is already registered");
        }

        User user = userService.createUserWithProfile(
                request.getPhone(),
                request.getFirstName(),
                request.getLastName(),
                request.getEmail());

        applyRoles(user, request.getRoles());
        return getUser(user.getId());
    }

    @Transactional
    public AdminUserDetailResponse updateUser(UUID userId, UpdateAdminUserRequest request, UUID adminId) {
        if (userId.equals(adminId) && request.getStatus() != null && !UserStatus.ACTIVE.name().equals(request.getStatus())) {
            throw new ClosiqException(ErrorCode.FORBIDDEN, "Cannot suspend or delete your own admin account");
        }

        User user = requireUser(userId);

        if (request.getStatus() != null) {
            user.setStatus(UserStatus.valueOf(request.getStatus()));
            user.setUpdatedBy(adminId);
            userRepository.save(user);
        }

        if (request.getRoles() != null) {
            if (userId.equals(adminId) && !request.getRoles().contains(RoleType.ADMIN.name())) {
                throw new ClosiqException(ErrorCode.FORBIDDEN, "Cannot remove your own admin role");
            }
            syncRoles(user, request.getRoles());
        }

        return getUser(userId);
    }

    @Transactional
    public void deleteUser(UUID userId, UUID adminId) {
        if (userId.equals(adminId)) {
            throw new ClosiqException(ErrorCode.FORBIDDEN, "Cannot delete your own admin account");
        }

        User user = requireUser(userId);
        user.setStatus(UserStatus.DELETED);
        user.setDeletedAt(Instant.now());
        user.setUpdatedBy(adminId);
        userRepository.save(user);
    }

    private AdminUserListItemResponse toListItem(User user) {
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        List<String> roles = userService.getUserRoles(user.getId()).stream().map(Enum::name).toList();

        return AdminUserListItemResponse.builder()
                .id(user.getId().toString())
                .userCode(user.getUserCode())
                .phone(user.getPhone())
                .email(user.getEmail())
                .firstName(profile != null ? profile.getFirstName() : "")
                .lastName(profile != null ? profile.getLastName() : "")
                .displayName(profile != null ? profile.getDisplayName() : user.getPhone())
                .status(user.getStatus().name())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private void applyRoles(User user, List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return;
        }
        for (String roleCode : roles) {
            if (RoleType.CUSTOMER.name().equals(roleCode)) {
                continue;
            }
            userService.assignRole(user, RoleType.valueOf(roleCode));
        }
    }

    private void syncRoles(User user, List<String> desiredRoles) {
        EnumSet<RoleType> desired = EnumSet.noneOf(RoleType.class);
        for (String roleCode : desiredRoles) {
            desired.add(RoleType.valueOf(roleCode));
        }
        desired.add(RoleType.CUSTOMER);

        List<RoleType> current = userService.getUserRoles(user.getId());
        for (RoleType role : current) {
            if (!desired.contains(role)) {
                roleRepository.findByCode(role.name()).ifPresent(r ->
                        userRoleRepository.deleteByIdUserIdAndIdRoleId(user.getId(), r.getId()));
            }
        }
        for (RoleType role : desired) {
            userService.assignRole(user, role);
        }
    }

    private User requireUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "User not found"));
    }

    private UserStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return UserStatus.valueOf(status);
    }

    private int clampLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}

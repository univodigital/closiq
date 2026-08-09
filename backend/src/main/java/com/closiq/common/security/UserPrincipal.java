package com.closiq.common.security;

import java.util.List;
import java.util.UUID;

public record UserPrincipal(
        UUID userId,
        List<RoleType> roles,
        boolean phoneVerified,
        UUID sellerId) {

    public boolean hasRole(RoleType role) {
        return roles.contains(role);
    }
}

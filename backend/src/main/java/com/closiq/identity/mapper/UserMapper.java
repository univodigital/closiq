package com.closiq.identity.mapper;

import com.closiq.common.security.RoleType;
import com.closiq.identity.domain.User;
import com.closiq.identity.domain.UserProfile;
import com.closiq.identity.web.dto.UserSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", source = "user.id", qualifiedByName = "uuidToString")
    @Mapping(target = "userCode", source = "user.userCode")
    @Mapping(target = "phone", source = "user.phone")
    @Mapping(target = "phoneVerified", source = "user.phoneVerified")
    @Mapping(target = "alternatePhone", source = "user.alternatePhone")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "emailVerified", source = "user.emailVerified")
    @Mapping(target = "alternateEmail", source = "user.alternateEmail")
    @Mapping(target = "firstName", source = "profile.firstName")
    @Mapping(target = "lastName", source = "profile.lastName")
    @Mapping(target = "displayName", source = "profile.displayName")
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "roles", source = "roles")
    @Mapping(target = "createdAt", source = "user.createdAt")
    @Mapping(target = "sellerProfile", ignore = true)
    UserSummaryResponse toSummary(User user, UserProfile profile, List<String> roles);

    default UserSummaryResponse toSummaryWithRoleTypes(User user, UserProfile profile, List<RoleType> roleTypes) {
        return toSummary(user, profile, roleTypes.stream().map(Enum::name).toList());
    }

    @Named("uuidToString")
    default String uuidToString(java.util.UUID id) {
        return id != null ? id.toString() : null;
    }
}

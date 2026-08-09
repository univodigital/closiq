package com.closiq.identity.repository;

import com.closiq.identity.domain.UserRole;
import com.closiq.identity.domain.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.role WHERE ur.id.userId = :userId")
    List<UserRole> findByUserIdWithRole(@Param("userId") UUID userId);

    void deleteByIdUserIdAndIdRoleId(UUID userId, Short roleId);
}

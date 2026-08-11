package com.closiq.identity.repository;

import com.closiq.identity.domain.User;
import com.closiq.identity.domain.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByPhoneAndDeletedAtIsNull(String phone);

    Optional<User> findFirstByPhone(String phone);

    Optional<User> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    Optional<User> findFirstByEmailIgnoreCase(String email);

    boolean existsByPhoneAndPhoneVerifiedTrueAndDeletedAtIsNull(String phone);

    Optional<User> findByIdAndDeletedAtIsNull(UUID id);

    Optional<User> findByIdAndStatusAndDeletedAtIsNull(UUID id, UserStatus status);

    long countByDeletedAtIsNull();

    long countByStatusAndDeletedAtIsNull(UserStatus status);
}

package com.closiq.user.repository;

import com.closiq.user.domain.ServiceablePincode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceablePincodeRepository extends JpaRepository<ServiceablePincode, String> {

    Optional<ServiceablePincode> findByPincodeAndStatus(String pincode, String status);
}

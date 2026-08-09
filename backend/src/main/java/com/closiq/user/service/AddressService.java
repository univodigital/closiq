package com.closiq.user.service;

import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.security.RoleType;
import com.closiq.common.util.IdGenerator;
import com.closiq.identity.domain.User;
import com.closiq.identity.domain.UserProfile;
import com.closiq.identity.repository.UserProfileRepository;
import com.closiq.identity.repository.UserRepository;
import com.closiq.identity.service.UserService;
import com.closiq.user.domain.Address;
import com.closiq.user.mapper.UserProfileMapper;
import com.closiq.user.repository.AddressRepository;
import com.closiq.user.repository.ServiceablePincodeRepository;
import com.closiq.user.web.dto.AddressResponse;
import com.closiq.user.web.dto.CreateAddressRequest;
import com.closiq.user.web.dto.UpdateAddressRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddressService {

    private static final int MAX_ADDRESSES = 10;
    private static final String ACTIVE_PINCODE = "ACTIVE";

    private final AddressRepository addressRepository;
    private final ServiceablePincodeRepository serviceablePincodeRepository;
    private final UserService userService;
    private final UserProfileMapper userProfileMapper;

    @Transactional(readOnly = true)
    public List<AddressResponse> listAddresses(UUID userId) {
        return addressRepository.findByUserIdAndDeletedAtIsNullOrderByIsDefaultDescCreatedAtAsc(userId).stream()
                .map(address -> userProfileMapper.toAddressResponse(address, isServiceable(address.getPincode())))
                .toList();
    }

    @Transactional
    public AddressResponse createAddress(UUID userId, CreateAddressRequest request) {
        if (addressRepository.countByUserIdAndDeletedAtIsNull(userId) >= MAX_ADDRESSES) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Maximum of 10 addresses allowed");
        }

        User user = userService.requireActiveUser(userId);
        boolean isDefault = Boolean.TRUE.equals(request.getIsDefault())
                || addressRepository.countByUserIdAndDeletedAtIsNull(userId) == 0;

        if (isDefault) {
            addressRepository.clearDefaultForUser(userId);
        }

        Address address = Address.builder()
                .id(IdGenerator.uuidV7())
                .user(user)
                .label(request.getLabel())
                .line1(request.getLine1())
                .line2(request.getLine2())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .phone(request.getPhone())
                .countryCode("IN")
                .isDefault(isDefault)
                .build();

        addressRepository.save(address);
        return userProfileMapper.toAddressResponse(address, isServiceable(address.getPincode()));
    }

    @Transactional
    public AddressResponse updateAddress(UUID userId, UUID addressId, UpdateAddressRequest request) {
        Address address = requireOwnedAddress(userId, addressId);

        if (request.getLabel() != null) {
            address.setLabel(request.getLabel());
        }
        if (request.getLine1() != null) {
            address.setLine1(request.getLine1());
        }
        if (request.getLine2() != null) {
            address.setLine2(request.getLine2());
        }
        if (request.getCity() != null) {
            address.setCity(request.getCity());
        }
        if (request.getState() != null) {
            address.setState(request.getState());
        }
        if (request.getPincode() != null) {
            address.setPincode(request.getPincode());
        }
        if (request.getPhone() != null) {
            address.setPhone(request.getPhone());
        }
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultForUser(userId);
            address.setDefault(true);
        } else if (Boolean.FALSE.equals(request.getIsDefault())) {
            address.setDefault(false);
        }

        addressRepository.save(address);
        return userProfileMapper.toAddressResponse(address, isServiceable(address.getPincode()));
    }

    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        Address address = requireOwnedAddress(userId, addressId);
        address.setDeletedAt(Instant.now());
        address.setDefault(false);
        addressRepository.save(address);
    }

    @Transactional(readOnly = true)
    public boolean isServiceable(String pincode) {
        return serviceablePincodeRepository.findByPincodeAndStatus(pincode, ACTIVE_PINCODE).isPresent();
    }

    private Address requireOwnedAddress(UUID userId, UUID addressId) {
        return addressRepository.findByIdAndUserIdAndDeletedAtIsNull(addressId, userId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Address not found"));
    }
}

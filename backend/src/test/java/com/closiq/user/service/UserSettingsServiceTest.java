package com.closiq.user.service;

import com.closiq.identity.repository.UserProfileRepository;
import com.closiq.identity.service.UserService;
import com.closiq.user.domain.ServiceablePincode;
import com.closiq.user.mapper.UserProfileMapper;
import com.closiq.user.repository.ServiceablePincodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSettingsServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private ServiceablePincodeRepository serviceablePincodeRepository;
    @Mock
    private UserService userService;
    @Mock
    private UserProfileMapper userProfileMapper;
    @Mock
    private UserPreferencesHelper preferencesHelper;

    @InjectMocks
    private UserSettingsService userSettingsService;

    @Test
    void checkPincode_returnsServiceableForKnownMumbaiPincode() {
        when(serviceablePincodeRepository.findByPincodeAndStatus("400026", "ACTIVE"))
                .thenReturn(Optional.of(ServiceablePincode.builder()
                        .pincode("400026")
                        .city("Mumbai")
                        .state("Maharashtra")
                        .estimatedDeliveryDays((short) 1)
                        .launchPhase("MUMBAI")
                        .status("ACTIVE")
                        .build()));

        var response = userSettingsService.checkPincode("400026");

        assertThat(response.isServiceable()).isTrue();
        assertThat(response.getCity()).isEqualTo("Mumbai");
        assertThat(response.getEstimatedDeliveryDays()).isEqualTo(1);
    }

    @Test
    void checkPincode_returnsNotServiceableForUnknownPincode() {
        when(serviceablePincodeRepository.findByPincodeAndStatus("110001", "ACTIVE"))
                .thenReturn(Optional.empty());

        var response = userSettingsService.checkPincode("110001");

        assertThat(response.isServiceable()).isFalse();
        assertThat(response.getPincode()).isEqualTo("110001");
    }
}

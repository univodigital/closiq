package com.closiq.common.identifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessIdentifierGeneratorsTest {

    @Mock
    private BusinessSequenceRepository sequences;

    private ProductCodeGenerator productCodeGenerator;
    private UserCodeGenerator userCodeGenerator;
    private OrderNumberGenerator orderNumberGenerator;
    private RentalNumberGenerator rentalNumberGenerator;

    @BeforeEach
    void setUp() {
        productCodeGenerator = new ProductCodeGenerator(sequences);
        userCodeGenerator = new UserCodeGenerator(sequences);
        orderNumberGenerator = new OrderNumberGenerator(sequences);
        rentalNumberGenerator = new RentalNumberGenerator(sequences);
    }

    @Test
    void productCodeGenerator_formatsVstProdCode() {
        when(sequences.nextProductCodeSequence()).thenReturn(100042L);

        assertThat(productCodeGenerator.nextCode()).isEqualTo("VST-PROD-100042");
    }

    @Test
    void userCodeGenerator_formatsVstUsrCode() {
        when(sequences.nextUserCodeSequence()).thenReturn(100099L);

        assertThat(userCodeGenerator.nextCode()).isEqualTo("VST-USR-100099");
    }

    @Test
    void orderNumberGenerator_includesUtcDateAndSequence() {
        when(sequences.nextOrderNumberSequence()).thenReturn(42L);
        String expectedDate = LocalDate.now(ZoneOffset.UTC).toString().replace("-", "");

        assertThat(orderNumberGenerator.nextCode()).isEqualTo("VST-ORD-" + expectedDate + "-0042");
    }

    @Test
    void rentalNumberGenerator_includesUtcDateAndSequence() {
        when(sequences.nextRentalNumberSequence()).thenReturn(482L);
        String expectedDate = LocalDate.now(ZoneOffset.UTC).toString().replace("-", "");

        assertThat(rentalNumberGenerator.nextCode()).isEqualTo("VST-RNT-" + expectedDate + "-0482");
    }
}

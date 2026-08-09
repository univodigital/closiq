package com.closiq.identity.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.closiq.common.exception.GlobalExceptionHandler;
import com.closiq.common.security.JwtService;
import com.closiq.identity.service.AuthService;
import com.closiq.identity.web.dto.OtpInitiateResponse;
import com.closiq.identity.web.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private com.closiq.identity.service.RefreshTokenService refreshTokenService;

    @MockBean
    private JwtService jwtService;

    @Test
    void register_returnsOtpSession() throws Exception {
        when(authService.register(any())).thenReturn(OtpInitiateResponse.builder()
                .otpSessionId("550e8400-e29b-41d4-a716-446655440000")
                .phone("+919876543210")
                .expiresInSeconds(300)
                .resendAvailableInSeconds(60)
                .isExistingUser(false)
                .build());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("+919876543210", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.otpSessionId").exists())
                .andExpect(jsonPath("$.data.phone").value("+919876543210"));
    }

    @Test
    void register_rejectsMissingTermsAcceptance() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("+919876543210", false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}

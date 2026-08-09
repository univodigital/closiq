package com.closiq.identity.service;

import com.closiq.common.security.JwtService;
import com.closiq.common.security.UserPrincipal;
import com.closiq.common.util.HashUtils;
import com.closiq.common.util.IdGenerator;
import com.closiq.config.ClosiqProperties;
import com.closiq.identity.domain.RefreshToken;
import com.closiq.identity.domain.RefreshTokenStatus;
import com.closiq.identity.domain.User;
import com.closiq.identity.repository.RefreshTokenRepository;
import com.closiq.identity.web.dto.RefreshTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    public static final String REFRESH_COOKIE_NAME = "refreshToken";
    public static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    private final RefreshTokenRepository refreshTokenRepository;
    private final HashUtils hashUtils;
    private final JwtService jwtService;
    private final UserService userService;
    private final ClosiqProperties properties;

    public record IssuedRefreshToken(String rawToken, RefreshToken entity) {
    }

    @Transactional
    public IssuedRefreshToken issue(User user, String ipAddress, String userAgent) {
        String rawToken = hashUtils.generateSecureToken();
        UUID familyId = IdGenerator.uuidV7();
        Instant expiresAt = Instant.now().plusSeconds(
                properties.getJwt().getRefreshTokenExpirationDays() * 24L * 3600L);

        RefreshToken refreshToken = RefreshToken.builder()
                .id(IdGenerator.uuidV7())
                .user(user)
                .tokenHash(hashUtils.hashToken(rawToken))
                .familyId(familyId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .status(RefreshTokenStatus.ACTIVE)
                .expiresAt(expiresAt)
                .build();

        refreshTokenRepository.save(refreshToken);
        return new IssuedRefreshToken(rawToken, refreshToken);
    }

    @Transactional
    public IssuedRefreshToken rotate(String rawToken, String ipAddress, String userAgent) {
        String tokenHash = hashUtils.hashToken(rawToken);
        RefreshToken existing = refreshTokenRepository
                .findByTokenHashAndStatus(tokenHash, RefreshTokenStatus.ACTIVE)
                .orElseThrow(() -> new com.closiq.common.exception.ClosiqException(
                        com.closiq.common.exception.ErrorCode.UNAUTHORIZED, "Invalid refresh token"));

        if (existing.getExpiresAt().isBefore(Instant.now())) {
            existing.setStatus(RefreshTokenStatus.EXPIRED);
            refreshTokenRepository.save(existing);
            throw new com.closiq.common.exception.ClosiqException(
                    com.closiq.common.exception.ErrorCode.TOKEN_EXPIRED, "Refresh token expired");
        }

        Instant now = Instant.now();
        existing.setStatus(RefreshTokenStatus.REVOKED);
        existing.setRevokedAt(now);
        refreshTokenRepository.save(existing);

        String newRawToken = hashUtils.generateSecureToken();
        Instant expiresAt = Instant.now().plusSeconds(
                properties.getJwt().getRefreshTokenExpirationDays() * 24L * 3600L);

        RefreshToken rotated = RefreshToken.builder()
                .id(IdGenerator.uuidV7())
                .user(existing.getUser())
                .tokenHash(hashUtils.hashToken(newRawToken))
                .familyId(existing.getFamilyId())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .status(RefreshTokenStatus.ACTIVE)
                .expiresAt(expiresAt)
                .build();

        refreshTokenRepository.save(rotated);
        return new IssuedRefreshToken(newRawToken, rotated);
    }

    @Transactional
    public AuthSessionResult.RefreshPair refreshAccessToken(String rawToken, String ipAddress, String userAgent) {
        IssuedRefreshToken rotated = rotate(rawToken, ipAddress, userAgent);
        User user = rotated.entity().getUser();
        UserPrincipal principal = userService.buildPrincipal(user);
        String accessToken = jwtService.generateAccessToken(principal);

        RefreshTokenResponse response = RefreshTokenResponse.builder()
                .accessToken(accessToken)
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .tokenType("Bearer")
                .build();

        return AuthSessionResult.RefreshPair.builder()
                .auth(response)
                .rawRefreshToken(rotated.rawToken())
                .build();
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        String tokenHash = hashUtils.hashToken(rawToken);
        refreshTokenRepository.findByTokenHashAndStatus(tokenHash, RefreshTokenStatus.ACTIVE)
                .ifPresent(token -> {
                    token.setStatus(RefreshTokenStatus.REVOKED);
                    token.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(token);
                });
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.revokeAllActiveForUser(
                userId,
                RefreshTokenStatus.ACTIVE,
                RefreshTokenStatus.REVOKED,
                Instant.now());
    }

    public int getRefreshCookieMaxAgeSeconds() {
        return properties.getJwt().getRefreshTokenExpirationDays() * 24 * 3600;
    }
}

package com.closiq.common.security;

import com.closiq.config.ClosiqProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final ClosiqProperties properties;

    public String generateAccessToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(properties.getJwt().getAccessTokenExpirationMinutes() * 60L);

        var builder = Jwts.builder()
                .subject(principal.userId().toString())
                .claim("roles", principal.roles().stream().map(Enum::name).toList())
                .claim("phoneVerified", principal.phoneVerified())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry));

        if (principal.sellerId() != null) {
            builder.claim("sellerId", principal.sellerId().toString());
        }

        return builder.signWith(signingKey()).compact();
    }

    public long getAccessTokenExpirationSeconds() {
        return properties.getJwt().getAccessTokenExpirationMinutes() * 60L;
    }

    public UserPrincipal parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            @SuppressWarnings("unchecked")
            List<String> roleNames = claims.get("roles", List.class);
            List<RoleType> roles = roleNames == null
                    ? List.of()
                    : roleNames.stream().map(RoleType::valueOf).toList();
            Boolean phoneVerified = claims.get("phoneVerified", Boolean.class);
            String sellerIdStr = claims.get("sellerId", String.class);
            UUID sellerId = sellerIdStr != null ? UUID.fromString(sellerIdStr) : null;

            return new UserPrincipal(userId, roles, Boolean.TRUE.equals(phoneVerified), sellerId);
        } catch (ExpiredJwtException ex) {
            throw new TokenExpiredException("Access token expired");
        } catch (Exception ex) {
            throw new InvalidTokenException("Invalid access token");
        }
    }

    private SecretKey signingKey() {
        byte[] keyBytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

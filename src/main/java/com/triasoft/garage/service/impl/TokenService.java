package com.triasoft.garage.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.function.Function;

@Service
public class TokenService {

    private static final long EXPIRATION_TIME_MS = 1000 * 60 * 60 * 24;

    private final SecretKey secretKey;

    @Value("${app.jwt.refresh-token.expiration-ms}")
    private long refreshExpiration;

    @Value("${app.jwt.expiration-ms}")
    private long tokenExpiration;

    public TokenService(@Value("${app.jwt.secret}") String secretString) {
        this.secretKey = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UserDetails userDetails, UserDTO userDTO) {
        String userJson = "";
        try {
            userJson = new ObjectMapper().writeValueAsString(userDTO);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.General.GENERAL_ERROR);
        }
        return Jwts.builder()
                .claim("user", userJson)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + tokenExpiration))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Hashes an opaque refresh token (SHA-256) so only the digest is persisted;
     * the raw token is never stored and cannot be replayed from a DB leak.
     */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCode.General.GENERAL_ERROR);
        }
    }

    public long getRefreshExpirationMs() {
        return refreshExpiration;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Validates the token against the UserDetails.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}

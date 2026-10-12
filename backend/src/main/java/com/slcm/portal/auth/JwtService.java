package com.slcm.portal.auth;

import com.slcm.portal.user.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key; private final long expirationMinutes;
    public JwtService(@Value("${slcm.jwt.secret}") String secret, @Value("${slcm.jwt.expiration-minutes:120}") long expirationMinutes) {
        if (secret == null || secret.isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < 32) throw new IllegalArgumentException("SLCM_JWT_SECRET must be at least 32 bytes");
        this.key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); this.expirationMinutes=expirationMinutes;
    }
    public String issue(User user) { Instant now=Instant.now(); return Jwts.builder().subject(user.getUsername()).claim("role", user.getRole().name()).issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expirationMinutes*60))).signWith(key).compact(); }
    public String username(String token) { return parse(token).getPayload().getSubject(); }
    public boolean valid(String token) { try { parse(token); return true; } catch (JwtException|IllegalArgumentException e) { return false; } }
    private Jws<Claims> parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token); }
}

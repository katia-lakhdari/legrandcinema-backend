package com.legrandcinema.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String cleSecrete;

    private SecretKey secretKey;

    private final long dureeValiditeMs = 3600000;

    @PostConstruct
    public void init() {
        secretKey = Keys.hmacShaKeyFor(cleSecrete.getBytes());
    }

    public String genererToken(String email, String role) {
        Date maintenant = new Date();
        Date expiration = new Date(maintenant.getTime() + dureeValiditeMs);

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(maintenant)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public String extraireEmail(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String extraireRole(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    public boolean tokenValide(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
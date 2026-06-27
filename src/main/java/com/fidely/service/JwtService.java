package com.fidely.service;

import com.fidely.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.email-expiration}")
    private long emailExpiration;

    public String getSubject(String token) {
        return getClaim(token, Claims::getSubject);
    }

    public String getRole(String token) {
        return getClaim(token, claims -> claims.get("role", String.class));
    }

    public String getType(String token) {
        return getClaim(token, claims -> claims.get("type", String.class));
    }

    private Date getExpiration(String token) {
        return getClaim(token, Claims::getExpiration);
    }

    private <T> T getClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(getAllClaims(token));
    }

    private Claims getAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getTokenSubject())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .claim("role", user.getRole())
                .claim("type", "ACCESS")
                .signWith(getSignInKey())
                .compact();
    }

    public String generateEmailToken(User user) {
        return Jwts.builder()
                .subject(user.getTokenSubject())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + emailExpiration))
                .claim("role", user.getRole())
                .claim("type", "EMAIL_VERIFICATION")
                .signWith(getSignInKey())
                .compact();
    }

    public boolean validateToken(String token, User user) {
        String subject = getSubject(token);
        return subject.equals(user.getTokenSubject()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return getExpiration(token).before(new Date());
    }
}
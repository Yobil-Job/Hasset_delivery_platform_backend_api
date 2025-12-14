package com.kuru.delivery.config;

import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration:1800000}")
    private long jwtExpiration;
    
    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;
    
    public Key getSigningKey() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters long for security");
        }
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
    
    public String generateAccessToken(long id,String email,String role){
        return Jwts.builder()
                .setSubject(email)
                .claim("id",id)
                .claim("role",role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+jwtExpiration))
                .signWith(getSigningKey(), io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();
        
    }
    
    public String generateRefreshToken(long id,String email,String role) {
        return Jwts.builder()
                .setSubject(email) 
                .claim("id", id) 
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+refreshExpiration))
                .signWith(getSigningKey(), io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();
        
    }
    
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }
    
    public String extractRole(String token) {
        return extractAllClaims(token).get("role",String.class);
    }

    public boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
    
    public boolean isTokenExpired(String token) {
        try {
            return extractAllClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public long getTokenExpirationSeconds(String token) {
        try {
            Date expiration = extractAllClaims(token).getExpiration();
            long expirationTime = expiration.getTime();
            long currentTime = System.currentTimeMillis();
            long remainingSeconds = (expirationTime - currentTime) / 1000;
            return Math.max(0, remainingSeconds);
        } catch (Exception e) {
            return 0;
        }
    }


    
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

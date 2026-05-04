package com.example.WorkHub.services;

import com.example.WorkHub.models.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import io.jsonwebtoken.security.Keys;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
@Setter
@ConfigurationProperties(prefix = "auth")
public class JwtService {
    private String secretKey;
    private Key getSigningKey(){
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user){
        return Jwts.builder().setSubject(user.getEmail()).claim("tenantId", user.getTenant().getId()).setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis()+1000*60*60*24))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256).compact();
    }

    public boolean isTokenExpired(String token){
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody().getExpiration().before(new Date());
    }

    public boolean isTokenValid(String token, UserDetails userDetails){
        return extractUsername(token).equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public String extractUsername(String token){
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody().getSubject();
    }

    public Long extractTenantId(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody().get("tenantId", Long.class);
    }
}

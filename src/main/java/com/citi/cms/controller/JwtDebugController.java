package com.citi.cms.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.citi.cms.security.JwtAuthenticationFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@RestController
@RequestMapping("/debug")
public class JwtDebugController {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter; // Assuming you have access to your JWT utilities
    
    /**
     * Debug endpoint to decode JWT token content
     * Usage: POST /debug/decode-jwt with JWT token in the body
     */
    @PostMapping("/decode-jwt")
    public ResponseEntity<Map<String, Object>> decodeJwt(@RequestBody String jwtToken) {
        try {
            // Remove "Bearer " prefix if present
            if (jwtToken.startsWith("Bearer ")) {
                jwtToken = jwtToken.substring(7);
            }
            
            // Decode JWT using your existing JWT utility
            // You'll need to replace this with your actual JWT utility method
            Claims claims = Jwts.parser()
                .setSigningKey("eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJoci5zcGVjaWFsaXN0IiwiaWF0IjoxNzQ4NDUzOTIzLCJleHAiOjE3NDg1NDAzMjMsInVzZXJJZCI6Miwicm9sZXMiOlt7ImF1dGhvcml0eSI6IlJPTEVfSFJfU1BFQ0lBTElTVCJ9XX0.ZltUaeVHIcThU6uZ1-5jJPm5CvRq3DzRyalK2P6s6vuXKwBbc-WaGv_W5wtw94Bv") // Replace with your actual secret key
                .parseClaimsJws(jwtToken)
                .getBody();
            
            Map<String, Object> tokenInfo = new HashMap<>();
            tokenInfo.put("subject", claims.getSubject()); // This should be the username
            tokenInfo.put("roles", claims.get("roles"));
            tokenInfo.put("authorities", claims.get("authorities"));
            tokenInfo.put("issued_at", claims.getIssuedAt());
            tokenInfo.put("expires_at", claims.getExpiration());
            tokenInfo.put("all_claims", claims);
            
            return ResponseEntity.ok(tokenInfo);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to decode JWT: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
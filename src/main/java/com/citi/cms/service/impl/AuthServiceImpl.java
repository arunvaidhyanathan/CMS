package com.citi.cms.service.impl;

import com.citi.cms.dto.request.LoginRequest;
import com.citi.cms.dto.response.LoginResponse;
import com.citi.cms.entity.User;
import com.citi.cms.repository.UserRepository;
import com.citi.cms.security.JwtTokenProvider;
import com.citi.cms.security.UserPrincipal;
import com.citi.cms.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Override
public LoginResponse authenticateUser(LoginRequest loginRequest) {
    // ADD DEBUG LOGS HERE
    logger.info("=== AUTHENTICATION DEBUG START ===");
    logger.info("Login request for username: {}", loginRequest.getUsername());
    logger.info("Password provided length: {}", loginRequest.getPassword() != null ? loginRequest.getPassword().length() : "null");
    
    try {
        // ADD DEBUG: Check if user exists in database first
        User userFromDb = userRepository.findByUsername(loginRequest.getUsername()).orElse(null);
        if (userFromDb == null) {
            logger.error("User not found in database: {}", loginRequest.getUsername());
            throw new RuntimeException("User not found");
        }
        
        logger.info("User found in database: {}", userFromDb.getUsername());
        logger.info("User ID: {}, Email: {}", userFromDb.getUserId(), userFromDb.getEmail());
        logger.info("Password hash from DB: {}", userFromDb.getPasswordHash());
        logger.info("User status: {}, Enabled: {}", userFromDb.getUserStatus(), userFromDb.isEnabled());
        
        // ADD DEBUG: Test password encoding manually
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        boolean passwordMatches = encoder.matches(loginRequest.getPassword(), userFromDb.getPasswordHash());
        logger.info("Manual password verification result: {}", passwordMatches);
        
        if (!passwordMatches) {
            logger.error("Password does not match!");
            logger.error("Provided password: '{}'", loginRequest.getPassword());
            logger.error("Expected hash: {}", userFromDb.getPasswordHash());
            
            // Test with known working hash
            String knownWorkingHash = "$2a$10$N.zmdr9k7uOsaLQJeuOISOXzDUz5vbMRoATWY4EABP/CL/8AUed0O";
            boolean testKnownHash = encoder.matches("demo123", knownWorkingHash);
            logger.info("Test known hash 'demo123' vs known working hash: {}", testKnownHash);
            
            throw new RuntimeException("Password verification failed");
        }
        
        logger.info("Manual password verification successful");
        
        // NOW TRY SPRING SECURITY AUTHENTICATION
        logger.info("Attempting Spring Security authentication...");
        
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(),
                loginRequest.getPassword()
            )
        );

        logger.info("Spring Security authentication successful");
        logger.info("Authentication object: {}", authentication);
        logger.info("Principal: {}", authentication.getPrincipal());
        logger.info("Authorities: {}", authentication.getAuthorities());

        // GENERATE JWT TOKEN
        String jwt = tokenProvider.generateToken(authentication);
        logger.info("JWT token generated successfully");
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        User user = userRepository.findByUsername(userPrincipal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setUserId(user.getUserId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setFirstName(user.getFirstName());
        userInfo.setLastName(user.getLastName());
        userInfo.setRoles(user.getRoles().stream()
                .map(role -> role.getRoleCode())
                .collect(Collectors.toSet()));

        logger.info("=== AUTHENTICATION DEBUG SUCCESS ===");
        
        return LoginResponse.builder()
                .success(true)
                .message("Authentication successful")
                .token(jwt)
                .user(userInfo)
                .build();

    } catch (AuthenticationException e) {
        logger.error("AuthenticationException: {}", e.getMessage());
        logger.error("Exception type: {}", e.getClass().getSimpleName());
        throw new RuntimeException("Invalid credentials");
    } catch (Exception e) {
        logger.error("Unexpected exception during authentication: {}", e.getMessage(), e);
        throw new RuntimeException("Authentication failed: " + e.getMessage());
    }
}

    @Override
    public boolean validateToken(String token) {
        return tokenProvider.validateToken(token);
    }

    @Override
    public void logout(String token) {
        // In a production system, you might want to blacklist the token
        // For now, we'll just log the logout
        logger.info("User logged out with token: {}", token.substring(0, 10) + "...");
    }
}
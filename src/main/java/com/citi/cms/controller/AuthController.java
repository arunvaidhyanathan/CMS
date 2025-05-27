package com.citi.cms.controller;

import com.citi.cms.dto.request.LoginRequest;
import com.citi.cms.dto.response.LoginResponse;
import com.citi.cms.entity.User;
import com.citi.cms.repository.UserRepository;
import com.citi.cms.service.AuthService;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("Login attempt for username: {}", loginRequest.getUsername());
        
        try {
            LoginResponse response = authService.authenticateUser(loginRequest);
            logger.info("Login successful for username: {}", loginRequest.getUsername());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Login failed for username: {}: {}", loginRequest.getUsername(), e.getMessage());
            return ResponseEntity.badRequest().body(
                LoginResponse.builder()
                    .success(false)
                    .message("Invalid username or password")
                    .build()
            );
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<Boolean> validateToken(@RequestHeader("Authorization") String token) {
        try {
            boolean isValid = authService.validateToken(token.replace("Bearer ", ""));
            return ResponseEntity.ok(isValid);
        } catch (Exception e) {
            return ResponseEntity.ok(false);
        }
    }

@RestController
@RequestMapping("/test")
@Profile("dev")
public class PasswordTestController {
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private UserRepository userRepository;
    
    @PostMapping("/password-verify")
    public ResponseEntity<Map<String, Object>> testPasswordVerification(@RequestParam String username, @RequestParam String password) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                result.put("error", "User not found");
                return ResponseEntity.ok(result);
            }
            
            result.put("username", username);
            result.put("providedPassword", password);
            result.put("storedHash", user.getPasswordHash());
            
            boolean matches = passwordEncoder.matches(password, user.getPasswordHash());
            result.put("passwordMatches", matches);
            
            // Test with known working hash
            String knownHash = "$2a$10$N.zmdr9k7uOsaLQJeuOISOXzDUz5vbMRoATWY4EABP/CL/8AUed0O";
            boolean knownMatches = passwordEncoder.matches("demo123", knownHash);
            result.put("knownHashTest", knownMatches);
            
            if (!matches) {
                String newHash = passwordEncoder.encode(password);
                result.put("newGeneratedHash", newHash);
                result.put("sqlUpdate", "UPDATE cms_workflow.users SET password_hash = '" + newHash + "' WHERE username = '" + username + "';");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
}
}

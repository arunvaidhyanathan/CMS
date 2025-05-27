package com.citi.cms.controller;

import com.citi.cms.entity.User;
import com.citi.cms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Development only controller for password management
 * Only active in 'dev' profile
 */
@RestController
@RequestMapping("/dev")
@Profile("dev")
@CrossOrigin(origins = "*")
public class DevPasswordController {

    private static final Logger logger = LoggerFactory.getLogger(DevPasswordController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Reset all user passwords to "demo123" for testing
     * WARNING: Development use only!
     */
    @PostMapping("/reset-passwords")
    public ResponseEntity<Map<String, Object>> resetAllPasswords() {
        logger.warn("DEVELOPMENT: Resetting all user passwords to 'demo123'");
        
        try {
            String newPassword = "demo123";
            String hashedPassword = passwordEncoder.encode(newPassword);
            
            List<User> users = userRepository.findAll();
            int updatedCount = 0;
            
            for (User user : users) {
                user.setPasswordHash(hashedPassword);
                userRepository.save(user);
                updatedCount++;
                logger.info("Updated password for user: {}", user.getUsername());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All passwords reset to 'demo123'");
            response.put("usersUpdated", updatedCount);
            response.put("newPassword", newPassword);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error resetting passwords: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to reset passwords: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Reset password for a specific user
     */
    @PostMapping("/reset-password/{username}")
    public ResponseEntity<Map<String, Object>> resetUserPassword(
            @PathVariable String username,
            @RequestParam(defaultValue = "demo123") String newPassword) {
        
        logger.warn("DEVELOPMENT: Resetting password for user: {}", username);
        
        try {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "User not found: " + username);
                return ResponseEntity.notFound().build();
            }
            
            String hashedPassword = passwordEncoder.encode(newPassword);
            user.setPasswordHash(hashedPassword);
            userRepository.save(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Password reset successfully");
            response.put("username", username);
            response.put("newPassword", newPassword);
            
            logger.info("Password reset successful for user: {}", username);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error resetting password for user {}: {}", username, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to reset password: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Generate BCrypt hash for any password
     */
    @PostMapping("/generate-hash")
    public ResponseEntity<Map<String, Object>> generatePasswordHash(@RequestParam String password) {
        try {
            String hash = passwordEncoder.encode(password);
            
            Map<String, Object> response = new HashMap<>();
            response.put("password", password);
            response.put("hash", hash);
            response.put("sqlUpdate", String.format(
                "UPDATE cms_workflow.users SET password_hash = '%s' WHERE username = 'your_username';", 
                hash));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to generate hash: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * List all users with their current status
     */
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> listUsers() {
        try {
            List<User> users = userRepository.findAll();
            List<Map<String, Object>> userList = users.stream().map(user -> {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("username", user.getUsername());
                userInfo.put("email", user.getEmail());
                userInfo.put("fullName", user.getFullName());
                userInfo.put("status", user.getUserStatus());
                userInfo.put("roles", user.getRoles().stream()
                    .map(role -> role.getRoleCode())
                    .toList());
                return userInfo;
            }).toList();
            
            return ResponseEntity.ok(userList);
            
        } catch (Exception e) {
            logger.error("Error listing users: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    @PostMapping("/encoding-debug")
public ResponseEntity<Map<String, Object>> debugEncoding(@RequestParam String password) {
    Map<String, Object> result = new HashMap<>();
    
    // Test different ways of handling the password
    result.put("originalPassword", password);
    result.put("passwordLength", password.length());
    result.put("passwordBytes", Arrays.toString(password.getBytes()));
    result.put("passwordBytesUTF8", Arrays.toString(password.getBytes(StandardCharsets.UTF_8)));
    
    // Test with explicit UTF-8
    String utf8Password = new String(password.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    result.put("utf8Password", utf8Password);
    result.put("utf8PasswordEquals", password.equals(utf8Password));
    
    // Test trimming
    String trimmedPassword = password.trim();
    result.put("trimmedPassword", trimmedPassword);
    result.put("trimmedEquals", password.equals(trimmedPassword));
    
    // Generate hash with different approaches
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    
    String hash1 = encoder.encode(password);
    String hash2 = encoder.encode(password.trim());
    String hash3 = encoder.encode(utf8Password);
    
    result.put("hash1", hash1);
    result.put("hash2", hash2);
    result.put("hash3", hash3);
    
    result.put("hash1Matches", encoder.matches(password, hash1));
    result.put("hash2Matches", encoder.matches(password, hash2));
    result.put("hash3Matches", encoder.matches(password, hash3));
    
    return ResponseEntity.ok(result);
}
}